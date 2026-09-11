package io.legado.app.help.site.hotupub

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI

enum class HotuSignMethod {
    GET,
    POST
}

data class HotuSignAction(
    val method: HotuSignMethod,
    val url: String,
    val params: Map<String, String> = emptyMap()
)

/**
 * Pure parser for the current Hotu mobile sign-in page.
 *
 * Only actions explicitly exposed by the page are accepted. We never execute arbitrary page JS,
 * evaluate dynamic expressions or send an account Cookie outside Hotu's HTTPS domain.
 */
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
            doc.selectFirst(".todaysignin")?.text()?.let {
                it.contains("签到领書幣") || it.contains("簽到領書幣") || it.contains("签到领书币")
            } == true
    }

    fun resolveAction(html: String, baseUrl: String): HotuSignAction? {
        val doc = Jsoup.parse(html, baseUrl)
        val image = doc.selectFirst(".lovesign-item-box img[src*=todaybtn.png]")
        val today = doc.selectFirst(".todaysignin")
        val clickable = image?.closest("a,button,input") ?: today
        val trigger = clickable ?: image ?: today ?: return null

        resolveForm(trigger, baseUrl)?.let { return it }

        if (clickable != null && clickable.tagName().equals("a", ignoreCase = true)) {
            val href = clickable.attr("abs:href").ifBlank { clickable.attr("href") }
            normalizeOfficialUrl(href, baseUrl)?.let {
                return HotuSignAction(HotuSignMethod.GET, it)
            }
        }

        sequenceOf(clickable, image, today)
            .filterNotNull()
            .mapNotNull { resolveFromScript(it.attr("onclick"), baseUrl) }
            .firstOrNull()
            ?.let { return it }

        return null
    }

    fun isOfficialUrl(url: String): Boolean {
        return runCatching {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: return@runCatching false
            uri.scheme.equals("https", ignoreCase = true) &&
                (host == "hotupub.net" || host.endsWith(".hotupub.net"))
        }.getOrDefault(false)
    }

    private fun resolveForm(trigger: Element, baseUrl: String): HotuSignAction? {
        val form = if (trigger.tagName().equals("form", ignoreCase = true)) {
            trigger
        } else {
            trigger.closest("form")
        } ?: return null

        val rawAction = form.attr("abs:action")
            .ifBlank { form.attr("action") }
            .ifBlank { baseUrl }
        val url = normalizeOfficialUrl(rawAction, baseUrl) ?: return null
        val method = if (form.attr("method").equals("post", ignoreCase = true)) {
            HotuSignMethod.POST
        } else {
            HotuSignMethod.GET
        }

        val params = linkedMapOf<String, String>()
        form.select("input[name]").forEach { input ->
            if (input.hasAttr("disabled")) return@forEach
            val type = input.attr("type").lowercase()
            if ((type == "checkbox" || type == "radio") && !input.hasAttr("checked")) {
                return@forEach
            }
            if (type in setOf("submit", "button", "image", "reset", "file")) {
                return@forEach
            }
            val name = input.attr("name").trim()
            if (name.isNotEmpty()) params[name] = input.attr("value")
        }
        form.select("textarea[name]").forEach { area ->
            if (!area.hasAttr("disabled")) {
                params[area.attr("name")] = area.text()
            }
        }
        form.select("select[name]").forEach { select ->
            if (select.hasAttr("disabled")) return@forEach
            val option = select.selectFirst("option[selected]") ?: select.selectFirst("option")
            option?.let { params[select.attr("name")] = it.attr("value").ifBlank { it.text() } }
        }

        val submit = when {
            trigger.tagName().equals("button", ignoreCase = true) -> trigger
            trigger.tagName().equals("input", ignoreCase = true) -> trigger
            else -> trigger.closest("button,input[type=submit]")
        }
        submit?.attr("name")?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            params[name] = submit.attr("value")
        }

        return HotuSignAction(method, url, params)
    }

    private fun resolveFromScript(script: String?, baseUrl: String): HotuSignAction? {
        if (script.isNullOrBlank()) return null
        val text = script.trim()
        val call = Regex(
            """(?is)\$\s*\.\s*(get|post)\s*\(\s*(['\"])(.*?)\2(.*)\)\s*;?\s*$"""
        ).matchEntire(text) ?: return null

        val method = if (call.groupValues[1].equals("post", ignoreCase = true)) {
            HotuSignMethod.POST
        } else {
            HotuSignMethod.GET
        }
        val url = normalizeOfficialUrl(call.groupValues[3], baseUrl) ?: return null
        val tail = call.groupValues[4].trim()
        if (tail.isEmpty()) return HotuSignAction(method, url)
        if (!tail.startsWith(',')) return null

        val args = tail.removePrefix(",").trim()
        if (args.isEmpty()) return HotuSignAction(method, url)
        if (!args.startsWith("{")) return null
        val close = findLiteralObjectEnd(args) ?: return null
        val objectText = args.substring(0, close + 1)
        val trailing = args.substring(close + 1).trim()
        if (trailing.isNotEmpty() && !trailing.startsWith(',')) return null
        val params = parseLiteralObject(objectText) ?: return null
        return HotuSignAction(method, url, params)
    }

    private fun findLiteralObjectEnd(text: String): Int? {
        var quote: Char? = null
        var escaped = false
        for (index in 1 until text.length) {
            val ch = text[index]
            if (quote != null) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == quote) {
                    quote = null
                }
                continue
            }
            when (ch) {
                '\'', '"' -> quote = ch
                '{' -> return null // nested/dynamic objects are deliberately unsupported
                '}' -> return index
            }
        }
        return null
    }

    private fun parseLiteralObject(text: String): Map<String, String>? {
        if (!text.startsWith('{') || !text.endsWith('}')) return null
        val body = text.substring(1, text.length - 1).trim()
        if (body.isEmpty()) return emptyMap()

        val result = linkedMapOf<String, String>()
        var index = 0
        while (index < body.length) {
            while (index < body.length && body[index].isWhitespace()) index++
            val keyRead = readLiteralKey(body, index) ?: return null
            val key = keyRead.first
            index = keyRead.second
            while (index < body.length && body[index].isWhitespace()) index++
            if (index >= body.length || body[index] != ':') return null
            index++
            while (index < body.length && body[index].isWhitespace()) index++
            val valueRead = readLiteralValue(body, index) ?: return null
            result[key] = valueRead.first
            index = valueRead.second
            while (index < body.length && body[index].isWhitespace()) index++
            if (index == body.length) break
            if (body[index] != ',') return null
            index++
        }
        return result
    }

    private fun readLiteralKey(text: String, start: Int): Pair<String, Int>? {
        if (start >= text.length) return null
        val quote = text[start].takeIf { it == '\'' || it == '"' }
        if (quote != null) return readQuoted(text, start)
        var end = start
        while (end < text.length && (text[end].isLetterOrDigit() || text[end] in "_-$")) end++
        if (end == start) return null
        return text.substring(start, end) to end
    }

    private fun readLiteralValue(text: String, start: Int): Pair<String, Int>? {
        if (start >= text.length) return null
        if (text[start] == '\'' || text[start] == '"') return readQuoted(text, start)
        val token = Regex("-?\\d+(?:\\.\\d+)?|true|false|null")
            .find(text, start)
            ?.takeIf { it.range.first == start }
            ?: return null
        return token.value to (token.range.last + 1)
    }

    private fun readQuoted(text: String, start: Int): Pair<String, Int>? {
        val quote = text[start]
        val out = StringBuilder()
        var escaped = false
        var index = start + 1
        while (index < text.length) {
            val ch = text[index]
            if (escaped) {
                // Keep only simple escaped literal characters. Dynamic escape expressions are not
                // evaluated; this is intentionally a tiny data parser, not a JavaScript engine.
                out.append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == quote) {
                return out.toString() to (index + 1)
            } else {
                out.append(ch)
            }
            index++
        }
        return null
    }

    private fun normalizeOfficialUrl(raw: String?, baseUrl: String): String? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()
        if (value.startsWith("javascript:", ignoreCase = true) || value == "#") return null
        return runCatching {
            URI(baseUrl).resolve(value).toString()
        }.getOrNull()?.takeIf(::isOfficialUrl)
    }
}
