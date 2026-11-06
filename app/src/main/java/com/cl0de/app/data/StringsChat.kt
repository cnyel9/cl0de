package com.cl0de.app.data

/**
 * Strings for the Home & Chat area: home screens and looks, the chat screen,
 * messages, composer and its pickers, file/media cards, usage panel.
 * Same pattern as [S]: Indonesian first, the app's original English second.
 * Not here: provider/model names, brand names (GitHub, OpenCode, Cl0de),
 * code/terminal content, slash-command tokens and other identifiers, and
 * example prompts that are actually sent to the server as message text.
 */
object SChat {
    // ---------- home ----------
    val anonymousChat get() = AppLang.s("Chat anonim", "Anonymous chat")
    val anonymousDesc get() = AppLang.s(
        "Tidak ada yang disimpan: tidak masuk riwayat dan memori, dan langsung dihapus begitu kamu keluar.",
        "Nothing is kept: it stays out of your history and memory, and it's deleted as soon as you leave.",
    )
    val incognitoDesc get() = AppLang.s(
        "Chat samaran tidak masuk daftar terakhir dan dihapus saat ditinggalkan.",
        "Incognito chats stay out of your recents and are deleted when you leave them.",
    )
    val incognitoSession get() = AppLang.s("Sesi samaran", "Incognito session")
    val askMePlaceholder get() = AppLang.s("Tanya saya. Beri saya tugas.", "Ask me. Give me a task.")
    val describeTask get() = AppLang.s("Jelaskan tugasnya…", "Describe a task…")
    val chatWithOpenCode get() = AppLang.s("Chat dengan OpenCode…", "Chat with OpenCode…")
    val couldNotStartChat get() = AppLang.s("Chat tidak bisa dimulai", "Could not start the chat")
    val noModel get() = AppLang.s("belum ada model", "no model")
    val tip get() = AppLang.s("Tips", "Tip")
    val noGit get() = AppLang.s("Tanpa Git", "No Git")
    val memoryOn get() = AppLang.s("Memori aktif", "Memory on")
    val memoryOff get() = AppLang.s("Memori mati", "Memory off")
    val tipRemember get() = AppLang.s("Ucapkan \"ingat ini…\" — OpenCode menyimpannya di rak memori.", "Say \"remember that…\" — OpenCode keeps it on a memory shelf.")
    val tipHold get() = AppLang.s("Tahan pesan untuk menyalin, mengubah, atau memilih teksnya.", "Hold a message to copy it, edit it or select text.")
    val tipGhost get() = AppLang.s("Ketuk ikon hantu untuk chat tanpa jejak.", "Tap the ghost for a chat that leaves no trace.")
    val tipScheduled get() = AppLang.s("Terjadwal menjalankan prompt setiap hari pada jam pilihanmu.", "Scheduled runs a prompt every day at a time you pick.")
    val tipSwitchModel get() = AppLang.s("Ganti model langsung dari kolom pesan.", "Switch the model right from the composer.")
    val tipAttach get() = AppLang.s("Lampirkan tangkapan layar dan foto, OpenCode bisa membacanya.", "Attach screenshots and photos — OpenCode can read them.")
    val tipPickProject get() = AppLang.s("Pilih proyek di pojok sebelum mulai.", "Pick the project in the corner before you start.")
    val tipAddDevice get() = AppLang.s("Kode → Tambah perangkat untuk mengendalikan OpenCode di komputermu.", "Code → Add device lets you steer OpenCode on your computer.")
    val tipPermissions get() = AppLang.s("Izin menentukan apa yang boleh diubah OpenCode tanpa bertanya.", "Permissions decide what OpenCode may change without asking.")
    val tipArtifacts get() = AppLang.s("Artefak mengumpulkan file yang dibuat OpenCode untukmu.", "Artifacts collects the files OpenCode wrote for you.")

