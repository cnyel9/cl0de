package com.cl0de.app.data

/**
 * UI strings for the Models & Agents, Skills, Team, Voice, Phone tools,
 * Onboarding, Edition, Icon picker, Mascot, Preview and Setup areas.
 * Same pattern as [S]: Indonesian first, original English second, picked
 * through [AppLang]. Interpolated strings are functions.
 * Provider/model names, brand names, code/terminal content and identifier
 * strings are not here on purpose.
 */
object SMisc {
    // Models, Agents, Permissions
    val permissions get() = AppLang.s("Izin", "Permissions")
    val agents get() = AppLang.s("Agen", "Agents")
    val modelForNewMessages get() = AppLang.s("Model untuk pesan baru", "Model for new messages")
    /** ChatHub aborts a send when the user changed the model while the local-provider gate was waiting. */
    val modelChangedWhileSending get() = AppLang.s("Model diganti saat pengiriman disiapkan, kirim lagi.", "The model was switched while the send was being prepared, please send again.")
    val smallModel get() = AppLang.s("Model kecil", "Small model")
    val autoLabel get() = AppLang.s("Otomatis", "Auto")
    val autoTitlesSummaries get() = AppLang.s("Otomatis (judul & ringkasan)", "Auto (titles & summaries)")
    val letOpenCodeChoose get() = AppLang.s("Biarkan OpenCode memilih", "Let OpenCode choose")
    val available get() = AppLang.s("Tersedia", "Available")
    val allow get() = AppLang.s("Izinkan", "Allow")
    val ask get() = AppLang.s("Tanya dulu", "Ask")
    val deny get() = AppLang.s("Tolak", "Deny")
    val autoEdit get() = AppLang.s("Ubah otomatis", "Auto-edit")
    val newSessions get() = AppLang.s("Sesi baru", "New sessions")
    val permissionsDesc get() = AppLang.s(
        "Berlaku untuk setiap sesi yang Anda mulai. Ubah otomatis membiarkan agen mengubah file tapi bertanya sebelum perintah shell dan akses web.",
        "Applied to every session you start. Auto-edit lets the agent change files but asks before shell commands and web access.",
    )
    val globalRules get() = AppLang.s("Aturan global (opencode.json)", "Global rules (opencode.json)")
    val fileEdits get() = AppLang.s("Perubahan file", "File edits")
    val shellCommands get() = AppLang.s("Perintah shell", "Shell commands")
    val webFetch get() = AppLang.s("Ambil web", "Web fetch")
    val outsideProject get() = AppLang.s("Di luar proyek", "Outside the project")
    val active get() = AppLang.s("Aktif", "Active")
    val customAgentsNote get() = AppLang.s(
        "Agen kustom bisa ditambahkan di config (bagian agent) atau sebagai file markdown di ~/.config/opencode/agent di dalam kontainer.",
        "Custom agents can be added in the config (agent section) or as markdown files in ~/.config/opencode/agent inside the container.",
    )
    fun modelsCount(n: Int) = AppLang.s("$n model", "$n models")

