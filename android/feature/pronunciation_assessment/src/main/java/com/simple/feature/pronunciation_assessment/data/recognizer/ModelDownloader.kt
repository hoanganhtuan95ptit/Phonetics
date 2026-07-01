package com.simple.feature.pronunciation_assessment.data.recognizer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val CONNECT_TIMEOUT_MILLIS = 15_000
private const val READ_TIMEOUT_MILLIS = 60_000
private const val BUFFER_SIZE_BYTES = 64 * 1024
private const val DOWNLOADING_MAX_PROGRESS = 99

/**
 * Tải file model về cache, có hỗ trợ progress qua Flow và file `.tmp`
 * để tránh để lại file incomplete khi bị gián đoạn.
 */
internal object ModelDownloader {

    /**
     * Tải model từ [url] về [cacheDir] với tên [fileName] nếu chưa có.
     */
    fun downloadIfNeeded(
        url: String,
        cacheDir: File,
        fileName: String,
    ): Flow<ModelDownloadEvent> = flow {

        val outFile = File(cacheDir, fileName)
        
        // Cache hit vẫn emit đủ event để caller dùng chung một flow xử lý UI.
        if (outFile.isReady()) {

            emit(ModelDownloadEvent.Progress(100))
            emit(ModelDownloadEvent.Completed(outFile))
            return@flow
        }

        // Ghi vào file tạm trước, chỉ promote sang file chính khi tải xong và
        // sync thành công. Nếu app bị kill giữa chừng thì file chính vẫn sạch.
        val tempFile = File(cacheDir, "$fileName.tmp")
        try {

            emitAll(downloadToTempFile(url, tempFile))
            tempFile.requireReady()
            tempFile.moveTo(outFile)
            emit(ModelDownloadEvent.Progress(100))
            emit(ModelDownloadEvent.Completed(outFile))
        } catch (t: Throwable) {

            // Không giữ lại `.tmp` lỗi vì lần sau không có cách biết nó đã tải
            // được bao nhiêu hay có bị server trả HTML/error body không.
            tempFile.delete()
            throw t
        }
    }

    private fun downloadToTempFile(url: String, tempFile: File): Flow<ModelDownloadEvent.Progress> = flow {

        val connection = url.openModelConnection()
        try {

            // Kiểm tra HTTP trước khi đọc stream để không cache nhầm trang lỗi.
            connection.requireSuccessfulResponse()
            emitAll(connection.writeTo(tempFile))
        } finally {

            connection.disconnect()
        }
    }

    private fun String.openModelConnection(): HttpURLConnection {

        val connection = URL(this).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.connect()
        return connection
    }

    private fun HttpURLConnection.requireSuccessfulResponse() {

        if (responseCode in 200..299) {

            return
        }

        error("Download model failed: HTTP $responseCode")
    }

    private fun HttpURLConnection.writeTo(tempFile: File): Flow<ModelDownloadEvent.Progress> = flow {

        inputStream.use { input ->

            // FileOutputStream được giữ đến cuối để gọi fd.sync(), giảm rủi ro
            // Android ghi cache trễ rồi app mở model khi file chưa flush hết.
            FileOutputStream(tempFile).use { output ->

                emitAll(input.copyTo(output, contentLengthLong))
                output.fd.sync()
            }
        }
    }

    private fun InputStream.copyTo(
        output: FileOutputStream,
        contentLength: Long,
    ): Flow<ModelDownloadEvent.Progress> = flow {

        val buffer = ByteArray(BUFFER_SIZE_BYTES)
        var downloaded = 0L
        var lastProgress = -1

        while (true) {

            val bytesRead = read(buffer)
            if (bytesRead <= 0) {

                break
            }

            output.write(buffer, 0, bytesRead)
            downloaded += bytesRead

            // Một số server không trả Content-Length; khi đó chỉ báo 100 ở
            // bước Completed, tránh hiển thị progress giả.
            val progress = downloaded.toProgress(contentLength) ?: continue
            if (progress == lastProgress) {

                continue
            }

            lastProgress = progress
            emit(ModelDownloadEvent.Progress(progress))
        }
    }

    private fun Long.toProgress(contentLength: Long): Int? {

        if (contentLength <= 0) {

            return null
        }

        // Trong khi còn đang copy stream chỉ báo tối đa 99; 100 chỉ phát sau
        // khi file tạm đã được validate và promote sang file chính.
        return (this * 100L / contentLength).toInt().coerceIn(0, DOWNLOADING_MAX_PROGRESS)
    }

    private fun File.isReady(): Boolean =
        exists() && length() > 0

    private fun File.requireReady() {

        check(isReady()) {

            "Downloaded model is empty"
        }
    }

    private fun File.moveTo(target: File) {

        // renameTo nhanh và gần atomic khi cùng filesystem; fallback copy để
        // vẫn chạy ổn trên các cache dir/provider không hỗ trợ rename.
        if (renameTo(target)) {

            return
        }

        copyTo(target, overwrite = true)
        delete()
    }
}

internal sealed interface ModelDownloadEvent {

    data class Progress(val percent: Int) : ModelDownloadEvent

    data class Completed(val file: File) : ModelDownloadEvent
}