    // ---------- look homes ----------
    val whatBuilding get() = AppLang.s("Hari ini kita buat apa?", "What are we building today?")
    val whatWrite get() = AppLang.s("Hari ini kita tulis apa?", "What shall we write today?")
    val shipIt get() = AppLang.s("Gas, kirim ", "Ship it ")
    val shipToday get() = AppLang.s("hari ini.", "today.")
    val readyWhen get() = AppLang.s("Siap kapan pun ", "Ready when ")
    val youAre get() = AppLang.s("kamu siap.", "you are.")
    val onlineReady get() = AppLang.s("online · siap ngoding", "online · ready to code")
    val onlineStatus get() = AppLang.s("online", "online")
    val noMessagesYet get() = AppLang.s("Belum ada pesan di sini…", "No messages here yet…")
    val sendOrTap get() = AppLang.s("Kirim pesan atau ketuk sapaan di bawah.", "Send a message or tap a greeting below.")
    val startTask get() = AppLang.s("Mulai tugas", "Start a task")
    val startSomething get() = AppLang.s("Mulai sesuatu", "Start something")
    val pickCard get() = AppLang.s("Pilih kartu atau langsung ketik di bawah.", "Pick a card or just type below.")
    val whatWorking get() = AppLang.s("Kita mau mengerjakan apa?", "What are we working on?")
    val whatHelp get() = AppLang.s("Bisa saya bantu apa?", "What can I help with?")
    // suggestion card titles/subtitles (the prompt text itself is sent to the server and stays as is)
    val ideaExplain get() = AppLang.s("Jelaskan", "Explain")
    val ideaExplainSub get() = AppLang.s("proyek ini", "this project")
    val ideaFix get() = AppLang.s("Perbaiki", "Fix")
    val ideaFixSub get() = AppLang.s("build yang gagal", "a failing build")
    val ideaPlan get() = AppLang.s("Rencanakan", "Plan")
    val ideaPlanSub get() = AppLang.s("fitur baru", "a new feature")
    val ideaReview get() = AppLang.s("Tinjau", "Review")
    val ideaReviewSub get() = AppLang.s("perubahanku", "my changes")
    val ideaWrite get() = AppLang.s("Tulis", "Write")
    val ideaWriteSub get() = AppLang.s("atau ubah teks", "or edit a text")
    val ideaSearch get() = AppLang.s("Cari", "Search")
    val ideaSearchSub get() = AppLang.s("di web", "the web")
    val ideaBrainstorm get() = AppLang.s("Cari ide", "Brainstorm")
    val ideaBrainstormSub get() = AppLang.s("beberapa ide", "a few ideas")
    val ideaCodeSub get() = AppLang.s("skrip kecil", "a small script")

