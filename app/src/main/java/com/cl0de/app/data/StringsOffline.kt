package com.cl0de.app.data

/** Offline model strings: the llama.cpp engine, the model catalog, downloads, the local server. See Strings.kt for the rules. */
object SOffline {
    val title get() = AppLang.s("Model Offline", "Offline Models")
    val settingsSubtitle get() = AppLang.s("Model AI yang berjalan di HP ini, tanpa internet", "AI models that run on this phone, no internet needed")

    // Screen sections
    val serverTitle get() = AppLang.s("Server lokal", "Local server")
    val catalogTitle get() = AppLang.s("Katalog model", "Model catalog")
    val engineTitle get() = AppLang.s("Mesin llama.cpp", "llama.cpp engine")
    val deviceRam get() = AppLang.s("RAM perangkat", "Device RAM")

    fun ramInfo(gb: String, highClass: Boolean) = AppLang.s(
        "$gb GB · ${if (highClass) "kelas 8 GB ke atas" else "kelas di bawah 8 GB"}",
        "$gb GB · ${if (highClass) "8 GB and up class" else "under 8 GB class"}",
    )

    // Local server status
    val serverStopped get() = AppLang.s("Berhenti · pilih model terunduh untuk menyalakannya", "Stopped · pick a downloaded model to start it")
    fun serverStarting(name: String) = AppLang.s("Sedang menyalakan $name…", "Starting $name…")
    fun serverRunning(name: String) = AppLang.s("Berjalan · $name", "Running · $name")
    fun serverFailed(msg: String) = AppLang.s("Gagal menyala · $msg", "Failed to start · $msg")
    val failRuntimeNotReady get() = AppLang.s("runtime belum siap", "runtime not ready")
    val failNoBackend get() = AppLang.s("tidak ada backend yang termuat", "no backends are loaded")
    val stopServer get() = AppLang.s("Matikan server lokal", "Stop local server")
    val activeLabel get() = AppLang.s("Aktif", "Active")

    // Engine setup
    val engineMissing get() = AppLang.s(
        "Mesin untuk menjalankan model offline belum ada di perangkat ini. Diunduh sekali saja, sekitar 19 MB.",
        "The engine that runs offline models isn't on this device yet. Downloaded just once, about 19 MB.",
    )
    val setUpEngine get() = AppLang.s("Siapkan mesin", "Set up the engine")
    val engineReady get() = AppLang.s("Mesin siap", "Engine ready")
    val engineFailed get() = AppLang.s("Penyiapan mesin gagal, periksa koneksi lalu coba lagi", "Engine setup failed, check the connection and try again")
    val engineDownloading get() = AppLang.s("Mengunduh mesin", "Downloading the engine")
    val engineExtracting get() = AppLang.s("Memasang mesin", "Installing the engine")

    // Model entries
    val ready get() = AppLang.s("Siap", "Ready")
    val use get() = AppLang.s("Pakai", "Use")
    val resumeDownload get() = AppLang.s("Lanjutkan", "Resume")
    val downloadingLabel get() = AppLang.s("Mengunduh model", "Downloading model")
    val downloadFailed get() = AppLang.s("Unduhan gagal, berkas sebagian sudah dibersihkan", "Download failed, the partial file was cleaned up")
    fun minRam(gb: String) = AppLang.s("RAM min $gb GB", "Min RAM $gb GB")
    fun needsRam(gb: String) = AppLang.s("Butuh RAM perangkat minimal $gb GB", "Needs a device with at least $gb GB of RAM")
    val deleteTitle get() = AppLang.s("Hapus model ini?", "Delete this model?")
    fun deleteBody(name: String) = AppLang.s(
        "$name akan dihapus dari perangkat. Model bisa diunduh lagi kapan pun.",
        "$name will be removed from this device. It can be downloaded again any time.",
    )
    fun sizeMb(mb: String) = "$mb MB"

    // Speed test (llama-bench)
    val benchButton get() = AppLang.s("Tes Kecepatan", "Speed Test")
    val benchRunning get() = AppLang.s("Mengukur kecepatan di HP ini…", "Measuring speed on this phone…")
    fun benchResult(pp: String, tg: String) = AppLang.s(
        "Terukur di HP ini · olah prompt $pp · menulis $tg token/detik",
        "Measured on this phone · prompt $pp · writes $tg tokens/sec",
    )
    val benchFailed get() = AppLang.s("Tes kecepatan gagal, coba lagi", "Speed test failed, try again")

    // Chat gate messages (shown as a toast when a chat can't be sent yet)
    val gateNotDownloaded get() = AppLang.s(
        "Model offline ini belum terunduh. Buka Pengaturan, Model Offline, untuk mengunduhnya.",
        "This offline model isn't downloaded yet. Open Settings, Offline Models, to download it.",
    )
    val gateEngineMissing get() = AppLang.s(
        "Mesin model offline belum disiapkan. Buka Pengaturan, Model Offline, untuk menyiapkannya.",
        "The offline model engine isn't set up yet. Open Settings, Offline Models, to set it up.",
    )
    val gateServerFailed get() = AppLang.s(
        "Server lokal gagal menyala, coba lagi sebentar lagi.",
        "The local server failed to start, please try again in a moment.",
    )
    val gateServerSlow get() = AppLang.s(
        "Server lokal masih menyala, tunggu sebentar lalu kirim lagi.",
        "The local server is still starting, wait a moment and send again.",
    )

