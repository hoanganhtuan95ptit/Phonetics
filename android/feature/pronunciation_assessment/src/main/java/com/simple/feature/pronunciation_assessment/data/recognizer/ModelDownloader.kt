package com.simple.feature.pronunciation_assessment.data.recognizer

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tải file model về cache, có hỗ trợ progress callback và file `.tmp`
 * để tránh để lại file incomplete khi bị gián đoạn.
 */
internal object ModelDownloader {

    /**
     * Tải model từ [url] về [cacheDir] với tên [fileName] nếu chưa có.
     *
     * @param onProgress callback (0–100). Không gọi nếu server không trả
     *                   Content-Length. Luôn báo 100 khi hoàn thành.
     */
    fun downloadIfNeeded(
        url: String,
        cacheDir: File,
        fileName: String,
        onProgress: ((Int) -> Unit)?,
    ): File {
        val outFile = File(cacheDir, fileName)

        // File hợp lệ — bỏ qua download
        if (outFile.exists() && outFile.length() > 0) {
            onProgress?.invoke(100)
            return outFile
        }

        val tempFile = File(cacheDir, "$fileName.tmp")
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.connect()

            val contentLength = connection.contentLengthLong // -1 nếu không có header

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var lastProgress = -1

                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                        downloaded += n

                        if (onProgress != null && contentLength > 0) {
                            val progress =
                                (downloaded * 100L / contentLength).toInt().coerceIn(0, 99)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                    output.fd.sync()
                }
            }

            tempFile.renameTo(outFile)
            onProgress?.invoke(100)
            return outFile
        } catch (t: Throwable) {
            tempFile.delete()
            throw t
        }
    }
}