    // ---------- Kimi look ----------
    val scheduled get() = AppLang.s("Terjadwal", "Scheduled")
    val writeLabel get() = AppLang.s("Tulis", "Write")
    val searchWeb get() = AppLang.s("Cari di web", "Search the web")
    val moreModels get() = AppLang.s("Model lain", "More models")
    val reasoningLevel get() = AppLang.s("Tingkat penalaran", "Reasoning level")
    val modeLabel get() = AppLang.s("Mode", "Mode")
    val defaultLabel get() = AppLang.s("Bawaan", "Default")
    val letModelDecide get() = AppLang.s("Biarkan model yang memutuskan", "Let the model decide")
    val thinksLonger get() = AppLang.s("Berpikir lebih lama, menjawab lebih lambat", "Thinks longer, answers slower")
    val buildDesc get() = AppLang.s("Membuat perubahan dan menjalankan perintah", "Makes changes and runs commands")
    val planDesc get() = AppLang.s("Membaca dan merencanakan, tidak mengubah file", "Reads and plans, doesn't change files")
    val searchModels get() = AppLang.s("Cari model", "Search models")
    val noProvidersYet get() = AppLang.s("Belum ada provider yang terhubung.", "No connected providers yet.")
    val standard get() = AppLang.s("Standar", "Standard")
    val thinksBeforeAnswering get() = AppLang.s("berpikir sebelum menjawab", "thinks before answering")
    val seesImages get() = AppLang.s("bisa melihat gambar", "sees images")
    fun contextOf(size: String) = AppLang.s("$size konteks", "$size context")
    val camera get() = AppLang.s("Kamera", "Camera")
    val photos get() = AppLang.s("Foto", "Photos")
    val localFile get() = AppLang.s("File lokal", "Local file")
    val callLabel get() = AppLang.s("Panggil", "Call")
    val devices get() = AppLang.s("Perangkat", "Devices")
    val connectors get() = AppLang.s("Konektor", "Connectors")
    val connectAppsDesc get() = AppLang.s("Hubungkan aplikasi dan basis data untuk mengotomatiskan pekerjaanmu", "Connect apps and databases to automate what you do")
    val reuseSkillsDesc get() = AppLang.s("Pakai ulang skill khusus agar tugas selesai dengan andal", "Reuse specialised skills to get tasks done reliably")
    val scheduledDesc get() = AppLang.s("OpenCode menjalankan tugas sendiri pada jam yang kamu pilih", "OpenCode runs a task on its own at the time you pick")
    val permissions get() = AppLang.s("Izin", "Permissions")
    val asksBeforeEvery get() = AppLang.s("Bertanya sebelum setiap perubahan", "Asks before every change")
    val editsFreelyAsks get() = AppLang.s("Mengubah file bebas, bertanya sebelum perintah", "Edits files freely, asks before commands")
    val runsEverything get() = AppLang.s("Menjalankan semua tanpa bertanya", "Runs everything without asking")
    val hiPlain get() = AppLang.s("Halo.", "Hi.")
    fun hiName(name: String) = AppLang.s("Halo, $name.", "Hi, $name.")
    val whatDoToday get() = AppLang.s("Hari ini kita mau apa?", "What shall we do today?")
    val friend get() = AppLang.s("teman", "friend")