    /** Short strength description per catalog model id. Content data, kept next to the other offline strings. */
    fun desc(modelId: String): String = when (modelId) {
        "lfm2.5-350m" -> AppLang.s(
            "Paling ringan dan paling kencang di HP lemah, cadangan darurat; kemampuan dasar, jawaban pendek sederhana saja.",
            "The lightest and fastest on weak phones, an emergency fallback; basic ability, short simple answers only.",
        )
        "qwen3-0.6b" -> AppLang.s(
            "Kecil tapi keluarga Qwen3, multibahasa termasuk Indonesia; buat tugas ringan dan draf cepat.",
            "Small but from the Qwen3 family, multilingual including Indonesian; for light tasks and quick drafts.",
        )
        "lfm2.5-1.2b" -> AppLang.s(
            "Dirancang buat kecepatan di perangkat (teramati 21,2 token/detik di tier mid PocketPal), patuh instruksi buat ukurannya.",
            "Built for on-device speed (observed at 21.2 tokens/sec on PocketPal's mid tier), follows instructions well for its size.",
        )
        "gemma-3-1b" -> AppLang.s(
            "Jangkar aman, multibahasa kuat 140+ bahasa, kualitas chat umum terbaik di kelas 1B.",
            "The safe anchor, strong in 140+ languages, the best general chat quality in the 1B class.",
        )
        "bonsai-1.7b" -> AppLang.s(
            "Kuantisasi ekstrem 1-bit: berkas sangat kecil untuk model 1,7B, kualitas di bawah kelas Q4, cocok buat HP paling sempit.",
            "Extreme 1-bit quantization: a very small file for a 1.7B model, quality below the Q4 class, fits the tightest phones.",
        )
        "spark-x2.5-1.7b" -> AppLang.s(
            "Alternatif 1,7B berkarakter seimbang; posisi cadangan di kelas kecil.",
            "A balanced 1.7B alternative; the backup pick in the small class.",
        )
        "qwen3-1.7b" -> AppLang.s(
            "Kualitas chat, coding, dan matematika terkuat di kelas kecil, multibahasa; konsekuensinya lebih lambat di HP lemah.",
            "The strongest chat, coding and math quality in the small class, multilingual; the trade-off is more slowness on weak phones.",
        )
        "lfm2-vl-1.6b" -> AppLang.s(
            "Keluarga LFM2 ringkas berorientasi kecepatan, varian VL dengan berkas proyektor pendamping.",
            "A compact speed-oriented LFM2 family model, the VL variant with a companion projector file.",
        )
        "lfm2-2.6b" -> AppLang.s(
            "Pengetahuan dan nalar di atas kelas 1B dengan arsitektur berorientasi kecepatan; terberat dan terlambat di kelasnya.",
            "Knowledge and reasoning above the 1B class with a speed-oriented design; the heaviest and slowest in its class.",
        )
        "bonsai-8b" -> AppLang.s(
            "Model 8B yang muat di berkas sekitar 1,2 GB berkat kuantisasi 1-bit; kompromi kualitas demi ukuran.",
            "An 8B model squeezed into a roughly 1.2 GB file by 1-bit quantization; trades quality for size.",
        )
        "smollm3-3b" -> AppLang.s(
            "Terbuka penuh (data dan resep latih terbuka), konteks 64K, mode pikir bisa dinyalakan dan dimatikan.",
            "Fully open (training data and recipe included), 64K context, thinking mode can be switched on and off.",
        )
        "bonsai-4b" -> AppLang.s(
            "Versi 4B keluarga Bonsai, kuantisasi 1-bit, berkas hanya sekitar 0,6 GB.",
            "The 4B member of the Bonsai family, 1-bit quantization, a file of only about 0.6 GB.",
        )
        "gemma-3-4b" -> AppLang.s(
            "Lompatan kualitas chat dan multibahasa dari kelas 1B, jangkar tier high PocketPal.",
            "A leap in chat quality and languages over the 1B class, PocketPal's high-tier anchor.",
        )
        "gemma-4-e2b" -> AppLang.s(
            "Generasi Gemma 4 versi efisien 2B, kualitas per parameter tinggi khas Gemma, unduhannya besar karena ada berkas pendamping.",
            "The efficient 2B member of the Gemma 4 generation, high quality per parameter, a large download because of its companion file.",
        )
        "phi-4-mini-instruct" -> AppLang.s(
            "Spesialis nalar, matematika, dan coding di kelasnya (keluarga Phi dilatih data buku teks berkualitas).",
            "A reasoning, math and coding specialist in its class (the Phi family is trained on quality textbook data).",
        )
        "gemma-4-e4b" -> AppLang.s(
            "Gemma 4 versi efisien 4B, yang terkuat di katalog kelas atas, unduhan terbesar.",
            "The 4B efficient Gemma 4, the strongest in the high-class catalog, the largest download.",
        )
        "lfm2.5-8b-a1b" -> AppLang.s(
            "Trik MoE, total 8B tapi yang aktif per token sekitar 1B, jadi rasa cepat model kecil dengan pengetahuan model besar; berkas terbesar 4.845 MB, RAM min 6,3 GB.",
            "An MoE trick: 8B in total but only about 1B active per token, so it feels like a small model with a big model's knowledge; the largest file at 4,845 MB, min RAM 6.3 GB.",
        )
        "ministral-3-3b" -> AppLang.s(
            "Edge nya Mistral, efisien, kuat mengikuti instruksi dan konteks panjang buat kelas 3B.",
            "Mistral's edge model, efficient, strong at following instructions and long contexts for the 3B class.",
        )
        "qwen3.5-4b" -> AppLang.s(
            "Generasi Qwen terbaru di daftar, umum + coding kuat, multibahasa.",
            "The newest Qwen generation on the list, strong all-round and at coding, multilingual.",
        )
        "spark-x2.5-4b" -> AppLang.s(
            "Versi 4B dari alternatif Spark, seimbang; posisi cadangan kelas besar.",
            "The 4B version of the Spark alternative, balanced; the backup pick in the big class.",
        )
        else -> ""
    }
}
