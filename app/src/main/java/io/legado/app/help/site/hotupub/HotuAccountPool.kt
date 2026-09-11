package io.legado.app.help.site.hotupub

import android.webkit.CookieManager
import io.legado.app.help.http.CookieStore
import io.legado.app.help.site.PrivateSecretStore
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import splitties.init.appCtx
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Local account pool for HotuPub.
 *
 * Passwords are never stored. Every account contributes only its already-authenticated Cookie.
 * Metadata is stored in private preferences; Cookie values live in [PrivateSecretStore].
 */
object HotuAccountPool {

    const val SITE_URL = "https://m.hotupub.net/"
    const val WEB_SITE_URL = "https://www.hotupub.net/"
    const val SIGN_URL = "https://m.hotupub.net/Activity/Sign"

    private const val PREFS = "penrix_hotu_account_pool"
    private const val META_KEY = "accounts"
    private const val ACTIVE_KEY = "active_account_id"
    private const val COOKIE_KEY_PREFIX = "hotu_cookie_"
    private val SITE_ZONE = ZoneId.of("Asia/Taipei")

    enum class SignStatus {
        NEVER,
        SUCCESS,
        ALREADY,
        EXPIRED,
        UNSUPPORTED,
        FAILED
    }

    data class Account(
        val id: String,
        var label: String,
        var enabled: Boolean = true,
        var lastAttemptDate: String? = null,
        var lastSignDate: String? = null,
        var lastSignStatus: SignStatus = SignStatus.NEVER,
        var lastSignMessage: String? = null
    )

    private val prefs by lazy {
        appCtx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
    }

    @Synchronized
    fun accounts(): List<Account> = loadAccounts().sortedBy { it.label }

    @Synchronized
    fun activeAccount(): Account? {
        val activeId = prefs.getString(ACTIVE_KEY, null)
        val all = loadAccounts()
        return all.firstOrNull { it.id == activeId } ?: all.firstOrNull()
    }

    @Synchronized
    fun captureCurrentLogin(label: String? = null): Account? {
        val cookie = currentBrowserCookie()
        if (cookie.isBlank()) return null
        loadAccounts().firstOrNull { existing -> cookie(existing.id) == cookie }?.let { existing ->
            setActive(existing.id)
            return existing
        }
        val account = addAccount(
            label = label?.takeIf { it.isNotBlank() } ?: nextDefaultLabel(),
            cookie = cookie
        )
        setActive(account.id)
        return account
    }

    @Synchronized
    fun addAccount(label: String, cookie: String): Account {
        require(cookie.isNotBlank()) { "Cookie must not be blank" }
        val account = Account(
            id = UUID.randomUUID().toString(),
            label = label.ifBlank { nextDefaultLabel() }
        )

        // Persist the secret first. If AndroidKeyStore fails, do not leave a metadata-only ghost
        // account that can never be selected or signed in.
        PrivateSecretStore.putString(cookieKey(account.id), cookie)
        val all = loadAccounts().toMutableList().apply { add(account) }
        saveAccounts(all)

        if (prefs.getString(ACTIVE_KEY, null).isNullOrBlank()) {
            if (!setActive(account.id)) {
                all.removeAll { it.id == account.id }
                saveAccounts(all)
                PrivateSecretStore.remove(cookieKey(account.id))
                error("Unable to activate stored Hotu account")
            }
        }
        return account
    }

    @Synchronized
    fun rename(accountId: String, label: String): Boolean {
        val all = loadAccounts().toMutableList()
        val account = all.firstOrNull { it.id == accountId } ?: return false
        account.label = label.ifBlank { account.label }
        saveAccounts(all)
        return true
    }

    @Synchronized
    fun setEnabled(accountId: String, enabled: Boolean): Boolean {
        val all = loadAccounts().toMutableList()
        val account = all.firstOrNull { it.id == accountId } ?: return false
        account.enabled = enabled
        saveAccounts(all)
        return true
    }

    @Synchronized
    fun remove(accountId: String): Boolean {
        val all = loadAccounts().toMutableList()
        val removed = all.removeAll { it.id == accountId }
        if (!removed) return false
        saveAccounts(all)
        PrivateSecretStore.remove(cookieKey(accountId))

        val active = prefs.getString(ACTIVE_KEY, null)
        if (active == accountId) {
            val replacement = all.firstOrNull { !cookie(it.id).isNullOrBlank() }
            if (replacement == null || !setActive(replacement.id)) {
                prefs.edit().remove(ACTIVE_KEY).apply()
                clearHotuSession()
            }
        }
        return true
    }

