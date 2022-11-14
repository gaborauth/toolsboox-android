package com.toolsboox.ot

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Zip file manager.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class ZipManager {
    companion object {
        /**
         * Create a zip file and add all files from source path recursively.
         *
         * @param sourcePath the source path
         * @param zipFile the zip file
         */
        fun zip(sourcePath: File, zipFile: File) {
            if (!sourcePath.exists()) return

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val data = ByteArray(1024 * 16)

                val sourcePathLength = sourcePath.absolutePath.length + 1
                Files.walk(Paths.get(sourcePath.toURI())).use { stream ->
                    stream.map(Path::toFile).filter(File::isFile).forEach { item ->
                        BufferedInputStream(FileInputStream(item)).use { bis ->
                            val entry = ZipEntry(item.absolutePath.substring(sourcePathLength))
                            zos.putNextEntry(entry)

                            var count: Int
                            while (bis.read(data, 0, 1024 * 16).also { count = it } != -1) {
                                zos.write(data, 0, count)
                            }
                        }
                    }
                }
            }
        }
    }
}