    // ---------- chat screen ----------
    val retryFailed get() = AppLang.s("Coba lagi gagal", "Retry failed")
    fun fileNotFound(uri: String) = AppLang.s("File tidak ditemukan: $uri", "File not found: $uri")
    val pin get() = AppLang.s("Sematkan", "Pin")
    val unpin get() = AppLang.s("Lepas sematan", "Unpin")
    val changes get() = AppLang.s("Perubahan", "Changes")
    val fork get() = AppLang.s("Cabang", "Fork")
    val compactContext get() = AppLang.s("Padatkan konteks", "Compact context")
    val archive get() = AppLang.s("Arsipkan", "Archive")
    val playWithCl0de get() = AppLang.s("Main dengan Cl0de sambil menunggu ›", "Play with Cl0de while you wait ›")
    val editingMessage get() = AppLang.s("Mengubah pesan, sesi mundur ke titik ini", "Editing message — session rewinds to it")
    val sending get() = AppLang.s("Mengirim…", "Sending…")
    val editYourMessage get() = AppLang.s("Ubah pesanmu", "Edit your message")
    fun replyTo(name: String) = AppLang.s("Balas $name…", "Reply to $name…")
    val sendFailed get() = AppLang.s("Gagal mengirim", "Send failed")
    val renameSession get() = AppLang.s("Ganti nama sesi", "Rename session")
    val deleteSessionQ get() = AppLang.s("Hapus sesi?", "Delete session?")
    val deleteSessionBody get() = AppLang.s("Ini menghapus percakapan dan riwayatnya secara permanen.", "This permanently removes the conversation and its history.")
    val noFileChanges get() = AppLang.s("Belum ada perubahan file di sesi ini.", "No file changes in this session yet.")
    val kept get() = AppLang.s("Dipertahankan", "Kept")
    val undoneFileBack get() = AppLang.s("Dibatalkan, file kembali seperti semula", "Undone — the file is back as it was")
    val keep get() = AppLang.s("Pertahankan", "Keep")
    val undo get() = AppLang.s("Urungkan", "Undo")
    fun undoChangesTo(name: String) = AppLang.s("Urungkan perubahan pada $name?", "Undo changes to $name?")
    val couldntUndo get() = AppLang.s("Tidak bisa diurungkan", "Couldn't undo")
    val undoExplain get() = AppLang.s(
        "File kembali ke keadaan sebelum agen mengubahnya di chat ini. Agen tidak akan tahu kecuali kamu memberi tahu.",
        "The file goes back to how it was before the agent changed it in this chat. The agent won't know unless you tell it.",
    )
    val providerNotAnswering get() = AppLang.s("Provider tidak menjawab", "The provider is not answering")
    val freeUsageExplain get() = AppLang.s(
        "Model gratis bawaan dibatasi server OpenCode. Resetnya tidak selalu berlaku, jadi menunggu belum tentu menolong. Ganti ke model gratis lain di bawah.",
        "The built-in free models are capped by OpenCode's server. The reset is not always applied, so waiting may not help. Switch to another free model below.",
    )
    fun nextAttemptIn(totalMin: Long, leftMs: Long): String =
        if (AppLang.indonesian()) {
            "Percobaan berikutnya dalam " + when {
                totalMin >= 60 -> "${totalMin / 60} jam ${totalMin % 60} menit"
                totalMin >= 1 -> "$totalMin menit"
                else -> "${leftMs / 1000} detik"
            }
        } else {
            "Next attempt in " + when {
                totalMin >= 60 -> "${totalMin / 60} h ${totalMin % 60} min"
                totalMin >= 1 -> "$totalMin min"
                else -> "${leftMs / 1000} s"
            }
        }
    val freeOptions get() = AppLang.s("Pilihan gratis", "Free options")
    val noAccount get() = AppLang.s("Tanpa akun", "No account")
    val switchedPollinations get() = AppLang.s("Beralih ke GPT-OSS 20B (Pollinations). Kirim lagi.", "Switched to GPT-OSS 20B (Pollinations). Send again.")
    val connectProvider get() = AppLang.s("Hubungkan provider", "Connect provider")
    val openAction get() = AppLang.s("Buka", "Open")

