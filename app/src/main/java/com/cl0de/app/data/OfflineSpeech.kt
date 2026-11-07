package com.cl0de.app.data

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cl0de.app.runtime.Downloader
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Offline dictation with sherpa-onnx and the multilingual Whisper tiny model (int8).
 *
 * The phone this app mostly runs on has no system speech recognition service at all,
 * so the system SpeechRecognizer can never work there. The model is NOT bundled:
 * its three files (about 103.6 MB in total) are downloaded once from the model's
 * official Hugging Face repo into the app's private storage and verified byte-exact.
 */
object OfflineSpeech {
    private data class ModelFile(val name: String, val size: Long)

    private const val BASE = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main"
    private val FILES = listOf(
        ModelFile("tiny-encoder.int8.onnx", 12_937_772),
        ModelFile("tiny-decoder.int8.onnx", 89_855_401),
        ModelFile("tiny-tokens.txt", 816_730),
    )
    val TOTAL_BYTES: Long = FILES.sumOf { it.size }

    enum class State { Missing, Downloading, Ready, Failed }

    var state by mutableStateOf(State.Missing)
        private set
    var progress by mutableFloatStateOf(0f)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun dir(ctx: Context) = File(ctx.filesDir, "sherpa-onnx-whisper-tiny")

    fun isReady(ctx: Context): Boolean {
        val d = dir(ctx)
        val ok = FILES.all { f -> File(d, f.name).let { it.exists() && it.length() == f.size } }
        if (ok && state != State.Downloading) state = State.Ready
        return ok
    }

    /** Download the three model files once, verifying each file's exact size. */
    fun download(ctx: Context, onDone: (Boolean) -> Unit) {
        if (state == State.Downloading) return
        state = State.Downloading
        progress = 0f
        val appCtx = ctx.applicationContext
        scope.launch {
            val ok = runCatching {
                val d = dir(appCtx)
                var base = 0L
                for (f in FILES) {
                    val dest = File(d, f.name)
                    if (dest.exists() && dest.length() == f.size) { base += f.size; continue }
                    Downloader.fetch("$BASE/${f.name}", dest, null) { done, _ ->
                        progress = ((base + done).toFloat() / TOTAL_BYTES).coerceIn(0f, 1f)
                    }
                    check(dest.length() == f.size) { "Size mismatch for ${f.name}: ${dest.length()} != ${f.size}" }
                    base += f.size
                }
                true
            }.getOrDefault(false)
            state = if (ok) State.Ready else State.Failed
            withContext(Dispatchers.Main) { onDone(ok) }
        }
    }

    // ---- recognizer (created lazily, rebuilt when the language changes) ----

    private var recognizer: OfflineRecognizer? = null
    private var recognizerLang: String? = null

    private fun recognizer(ctx: Context, lang: String): OfflineRecognizer {
        recognizer?.let { if (recognizerLang == lang) return it }
        recognizer?.release()
        val d = dir(ctx)
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = File(d, "tiny-encoder.int8.onnx").absolutePath,
                    decoder = File(d, "tiny-decoder.int8.onnx").absolutePath,
                    language = lang,
                    task = "transcribe",
                ),
                tokens = File(d, "tiny-tokens.txt").absolutePath,
                numThreads = 2,
                modelType = "whisper",
            ),
        )
        return OfflineRecognizer(config = config).also { recognizer = it; recognizerLang = lang }
    }

    /** Transcribe recorded 16 kHz mono samples. Blocking: call from a background dispatcher. */
    fun transcribe(ctx: Context, samples: FloatArray, lang: String): String {
        if (samples.isEmpty()) return ""
        val rec = recognizer(ctx, lang)
        val stream = rec.createStream()
        try {
            stream.acceptWaveform(samples, 16000)
            rec.decode(stream)
            return rec.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    /** Whisper language code for the current speech language choice ("id", "en", …). */
    fun whisperLang(): String {
        val tag = Speech.tag()
        val two = tag.take(2).lowercase()
        return two.ifBlank { AppLang.current() }
    }
}

/**
 * Microphone capture for offline dictation: 16 kHz mono PCM from AudioRecord,
 * chunks converted to float samples (value / 32768) like the official sherpa-onnx
 * examples, with an RMS level callback for the recording wave.
 */
class OfflineRecorder {
    @Volatile private var recording = false
    private var thread: Thread? = null
    private var record: AudioRecord? = null

    val isRecording get() = recording

    fun start(onChunk: (FloatArray) -> Unit, onLevel: (Float) -> Unit) {
        if (recording) return
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val ar = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, sampleRate) * 2,
        )
        record = ar
        recording = true
        thread = Thread {
            val buffer = ShortArray(1600) // 100 ms
            runCatching { ar.startRecording() }
            while (recording) {
                val n = runCatching { ar.read(buffer, 0, buffer.size) }.getOrDefault(-1)
                if (n > 0) {
                    val samples = FloatArray(n) { buffer[it] / 32768f }
                    var sum = 0.0
                    for (s in samples) sum += (s * s).toDouble()
                    val rms = sqrt(sum / n).toFloat()
                    onChunk(samples)
                    onLevel((rms * 5f).coerceIn(0.04f, 1f))
                }
            }
            runCatching { ar.stop() }
            runCatching { ar.release() }
        }.also { it.start() }
    }

    fun stop() {
        recording = false
        thread?.join(600)
        thread = null
        record = null
    }
}