    // Skills
    val createSkillAsk get() = AppLang.s(
        "Saya mau membuat skill baru. Tanya saya skill itu harus bisa apa, lalu tuliskan.",
        "I want to create a new skill. Ask me what it should do, then write it.",
    )
    fun createSkillAbout(about: String) = AppLang.s("Buat skill: $about", "Create a skill: $about")
    fun addedSkills(joined: String) = AppLang.s("Ditambahkan $joined", "Added $joined")
    val couldNotImport get() = AppLang.s("Tidak bisa mengimpor", "Could not import")
    val couldNotStartChat get() = AppLang.s("Chat tidak bisa dimulai", "Could not start the chat")
    val couldNotSave get() = AppLang.s("Tidak bisa menyimpan", "Could not save")
    fun savedSkill(name: String) = AppLang.s("Tersimpan /$name", "Saved /$name")
    val skillsIntro get() = AppLang.s(
        "Skills mengajarkan OpenCode cara mengerjakan sesuatu dengan cara Anda. OpenCode memuat skill saat tugasnya cocok, atau saat Anda mengetik /nama di chat.",
        "Skills teach OpenCode how to do a job your way. It loads one when the task fits, or when you type /name in the chat.",
    )
    val createInChat get() = AppLang.s("Buat di chat", "Create in chat")
    val createInChatSub get() = AppLang.s("Jelaskan saja, OpenCode yang menulis", "Describe it, OpenCode writes it")
    val writeIt get() = AppLang.s("Tulis sendiri", "Write it")
    val writeItSub get() = AppLang.s("Nama, kapan dipakai, langkahnya", "Name, when to use, steps")
    val importFile get() = AppLang.s("Impor file", "Import file")
    val pasteInstructions get() = AppLang.s("Tempel instruksi", "Paste instructions")
    val pasteInstructionsSub get() = AppLang.s("Dari GPT kustom atau proyek Claude", "From a custom GPT or a Claude project")
    val yourSkills get() = AppLang.s("Skills Anda", "Your skills")
    val noSkillsYet get() = AppLang.s(
        "Belum ada skills. Buat satu di atas, atau impor yang Anda pakai di Claude atau ChatGPT.",
        "No skills yet. Make one above, or import the ones you use in Claude or ChatGPT.",
    )
    fun filesCount(n: Int) = AppLang.s("$n file", "$n files")
    val builtIn get() = AppLang.s("Bawaan", "Built in")
    val readOnly get() = AppLang.s("hanya baca", "read only")
    val commandsLabel get() = AppLang.s("Perintah", "Commands")
    val commandBadge get() = AppLang.s("perintah", "command")
    val searchSkills get() = AppLang.s("Cari skills", "Search skills")
    val pasteSheetBody get() = AppLang.s(
        "ChatGPT: buka GPT Anda → Edit → Configure dan salin Instructions. Claude: salin instruksi sebuah proyek. Keduanya menjadi skill yang bisa Anda panggil dengan /nama.",
        "ChatGPT: open your GPT → Edit → Configure and copy Instructions. Claude: copy a project's instructions. They become a skill you can call with /name.",
    )
    val nameExample get() = AppLang.s("Nama, mis. code-reviewer", "Name, e.g. code-reviewer")
    val whenToUseOptional get() = AppLang.s("Kapan dipakai (opsional)", "When to use it (optional)")
    val pasteTheInstructions get() = AppLang.s("Tempel instruksinya", "Paste the instructions")
    val saveSkill get() = AppLang.s("Simpan skill", "Save skill")
    val newSkill get() = AppLang.s("Skill baru", "New skill")
    val whenToUseIt get() = AppLang.s("Kapan dipakai", "When to use it")
    val instructionsLabel get() = AppLang.s("Instruksi", "Instructions")
    val whenExample get() = AppLang.s(
        "Mis. Pakai saat saya minta surat lamaran atau CV",
        "E.g. Use when I ask for a cover letter or a CV",
    )
    val instructionsPlaceholder get() = AppLang.s(
        "Langkah demi langkah: yang dilakukan, yang dihindari, format hasilnya…",
        "Step by step: what to do, what to avoid, the format of the result…",
    )
    fun plusMoreFiles(more: Int, path: String) = AppLang.s(
        "Ditambah $more file lain di $path",
        "Plus $more more files in $path",
    )
    val tryInChat get() = AppLang.s("Coba di chat", "Try in chat")
    val skillFolderRemoved get() = AppLang.s(
        "Folder skill dan file di dalamnya akan dihapus.",
        "The skill folder and its files will be removed.",
    )

