package com.cl0de.app.data

/** Deep provider strings: the local bridge, its Node.js runtime, the light chat agent. See Strings.kt for the rules. */
object SDeep {
    // Light agent (registered in the OpenCode config next to the provider, for Deep sessions)
    val agentDescription get() = AppLang.s("Chat ringan tanpa alat untuk model Deep", "Light tool-free chat for Deep models")
    val agentPrompt get() = AppLang.s(
        "Kamu asisten yang membantu untuk chat sehari hari, berjalan lewat layanan DeepChat, tanpa alat apa pun. Jawab singkat, jelas, dan langsung dalam bahasa pengguna. Jangan mengaku bisa membuka berkas, menjalankan perintah, atau memakai alat; bila diminta hal yang butuh alat, jelaskan dengan sopan bahwa model Deep hanya untuk chat.",
        "You are a helpful assistant for everyday chat, running through the DeepChat service, with no tools at all. Answer briefly, clearly and directly, in the user's language. Never claim you can open files, run commands or use tools; when asked for something that needs tools, politely explain that Deep models are for chat only.",
    )

    // Chat gate messages (shown as a toast when a chat can't be sent yet)
    val gateModelGone get() = AppLang.s(
        "Model Deep ini sudah tidak ada di katalog. Pilih model Deep lain di pemilih model.",
        "This Deep model is no longer in the catalog. Pick another Deep model in the model picker.",
    )
    val gateServerFailed get() = AppLang.s(
        "Bridge Deep gagal menyala, coba lagi sebentar lagi.",
        "The Deep bridge failed to start, please try again in a moment.",
    )
    val gateServerSlow get() = AppLang.s(
        "Bridge Deep masih menyala, tunggu sebentar lalu kirim lagi.",
        "The Deep bridge is still starting, wait a moment and send again.",
    )

    // Bridge supervisor state fragments (short, like the llama ones)
    val failRuntimeNotReady get() = AppLang.s("runtime belum siap", "runtime not ready")
    val failStaging get() = AppLang.s("berkas bridge gagal disiapkan", "bridge files could not be prepared")
    val failNoNode get() = AppLang.s("Node.js gagal dipasang", "Node.js could not be installed")
}
