package com.cl0de.app.data

import android.content.Context
import android.net.Uri
import com.cl0de.app.Graph
import com.cl0de.app.api.OcJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * Import an OpenCode config from the old app into this app's global config.
 * The user picks the file in shared storage (Android does not let one app read
 * another app's private data): either a plain opencode.json / opencode.jsonc,
 * or a full backup archive (.tar.gz) from the old app, which carries the config
 * at root/.config/opencode/opencode.jsonc inside it.
 * The config is merged into the global config the same way the config editor
 * saves: PATCH /global/config (deep merge).
 */
object ConfigImport {
    data class Result(val providers: Int, val models: Int)

    private const val CONFIG_IN_BACKUP = "root/.config/opencode/opencode.jsonc"
    private const val CONFIG_IN_BACKUP_JSON = "root/.config/opencode/opencode.json"
    private const val MAX_TEXT_BYTES = 8 * 1024 * 1024

    suspend fun import(ctx: Context, uri: Uri): Result {
        val text = withContext(Dispatchers.IO) { readConfigText(ctx, uri) }
            ?: throw IllegalArgumentException(
                AppLang.s(
                    "File tidak berisi config yang bisa dibaca. Pilih file opencode.jsonc atau arsip backup (.tar.gz) dari aplikasi lama.",
                    "The file holds no readable config. Pick an opencode.jsonc file or a backup archive (.tar.gz) from the old app.",
                )
            )
        val parsed = runCatching { OcJson.parseToJsonElement(stripJsonc(text)) as? JsonObject }.getOrNull()
            ?: throw IllegalArgumentException(
                AppLang.s(
                    "Isi file bukan JSON/JSONC yang valid, impor dibatalkan.",
                    "The file is not valid JSON/JSONC, import cancelled.",
                )
            )
        val providers = (parsed["provider"] as? JsonObject) ?: JsonObject(emptyMap())
        val models = providers.values.sumOf { (it as? JsonObject)?.get("models")?.let { m -> (m as? JsonObject)?.size ?: 0 } ?: 0 }
        Graph.api.patchGlobalConfig(parsed)
        return Result(providers.size, models)
    }

    /** The config text from the picked file, or null when the file holds none. */
    private fun readConfigText(ctx: Context, uri: Uri): String? {
        val name = uri.lastPathSegment?.lowercase().orEmpty()
        val stream = ctx.contentResolver.openInputStream(uri) ?: return null
        val buffered = BufferedInputStream(stream)
        buffered.mark(4)
        val magic = ByteArray(2)
        val got = buffered.read(magic)
        buffered.reset()
        val isGzip = got == 2 && magic[0] == 0x1f.toByte() && magic[1] == 0x8b.toByte()
        return buffered.use { input ->
            if (isGzip || name.endsWith(".tgz") || name.endsWith(".tar.gz") || name.endsWith(".gz")) {
                extractFromTarGz(input)
            } else {
                val out = ByteArrayOutputStream()
                val buf = ByteArray(64 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_TEXT_BYTES) return@use null
                    out.write(buf, 0, n)
                }
                out.toString(Charsets.UTF_8.name())
            }
        }
    }

    /** Stream through a tar.gz and return the config entry's text; other entries are skipped, never loaded whole. */
    private fun extractFromTarGz(input: InputStream): String? {
        val gz = GZIPInputStream(input)
        val header = ByteArray(512)
        while (true) {
            if (!readFully(gz, header)) return null
            if (header.all { it == 0.toByte() }) return null
            val rawName = header.copyOfRange(0, 100).toString(Charsets.UTF_8).trimEnd('\u0000', ' ')
            val prefix = header.copyOfRange(345, 500).toString(Charsets.UTF_8).trimEnd('\u0000', ' ')
            val entry = if (prefix.isNotEmpty()) "$prefix/$rawName" else rawName
            val sizeText = header.copyOfRange(124, 136).toString(Charsets.UTF_8).trim('\u0000', ' ')
            val size = sizeText.toLongOrNull(8) ?: return null
            val wanted = entry == CONFIG_IN_BACKUP || entry == CONFIG_IN_BACKUP_JSON || entry.endsWith("/$CONFIG_IN_BACKUP")
            if (wanted) {
                if (size > MAX_TEXT_BYTES) return null
                val data = ByteArray(size.toInt())
                if (!readFully(gz, data)) return null
                return data.toString(Charsets.UTF_8)
            }
            skipFully(gz, size + (512 - size % 512) % 512)
        }
    }

    private fun readFully(input: InputStream, buf: ByteArray): Boolean {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) return false
            off += n
        }
        return true
    }

    private fun skipFully(input: InputStream, count: Long) {
        var left = count
        val buf = ByteArray(64 * 1024)
        while (left > 0) {
            val n = input.read(buf, 0, minOf(left, buf.size.toLong()).toInt())
            if (n < 0) return
            left -= n
        }
    }

    /** Remove // and block comments and trailing commas, string-aware, so plain JSON parsing accepts JSONC. */
    fun stripJsonc(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        var inString = false
        var escaped = false
        while (i < text.length) {
            val ch = text[i]
            when {
                inString -> {
                    out.append(ch)
                    if (escaped) escaped = false else if (ch == '\\') escaped = true else if (ch == '"') inString = false
                    i++
                }
                ch == '"' -> { inString = true; out.append(ch); i++ }
                ch == '/' && i + 1 < text.length && text[i + 1] == '/' -> { while (i < text.length && text[i] != '\n') i++ }
                ch == '/' && i + 1 < text.length && text[i + 1] == '*' -> {
                    i += 2
                    while (i + 1 < text.length && !(text[i] == '*' && text[i + 1] == '/')) i++
                    i = minOf(i + 2, text.length)
                }
                else -> { out.append(ch); i++ }
            }
        }
        // Second pass: drop a comma whose next non-space char closes an object or array.
        val clean = out.toString()
        val result = StringBuilder(clean.length)
        var inStr = false
        var esc = false
        var j = 0
        while (j < clean.length) {
            val ch = clean[j]
            if (inStr) {
                result.append(ch)
                if (esc) esc = false else if (ch == '\\') esc = true else if (ch == '"') inStr = false
                j++
            } else if (ch == '"') { inStr = true; result.append(ch); j++ }
            else if (ch == ',') {
                var k = j + 1
                while (k < clean.length && clean[k].isWhitespace()) k++
                if (k < clean.length && (clean[k] == '}' || clean[k] == ']')) j++ else { result.append(ch); j++ }
            } else { result.append(ch); j++ }
        }
        return result.toString()
    }
}