    // Team
    val team get() = AppLang.s("Tim", "Team")
    val myTeam get() = AppLang.s("Tim saya", "My team")
    val automatic get() = AppLang.s("Otomatis", "Automatic")
    val teamManualDesc get() = AppLang.s(
        "Agen utama membagi tiap tugas ke pembantu Anda dan menjalankannya paralel, lalu memeriksa dan menggabungkan hasilnya.",
        "The main agent splits each task between your helpers and runs them in parallel, then checks and combines what they did.",
    )
    val teamAutoDesc get() = AppLang.s(
        "Agen utama menentukan berapa pembantu yang dibutuhkan dan model untuk masing-masing. Tugas sederhana tidak pakai pembantu.",
        "The main agent decides how many helpers a task needs and which model each one uses. Simple tasks get none.",
    )
    val teamOffDesc get() = AppLang.s(
        "Satu agen mengerjakan semua, seperti sebelumnya.",
        "One agent does everything, as before.",
    )
    val helpers get() = AppLang.s("Pembantu", "Helpers")
    val addHelper get() = AppLang.s("Tambah pembantu", "Add a helper")
    val helpersExample get() = AppLang.s(
        "Contoh: Peneliti (membaca kode dan mencari yang harus diubah), Penulis Kode (membuat perubahan), Peninjau (memeriksa dan menjalankan tes).",
        "For example: Researcher (reads the code and finds what to change), Coder (makes the change), Reviewer (checks it and runs the tests).",
    )
    val atMost get() = AppLang.s("Paling banyak", "At most")
    val helperModels get() = AppLang.s("Model yang boleh dipakai pembantu", "Models the helpers can use")
    val addModel get() = AppLang.s("Tambah model", "Add a model")
    val poolEmpty get() = AppLang.s(
        "Belum ada model dipilih: pembantu memakai model yang Anda pakai chat.",
        "No models picked: helpers use the model you chat with.",
    )
    val poolMix get() = AppLang.s(
        "Campur model kuat dengan model cepat: agen utama memberi bagian sulit ke yang pertama dan bagian mudah ke yang kedua.",
        "Mix a strong model with a fast one: the main agent gives hard parts to the first and simple ones to the second.",
    )
    val helpersNote get() = AppLang.s(
        "Pembantu tampil di chat pada bagian Tim saat mereka bekerja. Tiap pembantu mengurangi batas model Anda.",
        "Helpers show up in the chat under Team while they work. Each one counts against your model limits.",
    )
    val chatModel get() = AppLang.s("model chat", "chat model")
    val newHelper get() = AppLang.s("Pembantu baru", "New helper")
    val nameExampleReviewer get() = AppLang.s("Nama, mis. Peninjau", "Name, e.g. Reviewer")
    val whatItDoes get() = AppLang.s("Apa tugasnya", "What it does")
    val sameAsChat get() = AppLang.s("Sama seperti chat", "Same as the chat")
    val chooseModel get() = AppLang.s("Pilih model", "Choose a model")
    val searchModels get() = AppLang.s("Cari model", "Search models")

    // Voice mode (display text only; spoken/TTS sample text stays untouched)
    val micNeededToTalk get() = AppLang.s("Perlu akses mikrofon untuk bicara", "Microphone access is needed to talk")
    val somethingWentWrong get() = AppLang.s("Ada yang salah", "Something went wrong")
    val speechNotAvailable get() = AppLang.s(
        "Pengenal bicara tidak tersedia di perangkat ini",
        "Speech recognition is not available on this device",
    )
    val letsTalk get() = AppLang.s("Ayo bicara", "Let's talk")
    val tapToTalk get() = AppLang.s("Ketuk untuk bicara", "Tap to talk")
    val listening get() = AppLang.s("Mendengarkan…", "Listening…")
    val thinking get() = AppLang.s("Berpikir…", "Thinking…")
    val tapToInterrupt get() = AppLang.s("Ketuk untuk menyela", "Tap to interrupt")

    // Phone tools
    val phoneTools get() = AppLang.s("Alat HP", "Phone tools")
    val phoneToolsIntro get() = AppLang.s(
        "OpenCode berjalan di HP ini, jadi dia juga bisa bertindak di HP ini: mengingatkan, memasang alarm, membaca atau mengisi clipboard, mengambil foto, mencari lokasi Anda, menaruh file ke Downloads, dan menampilkan situs yang dibuatnya.",
        "OpenCode runs on this phone, so it can also act on it: remind you, set an alarm, read or fill the clipboard, take a photo, find where you are, put files into Downloads and show the sites it builds.",
    )
    val letOpenCodeUsePhone get() = AppLang.s("Izinkan OpenCode memakai HP ini", "Let OpenCode use this phone")
    val onlyForThisPhoneServer get() = AppLang.s("Hanya untuk server di HP ini", "Only for the server on this phone")
    val startsWithServer get() = AppLang.s("Mulai bersama server", "Starts with the server")
    fun connectedTools(n: Int) = AppLang.s("Terhubung · $n alat", "Connected · $n tools")
    val allowLocation get() = AppLang.s("Izinkan lokasi", "Allow location")
    val locationToolNeeded get() = AppLang.s("Diperlukan untuk alat lokasi", "Needed for the location tool")
    val toolsLabel get() = AppLang.s("Alat", "Tools")
    val phoneToolsTry get() = AppLang.s(
        "Coba: “ingatkan saya 20 menit lagi untuk peregangan”, “foto papan itu lalu jadikan catatan”, “simpan laporan ke Downloads”.\n\nKamera, alarm, clipboard, dan tautan bekerja saat aplikasi terbuka: Android tidak mengizinkan aplikasi melakukannya dari latar.",
        "Try: “remind me in 20 minutes to stretch”, “take a photo of the board and turn it into notes”, “save the report to Downloads”.\n\nCamera, alarm, clipboard and links work while the app is open: Android doesn't let apps do that from the background.",
    )

