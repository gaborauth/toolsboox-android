package com.toolsboox.plugin.calendar.nw

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.toolsboox.ProductConstants
import com.toolsboox.ot.CryptoUtils
import com.toolsboox.ot.DateJsonAdapter
import com.toolsboox.ot.LocaleJsonAdapter
import com.toolsboox.ot.UUIDJsonAdapter
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncItem
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager-based periodic cloud auto-sync worker for calendar data.
 *
 * This worker constructs all dependencies internally (OkHttp, Retrofit, Moshi)
 * because the app's DI graph is scoped to ActivityComponent, which is not
 * available to WorkManager workers.
 */
class CalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "CalendarSyncWorker"
        private const val ENCRYPTED_PREFS_NAME = "calendar_sync_encrypted_prefs"
        private const val MAIN_PREFS_NAME = "MAIN"

        /**
         * Build a Moshi instance matching the app's NetworkModule.provideMoshi().
         */
        private fun buildMoshi(): Moshi {
            return Moshi.Builder()
                .add(LocaleJsonAdapter())
                .add(DateJsonAdapter())
                .add(UUIDJsonAdapter())
                .build()
        }

        /**
         * Build an OkHttpClient with bearer token authentication.
         */
        private fun buildOkHttpClient(accessToken: String?): OkHttpClient {
            val bearerInterceptor = Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                accessToken?.let {
                    requestBuilder.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(requestBuilder.build())
            }

            return OkHttpClient.Builder()
                .addInterceptor(bearerInterceptor)
                .connectTimeout(30_000, TimeUnit.MILLISECONDS)
                .writeTimeout(30_000, TimeUnit.MILLISECONDS)
                .readTimeout(30_000, TimeUnit.MILLISECONDS)
                .build()
        }

        /**
         * Build a CalendarService via Retrofit.
         */
        private fun buildCalendarService(okHttpClient: OkHttpClient): CalendarService {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .baseUrl(ProductConstants.SERVICE_BASE_URL)
                .client(okHttpClient)
                .build()

            return retrofit.create(CalendarService::class.java)
        }
    }

    /**
     * Get or create EncryptedSharedPreferences and migrate the plaintext
     * passphrase from MAIN prefs on first use.
     */
    private fun getEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            applicationContext,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Migrate plaintext passphrase from MAIN prefs if present and not yet migrated
        val mainPrefs = applicationContext.getSharedPreferences(MAIN_PREFS_NAME, Context.MODE_PRIVATE)
        val plaintextPassphrase = mainPrefs.getString("calendarItemPassphrase", null)
        if (plaintextPassphrase != null && encryptedPrefs.getString("calendarItemPassphrase", null) == null) {
            encryptedPrefs.edit().putString("calendarItemPassphrase", plaintextPassphrase).apply()
            mainPrefs.edit().remove("calendarItemPassphrase").apply()
            Timber.i("$TAG: Migrated plaintext passphrase to EncryptedSharedPreferences")
        }

        return encryptedPrefs
    }

    override suspend fun doWork(): Result {
        Timber.i("$TAG: Starting periodic calendar cloud sync")

        try {
            val mainPrefs = applicationContext.getSharedPreferences(MAIN_PREFS_NAME, Context.MODE_PRIVATE)
            val accessToken = mainPrefs.getString("accessToken", null)
            if (accessToken.isNullOrBlank()) {
                Timber.w("$TAG: No access token found, skipping sync")
                return Result.success()
            }

            val userId = mainPrefs.getString("userId", null)
            if (userId.isNullOrBlank()) {
                Timber.w("$TAG: No userId found, skipping sync")
                return Result.success()
            }

            val encryptedPrefs = getEncryptedPrefs()
            val passphrase = encryptedPrefs.getString("calendarItemPassphrase", null)
            if (passphrase.isNullOrBlank()) {
                Timber.w("$TAG: No passphrase configured, skipping sync")
                return Result.success()
            }

            val okHttpClient = buildOkHttpClient(accessToken)
            val calendarService = buildCalendarService(okHttpClient)

            // List cloud items
            val cloudResponse = calendarService.listAsync().await()
            if (!cloudResponse.isSuccessful) {
                Timber.w("$TAG: Cloud list failed with code ${cloudResponse.code()}")
                return if (cloudResponse.code() in 500..599) Result.retry() else Result.failure()
            }
            val cloudItems = cloudResponse.body() ?: emptyList()

            // Build a lookup map for cloud items: "path|baseName|version" -> CalendarSyncItem
            val cloudMap = cloudItems.associateBy { "${it.path}|${it.baseName}|${it.version}" }

            val rootDir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            }
            val calendarDir = File(rootDir, "calendar/")
            if (!calendarDir.exists()) {
                Timber.i("$TAG: No local calendar directory, nothing to sync")
                return Result.success()
            }

            val localFiles = mutableListOf<File>()
            Files.walk(Paths.get(calendarDir.toURI())).use { stream ->
                stream.map(Path::toFile)
                    .filter(File::isFile)
                    .filter { it.name.endsWith(".json") }
                    .filter { !it.name.startsWith("pattern-") }
                    .forEach { localFiles.add(it) }
            }

            var uploadCount = 0
            val userUUID = UUID.fromString(userId)

            for (file in localFiles) {
                try {
                    val syncItem = inferSyncItem(file, calendarDir, userUUID) ?: continue

                    // Check if we need to upload (local newer than cloud)
                    val key = "${syncItem.path}|${syncItem.baseName}|${syncItem.version}"
                    val cloudItem = cloudMap[key]

                    val shouldUpload = when {
                        syncItem.updated == null -> false  // No local timestamp, skip
                        cloudItem == null -> true          // Not in cloud yet
                        cloudItem.updated == null -> true  // Cloud has no timestamp
                        cloudItem.updated < syncItem.updated -> true  // Local is newer
                        else -> false
                    }

                    if (!shouldUpload) continue

                    // Read, encrypt, and upload
                    val json = file.readText(Charsets.UTF_8)
                    val encrypted = CryptoUtils.encrypt(json.toByteArray(Charsets.UTF_8), passphrase)
                    val encryptedBase64 = Base64.getEncoder().encodeToString(encrypted)

                    val path = syncItem.path.replace("/", "%2F")
                    val baseName = syncItem.baseName.replace("/", "%2F")
                    val updateResponse = calendarService.updateAsync(
                        path, baseName, syncItem.version, encryptedBase64
                    ).await()

                    if (updateResponse.isSuccessful) {
                        uploadCount++
                        Timber.i("$TAG: Uploaded ${syncItem.path}${syncItem.baseName}")
                    } else {
                        Timber.w("$TAG: Upload failed for ${syncItem.path}${syncItem.baseName}: ${updateResponse.code()}")
                        if (updateResponse.code() in 500..599) {
                            return Result.retry()
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Error processing file ${file.name}")
                }
            }

            Timber.i("$TAG: Sync complete. Uploaded $uploadCount items.")
            return Result.success()

        } catch (e: java.net.UnknownHostException) {
            Timber.w(e, "$TAG: Network unavailable, will retry")
            return Result.retry()
        } catch (e: java.net.SocketTimeoutException) {
            Timber.w(e, "$TAG: Network timeout, will retry")
            return Result.retry()
        } catch (e: java.io.IOException) {
            Timber.w(e, "$TAG: IO error, will retry")
            return Result.retry()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Unexpected error during sync")
            return Result.failure()
        }
    }

    /**
     * Infer a CalendarSyncItem from a local JSON file by parsing its filename
     * and relative path within the calendar directory.
     *
     * Filename patterns:
     *   year-YYYY-v2.json, year-YYYY.json
     *   quarter-YYYY-QN-v2.json
     *   month-YYYY-MM-v2.json
     *   week-YYYY-WNN-v2.json
     *   day-YYYY-MM-DD-v2.json
     */
    private fun inferSyncItem(file: File, calendarDir: File, userId: UUID): CalendarSyncItem? {
        val name = file.name
        val relativePath = file.parentFile?.let {
            val rel = calendarDir.toPath().relativize(it.toPath()).toString()
            if (rel.isEmpty()) "" else "$rel/"
        } ?: ""

        // Determine version and baseName from filename
        val version: String
        val baseName: String

        when {
            name.endsWith("-v2.json") -> {
                version = "v2"
                baseName = name.removeSuffix("-v2.json")
            }
            name.endsWith(".json") -> {
                version = "v1"
                baseName = name.removeSuffix(".json")
            }
            else -> return null
        }

        // Validate it's a recognized calendar type
        val isCalendarFile = baseName.startsWith("year-") ||
                baseName.startsWith("quarter-") ||
                baseName.startsWith("month-") ||
                baseName.startsWith("week-") ||
                baseName.startsWith("day-")

        if (!isCalendarFile) return null

        // Read file modification time as updated timestamp
        val lastModified = Date(file.lastModified())

        return CalendarSyncItem(
            userId = userId,
            path = relativePath,
            baseName = baseName,
            version = version,
            created = lastModified,
            updated = lastModified
        )
    }
}