    // ---------- messages ----------
    val youPrefix get() = AppLang.s("ANDA · ", "YOU · ")
    val youCard get() = AppLang.s("Anda", "You")
    val copyMessage get() = AppLang.s("Salin pesan", "Copy message")
    fun mistakesNote(name: String) = AppLang.s("$name bisa berbuat salah. Periksa perubahan penting.", "$name can make mistakes. Check important changes.")
    val subtaskPrefix get() = AppLang.s("Subtugas: ", "Subtask: ")
    fun patchFiles(n: Int) = AppLang.s("Patch · $n file", "Patch · $n files")
    val contextCompacted get() = AppLang.s("Konteks dipadatkan", "Context compacted")
    val retrying get() = AppLang.s("Mencoba lagi…", "Retrying…")
    val providerNotConnected get() = AppLang.s("Provider tidak terhubung", "Provider not connected")
    val thinkingLabel get() = AppLang.s("Berpikir", "Thinking")
    val thoughtPast get() = AppLang.s("Selesai berpikir", "Thought")
    val stepThought get() = AppLang.s("berpikir", "thought")
    fun stepHelpers(k: Int) = if (AppLang.indonesian()) "meminta " + (if (k == 1) "pembantu" else "$k pembantu") else "asked " + (if (k == 1) "a helper" else "$k helpers")
    fun stepCommands(k: Int) = if (AppLang.indonesian()) "menjalankan " + (if (k == 1) "perintah" else "$k perintah") else "ran " + (if (k == 1) "a command" else "$k commands")
    fun stepReads(k: Int) = if (AppLang.indonesian()) "membaca " + (if (k == 1) "sebuah file" else "$k file") else "read " + (if (k == 1) "a file" else "$k files")
    fun stepEdits(k: Int) = if (AppLang.indonesian()) "mengubah " + (if (k == 1) "sebuah file" else "$k file") else "edited " + (if (k == 1) "a file" else "$k files")
    fun stepSearches(k: Int) = if (AppLang.indonesian()) "mencari " + (if (k == 1) "sekali" else "$k kali") else "searched " + (if (k == 1) "once" else "$k times")
    fun stepWeb(k: Int) = if (AppLang.indonesian()) "membuka " + (if (k == 1) "halaman" else "$k halaman") else "browsed " + (if (k == 1) "a page" else "$k pages")
    fun stepTools(k: Int) = if (AppLang.indonesian()) "memakai " + (if (k == 1) "alat" else "$k alat") else "used " + (if (k == 1) "a tool" else "$k tools")
    fun stepFailedSuffix(k: Int) = AppLang.s(" · $k gagal", " · $k failed")
    val editedFileLower get() = AppLang.s("mengubah sebuah file", "edited a file")
    val editedFileCap get() = AppLang.s("Mengubah sebuah file", "Edited a file")
    val readFileLower get() = AppLang.s("membaca sebuah file", "read a file")
    val readFileCap get() = AppLang.s("Membaca sebuah file", "Read a file")
    val permissionNeeded get() = AppLang.s("Perlu izin", "Permission needed")
    val deny get() = AppLang.s("Tolak", "Deny")
    val allowOnce get() = AppLang.s("Izinkan sekali ini", "Allow once")
    val always get() = AppLang.s("Selalu", "Always")
    val somethingElse get() = AppLang.s("Yang lain", "Something else")
    val nextLabel get() = AppLang.s("Berikutnya", "Next")
    val skipLabel get() = AppLang.s("Lewati", "Skip")
    fun ofPage(p: Int, n: Int) = AppLang.s("$p dari $n", "$p of $n")
    fun allowPermission(perm: String) = AppLang.s("Izinkan $perm?", "Allow $perm?")
    val questionLabel get() = AppLang.s("Pertanyaan", "Question")
    val skippedLabel get() = AppLang.s("Dilewati", "Skipped")
    val waitingAnswer get() = AppLang.s("Menunggu jawabanmu…", "Waiting for your answer…")
    fun moreChars(n: Int) = AppLang.s("\n… ($n karakter lagi)", "\n… ($n more chars)")

    // ---------- message / session actions ----------
    val selectText get() = AppLang.s("Pilih teks", "Select text")
    val editRewindsSub get() = AppLang.s("Sesi mundur ke pesan ini", "Rewinds the session to this message")
    val forkFromHere get() = AppLang.s("Cabangkan dari sini", "Fork from here")
    val deleteMessage get() = AppLang.s("Hapus pesan", "Delete message")

    // ---------- work steps ----------
    val editingPrefix get() = AppLang.s("Mengubah ", "Editing ")
    val editedPrefix get() = AppLang.s("Diubah ", "Edited ")
    val writingPrefix get() = AppLang.s("Menulis ", "Writing ")
    val rewrotePrefix get() = AppLang.s("Ditulis ulang ", "Rewrote ")
    val createdPrefix get() = AppLang.s("Dibuat ", "Created ")
    val readPrefix get() = AppLang.s("Dibaca ", "Read ")
    val searchedPrefix get() = AppLang.s("Dicari ", "Searched ")
    val openedPrefix get() = AppLang.s("Dibuka ", "Opened ")
    val helperPrefix get() = AppLang.s("Pembantu: ", "Helper: ")
    val doneLower get() = AppLang.s("selesai", "done")