    // Onboarding
    val skip get() = AppLang.s("Lewati", "Skip")
    val showMeAround get() = AppLang.s("Tunjukkan keliling", "Show me around")
    val next get() = AppLang.s("Lanjut", "Next")
    val letsSetUp get() = AppLang.s("Ayo atur", "Let's set up")
    val continueLabel get() = AppLang.s("Lanjutkan", "Continue")
    val settingUpOpenCode get() = AppLang.s("Menyiapkan OpenCode", "Setting up OpenCode")
    val setupStopped get() = AppLang.s(
        "Penyiapan berhenti, Anda bisa mencoba lagi di langkah terakhir",
        "Setup stopped — you can retry on the last step",
    )
    val openCodeReady get() = AppLang.s("OpenCode siap", "OpenCode is ready")
    val preparing get() = AppLang.s("Menyiapkan…", "Preparing…")
    val startingOpenCode get() = AppLang.s("Memulai OpenCode…", "Starting OpenCode…")
    val aboutYouTitle get() = AppLang.s("Sedikit tentang Anda", "A bit about you")
    val aboutYouSub get() = AppLang.s(
        "Supaya saya bisa menyapa Anda dan, kalau Anda mau, mengingat hal yang penting.",
        "So I can greet you and, if you want, remember what matters.",
    )
    val whatCallYou get() = AppLang.s("Dia memanggil Anda apa?", "What should it call you?")
    val yourName get() = AppLang.s("Nama Anda", "Your name")
    val memory get() = AppLang.s("Memori", "Memory")
    val rememberMe get() = AppLang.s("Ingat saya", "Remember me")
    val rememberMeSub get() = AppLang.s(
        "Menyimpan catatan singkat tentang Anda dan proyek Anda di HP ini, jadi Anda tidak perlu mengulang. Anda bisa melihat dan menghapus semuanya.",
        "Keeps short notes about you and your projects on this phone, so you don't repeat yourself. You can see and delete everything.",
    )
    val startFresh get() = AppLang.s("Mulai baru tiap chat", "Start fresh every chat")
    val startFreshSub get() = AppLang.s(
        "Tidak ada yang disimpan antar chat. Memori bisa dinyalakan nanti.",
        "Nothing is saved between chats. You can turn memory on later.",
    )
    val giveHandTitle get() = AppLang.s("Beri bantuan", "Give it a hand")
    val giveHandSub get() = AppLang.s(
        "Semua opsional. Tiap izin membuat satu fitur bekerja, bisa diubah nanti di pengaturan Android.",
        "All optional. Each one makes a feature work — you can change them later in Android settings.",
    )
    val notifications get() = AppLang.s("Notifikasi", "Notifications")
    val notificationsSub get() = AppLang.s(
        "Jawaban, progres di layar kunci, dan tombol Izinkan / Tolak",
        "Answers, progress on the lock screen and Allow / Deny buttons",
    )
    val keepWorkingBg get() = AppLang.s("Tetap bekerja di latar", "Keep working in the background")
    val keepWorkingBgSub get() = AppLang.s(
        "Agar tugas panjang tidak berhenti saat layar padam",
        "So long tasks don't stop when the screen is off",
    )
    val microphone get() = AppLang.s("Mikrofon", "Microphone")
    val micSub get() = AppLang.s("Dikte dan mode suara", "Dictation and voice mode")
    val location get() = AppLang.s("Lokasi", "Location")
    val locationSub get() = AppLang.s(
        "Hanya saat Anda bertanya OpenCode Anda sedang di mana",
        "Only when you ask OpenCode where you are",
    )
    val phoneUseSub get() = AppLang.s(
        "Pengingat, alarm, clipboard, kamera, Downloads",
        "Reminders, alarms, clipboard, camera, Downloads",
    )
    val connectAccountTitle get() = AppLang.s("Hubungkan akun", "Connect an account")
    val connectAccountSub get() = AppLang.s(
        "Model gratis bekerja tanpa akun. Masuk untuk membuka lebih banyak, kunci tetap di HP ini.",
        "Free models work without one. Sign in to unlock more — keys stay on this phone.",
    )
    val startFree get() = AppLang.s("Mulai gratis", "Start free")
    val startFreeSub get() = AppLang.s("Tanpa akun, model gratis langsung bisa", "No account — free models right away")
    val continueGitHub get() = AppLang.s("Lanjutkan dengan GitHub", "Continue with GitHub")
    val copilotFreeModels get() = AppLang.s("Model gratis Copilot", "Copilot's free models")
    val openCodeAccount get() = AppLang.s("Akun OpenCode", "OpenCode account")
    val zenGoPlans get() = AppLang.s("Paket Zen dan Go", "Zen and Go plans")
    val retrySetup get() = AppLang.s("Coba lagi penyiapan", "Retry setup")
    val almostThere get() = AppLang.s(
        "Sedikit lagi, terbuka begitu penyiapan selesai.",
        "Almost there — it opens as soon as setup finishes.",
    )
    val waitingForSetup get() = AppLang.s("Menunggu penyiapan…", "Waiting for setup…")
    val tour1Say get() = AppLang.s("Hai! Saya Cl0de.", "Hi! I'm Cl0de.")
    val tour1Note get() = AppLang.s(
        "Awan kecil yang tinggal di HP Anda. Saya menulis kode, menjalankannya, dan chat dengan Anda, semua di sini, tanpa perlu komputer.",
        "A little cloud that lives on your phone. I write code, run it and chat with you — all right here, no computer needed.",
    )
    val tour2Say get() = AppLang.s("Langsung tanya saja.", "Just ask.")
    val tour2Note get() = AppLang.s(
        "Saat saya bekerja, ketuk baris ringkasan untuk melihat tiap langkah: perintah yang saya jalankan, file yang saya ubah, terminal langsungnya.",
        "While I work, tap the summary line to see every step: the commands I run, the files I edit, the live terminal.",
    )
    val tour3Say get() = AppLang.s("Punya komputer?", "Got a computer?")
    val tour3Note get() = AppLang.s(
        "Jalankan satu perintah di sana lalu pindai kode QR di Perangkat jarak jauh, sesudah itu saya bekerja di sana juga, langsung dari HP ini.",
        "Run one command on it and scan the QR code in Remote devices — then I work there too, right from this phone.",
    )
    val tour4Say get() = AppLang.s("Saya tidak bekerja sendirian.", "I don't work alone.")
    val tour4Note get() = AppLang.s(
        "Untuk tugas besar saya bisa memanggil pembantu dan menjalankan mereka paralel. Dan saya bisa mengingat hal penting bagi Anda, hanya kalau Anda mau.",
        "For big tasks I can call helpers and run them in parallel. And I can remember what matters to you — only if you want.",
    )
    val tour5Say get() = AppLang.s("Ayo berteman.", "Let's be friends.")
    val tour5Note get() = AppLang.s(
        "Ketuk saya, tahan saya, elus saya. Sedang menunggu tugas? Kunjungi kamar saya, saya belum terkalahkan di ping-pong. Hampir selalu.",
        "Tap me, hold me, pet me. Waiting for a task? Visit my room — I'm undefeated at ping-pong. Mostly.",
    )
    val ranCommandsEdited get() = AppLang.s("Menjalankan 2 perintah, mengubah ", "Ran 2 commands, edited ")
    val remoteDevicesAddScan get() = AppLang.s(
        "Perangkat jarak jauh → Tambah perangkat\n→ Pindai kode QR",
        "Remote devices → Add device\n→ Scan QR code",
    )
    val team2Working get() = AppLang.s("Tim · 2 bekerja", "Team · 2 working")
    val petMe get() = AppLang.s("Elus saya", "Pet me")
    val makeItYoursTitle get() = AppLang.s("Jadikan milik Anda", "Make it yours")
    val makeItYoursSub get() = AppLang.s(
        "Semua ini juga ada di Pengaturan, ubah kapan pun Anda mau.",
        "All of this lives in Settings too — change it whenever you like.",
    )
    val theme get() = AppLang.s("Tema", "Theme")
    val systemLabel get() = AppLang.s("Sistem", "System")
    val darkLabel get() = AppLang.s("Gelap", "Dark")
    val lightLabel get() = AppLang.s("Terang", "Light")
    val iconOnHomeScreen get() = AppLang.s("Ikon di layar utama Anda", "Icon on your home screen")
    val vibration get() = AppLang.s("Getaran", "Vibration")
    val vibrationSub get() = AppLang.s("Getaran ringan saat Anda menekan sesuatu", "A light tap when you press things")
    val cl0deOnHome get() = AppLang.s("Cl0de di layar utama", "Cl0de on the home screen")
    val cl0deOnHomeSub get() = AppLang.s("Awan yang menyambut Anda", "The cloud that greets you")
    val assistantIsCl0de get() = AppLang.s("Asistennya Cl0de", "The assistant is Cl0de")
    val assistantIsCl0deSub get() = AppLang.s(
        "Dia tahu namanya dan tempat tinggalnya; menyebut model aslinya saat ditanya",
        "It knows its name and where it lives; names its real model when asked",
    )
    val assistantIsCl0deSubLong get() = AppLang.s(
        "Tahu namanya, bahwa dia awan yang tinggal di aplikasi ini, dan menyebut model aslinya saat ditanya",
        "Knows its name, that it's a cloud living in this app, and names its real model when asked",
    )
    val holidayOutfits get() = AppLang.s("Kostum hari raya", "Holiday outfits")
    val holidayOutfitsSub get() = AppLang.s("Topi, salju, dan confetti di hari yang tepat", "Hats, snow and confetti on the right days")
    val enterCloudText get() = AppLang.s(
        "Silakan masuk —\nselamat datang di lingkungan dev saya!",
        "Come on in —\nwelcome to my dev environment!",
    )

