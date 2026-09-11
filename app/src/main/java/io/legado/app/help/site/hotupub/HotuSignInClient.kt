package io.legado.app.help.site.hotupub

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Signs one Hotu account without touching Sigma's global CookieStore.
 *
 * The account Cookie is supplied explicitly per request, so multiple accounts can be processed
 * sequentially without cross-account session leakage. Redirects are followed manually and only
 * inside Hotu's HTTPS domain, preventing an account Cookie from being forwarded to a third party.
 */
class HotuSignInClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
) {

    data class Result(
        val status: HotuAccountPool.SignStatus,
        val message: String,
        val updatedCookie: String? = null
    )

    fun sign(cookie: String): Result {
        if (cookie.isBlank()) {
            return Result(HotuAccountPool.SignStatus.EXPIRED, "账号 Cookie 为空")
        }
        return runCatching { signInternal(cookie) }.getOrElse {
            Result(
                HotuAccountPool.SignStatus.FAILED,
                it.message ?: it.javaClass.simpleName
            )
        }
    }

    private fun signInternal(initialCookie: String): Result {
        val first = get(HotuAccountPool.SIGN_URL, initialCookie)
        var cookie = mergeSetCookies(initialCookie, first.setCookies)

        if (HotuSignInParser.looksLoggedOut(first.body, first.finalUrl)) {
            return Result(HotuAccountPool.SignStatus.EXPIRED, "登录态已失效", cookie)
        }
        if (!HotuSignInParser.hasTodayButton(first.body)) {
            // A missing button is not proof that the account already signed in today. The site may
            // simply have changed its markup. Do not create a false-success record.
            return Result(
                HotuAccountPool.SignStatus.UNSUPPORTED,
                "未检测到今日签到按钮，无法确认是已签到还是页面结构已变化",
                cookie
            )
        }

        val action = HotuSignInParser.resolveAction(first.body, first.finalUrl)
            ?: return Result(
                HotuAccountPool.SignStatus.UNSUPPORTED,
                "签到页仍存在今日按钮，但当前动作无法安全解析；需要更新河图适配",
                cookie
            )

        val actionResponse = when (action.method) {
            HotuSignMethod.GET -> get(action.url, cookie, action.params)
            HotuSignMethod.POST -> post(action.url, cookie, action.params)
        }
        cookie = mergeSetCookies(cookie, actionResponse.setCookies)

        val verify = get(HotuAccountPool.SIGN_URL, cookie)
        cookie = mergeSetCookies(cookie, verify.setCookies)
        if (HotuSignInParser.looksLoggedOut(verify.body, verify.finalUrl)) {
            return Result(HotuAccountPool.SignStatus.EXPIRED, "签到后登录态失效", cookie)
        }
        return if (HotuSignInParser.hasTodayButton(verify.body)) {
            Result(HotuAccountPool.SignStatus.FAILED, "签到动作已执行，但页面仍显示今日签到按钮", cookie)
        } else {
            // Here absence of the button is meaningful: this request follows a concrete sign action
            // and verifies that the signable state disappeared.
            Result(HotuAccountPool.SignStatus.SUCCESS, "签到成功", cookie)
        }
    }

    private fun get(
        url: String,
        cookie: String,
        params: Map<String, String> = emptyMap()
    ): Page {
        val target = if (params.isEmpty()) {
            url
        } else {
            url.toHttpUrl().newBuilder().apply {
                params.forEach { (key, value) -> addQueryParameter(key, value) }
            }.build().toString()
        }
        val request = requestBuilder(target, cookie).get().build()
        return execute(request, cookie)
    }

    private fun post(
        url: String,
        cookie: String,
        params: Map<String, String>
    ): Page {
        val body = FormBody.Builder().apply {
            params.forEach { (key, value) -> add(key, value) }
        }.build()
        val request = requestBuilder(url, cookie)
            .post(body)
            .build()
        return execute(request, cookie)
    }

    private fun requestBuilder(url: String, cookie: String): Request.Builder {
        require(HotuSignInParser.isOfficialUrl(url)) { "拒绝向非河图官方地址发送账号 Cookie" }
        return Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0 Mobile Safari/537.36"
            )
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .header("Referer", HotuAccountPool.SIGN_URL)
            .header("Cookie", cookie)
    }

    private fun execute(initialRequest: Request, initialCookie: String): Page {
        var request = initialRequest
        var cookie = initialCookie
        val collectedSetCookies = mutableListOf<String>()

        repeat(MAX_REDIRECTS + 1) { redirectIndex ->
            client.newCall(request).execute().use { response ->
                val responseSetCookies = response.headers("Set-Cookie")
                collectedSetCookies += responseSetCookies
                cookie = mergeSetCookies(cookie, responseSetCookies)

                if (response.code in REDIRECT_CODES) {
                    if (redirectIndex >= MAX_REDIRECTS) error("河图重定向次数过多")
                    val location = response.header("Location") ?: error("HTTP ${response.code} 缺少 Location")
                    val nextUrl = response.request.url.resolve(location)?.toString()
                        ?: error("无法解析河图重定向地址")
                    require(HotuSignInParser.isOfficialUrl(nextUrl)) {
                        "拒绝携带河图账号 Cookie 跟随站外重定向"
                    }

                    val builder = requestBuilder(nextUrl, cookie)
                    request = when (response.code) {
                        307, 308 -> builder.method(request.method, request.body).build()
                        else -> builder.get().build()
                    }
                    return@use
                }

                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("HTTP ${response.code}")
                return Page(
                    finalUrl = response.request.url.toString(),
                    body = body,
                    setCookies = collectedSetCookies.toList()
                )
            }
        }
        error("河图请求未完成")
    }

    private data class Page(
        val finalUrl: String,
        val body: String,
        val setCookies: List<String>
    )

    internal fun mergeSetCookies(cookie: String, setCookies: List<String>): String {
        if (setCookies.isEmpty()) return cookie
        val map = linkedMapOf<String, String>()
        cookie.split(';').forEach { pair ->
            val index = pair.indexOf('=')
            if (index > 0) map[pair.substring(0, index).trim()] = pair.substring(index + 1).trim()
        }
        setCookies.forEach { header ->
            val first = header.substringBefore(';')
            val index = first.indexOf('=')
            if (index > 0) {
                val name = first.substring(0, index).trim()
                val value = first.substring(index + 1).trim()
                if (value.isEmpty()) map.remove(name) else map[name] = value
            }
        }
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private companion object {
        const val MAX_REDIRECTS = 5
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
