package com.cl0de.app.data

/**
 * Central UI string table. Every user-visible string in the app lives in this
 * package (this file plus the per-area Strings*.kt files) as a property whose
 * value follows [AppLang]: Indonesian by default, English when picked.
 *
 * Each getter reads [AppLang.indonesian], which reads the appLang Compose
 * state in Prefs, so any composable that reads a string here recomposes the
 * moment the language setting changes. English values are the app's original
 * texts; Indonesian values are natural UI Indonesian, not word-for-word.
 *
 * Not here on purpose: provider and model names, brand names (GitHub,
 * OpenCode, Cl0de), code and terminal content, and identifier strings.
 */
object S {
    val cancel get() = AppLang.s("Batal", "Cancel")
    val save get() = AppLang.s("Simpan", "Save")
    val delete get() = AppLang.s("Hapus", "Delete")
    val remove get() = AppLang.s("Keluarkan", "Remove")
    val done get() = AppLang.s("Selesai", "Done")
    val back get() = AppLang.s("Kembali", "Back")
    val close get() = AppLang.s("Tutup", "Close")
    val ok get() = AppLang.s("Oke", "OK")
    val yes get() = AppLang.s("Ya", "Yes")
    val no get() = AppLang.s("Tidak", "No")
    val add get() = AppLang.s("Tambah", "Add")
    val edit get() = AppLang.s("Ubah", "Edit")
    val copy get() = AppLang.s("Salin", "Copy")
    val copied get() = AppLang.s("Disalin", "Copied")
    val share get() = AppLang.s("Bagikan", "Share")
    val search get() = AppLang.s("Cari", "Search")
    val retry get() = AppLang.s("Coba lagi", "Retry")
    val create get() = AppLang.s("Buat", "Create")
    val rename get() = AppLang.s("Ganti nama", "Rename")
    val download get() = AppLang.s("Unduh", "Download")
    val settings get() = AppLang.s("Pengaturan", "Settings")
    val name get() = AppLang.s("Nama", "Name")
    val stop get() = AppLang.s("Berhenti", "Stop")
    val clear get() = AppLang.s("Bersihkan", "Clear")
    val send get() = AppLang.s("Kirim", "Send")
    val loading get() = AppLang.s("Memuat…", "Loading…")
    val error get() = AppLang.s("Galat", "Error")
    val on get() = AppLang.s("Nyala", "On")
    val off get() = AppLang.s("Mati", "Off")
    val all get() = AppLang.s("Semua", "All")
    val none get() = AppLang.s("Tidak ada", "None")
    val free get() = AppLang.s("Gratis", "Free")
    val today get() = AppLang.s("Hari ini", "Today")
    val yesterday get() = AppLang.s("Kemarin", "Yesterday")
    val language get() = AppLang.s("Bahasa", "Language")
    val model get() = AppLang.s("Model", "Model")
    val models get() = AppLang.s("Model", "Models")
    val provider get() = AppLang.s("Provider", "Provider")
    val providers get() = AppLang.s("Provider", "Providers")
    val project get() = AppLang.s("Proyek", "Project")
    val projects get() = AppLang.s("Proyek", "Projects")
    val newProject get() = AppLang.s("Proyek baru", "New project")
    val chat get() = AppLang.s("Obrolan", "Chat")
    val chats get() = AppLang.s("Obrolan", "Chats")
    val newChat get() = AppLang.s("Obrolan baru", "New chat")

    fun deleteNamed(name: String) = AppLang.s("Hapus $name?", "Delete $name?")
    fun removeNamed(name: String) = AppLang.s("Keluarkan $name?", "Remove $name?")
}