    // Edition / interface looks
    val interfaceLabel get() = AppLang.s("Antarmuka", "Interface")
    val editionFooter get() = AppLang.s(
        "Warna, font, dan sudut langsung berubah. Mode terang dan gelap bekerja di semua antarmuka.",
        "Colors, fonts and corners switch right away. Light and dark mode work in every interface.",
    )
    val launchAnimation get() = AppLang.s("Animasi pembuka", "Launch animation")
    val launchAnimationSub get() = AppLang.s("Diputar saat aplikasi mulai; ketuk untuk melewati", "Plays when the app starts; tap to skip")
    val playItNow get() = AppLang.s("Putar sekarang", "Play it now")
    val iconMatches get() = AppLang.s("Ikon mengikuti antarmuka ini", "Icon matches this interface")
    fun useLookIcon(label: String) = AppLang.s("Pakai ikon $label", "Use the $label icon")
    val anyIconSettings get() = AppLang.s(
        "Ikon apa pun tetap bisa dipilih di Pengaturan → Ikon aplikasi",
        "Any icon can still be picked in Settings → App icon",
    )
    val makeItBlue get() = AppLang.s("jadikan biru", "make it blue")
    fun youMessage(msg: String) = AppLang.s("Anda — $msg", "You — $msg")
    val doneButtonBlue get() = AppLang.s("Selesai, tombolnya jadi biru.", "Done — the button is blue.")
    val askAnything get() = AppLang.s("Tanya apa saja", "Ask anything")

