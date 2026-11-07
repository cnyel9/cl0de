package com.cl0de.app.ui.components

import com.cl0de.app.data.AppLang
import com.cl0de.app.data.SChat

/**
 * Cl0de greetings on home: cloud puns, developer jokes and short nods
 * to films and games (retold in our own words, no quotes longer than a couple of words).
 * The first line has the name if there is one ({name}), the second is a question or a tease.
 * A new one on every visit. Each line is an (Indonesian, English) pair picked by [AppLang].
 */
object Greetings {
    private fun g(idA: String, enA: String, idB: String, enB: String) =
        (idA to enA) to (idB to enB)

    private val any = listOf(
        g("Halo, {name}.", "Hi, {name}.", "Berawan, mungkin turun kode.", "Cloudy with a chance of code."),
        g("Prakiraan hari ini:", "Forecast for today:", "100% pasti rilis.", "100% chance of shipping."),
        g("Hai, {name}.", "Hey, {name}.", "Itu bukan bug, itu fenomena cuaca.", "It's not a bug, it's a weather event."),
        g("Halo, {name}.", "Hello there, {name}.", "Kita mau buat apa?", "What are we making?"),
        g("Bangun, {name}.", "Wake up, {name}.", "Terminal menguasaimu.", "The terminal has you."),
        g("Semoga source", "May the source", "menyertaimu, {name}.", "be with you, {name}."),
        g("Deploy", "To deploy", "atau tidak deploy?", "or not to deploy?"),
        g("Aku akan kembali…", "I'll be back…", "dengan pull request.", "with a pull request."),
        g("Di awanku bisa.", "Works on my cloud.", "Ayo buat bisa di awanmu juga.", "Let's make it work on yours."),
        g("Siap, pemain {name}?", "Ready, player {name}?", "Tekan mulai.", "Press start."),
        g("Tetap tenang", "Keep calm", "dan git commit.", "and git commit."),
        g("Tidak semudah itu", "One does not simply", "push ke main.", "push to main."),
        g("Musim dingin tiba.", "Winter is coming.", "Deadline juga.", "So is the deadline."),
        g("Lakukan atau tidak.", "Do or do not.", "Tidak ada try-catch.", "There is no try-catch."),
        g("404:", "404:", "kekhawatiran tidak ditemukan.", "worries not found."),
        g("Houston,", "Houston,", "kita tidak punya masalah. Belum.", "we have no problems. Yet."),
        g("Omong itu murah.", "Talk is cheap.", "Tunjukkan prompt-nya.", "Show me the prompt."),
        g("Melayang-layang, {name}.", "Floating around, {name}.", "Ada apa?", "What's up?"),
        g("Merasa empuk hari ini.", "Feeling fluffy today.", "Kamu?", "You?"),
        g("Gerimis ide tipis-tipis.", "Light drizzle of ideas.", "Mau satu?", "Want one?"),
        g("Cumulus. Nimbus.", "Cumulus. Nimbus.", "Cl0de.", "Cl0de."),
        g("Sedang menyeduh badai", "Brewing a storm", "fitur.", "of features."),
        g("Cloud itu cuma HP orang lain.", "The cloud is just someone else's phone.", "Kali ini punyamu.", "This time it's yours."),
        g("Hello, world.", "Hello, world.", "Halo, {name}.", "Hello, {name}."),
        g("Ayo buat sesuatu", "Let's build something", "yang dibanggakan bebek karet.", "the rubber duck would be proud of."),
        g("Vibes: sempurna.", "Vibes: immaculate.", "Tes: kita lihat saja.", "Tests: let's find out."),
        g("Giliranmu, {name}.", "Your move, {name}.", "Aku siap mendengar. Eh, melihat.", "I'm all ears. Well, all eyes."),
        g("Pencapaian terbuka:", "Achievement unlocked:", "membuka aplikasinya.", "opened the app."),
        g("Memuat kreativitas…", "Loading creativity…", "99%. Giliranmu.", "99%. Your turn."),
        g("Berbahaya ngoding sendirian.", "It's dangerous to code alone.", "Ajak aku.", "Take me with you."),
        g("Kuenya bukan bohong.", "The cake is not a lie.", "Dokumentasinya mungkin.", "The docs might be."),
        g("Elementer, {name} sayang.", "Elementary, my dear {name}.", "Kasusnya apa?", "What's the case?"),
        g("Kita butuh", "We're gonna need", "repo yang lebih besar.", "a bigger repo."),
        g("Panjang umur", "Live long", "dan sering push.", "and push often."),
        g("Aku melihat kode mati.", "I see dead code.", "Mau kubereskan?", "Want me to clean it up?"),
        g("Kenapa serius sekali?", "Why so serious?", "Ini cuma refactor.", "It's just a refactor."),
        g("Tidak ada tempat", "There's no place", "seperti 127.0.0.1.", "like 127.0.0.1."),
        g("Hakuna matata.", "Hakuna matata.", "Tidak ada konflik merge.", "No merge conflicts."),
        g("Wingardium Leviosa…", "Wingardium Leviosa…", "build-nya melayang.", "the build is floating."),
        g("Dekati temanmu", "Keep your friends close", "dan backup-mu lebih dekat lagi.", "and your backups closer."),
        g("Semua basemu", "All your base", "adalah milikmu, {name}.", "are belong to you, {name}."),
        g("Sudah coba dimatikan", "Have you tried turning it off", "lalu dinyalakan lagi?", "and on again?"),
        g("Ini aku, Cl0de.", "It's a-me, Cl0de.", "Ayo jalan!", "Let's-a go!"),
        g("Hei, dengar!", "Hey, listen!", "Aku punya waktu untuk satu tugas.", "I've got time for a task."),
        g("Putar barrel roll dulu,", "Do a barrel roll,", "lalu bilang aku harus apa.", "then tell me what to do."),
        g("Snake? Snake?!", "Snake? Snake?!", "Oh, cuma Python.", "Oh, it's just Python."),
        g("Lantainya lava.", "The floor is lava.", "Awan aman-aman saja.", "The cloud is fine."),
        g("Langit cerah, {name}.", "Sky's clear, {name}.", "Hari bagus untuk deploy.", "Good day for a deploy."),
        g("Sebagian berawan,", "Partly cloudy,", "sepenuhnya siap.", "fully ready."),
        g("Tidak hujan hari ini.", "No rain today.", "Kecuali kamu mau grafik waterfall.", "Unless you want a waterfall chart."),
        g("Di suatu tempat di atas pelangi,", "Somewhere over the rainbow,", "sebuah build jadi hijau.", "a build turned green."),
        g("Aturan pertama Cl0de:", "First rule of Cl0de:", "kamu selalu bicara soal tugasmu.", "you always talk about your task."),
        g("Apakah ini kehidupan nyata?", "Is this the real life?", "Atau cuma prompt?", "Is this just a prompt?"),
        g("Hai, {name}!", "Hi, {name}!", "Janji, aku bukan hantu.", "I promise I'm not a ghost."),
        g("Aku datang, aku melihat,", "I came, I saw,", "aku mengompilasi.", "I compiled."),
        g("Tidak semua yang berkelana itu tersesat.", "Not all who wander are lost.", "Sebagian cuma membaca dokumentasi.", "Some are just reading the docs."),
        g("Naik, naik, dan pergi,", "Up, up and away,", "{name}. Ke mana?", "{name}. Where to?"),
        g("Pusat kendali ke {name}:", "Ground control to {name}:", "siap kapan pun kamu siap.", "ready when you are."),
        g("Masukkan koin", "Insert coin", "untuk lanjut. Atau ketik saja.", "to continue. Or just type."),
        g("Waktunya otak besar, {name}.", "Big brain time, {name}.", "Rencananya apa?", "What's the plan?"),
    )
    private val codeOnly = listOf(
        g("Halo, {name}.", "Hi, {name}.", "Hari ini kita buat apa?", "What are we building today?"),
        g("git status:", "git status:", "siap untuk ide berikutnya.", "ready for your next idea."),
        g("Menyusun pikiran…", "Compiling thoughts…", "Selesai. Tugasnya apa?", "Done. What's the task?"),
        g("Titik koma siap.", "Semicolons ready.", "Kurung seimbang. Gas.", "Brackets balanced. Go."),
    )

    /** A pair of greeting lines; [code] is code mode, which adds coder phrases. */
    fun pick(name: String, code: Boolean): String {
        val pool = if (code) any + codeOnly + codeOnly else any
        val (a, b) = pool.random()
        val who = name.ifBlank { SChat.friend }
        return AppLang.s(a.first, a.second).replace("{name}", who) + "\n" + AppLang.s(b.first, b.second).replace("{name}", who)
    }
}
