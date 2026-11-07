package com.cl0de.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cl0de.app.Graph
import com.cl0de.app.data.OfflineRecorder
import com.cl0de.app.data.OfflineSpeech
import com.cl0de.app.data.SVoice
import com.cl0de.app.data.Speech
import com.cl0de.app.ui.theme.Ic
import com.cl0de.app.ui.theme.Oc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dictation right into the input, like in Claude: a recording bar with a live wave;
 * the text appears in the input when you finish.
 *
 * Engine order: the offline Whisper model (sherpa-onnx) once it is downloaded,
 * the system SpeechRecognizer as a fallback where a recognition service exists,
 * and — where neither exists — an offer to download the offline model once.
 * On phones without any system recognition service (heavily debloated ones)
 * the offline engine is the only path that can work at all.
 */
class Dictation(private val ctx: Context) {
    enum class Mode { Idle, System, Offline, AskDownload, Downloading, Transcribing }

    var active by mutableStateOf(false)
        private set
    var mode by mutableStateOf(Mode.Idle)
        private set
    /** Already recognized text and the current chunk (system engine). */
    var done by mutableStateOf("")
        private set
    var partial by mutableStateOf("")
        private set
    val levels = mutableStateListOf<Float>().apply { repeat(BARS) { add(0f) } }
    var startedAt by mutableLongStateOf(0L)
        private set

    /** Offline transcripts arrive asynchronously; the composer hooks this to insert the text. */
    var onTranscribed: ((String) -> Unit)? = null

    val asking get() = mode == Mode.AskDownload
    val downloading get() = mode == Mode.Downloading
    val transcribing get() = mode == Mode.Transcribing
    val downloadPct get() = (OfflineSpeech.progress * 100).toInt()

    private var rec: SpeechRecognizer? = null
    private var lastPush = 0L
    /** Set after the first client/network failure: the rest of this session listens offline. */
    private var offlineRetry = false

    private val recorder = OfflineRecorder()
    private val chunks = mutableListOf<FloatArray>()
    @Volatile private var sampleCount = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val text get() = listOf(done, partial).filter { it.isNotBlank() }.joinToString(" ")

    fun start() {
        if (active) return
        done = ""; partial = ""
        for (i in levels.indices) levels[i] = 0f
        startedAt = System.currentTimeMillis()
        when {
            OfflineSpeech.isReady(ctx) -> startOffline()
            SpeechRecognizer.isRecognitionAvailable(ctx) -> startSystem()
            else -> { active = true; mode = Mode.AskDownload }
        }
    }

    // ---- offline engine (sherpa-onnx + Whisper) ----

    private fun startOffline() {
        synchronized(chunks) { chunks.clear() }
        sampleCount = 0
        active = true
        mode = Mode.Offline
        recorder.start(
            onChunk = { samples ->
                // Whisper works on up to ~30 s windows; keep at most two minutes.
                if (sampleCount < 16000 * 120) {
                    synchronized(chunks) { chunks.add(samples) }
                    sampleCount += samples.size
                }
            },
            onLevel = { push(it) },
        )
    }

    /** The user accepted the model offer in the dictation bar. */
    fun startDownload() {
        if (mode != Mode.AskDownload) return
        mode = Mode.Downloading
        OfflineSpeech.download(ctx) { ok ->
            if (ok) {
                Graph.chats.toasts.tryEmit(SVoice.modelReady)
                if (active && mode == Mode.Downloading) startOffline()
            } else {
                Graph.chats.toasts.tryEmit(SVoice.modelFailed)
                if (active) stop()
            }
        }
    }

    private fun finishOffline() {
        recorder.stop()
        val all = synchronized(chunks) {
            val out = FloatArray(sampleCount.coerceAtMost(16000 * 120))
            var pos = 0
            for (c in chunks) {
                val n = minOf(c.size, out.size - pos)
                if (n <= 0) break
                c.copyInto(out, pos, 0, n)
                pos += n
            }
            chunks.clear()
            out
        }
        if (all.isEmpty()) { stop(); return }
        mode = Mode.Transcribing
        scope.launch {
            val text = runCatching { OfflineSpeech.transcribe(ctx, all, OfflineSpeech.whisperLang()) }.getOrDefault("")
            if (mode != Mode.Transcribing) return@launch // cancelled meanwhile
            mode = Mode.Idle
            active = false
            if (text.isBlank()) Graph.chats.toasts.tryEmit(SVoice.nothingHeard)
            else {
                done = text
                onTranscribed?.invoke(text)
            }
        }
    }

    // ---- system engine (fallback where a recognition service exists) ----

    private fun startSystem() {
        active = true
        mode = Mode.System
        offlineRetry = false
        rec = SpeechRecognizer.createSpeechRecognizer(ctx).also { it.setRecognitionListener(listener) }
        listen()
    }

    private fun listen() { if (active && mode == Mode.System) runCatching { rec?.startListening(Speech.recognizerIntent(offlineRetry)) } }

