package com.cl0de.app.data

import com.cl0de.app.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * GitHub sign-in for the environment on the phone: a device code (like `gh auth login --web`), then
 * git and gh are installed in Ubuntu, the token goes to gh, git is set to the account's name and noreply email.
 * After that the agent clones, commits, pushes and opens PRs as the user.
 */
object GitHub {
    /** GitHub CLI's public client id: the token really does go to gh. */
    private const val CLIENT_ID = "178c6fc778ccc68e1d6a"
    private const val SCOPES = "repo read:org gist workflow"

    val prompt = """
# Git and GitHub

- Commits are made as the user (git config user.name / user.email are already set). Never add "Co-Authored-By", "Generated with", "Signed-off-by" for yourself, or any other AI or assistant attribution to commit messages, pull requests, issues, release notes or code comments.
- If the user signed in to GitHub in the app, the gh CLI is logged in and git can push over https. Use gh for repos, pull requests, issues and releases.
""".trim()

    sealed interface State {
        data object Idle : State
        data class Code(val userCode: String, val url: String) : State
        data class Working(val step: String, val log: String = "") : State
        data object Done : State
        data class Failed(val message: String) : State
    }

    val state = MutableStateFlow<State>(State.Idle)

    /**
     * The guest-side truth about the sign-in, kept apart from [state] on purpose:
     * [signedIn] only reads the app's flag, and that flag survives a rootfs
     * rewrite that wipes the token (/root/.config/gh/hosts.yml) and the git
     * credential helper (/root/.gitconfig) inside the guest. Only [Ok] means
     * pushes can actually work.
     */
    sealed interface Health {
        data object Unknown : Health
        data object Checking : Health
        /** The environment itself is not installed yet, so nothing can be checked. */
        data object EnvNotReady : Health
        /** gh answers for the account, the git helper points at gh, and git can fetch the credential end to end. */
        data class Ok(val login: String) : Health
        /** No usable gh login in the guest (never signed in there, binaries gone, rootfs rewritten). Only a fresh sign-in helps. */
        data object NotLoggedIn : Health
        /** gh holds a token but GitHub rejected it. Only a fresh sign-in helps. */
        data object TokenRejected : Health
        /** gh is fine but git has no working credential helper for github.com, so pushes are refused. The repair action fixes exactly this. */
        data object HelperMissing : Health
        /** The check itself could not get through (network or DNS down, or a timeout). Says nothing about the token. */
        data class Unreachable(val detail: String) : Health
        data class Broken(val detail: String) : Health
    }

    val health = MutableStateFlow<Health>(Health.Unknown)
    @Volatile private var healthBusy = false
    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    val signedIn get() = Graph.prefs.githubLogin.isNotBlank()

    private fun post(url: String, form: Map<String, String>): JsonObject {
        val body = FormBody.Builder().apply { form.forEach { (k, v) -> add(k, v) } }.build()
        val req = Request.Builder().url(url).post(body).header("Accept", "application/json").build()
        http.newCall(req).execute().use { r -> return json.parseToJsonElement(r.body?.string().orEmpty()).jsonObject }
    }

