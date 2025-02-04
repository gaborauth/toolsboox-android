package com.toolsboox.fi

import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.OutputStream
import java.util.*
import javax.inject.Inject

/**
 * Google Drive services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class GoogleDriveService @Inject constructor() {
    companion object {

        /**
         * Get the file metadata in the folder.
         *
         * @param drive the Drive instance
         * @param parent the parent folder
         * @param fileName the file name
         * @return the file metadata
         * @throws Exception if the file metadata cannot be retrieved
         */
        fun getFile(drive: Drive, parent: File, fileName: String): File? {
            val files = drive.files().list().setSpaces("appDataFolder")
                .setQ("'${parent.id}' in parents and name='$fileName' and trashed=false")
                .setFields("files(id, kind, name, size, mimeType, createdTime, modifiedTime, properties, parents)")
                .execute().files.sortedByDescending { it.createdTime.value }
            return if (files.isNotEmpty()) files[0] else null
        }

        /**
         * Get or create the root folder of the application.
         *
         * @param drive the Drive instance
         * @param root the root folder name
         * @return the root folder
         * @throws Exception if the folder cannot be created
         */
        fun getOrCreateRootFolder(drive: Drive, root: String): File? {
            val files = drive.files().list().setSpaces("appDataFolder")
                .setQ("mimeType='application/vnd.google-apps.folder' and name='$root' and trashed=false")
                .setFields("files(id, kind, name, size, mimeType, createdTime, modifiedTime, properties, parents)")
                .execute().files.sortedByDescending { it.createdTime.value }
            if (files.isNotEmpty()) return files[0]

            val appRootFolder = File()
                .setParents(Collections.singletonList("appDataFolder"))
                .setMimeType("application/vnd.google-apps.folder")
                .setName(root)
            return drive.files().create(appRootFolder).execute()
        }

        /**
         * Get or create the folder in the parent folder.
         *
         * @param drive the Drive instance
         * @param parent the parent folder
         * @param folderName the folder name
         * @return the folder
         * @throws Exception if the folder cannot be created
         */
        fun getOrCreateFolder(drive: Drive, parent: File, folderName: String): File? {
            val files = drive.files().list().setSpaces("appDataFolder")
                .setQ("mimeType='application/vnd.google-apps.folder' and name='$folderName' and '${parent.id}' in parents and trashed=false")
                .setFields("files(id, kind, name, size, mimeType, createdTime, modifiedTime, properties, parents)")
                .execute().files.sortedByDescending { it.createdTime.value }
            if (files.isNotEmpty()) return files[0]

            val folder = File()
                .setParents(Collections.singletonList(parent.id))
                .setMimeType("application/vnd.google-apps.folder")
                .setName(folderName)
            return drive.files().create(folder).execute()
        }

        /**
         * Get or create the path as series of folders in the parent folder.
         *
         * @param drive the Drive instance
         * @param parent the parent folder
         * @param path the path, delimited by '/'
         * @return the last folder in the path
         * @throws Exception if the folder cannot be created
         */
        fun getOrCreatePath(drive: Drive, parent: File, path: String): File? {
            var current = parent
            path.split("/").filter { it.isNotEmpty() }.forEach {
                current = getOrCreateFolder(drive, current, it) ?: return null
            }

            return current
        }

        /**
         * Upload the file to the parent folder.
         *
         * @param drive the Drive instance
         * @param parent the parent folder
         * @param fileName the file name
         * @param fileContent the file content
         * @param properties the file properties
         * @return the file metadata
         * @throws Exception if the file cannot be uploaded
         */
        fun uploadFile(drive: Drive, parent: File, fileName: String, content: AbstractInputStreamContent, properties: Map<String, String>?): File? {
            val exists = getFile(drive, parent, fileName)

            val fileMetadata = File()
                .setParents(null)
                .setProperties(properties)
                .setName(fileName)

            if (exists == null) {
                fileMetadata.setParents(Collections.singletonList(parent.id))
                return drive.files().create(fileMetadata, content).execute()
            }

            return drive.files().update(exists.id, fileMetadata, content).execute()
        }

        /**
         * Download the file to the destination.
         *
         * @param drive the Drive instance
         * @param fileId the file ID
         * @param outputStream the output stream
         * @throws Exception if the file cannot be downloaded
         */
        fun downloadFile(drive: Drive, file: File, outputStream: OutputStream) {
            drive.files().get(file.id).executeMediaAndDownloadTo(outputStream)
        }

        /**
         * Walk through the folder structure.
         *
         * @param drive the Drive instance
         * @param parent the parent folder
         * @param relativePath the relative path of the folder
         * @return the list of file metadata
         * @throws Exception if the folder structure cannot be walked
         */
        fun walk(drive: Drive, parent: File, relativePath: String): List<File> {
            val filePaths = mutableListOf<File>()
            val files = drive.files().list().setSpaces("appDataFolder")
                .setQ("'${parent.id}' in parents and trashed=false")
                .setFields("files(id, kind, name, size, mimeType, createdTime, modifiedTime, properties, parents)")
                .execute().files.sortedBy { it.name }
            files.forEach {
                if (it.mimeType == "application/vnd.google-apps.folder") {
                    filePaths.addAll(walk(drive, it, relativePath + "/" + it.name))
                } else {
                    filePaths.add(it)
                }
            }

            return filePaths.sortedByDescending { it.name }
        }

        /**
         * Walk through the folder structure.
         *
         * @param drive the Drive instance
         * @param relativePath the relative path of the folder
         * @return the list of file metadata
         * @throws Exception if the folder structure cannot be walked
         */
        fun walkByProperty(drive: Drive, property: Pair<String, String>): List<File> {
            val files = drive.files().list().setSpaces("appDataFolder")
                .setQ("properties has { key='${property.first}' and value='${property.second}' } and trashed=false")
                .setFields("files(id, kind, name, size, mimeType, createdTime, modifiedTime, properties, parents)")
                .execute().files.sortedByDescending { it.name }

            return files
        }
    }
}