    // Icon picker
    val appIcon get() = AppLang.s("Ikon aplikasi", "App icon")
    val iconRefreshNote get() = AppLang.s(
        "Peluncur mungkin butuh beberapa detik untuk menyegarkan ikon.",
        "The launcher may take a few seconds to refresh the icon.",
    )

    // Mascot room
    val yourTaskRunning get() = AppLang.s("Tugas Anda sedang berjalan", "Your task is running")
    val backToChat get() = AppLang.s("Kembali ke chat", "Back to chat")
    fun pettedLine(pets: Int, best: String): String =
        if (AppLang.indonesian()) "Dielus $pets kali · reli terbaik $best"
        else "Petted $pets ${if (pets == 1) "time" else "times"} · best rally $best"
    val playPingPong get() = AppLang.s("Main ping-pong", "Play ping-pong")
    val tapHoldPetShakePhone get() = AppLang.s("Ketuk, tahan, elus, atau goyangkan HP", "Tap, hold, pet or shake the phone")
    val readyDrag get() = AppLang.s("Siap? Seret untuk bergerak.", "Ready? Drag to move.")
    fun rallyLabel(n: Int) = AppLang.s("reli $n", "rally $n")

    // Mascot settings
    val mascotTryHere get() = AppLang.s("Ketuk, tahan, elus, atau goyangkan, coba di sini.", "Tap, hold, pet or shake — try it here.")
    val showOnHomeScreen get() = AppLang.s("Tampilkan di layar utama", "Show on the home screen")
    val room get() = AppLang.s("Kamar", "Room")
    val roomSub get() = AppLang.s("Chat dengan Cl0de dan main ping-pong", "Chat with Cl0de and play ping-pong")
    val inChats get() = AppLang.s("Di chat", "In chats")
    val reactsTo get() = AppLang.s("Bereaksi pada", "Reacts to")
    val vibrationSubMascot get() = AppLang.s("Ketukan, elusan, dan ping-pong", "Taps, pets and ping-pong")
    val shakeTilt get() = AppLang.s("Goyang, miring, dan terbalik", "Shake, tilt and upside down")
    val shakeTiltSub get() = AppLang.s("Memakai sensor gerak saat tampil", "Uses the motion sensor while visible")
    val chargingLowBattery get() = AppLang.s("Mengisi daya dan baterai lemah", "Charging and low battery")
    val dozesOff get() = AppLang.s("Mengantuk saat ditinggal sendiri", "Dozes off when left alone")
    val morningStretch get() = AppLang.s("Peregangan pagi", "Morning stretch")
    val morningStretchSub get() = AppLang.s("Pembukaan pertama hari itu", "First launch of the day")
    val outfits get() = AppLang.s("Kostum", "Outfits")
    val outfit get() = AppLang.s("Kostum", "Outfit")
    val snowLeaves get() = AppLang.s("Salju, daun, dan confetti", "Snow, leaves and confetti")
    val snowLeavesSub get() = AppLang.s("Dan bersin musim gugur sesekali", "And the odd autumn sneeze")
    val together get() = AppLang.s("Bersama", "Together")
    val pettedLabel get() = AppLang.s("Dielus", "Petted")
    fun timesCount(pets: String): String =
        if (AppLang.indonesian()) "$pets kali"
        else "$pets ${if (pets == "1") "time" else "times"}"
    val bestRally get() = AppLang.s("Reli ping-pong terbaik", "Best ping-pong rally")
    val reset get() = AppLang.s("Atur ulang", "Reset")
    val byDate get() = AppLang.s("Per tanggal", "By date")
    fun outfitToday(label: String) = AppLang.s(" · $label hari ini", " · $label today")

