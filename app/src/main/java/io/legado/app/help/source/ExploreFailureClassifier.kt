package io.legado.app.help.source

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Turns opaque explore failures into maintenance-oriented messages. */
object ExploreFailureClassifier {

    fun fromThrowable(throwable: Throwable): String {
        val causes = generateSequence<Throwable?>(throwable) { it.cause }.filterNotNull().toList()
        val combined = causes.joinToString(" | ") { it.message.orEmpty() }
        val lower = combined.lowercase()

        return when {
            causes.any { it is UnknownHostException } ->
                "发现失败：域名解析失败。这个书源的发现域名可能已更换或失效。"

            causes.any { it is SocketTimeoutException } ->
                "发现失败：站点响应超时。Sigma 已等待约 60 秒，可能是站点过慢、被反爬拦截或发现接口失效。"

            causes.any { it is SSLException } ->
                "发现失败：TLS/证书连接异常。可能是站点证书、代理链或旧域名的问题。"

            "cloudflare" in lower || "just a moment" in lower ||
                "challenge" in lower || "turnstile" in lower || "captcha" in lower ->
                "发现失败：站点要求 Cloudflare/验证码验证。需要先完成验证或给该源增加验证回退。"

            Regex("(?:http\\s*)?403|forbidden").containsMatchIn(lower) ->
                "发现失败：HTTP 403。站点拒绝了当前请求，通常与 UA、Cookie、Referer、地区或反爬有关。"

            Regex("(?:http\\s*)?429|too many requests").containsMatchIn(lower) ->
                "发现失败：HTTP 429。请求被限流，稍后重试或降低请求频率。"

            causes.any { it is IOException } ->
                "发现失败：网络请求没有正常完成。${shortReason(throwable)}"

            else -> "发现加载失败：${shortReason(throwable)}"
        }
    }

    fun emptyFirstPage(sourceName: String): String {
        return "${sourceName}：请求已完成，但没有解析到任何书籍。网络可能正常，优先怀疑发现页面结构或 ruleExplore 已失效。"
    }

    private fun shortReason(throwable: Throwable): String {
        return throwable.localizedMessage
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.take(180)
            ?: throwable::class.java.simpleName
    }
}
