package io.legado.app.help.site.hotupub

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Signs one Hotu account without touching Sigma's global CookieStore.
 *
 * The account Cookie is supplied explicitly per request, so multiple accounts can be processed
 * sequentially without cross-account session leakage.
 */
class HotuSignInClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
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
            return Result(HotuAccountPool.SignStatus.ALREADY, "今日已签到或签到按钮不可见", cookie)
        }

        val action = HotuSignInParser.resolveAction(first.body, first.finalUrl)
            ?: return Result(
                HotuAccountPool.SignStatus.UNSUPPORTED,
                "签到页仍存在今日按钮，但当前动作无法安全解析；需要更新河图适配",
                cookie
            )

        val actionResponse = when (action.method) {
            HotuSignMethod.GET -> get(action.url, cookie)
            HotuSignMethod.POST -> post(action.url, cookie)
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
            Result(HotuAccountPool.SignStatus.SUCCESS, "签到成功", cookie)
        }
    }

    private fun get(url: String, cookie: String): Page {
        val request = requestBuilder(url, cookie).get().build()
        return execute(request)
    }

    private fun post(url: String, cookie: String): Page {
        val request = requestBuilder(url, cookie)
            .post(FormBody.Builder().build())
            .build()
        return execute(request)
    }

    private fun requestBuilder(url: String, cookie: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0 Mobile Safari/537.36"
            )
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .header("Referer", "https://m.hotupub.net/")
            .header("Cookie", cookie)
    }

    private fun execute(request: Request): Page {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful && response.code !in 300..399) {
                error("HTTP ${response.code}")
            }
            return Page(
                finalUrl = response.request.url.toString(),
                body = body,
                setCookies = response.headers("Set-Cookie")
            )
        }
    }

    private data class Page(
        val finalUrl: String,
        val body: String,
        val setCookies: List<String>
    )

    private fun mergeSetCookies(cookie: String, setCookies: List<String>): String {
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
}