    // Preview
    val couldNotOpenPage get() = AppLang.s("Halaman tidak bisa dibuka", "Could not open the page")
    val pageDidntOpen get() = AppLang.s("Halaman tidak terbuka", "The page didn't open")
    fun devServerHint(msg: String) = AppLang.s(
        "$msg\n\nServer dev masih berjalan? Minta OpenCode memulainya lagi.",
        "$msg\n\nIs the dev server still running? Ask OpenCode to start it again.",
    )
    fun liveReloaded(n: Int) = AppLang.s("Live · dimuat ulang ${n}×", "Live · reloaded ${n}×")
    val liveReloadsHint get() = AppLang.s(
        "Live · memuat ulang saat OpenCode mengubah file",
        "Live · reloads when OpenCode edits files",
    )
    val publish get() = AppLang.s("Terbitkan", "Publish")
    val publishPrompt get() = AppLang.s(
        "Terbitkan situs ini ke GitHub Pages (buat repo bila perlu) dan kirim tautannya ke saya.",
        "Publish this site to GitHub Pages (create the repo if needed) and send me the link.",
    )
    val preview get() = AppLang.s("Pratinjau", "Preview")

    // Setup
    val openCodeOnPhone get() = AppLang.s("OpenCode di HP ini", "OpenCode on this phone")
    val setupBody get() = AppLang.s(
        "Agen berjalan di kontainer Linux (Ubuntu via proot) langsung di perangkat Anda, tanpa perlu komputer. Penyiapan mengunduh sekitar 110 MB satu kali.",
        "The agent runs in a Linux container (Ubuntu via proot) right on your device — no computer needed. Setup downloads about 110 MB once.",
    )
    val tryAgain get() = AppLang.s("Coba lagi", "Try again")
    val setUp get() = AppLang.s("Siapkan", "Set up")