    // ---------- thinking indicators ----------
    val workThinking get() = AppLang.s("Berpikir", "Thinking")
    val workReadingFiles get() = AppLang.s("Membaca file", "Reading files")
    val workWriting get() = AppLang.s("Menulis", "Writing")
    val workEditingCode get() = AppLang.s("Mengubah kode", "Editing code")
    val workRunningCommand get() = AppLang.s("Menjalankan perintah", "Running a command")
    val workSearching get() = AppLang.s("Mencari", "Searching")
    val workBrowsingWeb get() = AppLang.s("Menjelajah web", "Browsing the web")
    val workPlanning get() = AppLang.s("Merencanakan", "Planning")
    val workWaitingProvider get() = AppLang.s("Menunggu provider", "Waiting for the provider")
    val workWorking get() = AppLang.s("Bekerja", "Working")
    val verbThinking get() = AppLang.s("berpikir", "thinking")
    val verbReadingFiles get() = AppLang.s("membaca file", "reading files")
    val verbTyping get() = AppLang.s("mengetik", "typing")
    val verbEditingCode get() = AppLang.s("mengubah kode", "editing code")
    val verbRunningCommand get() = AppLang.s("menjalankan perintah", "running a command")
    val verbSearching get() = AppLang.s("mencari", "searching")
    val verbBrowsingWeb get() = AppLang.s("menjelajah web", "browsing the web")
    val verbPlanning get() = AppLang.s("merencanakan", "planning")
    val verbWaitingProvider get() = AppLang.s("menunggu provider", "waiting for the provider")
    val verbWorking get() = AppLang.s("bekerja", "working")
    fun elapsedStop(s: Long) = AppLang.s("  $s dtk · ketuk berhenti untuk menyela", "  ${s}s · stop to interrupt")
    fun opencodeIs(verb: String) = AppLang.s("OpenCode sedang $verb…", "OpenCode is $verb…")

    // ---------- chat extras ----------
    val taskInterruptedCaps get() = AppLang.s("TUGAS TERHENTI", "TASK INTERRUPTED")
    val taskInterrupted get() = AppLang.s("Tugas terhenti", "The task was interrupted")
    val interruptedExplain get() = AppLang.s(
        "Server berhenti saat OpenCode bekerja, jadi tugas tidak selesai. Lanjutkan dari titik terakhir?",
        "The server stopped while OpenCode was working, so it didn't finish. Continue from where it stopped?",
    )
    val continueAction get() = AppLang.s("Lanjutkan", "Continue")
    val couldNotContinue get() = AppLang.s("Tidak bisa dilanjutkan", "Could not continue")
    fun interruptedOne(title: String) = AppLang.s("Terhenti: $title", "Interrupted: $title")
    val aTask get() = AppLang.s("sebuah tugas", "a task")
    fun interruptedMany(n: Int) = AppLang.s("$n tugas terhenti", "$n tasks were interrupted")
    fun previewLabel(label: String) = AppLang.s("Pratinjau $label", "Preview $label")
    val freeModel get() = AppLang.s("Model gratis", "Free model")
    fun fiveHourSuffix(num: String) = AppLang.s(" · $num% dari 5 jam", " · $num% of 5h")

    // ---------- team strip ----------
    fun teamWorking(w: Int, n: Int) = AppLang.s("Tim · $w dari $n bekerja", "Team · $w working of $n")
    fun teamDone(n: Int) = AppLang.s("Tim · $n selesai", "Team · $n done")
    val teamLabel get() = AppLang.s("Tim", "Team")
    val helperFallback get() = AppLang.s("Pembantu", "Helper")
    val workingWord get() = AppLang.s("bekerja", "working")

    // ---------- selection bar ----------
    val selectAll get() = AppLang.s("Pilih semua", "Select all")
    val replyAction get() = AppLang.s("Balas", "Reply")
    val readAloud get() = AppLang.s("Bacakan", "Read aloud")

