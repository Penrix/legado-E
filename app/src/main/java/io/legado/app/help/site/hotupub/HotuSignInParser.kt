package io.legado.app.help.site.hotupub

import org.jsoup.Jsoup
import java.net.URI

enum class HotuSignMethod {
    GET,
    POST
}

data class HotuSignAction(
    val method: HotuSignMethod,
    val url: String
)

/** Pure parser for the current Hotu mobile sign-in page. */
object HotuSignInParser {

    fun looksLoggedOut(html: String, finalUrl: String? = null): Boolean {
        if (finalUrl?.contains("/Login/", ignoreCase = true) == true) return true
        return html.contains("請輸入您的密碼") ||
            html.contains("请输入您的密码") ||
            (html.contains("立即登入") && html.contains("忘記密碼"))
    }

    fun hasTodayButton(html: String): Boolean {
        val doc = Jsoup.parse(html)
        return doc.selectFirst(".lovesign-item-box img[src*=todaybtn.png]") != null ||
            doc.selectFirst(".todaysignin")?.text()?.contains("签到领書幣") == true
    }

    fun resolveAction(html: String, baseUrl: String): HotuSignAction? {
        val doc = Jsoup.parse(html, baseUrl)
        val image = doc.selectFirst(".lovesign-item-box img[src*=todaybtn.png]")
        val clickable = image?.closest("a") ?: doc.selectFirst(".todaysignin")
        if (clickable != null) {
            val href = clickable.attr("abs:href").ifBlank { clickable.attr("href") }
            normalizeUrl(href, baseUrl)?.let { return HotuSignAction(HotuSignMethod.GET, it) }
            resolveFromScript(clickable.attr("onclick"), baseUrl)?.let { return it }
        }
        image?.let {
            resolveFromScript(it.attr("onclick"), baseUrl)?.let { action -> return action }
        }
        return null
    }

    private fun resolveFromScript(script: String?, baseUrl: String): HotuSignAction? {
        if (script.isNullOrBlank()) return null
        val method = if (
            script.contains("$.post", ignoreCase = true) ||
            script.contains("type:'POST'", ignoreCase = true) ||
            script.contains("type: 'POST'", ignoreCase = true) ||
            script.contains("method:'POST'", ignoreCase = true) ||
            script.contains("method: 'POST'", ignoreCase = true)
        ) HotuSignMethod.POST else HotuSignMethod.GET

        val quotedUrl = Regex("['\"]((?:https?://|/)[^'\"]+)['\"]")
            .find(script)
            ?.groupValues
            ?.getOrNull(1)
            ?: return null
        return normalizeUrl(quotedUrl, baseUrl)?.let { HotuSignAction(method, it) }
    }

    private fun normalizeUrl(raw: String?, baseUrl: String): String? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()
        if (value.startsWith("javascript:", ignoreCase = true) || value == "#") return null
        return runCatching {
            val base = URI(baseUrl)
            base.resolve(value).toString()
        }.getOrNull()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }
}
