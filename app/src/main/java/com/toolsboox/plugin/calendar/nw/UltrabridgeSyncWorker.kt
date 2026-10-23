package com.toolsboox.plugin.calendar.nw

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import com.toolsboox.ot.DateJsonAdapter
import com.toolsboox.ot.LocaleJsonAdapter
import com.toolsboox.ot.UUIDJsonAdapter
import com.toolsboox.plugin.calendar.da.v2.*
import com.toolsboox.plugin.calendar.ot.CalendarPdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * WorkManager-based periodic worker that renders calendar pages to PDF
 * and uploads them to a WebDAV server (Ultrabridge backup).
 *
 * Reads WebDAV credentials from EncryptedSharedPreferences:
 *   - ultrabridge_webdav_url
 *   - ultrabridge_webdav_user
 *   - ultrabridge_webdav_pass
 *
 * Tracks sync progress via ultrabridgeLastSyncMs in the main SharedPreferences.
 *
 * PDF naming convention:
 *   ToolsForBoox-Day-2026-05.pdf     (all days in a month, multi-page)
 *   ToolsForBoox-Week-2026-Q2.pdf    (all weeks in a quarter, multi-page)
 *   ToolsForBoox-Month-2026.pdf      (all months in a year, multi-page)
 *   ToolsForBoox-Quarter-2026.pdf    (all quarters in a year, multi-page)
 *   ToolsForBoox-Year-2026.pdf       (single year page)
 */
class UltrabridgeSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "UltrabridgeSyncWorker"
        const val WORK_NAME = "ultrabridge-sync"
        private const val ONE_SHOT_WORK_NAME = "ultrabridge-sync-now"
        private const val ENCRYPTED_PREFS_NAME = "ultrabridge_encrypted_prefs"
        private const val MAIN_PREFS_NAME = "MAIN"
        private const val PREF_LAST_SYNC_MS = "ultrabridgeLastSyncMs"

        fun syncNow(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<UltrabridgeSyncWorker>()
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .build()
            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork(ONE_SHOT_WORK_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request)
        }

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
    }

    /**
     * Get or create EncryptedSharedPreferences for Ultrabridge credentials.
     */
    private fun getEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            applicationContext,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun doWork(): Result {
        Timber.i("$TAG: Starting Ultrabridge PDF sync")

        try {
            val mainPrefs = applicationContext.getSharedPreferences(MAIN_PREFS_NAME, Context.MODE_PRIVATE)
            val encryptedPrefs = getEncryptedPrefs()

            // Read WebDAV credentials
            val webdavUrl = encryptedPrefs.getString("ultrabridge_webdav_url", null)
            val webdavUser = encryptedPrefs.getString("ultrabridge_webdav_user", null)
            val webdavPass = encryptedPrefs.getString("ultrabridge_webdav_pass", null)

            if (webdavUrl.isNullOrBlank() || webdavUser.isNullOrBlank() || webdavPass.isNullOrBlank()) {
                Timber.w("$TAG: WebDAV credentials not configured, skipping sync")
                return Result.success()
            }

            val lastSyncMs = mainPrefs.getLong(PREF_LAST_SYNC_MS, 0L)

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

            val allFiles = mutableListOf<File>()
            Files.walk(Paths.get(calendarDir.toURI())).use { stream ->
                stream.map(Path::toFile)
                    .filter(File::isFile)
                    .filter { it.name.endsWith(".json") }
                    .filter { !it.name.startsWith("pattern-") }
                    .forEach { allFiles.add(it) }
            }

            if (allFiles.isEmpty()) {
                Timber.i("$TAG: No calendar files found")
                return Result.success()
            }

            Timber.i("$TAG: Found ${allFiles.size} calendar files to process")

            val groupedFiles = groupFilesByTypeAndPeriod(allFiles, calendarDir)

            // Set up services
            val moshi = buildMoshi()
            val webdavService = UltrabridgeWebDavService(webdavUrl, webdavUser, webdavPass)

            // Create the remote base directory
            webdavService.ensureDirectory("ToolsForBoox")

            var uploadCount = 0
            // Count uploads that were attempted but did not succeed. A single swallowed
            // failure used to still return Result.success(), so WorkManager never retried
            // and the on-exit push silently died on one flaky request — leaving the cloud
            // (and the downstream OCR pipeline) stale until the next periodic window, which
            // Boox battery management often suppresses for days. We now retry instead.
            var failureCount = 0
            val tempDir = File(applicationContext.cacheDir, "ultrabridge-pdf")
            tempDir.mkdirs()

            try {
                for ((groupKey, files) in groupedFiles) {
                    try {
                        val pdfFile = renderGroupToPdf(groupKey, files, moshi, tempDir, calendarDir)
                            ?: continue

                        val remotePath = "ToolsForBoox/${pdfFile.name}"
                        val uploaded = withContext(Dispatchers.IO) {
                            webdavService.upload(pdfFile, remotePath)
                        }

                        if (uploaded) {
                            uploadCount++
                        } else {
                            failureCount++
                            Timber.w("$TAG: Failed to upload ${pdfFile.name}")
                        }

                        // Clean up temp PDF
                        pdfFile.delete()
                    } catch (e: Exception) {
                        failureCount++
                        Timber.e(e, "$TAG: Error processing group $groupKey")
                    }
                }

                // Upload raw day JSON files to WebDAV for downstream processing
                val dayJsonFiles = allFiles.filter { it.name.startsWith("day-") }
                var jsonUploadCount = 0
                webdavService.ensureDirectory("ToolsForBoox/json")
                for (file in dayJsonFiles) {
                    try {
                        val remotePath = "ToolsForBoox/json/${file.name}"
                        val uploaded = withContext(Dispatchers.IO) {
                            webdavService.upload(file, remotePath)
                        }
                        if (uploaded) jsonUploadCount++ else failureCount++
                    } catch (e: Exception) {
                        failureCount++
                        Timber.e(e, "$TAG: JSON upload error for ${file.name}")
                    }
                }
                Timber.i("$TAG: Uploaded $jsonUploadCount of ${dayJsonFiles.size} day JSON files")

                // If anything failed, ask WorkManager to retry with backoff rather than
                // reporting a clean success. The worker re-uploads everything each run
                // (self-healing), so a retry simply re-attempts the failed files.
                if (failureCount > 0) {
                    Timber.w("$TAG: $failureCount upload(s) failed; requesting retry")
                    return Result.retry()
                }

                // Update sync cursor (commit, not apply: the worker may be killed
                // immediately after returning, before an async apply() flushes).
                mainPrefs.edit().putLong(PREF_LAST_SYNC_MS, System.currentTimeMillis()).commit()
                Timber.i("$TAG: Sync complete. Uploaded $uploadCount PDFs.")
                return Result.success()

            } finally {
                // Clean up temp directory
                tempDir.listFiles()?.forEach { it.delete() }
            }

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
     * Grouping key that identifies a PDF file to produce.
     */
    private data class GroupKey(
        val pageType: String,  // "day", "week", "month", "quarter", "year"
        val period: String     // e.g. "2026-05" for days, "2026-Q2" for weeks, "2026" for months
    )

    /**
     * Group calendar JSON files by page type and time period for PDF batching.
     *
     * Grouping strategy:
     *   - day files    -> grouped by year-month   (ToolsForBoox-Day-2026-05.pdf)
     *   - week files   -> grouped by year-quarter (ToolsForBoox-Week-2026-Q2.pdf)
     *   - month files  -> grouped by year         (ToolsForBoox-Month-2026.pdf)
     *   - quarter files -> grouped by year        (ToolsForBoox-Quarter-2026.pdf)
     *   - year files   -> one per year            (ToolsForBoox-Year-2026.pdf)
     */
    private fun groupFilesByTypeAndPeriod(
        files: List<File>,
        calendarDir: File
    ): Map<GroupKey, List<File>> {
        val groups = mutableMapOf<GroupKey, MutableList<File>>()

        for (file in files) {
            val baseName = file.name.removeSuffix("-v2.json").removeSuffix(".json")
            val groupKey = inferGroupKey(baseName, file, calendarDir) ?: continue
            groups.getOrPut(groupKey) { mutableListOf() }.add(file)
        }

        return groups
    }

    /**
     * Infer the GroupKey from a calendar file's base name.
     */
    private fun inferGroupKey(baseName: String, file: File, calendarDir: File): GroupKey? {
        return when {
            // day-YYYY-MM-DD -> group by YYYY-MM
            baseName.startsWith("day-") -> {
                val parts = baseName.removePrefix("day-").split("-")
                if (parts.size >= 2) {
                    GroupKey("Day", "${parts[0]}-${parts[1]}")
                } else null
            }

            // week-YYYY-WNN -> group by YYYY-Q?
            baseName.startsWith("week-") -> {
                val parts = baseName.removePrefix("week-").split("-")
                if (parts.size >= 2) {
                    val year = parts[0]
                    val weekStr = parts[1].removePrefix("W").removePrefix("w")
                    val weekNum = weekStr.toIntOrNull() ?: return null
                    val quarter = ((weekNum - 1) / 13) + 1
                    GroupKey("Week", "$year-Q$quarter")
                } else null
            }

            // month-YYYY-MM -> group by YYYY
            baseName.startsWith("month-") -> {
                val parts = baseName.removePrefix("month-").split("-")
                if (parts.isNotEmpty()) {
                    GroupKey("Month", parts[0])
                } else null
            }

            // quarter-YYYY-QN -> group by YYYY
            baseName.startsWith("quarter-") -> {
                val parts = baseName.removePrefix("quarter-").split("-")
                if (parts.isNotEmpty()) {
                    GroupKey("Quarter", parts[0])
                } else null
            }

            // year-YYYY -> group by YYYY
            baseName.startsWith("year-") -> {
                val year = baseName.removePrefix("year-").split("-").firstOrNull()
                if (year != null) {
                    GroupKey("Year", year)
                } else null
            }

            else -> null
        }
    }

    /**
     * Render a group of calendar files to a single multi-page PDF.
     * Returns the temp PDF file, or null if no pages could be loaded.
     */
    private suspend fun renderGroupToPdf(
        groupKey: GroupKey,
        files: List<File>,
        moshi: Moshi,
        tempDir: File,
        calendarDir: File
    ): File? = withContext(Dispatchers.Default) {
        // For a modified group, load ALL files in that group's scope (not just modified ones)
        // so the PDF always contains the complete set.
        val allFilesInGroup = findAllFilesForGroup(groupKey, calendarDir)

        val pages = mutableListOf<Pair<String, Pair<Map<String, List<com.toolsboox.da.Stroke>>, Map<String, List<com.toolsboox.da.Stroke>>>>>()

        // Sort files for consistent page ordering
        val sortedFiles = allFilesInGroup.sortedBy { it.name }

        for (file in sortedFiles) {
            val calendarData = loadCalendarData(file, moshi) ?: continue
            pages.add(file.name to calendarData)
        }

        if (pages.isEmpty()) return@withContext null

        val pdfFileName = "ToolsForBoox-${groupKey.pageType}-${groupKey.period}.pdf"
        val pdfFile = File(tempDir, pdfFileName)

        CalendarPdfRenderer.renderMonthToPdf(pages, pdfFile)

        Timber.i("$TAG: Rendered ${pages.size} pages to $pdfFileName")
        pdfFile
    }

    /**
     * Find ALL calendar files belonging to a group, not just the modified ones.
     * This ensures the PDF always contains the complete set for its scope.
     */
    private fun findAllFilesForGroup(groupKey: GroupKey, calendarDir: File): List<File> {
        val allFiles = mutableListOf<File>()

        Files.walk(Paths.get(calendarDir.toURI())).use { stream ->
            stream.map(Path::toFile)
                .filter(File::isFile)
                .filter { it.name.endsWith(".json") }
                .filter { !it.name.startsWith("pattern-") }
                .forEach { file ->
                    val baseName = file.name.removeSuffix("-v2.json").removeSuffix(".json")
                    val fileGroupKey = inferGroupKey(baseName, file, calendarDir)
                    if (fileGroupKey == groupKey) {
                        allFiles.add(file)
                    }
                }
        }

        // Prefer v2 files over v1 when both exist
        val byBaseName = allFiles.groupBy { it.name.removeSuffix("-v2.json").removeSuffix(".json") }
        return byBaseName.map { (_, versions) ->
            versions.firstOrNull { it.name.contains("-v2.json") } ?: versions.first()
        }
    }

    /**
     * Load a calendar JSON file and extract its stroke maps.
     * Handles all calendar types (day, week, month, quarter, year) in both v1 and v2 formats.
     */
    private fun loadCalendarData(
        file: File,
        moshi: Moshi
    ): Pair<Map<String, List<com.toolsboox.da.Stroke>>, Map<String, List<com.toolsboox.da.Stroke>>>? {
        try {
            val json = file.readText(Charsets.UTF_8)
            val name = file.name
            val isV2 = name.endsWith("-v2.json")

            return when {
                name.startsWith("day-") -> {
                    if (isV2) {
                        moshi.adapter(CalendarDay::class.java).fromJson(json)?.let {
                            it.calendarStrokes to it.noteStrokes
                        }
                    } else {
                        moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarDay::class.java)
                            .fromJson(json)?.let { CalendarDay.convert(it) }?.let {
                                it.calendarStrokes to it.noteStrokes
                            }
                    }
                }

                name.startsWith("week-") -> {
                    if (isV2) {
                        moshi.adapter(CalendarWeek::class.java).fromJson(json)?.let {
                            it.calendarStrokes to it.noteStrokes
                        }
                    } else {
                        moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarWeek::class.java)
                            .fromJson(json)?.let { CalendarWeek.convert(it) }?.let {
                                it.calendarStrokes to it.noteStrokes
                            }
                    }
                }

                name.startsWith("month-") -> {
                    if (isV2) {
                        moshi.adapter(CalendarMonth::class.java).fromJson(json)?.let {
                            it.calendarStrokes to it.noteStrokes
                        }
                    } else {
                        moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarMonth::class.java)
                            .fromJson(json)?.let { CalendarMonth.convert(it) }?.let {
                                it.calendarStrokes to it.noteStrokes
                            }
                    }
                }

                name.startsWith("quarter-") -> {
                    if (isV2) {
                        moshi.adapter(CalendarQuarter::class.java).fromJson(json)?.let {
                            it.calendarStrokes to it.noteStrokes
                        }
                    } else {
                        moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarQuarter::class.java)
                            .fromJson(json)?.let { CalendarQuarter.convert(it) }?.let {
                                it.calendarStrokes to it.noteStrokes
                            }
                    }
                }

                name.startsWith("year-") -> {
                    if (isV2) {
                        moshi.adapter(CalendarYear::class.java).fromJson(json)?.let {
                            it.calendarStrokes to it.noteStrokes
                        }
                    } else {
                        moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarYear::class.java)
                            .fromJson(json)?.let { CalendarYear.convert(it) }?.let {
                                it.calendarStrokes to it.noteStrokes
                            }
                    }
                }

                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error loading calendar data from ${file.name}")
            return null
        }
    }
}