    // ---------- composer / pickers / slash menu ----------
    val selectModel get() = AppLang.s("Pilih model", "Select model")
    val listening get() = AppLang.s("Mendengarkan…", "Listening…")
    val askLabel get() = AppLang.s("Tanya", "Ask")
    val autoEditLabel get() = AppLang.s("Ubah otomatis", "Auto-edit")
    val autoLabel get() = AppLang.s("Otomatis", "Auto")
    val confirmEvery get() = AppLang.s("Konfirmasi setiap perubahan file dan perintah", "Confirm every file edit and command")
    val editFreelyAsk get() = AppLang.s("Ubah file bebas, tanya dulu sebelum perintah dan web", "Edit files freely, ask before commands and web")
    val runEverything get() = AppLang.s("Jalankan semua tanpa bertanya", "Run everything without asking")
    val permissionSingular get() = AppLang.s("Izin", "Permission")
    val agentLabel get() = AppLang.s("Agen", "Agent")
    val commandsTitle get() = AppLang.s("Perintah", "Commands")
    val addToSession get() = AppLang.s("Tambahkan ke sesi", "Add to session")
    val filesLabel get() = AppLang.s("File", "Files")
    val planMode get() = AppLang.s("Mode rencana", "Plan mode")
    val memoryLabel get() = AppLang.s("Memori", "Memory")
    val attachPhoto get() = AppLang.s("Lampirkan foto", "Attach photo")
    val attachImage get() = AppLang.s("Lampirkan gambar", "Attach image")
    val attachFile get() = AppLang.s("Lampirkan file", "Attach file")
    val switchPermission get() = AppLang.s("Ganti izin", "Switch permission")
    val switchAgent get() = AppLang.s("Ganti agen", "Switch agent")
    val togglePlanMode get() = AppLang.s("Alihkan mode rencana", "Toggle plan mode")
    val connectorsMcp get() = AppLang.s("Konektor (MCP)", "Connectors (MCP)")
    val onLower get() = AppLang.s("nyala", "on")
    val offLower get() = AppLang.s("mati", "off")
    val effortLabel get() = AppLang.s("Upaya", "Effort")
    val recommended get() = AppLang.s("Disarankan", "Recommended")
    val effortExplain get() = AppLang.s(
        "Upaya lebih tinggi berarti jawaban lebih teliti, tapi lebih lama dan kuota lebih cepat habis.",
        "Higher effort means more thorough responses, but takes longer and uses your limits faster.",
    )
    val noProvidersConnect get() = AppLang.s(
        "Belum ada provider terhubung. Hubungkan di Pengaturan → Provider.",
        "No connected providers yet. Connect one in Settings → Providers.",
    )
    val freeModelsShare get() = AppLang.s(
        "Model gratis berbagi satu batas harian per jaringan. Hubungkan provider lain di Pengaturan → Provider.",
        "Free models share one daily limit per network. Connect more providers in Settings → Providers.",
    )
    fun ctxShort(ctx: Long) = AppLang.s("${ctx / 1000}k konteks", "${ctx / 1000}k ctx")
    fun ctxFull(ctx: Long) = AppLang.s("${ctx / 1000}k konteks", "${ctx / 1000}k context")
    val imagesWord get() = AppLang.s("gambar", "images")
    val reasoningWord get() = AppLang.s("penalaran", "reasoning")
    val escBack get() = AppLang.s("esc kembali", "esc back")
    val loadingSkills get() = AppLang.s("Memuat skills…", "Loading skills…")
    fun noSkillNamed(q: String) = AppLang.s("Tidak ada skill bernama /$q", "No skill named /$q")
    val manageSkillsCaps get() = AppLang.s("KELOLA SKILLS", "MANAGE SKILLS")
    val manageSkills get() = AppLang.s("Kelola skills", "Manage skills")