    /** Human message for a recognizer error code, in the app language. Raw codes are never shown. */
    private fun errorText(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_SERVER -> SVoice.serviceProblem
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SVoice.networkProblem
        SpeechRecognizer.ERROR_AUDIO -> SVoice.audioProblem
        else -> SVoice.stopped
    }

    /** Stop and return everything recognized. Offline transcription finishes asynchronously via [onTranscribed]. */
    fun finish(): String {
        return when (mode) {
            Mode.Offline -> { val t = text; finishOffline(); t }
            Mode.AskDownload, Mode.Downloading -> { stop(); "" }
            else -> { val t = text; stop(); t }
        }
    }

    fun stop() {
        active = false
        mode = Mode.Idle
        recorder.stop()
        rec?.let { runCatching { it.cancel(); it.destroy() } }
        rec = null
    }

    private fun push(v: Float) {
        val now = System.currentTimeMillis()
        if (now - lastPush < 55) { levels[levels.lastIndex] = maxOf(levels.last(), v); return }
        lastPush = now
        levels.removeAt(0); levels.add(v)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) { push(((rmsdB + 2f) / 12f).coerceIn(0.04f, 1f)) }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            if (!active || mode != Mode.System) return
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> listen()
                // Client or network failure: one silent retry with offline recognition before giving up.
                SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_NETWORK ->
                    if (!offlineRetry) { offlineRetry = true; listen() }
                    else { Graph.chats.toasts.tryEmit(errorText(error)); stop() }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { Graph.chats.toasts.tryEmit(SVoice.micNeeded); stop() }
                else -> { Graph.chats.toasts.tryEmit(errorText(error)); stop() }
            }
        }
        override fun onResults(results: Bundle?) {
            val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (t.isNotBlank()) done = listOf(done, t).filter { it.isNotBlank() }.joinToString(" ")
            partial = ""
            listen()
        }
        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { partial = it }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object { const val BARS = 36 }
}

@Composable
fun rememberDictation(): Pair<Dictation, () -> Unit> {
    val ctx = LocalContext.current
    val d = remember { Dictation(ctx) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) d.start() else Graph.chats.toasts.tryEmit(SVoice.micNeededDictate)
    }
    DisposableEffect(Unit) { onDispose { d.stop() } }
    val start = {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) d.start()
        else perm.launch(Manifest.permission.RECORD_AUDIO)
    }
    return d to start
}

/** Recording bar: model offer / download progress / transcribing, or cancel, wave, time, "insert" and "send". */
@Composable
fun DictationBar(d: Dictation, onCancel: () -> Unit, onDone: () -> Unit, onSend: () -> Unit) {
    val c = Oc.c
    when {
        d.asking -> {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Txt(SVoice.modelOffer, size = 14.sp, color = c.text, lineHeight = 19.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallButton(SVoice.downloadModel, { d.startDownload() }, filled = true)
                    Spacer(Modifier.width(8.dp))
                    SmallButton(com.cl0de.app.data.S.cancel, onCancel)
                }
            }
            return
        }
        d.downloading -> {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Spinner(20.dp)
                Spacer(Modifier.width(10.dp))
                Txt(SVoice.downloadingModel(d.downloadPct), size = 14.sp, color = c.textDim, modifier = Modifier.weight(1f))
                IconBtn(Ic.Close, onCancel, size = 40.dp, bg = c.surfaceHigh, tint = c.textDim)
            }
            return
        }
        d.transcribing -> {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Spinner(20.dp)
                Spacer(Modifier.width(10.dp))
                Txt(SVoice.transcribing, size = 14.sp, color = c.textDim)
            }
            return
        }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(d.active) { while (d.active) { now = System.currentTimeMillis(); delay(250) } }
    val secs = ((now - d.startedAt) / 1000).coerceAtLeast(0)
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBtn(Ic.Close, onCancel, size = 40.dp, bg = c.surfaceHigh, tint = c.textDim)
        Spacer(Modifier.width(12.dp))
        val bar = c.text
        Canvas(Modifier.weight(1f).height(32.dp)) {
            val n = d.levels.size
            val step = size.width / n
            val w = (step * 0.5f).coerceAtMost(4.dp.toPx())
            d.levels.forEachIndexed { i, v ->
                val h = (size.height * v).coerceAtLeast(w)
                drawRoundRect(bar.copy(alpha = 0.35f + 0.65f * (i.toFloat() / n)), Offset(i * step + (step - w) / 2, (size.height - h) / 2), Size(w, h), CornerRadius(w / 2))
            }
        }
        Spacer(Modifier.width(10.dp))
        Txt("%d:%02d".format(secs / 60, secs % 60), size = 14.sp, color = c.textDim, family = Oc.mono)
        Spacer(Modifier.width(10.dp))
        IconBtn(Ic.Check, onDone, size = 40.dp, bg = c.surfaceHigh)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(40.dp).clip(Oc.pill).background(c.text).tap(onClick = onSend), contentAlignment = Alignment.Center) {
            Ico(Ic.ArrowUp, tint = c.bg, size = 20.dp)
        }
    }
}
