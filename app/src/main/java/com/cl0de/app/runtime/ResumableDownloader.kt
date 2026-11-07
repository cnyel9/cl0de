package com.cl0de.app.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Download for very large files (GGUF models, the llama.cpp engine): byte-level resume
 * through a `.part` file and HTTP Range, automatic retries with a growing pause, and a
 * hard rule for total failure: when the attempts run out (or the server answers a fatal
 * error, or the finished bytes don't match the expected size/checksum) the partial file
 * is deleted, so a later download starts from a clean zero instead of piling up.
 * Cancelling the calling coroutine is not a failure: the `.part` stays for a resume.
 */
object ResumableDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    class Fatal(message: String) : IOException(message)

    suspend fun fetch(
        url: String,
        dest: File,
        expectedSize: Long,
        sha256: String?,
        maxAttempts: Int = 8,
        onProgress: (done: Long, total: Long) -> Unit = { _, _ -> },
    ) = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()
        if (dest.isFile && dest.length() == expectedSize) { onProgress(expectedSize, expectedSize); return@withContext }
        val part = File(dest.path + ".part")
        var attempt = 0
        var lastError: Exception? = null
        while (attempt < maxAttempts) {
            attempt++
            try {
                downloadOnce(url, part, expectedSize, onProgress)
                // size is exact here (downloadOnce checked); the checksum is the last gate
                if (sha256 != null) {
                    val got = Downloader.sha256Of(part)
                    if (got != sha256) {
                        part.delete()
                        throw Fatal("Checksum mismatch for ${dest.name}: $got")
                    }
                }
                dest.delete()
                if (!part.renameTo(dest)) throw Fatal("Could not rename ${part.name}")
                onProgress(expectedSize, expectedSize)
                return@withContext
            } catch (e: CancellationException) {
                throw e // not a failure: keep the .part for a later resume
            } catch (e: Fatal) {
                part.delete()
                throw e
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxAttempts) delay((2_000L * attempt).coerceAtMost(30_000L))
            }
        }
        // attempts exhausted: total failure means a clean slate, never a leftover pile
        part.delete()
        throw IOException("Download of ${dest.name} failed after $maxAttempts attempts: ${lastError?.message}", lastError)
    }

    /** One attempt: resumes from the `.part` length when the server honours Range. Returns with the part at exactly [expectedSize]. */
    private fun downloadOnce(url: String, part: File, expectedSize: Long, onProgress: (Long, Long) -> Unit) {
        var existing = if (part.isFile) part.length() else 0L
        if (existing > expectedSize) { part.delete(); existing = 0L }
        if (existing == expectedSize && expectedSize > 0) { onProgress(existing, expectedSize); return }
        val req = Request.Builder().url(url).header("User-Agent", "opencode-mobile")
            .apply { if (existing > 0) header("Range", "bytes=$existing-") }
            .build()
        client.newCall(req).execute().use { r ->
            if (r.code == 416 && existing > 0) {
                // the server can't resume from here; if the part is short of the target it is worthless
                if (existing != expectedSize) part.delete()
                throw IOException("Range not satisfiable at $existing of $expectedSize")
            }
            if (!r.isSuccessful) {
                if (r.code in 400..499 && r.code != 408 && r.code != 429) throw Fatal("Download $url: HTTP ${r.code}")
                throw IOException("Download $url: HTTP ${r.code}")
            }
            var append = false
            if (existing > 0) {
                if (r.code == 206) {
                    val range = r.header("Content-Range").orEmpty()
                    val start = range.substringAfter("bytes ").substringBefore('-').toLongOrNull()
                    if (start == existing) append = true else { part.delete(); throw IOException("Unexpected Content-Range: $range") }
                } else {
                    existing = 0L // the server ignored Range and sends the whole file: start over in place
                }
            }
            val body = r.body
            var done = existing
            var last = done
            FileOutputStream(part, append).buffered(1 shl 16).use { out ->
                val buf = ByteArray(1 shl 16)
                val src = body.byteStream()
                while (true) {
                    val n = src.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    if (done - last > 256 * 1024) { last = done; onProgress(done, expectedSize) }
                }
            }
            onProgress(done, expectedSize)
            if (done != expectedSize) throw IOException("Short download of ${part.name}: $done of $expectedSize bytes")
        }
    }
}