    private fun user(token: String): JsonObject {
        val req = Request.Builder().url("https://api.github.com/user").header("Authorization", "Bearer $token").header("Accept", "application/vnd.github+json").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("GitHub said ${r.code}")
            return json.parseToJsonElement(r.body?.string().orEmpty()).jsonObject
        }
    }

    private fun JsonObject.s(k: String) = this[k]?.jsonPrimitive?.takeIf { it.isString }?.content

    /** The whole sign-in: code → wait for confirmation → set up the environment. */
    suspend fun signIn() = withContext(Dispatchers.IO) {
        try {
            val dc = post("https://github.com/login/device/code", mapOf("client_id" to CLIENT_ID, "scope" to SCOPES))
            val device = dc.s("device_code") ?: error(dc.s("error_description") ?: "GitHub did not give a code")
            val code = dc.s("user_code")!!
            var interval = (dc["interval"]?.jsonPrimitive?.int ?: 5).coerceAtLeast(5)
            val until = System.currentTimeMillis() + (dc["expires_in"]?.jsonPrimitive?.long ?: 900) * 1000
            state.value = State.Code(code, dc.s("verification_uri") ?: "https://github.com/login/device")
            var token: String? = null
            while (token == null) {
                if (state.value !is State.Code) return@withContext   // cancelled
                if (System.currentTimeMillis() > until) error("The code expired. Try again.")
                delay(interval * 1000L)
                val r = post("https://github.com/login/oauth/access_token", mapOf("client_id" to CLIENT_ID, "device_code" to device, "grant_type" to "urn:ietf:params:oauth:grant-type:device_code"))
                token = r.s("access_token")
                when (r.s("error")) {
                    null, "authorization_pending" -> {}
                    "slow_down" -> interval += 5
                    "access_denied" -> error("You declined the request on GitHub.")
                    "expired_token" -> error("The code expired. Try again.")
                    else -> if (token == null) error(r.s("error_description") ?: "GitHub sign-in failed")
                }
            }
            setup(token)
        } catch (e: Exception) {
            state.value = State.Failed(e.message ?: "GitHub sign-in failed")
        }
    }

    private suspend fun setup(token: String) {
        state.value = State.Working("Checking your account")
        val u = user(token)
        val login = u.s("login") ?: error("No login in the GitHub answer")
        val id = u["id"]?.jsonPrimitive?.long ?: 0
        val name = u.s("name")?.takeIf { it.isNotBlank() } ?: login
        val email = "$id+$login@users.noreply.github.com"

        state.value = State.Working("Installing git and GitHub CLI")
        val log = StringBuilder()
        val install = "command -v git >/dev/null && command -v gh >/dev/null || (apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends git gh ca-certificates)"
        var rc = Graph.runtime.runGuest(listOf("/bin/bash", "-lc", install), timeoutSec = 1200) { line ->
            log.appendLine(line); state.value = State.Working("Installing git and GitHub CLI", line.take(80))
        }
        if (rc != 0) error("Could not install git and gh:\n" + log.lines().takeLast(4).joinToString("\n"))

        state.value = State.Working("Signing in gh and git")
        val conf = """
            printf %s "${'$'}GH_T" | gh auth login --hostname github.com --git-protocol https --with-token && gh auth setup-git --hostname github.com &&
            git config --global user.name "${'$'}GIT_NAME" && git config --global user.email "${'$'}GIT_EMAIL" &&
            git config --global init.defaultBranch main && git config --global pull.rebase false
        """.trimIndent()
        log.clear()
        rc = Graph.runtime.runGuest(listOf("/bin/bash", "-lc", conf), extraEnv = mapOf("GH_T" to token, "GIT_NAME" to name, "GIT_EMAIL" to email)) { log.appendLine(it) }
        if (rc != 0) error("gh did not accept the sign-in:\n" + log.lines().takeLast(4).joinToString("\n"))

        Graph.prefs.githubLogin = login
        Graph.prefs.githubName = name
        Graph.prefs.githubAvatar = u.s("avatar_url").orEmpty()
        Memory.sync()
        state.value = State.Done
    }

    fun cancel() { state.value = State.Idle }

    suspend fun signOut() {
        runCatching { Graph.runtime.runGuest(listOf("/bin/bash", "-lc", "gh auth logout --hostname github.com </dev/null; git config --global --unset credential.https://github.com.helper; true")) }
        Graph.prefs.githubLogin = ""
        Graph.prefs.githubName = ""
        Graph.prefs.githubAvatar = ""
        health.value = Health.Unknown
        state.value = State.Idle
    }

    /**
     * One command inside the guest, output collected. GIT_TERMINAL_PROMPT=0 is
     * mandatory: there is no TTY in proot, so a git credential prompt would
     * hang until the timeout instead of failing.
     */
    private suspend fun guestBash(script: String, timeoutSec: Long = 90): Pair<Int, String> {
        val out = StringBuilder()
        val rc = runCatching {
            Graph.runtime.runGuest(listOf("/bin/bash", "-lc", script), timeoutSec = timeoutSec, extraEnv = mapOf("GIT_TERMINAL_PROMPT" to "0")) { out.appendLine(it) }
        }.getOrDefault(-1)
        return rc to out.toString()
    }

    private fun lastLine(out: String) = out.lines().map { it.trim() }.lastOrNull { it.isNotEmpty() }?.take(140).orEmpty()

    /** Failure text that blames the network, never the token (the EAI_AGAIN case). */
    private fun isNetworkFailure(out: String): Boolean {
        val t = out.lowercase()
        return listOf(
            "could not resolve host", "temporary failure in name resolution", "name or service not known",
            "no such host", "dial tcp", "eai_again", "network is unreachable", "connection refused",
            "connection reset", "i/o timeout", "tls handshake", "client.timeout",
        ).any { t.contains(it) }
    }

    private fun isTokenRejection(out: String): Boolean {
        val t = out.lowercase()
        return listOf("bad credentials", "invalid token", "token is invalid", "unauthorized", "401").any { t.contains(it) }
    }

    /** Check the connection inside the guest and publish the verdict to [health]. */
    suspend fun checkHealth() {
        if (healthBusy) return
        healthBusy = true
        try { healthLadder() } finally { healthBusy = false }
    }

    /**
     * The ladder, cheapest truth first. A rung that needs the network never
     * blames the token when the network itself is down, and a runGuest timeout
     * (-1 whatever the cause) is never read as damage.
     */
    private suspend fun healthLadder() {
        if (!Graph.paths.rootfsReady) { health.value = Health.EnvNotReady; return }
        health.value = Health.Checking

        // Rung 1: does gh hold a login GitHub still accepts?
        val (rcA, outA) = guestBash("gh auth status")
        if (rcA == -1) { health.value = Health.Unreachable(lastLine(outA)); return }
        if (rcA != 0) {
            health.value = when {
                isNetworkFailure(outA) -> Health.Unreachable(lastLine(outA))
                outA.contains("command not found") -> Health.NotLoggedIn
                isTokenRejection(outA) -> Health.TokenRejected
                outA.lowercase().contains("not logged in") || outA.lowercase().contains("not logged into") -> Health.NotLoggedIn
                else -> Health.Broken(lastLine(outA))
            }
            return
        }

        // Rung 2 (local): does git have a credential helper for github.com, pointing at gh?
        val (rcB, outB) = guestBash("git config --global --get credential.https://github.com.helper", timeoutSec = 30)
        if (rcB == -1) { health.value = Health.Unreachable(lastLine(outB)); return }
        val helper = outB.trim()
        if (rcB != 0 || helper.isBlank() || !helper.contains("gh")) { health.value = Health.HelperMissing; return }

        // Rung 3 (local): can git actually fetch the credential through the helper?
        // The password stays inside the guest shell; only the username is printed.
        val fill = """
            cred=${'$'}(printf 'protocol=https\nhost=github.com\n\n' | git credential fill 2>/dev/null)
            case "${'$'}cred" in
              *password=*) printf '%s' "${'$'}cred" | sed -n 's/^username=//p' | head -n 1 ;;
              *) echo NO_CREDENTIAL ;;
            esac
        """.trimIndent()
        val (rcC, outC) = guestBash(fill, timeoutSec = 30)
        if (rcC == -1) { health.value = Health.Unreachable(lastLine(outC)); return }
        val login = outC.lines().map { it.trim() }.firstOrNull { it.isNotEmpty() && it != "NO_CREDENTIAL" }
        health.value = when {
            rcC == 0 && login != null -> Health.Ok(login)
            isNetworkFailure(outC) -> Health.Unreachable(lastLine(outC))
            else -> Health.Broken(lastLine(outC))
        }
    }

    /**
     * Repair what can be repaired without a token: reinstall git and gh if a
     * binary vanished (the install command is gated on command -v, exactly as
     * in setup()), then re-run gh's setup-git, which is idempotent and safe to
     * repeat, then check again. A token that is gone or rejected cannot be
     * repaired here: the re-check lands on NotLoggedIn or TokenRejected and
     * the screen offers a fresh sign-in instead. Commit identity is not
     * touched; the numeric account id setup() used for the noreply email is
     * not stored anywhere.
     */
    suspend fun repair() {
        if (healthBusy) return
        healthBusy = true
        try {
            if (!Graph.paths.rootfsReady) { health.value = Health.EnvNotReady; return }
            health.value = Health.Checking
            val install = "command -v git >/dev/null && command -v gh >/dev/null || (apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends git gh ca-certificates)"
            val (rcI, outI) = guestBash(install, timeoutSec = 1200)
            if (rcI == -1) { health.value = Health.Unreachable(lastLine(outI)); return }
            if (rcI != 0) { health.value = Health.Broken(lastLine(outI)); return }
            guestBash("gh auth setup-git --hostname github.com", timeoutSec = 60)
            healthLadder()
        } finally { healthBusy = false }
    }

    /** Who commits are signed by now (from the environment's git config). */
    suspend fun gitIdentity(): Pair<String, String>? {
        val out = StringBuilder()
        val rc = runCatching { Graph.runtime.runGuest(listOf("/bin/bash", "-lc", "git config --global user.name; git config --global user.email"), timeoutSec = 30) { out.appendLine(it) } }.getOrDefault(-1)
        val l = out.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return if (rc == 0 && l.size >= 2) l[0] to l[1] else null
    }
}
