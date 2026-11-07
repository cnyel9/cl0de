package com.cl0de.app.data

/** Voice strings: offline dictation, the model download offer, voice mode. See Strings.kt for the rules. */
object SVoice {
    val noSpeechService get() = AppLang.s("Tidak ada layanan pengenal bicara di perangkat ini", "No speech recognizer on this device")
    val micNeeded get() = AppLang.s("Perlu izin mikrofon", "Microphone access is needed")
    val micNeededDictate get() = AppLang.s("Perlu izin mikrofon untuk dikte", "Microphone access is needed to dictate")
    val serviceProblem get() = AppLang.s("Layanan pengenal bicara sedang bermasalah, coba lagi nanti", "The speech recognition service ran into a problem, please try again later")
    val networkProblem get() = AppLang.s("Koneksi bermasalah, pengenal bicara berhenti", "Connection problem, speech recognition stopped")
    val audioProblem get() = AppLang.s("Mikrofon bermasalah, pengenal bicara berhenti", "Microphone problem, speech recognition stopped")
    val serverProblem get() = AppLang.s("Server pengenal bicara sedang bermasalah, coba lagi nanti", "The speech recognition server ran into a problem, please try again later")
    val stopped get() = AppLang.s("Pengenal bicara berhenti", "Speech recognition stopped")

    val modelOffer get() = AppLang.s(
        "Dikte offline butuh model suara Whisper (sekitar 104 MB), diunduh sekali saja. Unduh sekarang?",
        "Offline dictation needs the Whisper voice model (about 104 MB), downloaded just once. Download it now?",
    )
    val downloadModel get() = AppLang.s("Unduh model", "Download model")
    val modelFailed get() = AppLang.s("Unduhan model gagal, periksa koneksi lalu coba lagi", "Model download failed, check the connection and try again")
    val modelReady get() = AppLang.s("Model suara siap, dikte offline aktif", "Voice model ready, offline dictation is on")
    val transcribing get() = AppLang.s("Menyalin suara…", "Transcribing…")
    val nothingHeard get() = AppLang.s("Tidak ada suara yang tertangkap", "Nothing was heard")

    fun downloadingModel(pct: Int) = AppLang.s("Mengunduh model suara… $pct%", "Downloading voice model… $pct%")

    val ttsMissing get() = AppLang.s(
        "Tidak ada mesin teks-ke-suara di perangkat ini, jadi jawaban tidak bisa dibacakan dengan suara",
        "No text-to-speech engine on this device, so replies can't be read aloud",
    )
    val recognitionUnavailable get() = AppLang.s("Pengenal bicara tidak tersedia di perangkat ini", "Speech recognition is not available on this device")
}