    // Scheduled tasks screen + scheduler toasts
    val scheduledTasks get() = AppLang.s("Tugas terjadwal", "Scheduled tasks")
    val noScheduledTasks get() = AppLang.s("Belum ada tugas terjadwal", "No scheduled tasks yet")
    val scheduledTasksHint get() = AppLang.s("Jalankan prompt terjadwal, misalnya ringkasan harian proyekmu.", "Run a prompt on a schedule, like a daily summary of your project.")
    val newTask get() = AppLang.s("Tugas baru", "New task")
    val editTask get() = AppLang.s("Ubah tugas", "Edit task")
    val whatShouldDo get() = AppLang.s("Apa yang harus dilakukan OpenCode?", "What should OpenCode do?")
    val timeLabel get() = AppLang.s("Waktu", "Time")
    val onceLabel get() = AppLang.s("Sekali", "Once")
    val dailyLabel get() = AppLang.s("Harian", "Daily")
    val weeklyLabel get() = AppLang.s("Mingguan", "Weekly")
    val tasksRunNote get() = AppLang.s("Tugas berjalan selama aplikasi dan server hidup. Aplikasi menjaga servernya tetap berjalan di latar belakang.", "Tasks run while the app and the server are alive. The app keeps its server running in the background.")
    fun repeatSuffix(repeat: String): String = when (repeat) {
        "daily" -> AppLang.s("setiap hari", "every day")
        "weekly" -> AppLang.s("setiap minggu", "every week")
        else -> AppLang.s("sekali saja", "once")
    }
    fun scheduledTaskFailed(msg: String?): String = AppLang.s("Tugas terjadwal gagal: $msg", "Scheduled task failed: $msg")
    val zenCapped get() = AppLang.s("Zen mencapai batas, dialihkan ke GPT-OSS 20B (Pollinations)", "Zen is capped — switched to GPT-OSS 20B (Pollinations)")
    val couldNotSwitchModel get() = AppLang.s("Tidak bisa mengganti model", "Could not switch the model")

    // Phone tool titles/descriptions shown in the tools list (the canonical English
    // definitions in PhoneBridge also feed the AI's MCP tool list, so the UI maps by tool name)
    fun toolTitle(name: String, fallback: String): String = if (AppLang.current() != "id") fallback else when (name) {
        "notify" -> "Notifikasi"; "remind" -> "Pengingat"; "set_alarm" -> "Jam alarm"
        "clipboard_read" -> "Baca papan klip"; "clipboard_write" -> "Salin ke papan klip"
        "location" -> "Lokasi"; "take_photo" -> "Kamera"; "save_to_downloads" -> "Unduhan"
        "open_url" -> "Tautan"; "preview" -> "Pratinjau"; "share" -> "Bagikan"; "device_status" -> "Status HP"
        else -> fallback
    }
    fun toolDesc(name: String, fallback: String): String = if (AppLang.current() != "id") fallback else when (name) {
        "notify" -> "Tampilkan notifikasi di HP saat ini juga."
        "remind" -> "Ingatkan pengguna nanti lewat notifikasi. Beri in_minutes atau at (waktu lokal 24 jam HH:MM, hari ini atau besok bila sudah lewat)."
        "set_alarm" -> "Pasang alarm di aplikasi Jam bawaan HP."
        "clipboard_read" -> "Baca teks yang sedang ada di papan klip HP."
        "clipboard_write" -> "Taruh teks di papan klip HP agar bisa ditempel pengguna di mana saja."
        "location" -> "Ambil lokasi HP saat ini (lintang, bujur, akurasi, dan alamat bila tersedia)."
        "take_photo" -> "Buka kamera agar pengguna mengambil foto; mengembalikan path JPEG yang tersimpan untuk dibaca."
        "save_to_downloads" -> "Salin file dari lingkungan ini ke folder Unduhan HP agar bisa dibuka atau dibagikan pengguna."
        "open_url" -> "Buka tautan di HP (browser atau aplikasi yang menanganinya)."
        "preview" -> "Tampilkan halaman web buatanmu di pratinjau aplikasi dengan muat ulang langsung: beri URL server dev lokal (http://localhost:5173) atau path absolut file .html."
        "share" -> "Buka lembar bagikan Android dengan teks agar pengguna bisa mengirimnya ke aplikasi apa pun (Telegram, surat, dan lainnya)."
        "device_status" -> "Baterai, pengisian daya, jaringan, waktu lokal, dan sisa penyimpanan HP."
        else -> fallback
    }
}