    // ---------- files & media ----------
    fun noAppCanOpen(name: String) = AppLang.s("Tidak ada aplikasi yang bisa membuka $name", "No app can open $name")
    fun couldNotSave(name: String) = AppLang.s("Tidak bisa menyimpan $name", "Could not save $name")
    val savedToDownloads get() = AppLang.s("Tersimpan di Download/OpenCode", "Saved to Download/OpenCode")
    fun cantPlay(name: String) = AppLang.s("Tidak bisa memutar $name", "Can't play $name")
    fun videoSub(size: String) = AppLang.s("Video · $size", "Video · $size")
    fun imageSub(size: String) = AppLang.s("Gambar · $size", "Image · $size")
    val fileFallback get() = AppLang.s("BERKAS", "FILE")
    val savedToPictures get() = AppLang.s("Tersimpan di Pictures/OpenCode", "Saved to Pictures/OpenCode")

    // ---------- intro ----------
    val startingServer get() = AppLang.s("· memulai server di HP ini…", "· starting the server on this phone…")
    val hiImOpenCode get() = AppLang.s("Halo, saya OpenCode", "Hi, I'm OpenCode")

    // ---------- usage ring ----------
    fun resetLabel(at: Long?): String {
        if (at == null) return AppLang.s("Belum ada pemakaian", "No usage yet")
        if (at <= System.currentTimeMillis()) return AppLang.s("Sedang direset sekarang", "Resetting now")
        val soon = at - System.currentTimeMillis() < 24 * 3600_000L
        val fmt = if (AppLang.indonesian()) {
            java.text.SimpleDateFormat(if (soon) "'pukul' HH.mm" else "EEE HH.mm", java.util.Locale("id", "ID"))
        } else {
            java.text.SimpleDateFormat(if (soon) "'at' h:mm a" else "EEE h:mm a", java.util.Locale.ENGLISH)
        }
        return AppLang.s("Direset ", "Resets ") + fmt.format(java.util.Date(at))
    }
    val yourLimits get() = AppLang.s("Batasmu", "Your limits")
    val fiveHourLimit get() = AppLang.s("Batas 5 jam", "5-hour limit")
    val weeklyAllModels get() = AppLang.s("Mingguan · semua model", "Weekly · all models")
    val monthlyLabel get() = AppLang.s("Bulanan", "Monthly")
    val usageTitle get() = AppLang.s("Pemakaian", "Usage")
    val usageSoFar get() = AppLang.s("Pemakaian sejauh ini", "Usage so far")
    fun planUsageLimits(plan: String) = AppLang.s("Batas pemakaian paket · $plan", "Plan usage limits · $plan")
    val seeAll get() = AppLang.s("Lihat semua →", "See all →")
    val allArrow get() = AppLang.s("semua →", "all →")
    val contextTitle get() = AppLang.s("Konteks", "Context")
    val contextWindow get() = AppLang.s("Jendela konteks", "Context window")
    val openChatToSee get() = AppLang.s("Buka chat untuk melihat isi konteksnya.", "Open a chat to see what fills its context.")
    val counting get() = AppLang.s("Menghitung…", "Counting…")
    val noWindowSize get() = AppLang.s("Model ini tidak melaporkan ukuran jendelanya.", "This model doesn't report its window size.")
    val freeSpace get() = AppLang.s("Ruang kosong", "Free space")
    val autocompactBuffer get() = AppLang.s("Buffer autocompact", "Autocompact buffer")
    val estimatedNote get() = AppLang.s("Diperkirakan sampai balasan pertama masuk.", "Estimated until the first reply comes in.")
    val statMessages get() = AppLang.s("Pesan", "Messages")
    val statSpent get() = AppLang.s("Terpakai", "Spent")
    val statTokensIn get() = AppLang.s("Token masuk", "Tokens in")
    val statTokensOut get() = AppLang.s("Token keluar", "Tokens out")
    val thisChat get() = AppLang.s("Chat ini", "This chat")
    fun yoursLine(yours: Int, total: Int) = AppLang.s("$yours darimu, ${total - yours} dari OpenCode", "$yours from you, ${total - yours} from OpenCode")
    fun usedOf(used: String, limit: String) = AppLang.s("$used dari $limit", "$used of $limit")
}