    @Synchronized
    fun setActive(accountId: String): Boolean {
        if (loadAccounts().none { it.id == accountId }) return false
        // Apply first, then publish the pointer. A corrupted/missing secret must not make the App
        // claim that an unusable account is active.
        if (!applyAccountCookie(accountId)) return false
        prefs.edit().putString(ACTIVE_KEY, accountId).apply()
        return true
    }

    @Synchronized
    fun cookie(accountId: String): String? {
        return PrivateSecretStore.getString(cookieKey(accountId))
    }

    @Synchronized
    fun updateCookie(accountId: String, cookie: String) {
        if (cookie.isBlank()) return
        PrivateSecretStore.putString(cookieKey(accountId), cookie)
        if (prefs.getString(ACTIVE_KEY, null) == accountId) {
            applyAccountCookie(accountId)
        }
    }

    @Synchronized
    fun markSignResult(
        accountId: String,
        status: SignStatus,
        message: String? = null,
        date: LocalDate = siteToday()
    ) {
        val all = loadAccounts().toMutableList()
        val account = all.firstOrNull { it.id == accountId } ?: return
        account.lastAttemptDate = date.toString()
        if (status == SignStatus.SUCCESS || status == SignStatus.ALREADY) {
            account.lastSignDate = date.toString()
        }
        account.lastSignStatus = status
        account.lastSignMessage = message
        saveAccounts(all)
    }

    fun siteZone(): ZoneId = SITE_ZONE

    fun siteToday(): LocalDate = LocalDate.now(SITE_ZONE)

    fun isDue(account: Account, date: LocalDate = siteToday()): Boolean {
        return account.enabled && account.lastAttemptDate != date.toString()
    }

    @Synchronized
    fun applyActiveCookie(): Boolean {
        val account = activeAccount() ?: return false
        if (!applyAccountCookie(account.id)) return false
        prefs.edit().putString(ACTIVE_KEY, account.id).apply()
        return true
    }

    private fun applyAccountCookie(accountId: String): Boolean {
        val cookie = cookie(accountId)?.trim().orEmpty()
        if (cookie.isBlank()) return false

        // CookieStore.setWebCookie() clears all WebView session cookies globally, which would log
        // the user out of UAA/video sites. Clear only Hotu's own domain, then install this account
        // on both official hosts.
        CookieStore.removeCookie(WEB_SITE_URL)
        CookieStore.setCookie(WEB_SITE_URL, cookie)

        val manager = CookieManager.getInstance()
        cookie.split(';')
            .map { it.trim() }
            .filter { it.contains('=') }
            .forEach { pair ->
                manager.setCookie(WEB_SITE_URL, pair)
                manager.setCookie(SITE_URL, pair)
            }
        manager.flush()
        return true
    }

    private fun clearHotuSession() {
        // CookieStore.removeCookie uses a host/domain-scoped WebView cleanup; it does not call the
        // global removeSessionCookies(null) used by setWebCookie().
        CookieStore.removeCookie(WEB_SITE_URL)
        CookieStore.removeCookie(SITE_URL)
        CookieManager.getInstance().flush()
    }

    private fun currentBrowserCookie(): String {
        // The App's Hotu entry and Pure source use www.hotupub.net, so a freshly logged-in www
        // session is authoritative. Falling back to m is useful when the user logged in there.
        val web = CookieStore.getCookie(WEB_SITE_URL).trim()
        if (web.isNotBlank()) return web
        return CookieStore.getCookie(SITE_URL).trim()
    }

    private fun loadAccounts(): List<Account> {
        val raw = prefs.getString(META_KEY, null) ?: return emptyList()
        return GSON.fromJsonArray<Account>(raw).getOrNull() ?: emptyList()
    }

    private fun saveAccounts(accounts: List<Account>) {
        prefs.edit().putString(META_KEY, GSON.toJson(accounts)).apply()
    }

    private fun nextDefaultLabel(): String {
        val used = loadAccounts().map { it.label }.toSet()
        var index = 1
        while ("河图账号 $index" in used) index++
        return "河图账号 $index"
    }

    private fun cookieKey(accountId: String) = COOKIE_KEY_PREFIX + accountId
}
