package com.toolsboox.plugin.calendar.nw

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * WebDAV client for uploading PDF backups to an Ultrabridge-compatible server.
 *
 * Uses OkHttp HTTP PUT with Basic auth to write files. The server is expected
 * to accept standard WebDAV PUT requests (e.g. Nextcloud, ownCloud, any
 * WebDAV-capable endpoint).
 *
 * @param baseUrl the WebDAV base URL (e.g. "https://cloud.example.com/remote.php/dav/files/user")
 * @param username the WebDAV username
 * @param password the WebDAV password
 */
class UltrabridgeWebDavService(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    companion object {
        private const val TAG = "UltrabridgeWebDav"
        private val PDF_MEDIA_TYPE = "application/pdf".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30_000, TimeUnit.MILLISECONDS)
        .writeTimeout(60_000, TimeUnit.MILLISECONDS)
        .readTimeout(30_000, TimeUnit.MILLISECONDS)
        .build()

    /**
     * Upload a file to the WebDAV server at the given remote path.
     *
     * @param file the local file to upload
     * @param remotePath the path relative to [baseUrl] (e.g. "ToolsForBoox/Day-2026-05.pdf")
     * @return true if the upload succeeded (HTTP 2xx), false otherwise
     */
    fun upload(file: File, remotePath: String): Boolean {
        val normalizedBase = baseUrl.trimEnd('/')
        val normalizedPath = remotePath.trimStart('/')
        val url = "$normalizedBase/$normalizedPath"

        val credential = Credentials.basic(username, password)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", credential)
            .header("Overwrite", "T")
            .put(file.asRequestBody(PDF_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Timber.i("$TAG: Uploaded $remotePath (${response.code})")
                    true
                } else {
                    Timber.w("$TAG: Upload failed for $remotePath: ${response.code} ${response.message}")
                    false
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "$TAG: Network error uploading $remotePath")
            false
        }
    }

    /**
     * Ensure a remote directory exists by issuing a MKCOL request.
     * Ignores 405 (already exists) and 301 (redirect, already exists on some servers).
     *
     * @param remoteDirPath the directory path relative to [baseUrl]
     * @return true if the directory exists or was created
     */
    fun ensureDirectory(remoteDirPath: String): Boolean {
        val normalizedBase = baseUrl.trimEnd('/')
        val normalizedPath = remoteDirPath.trimEnd('/') + "/"
        val url = "$normalizedBase/$normalizedPath"

        val credential = Credentials.basic(username, password)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", credential)
            .method("MKCOL", null)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val ok = response.isSuccessful || response.code == 405 || response.code == 301
                if (!ok) {
                    Timber.w("$TAG: MKCOL failed for $remoteDirPath: ${response.code}")
                }
                ok
            }
        } catch (e: IOException) {
            Timber.e(e, "$TAG: Network error creating directory $remoteDirPath")
            false
        }
    }
}
