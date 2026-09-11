package io.legado.app.help.source

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ExploreFailureClassifierTest {

    @Test
    fun unknownHostIsReportedAsDomainFailure() {
        val message = ExploreFailureClassifier.fromThrowable(
            UnknownHostException("old-domain.example")
        )
        assertTrue(message.contains("域名解析失败"))
    }

    @Test
    fun timeoutIsReportedSeparately() {
        val message = ExploreFailureClassifier.fromThrowable(
            SocketTimeoutException("timeout")
        )
        assertTrue(message.contains("响应超时"))
    }

    @Test
    fun forbiddenIsNotReportedAsGenericNetworkFailure() {
        val message = ExploreFailureClassifier.fromThrowable(
            IllegalStateException("HTTP 403 Forbidden")
        )
        assertTrue(message.contains("HTTP 403"))
    }

    @Test
    fun emptyResultExplainsParserFailurePossibility() {
        val message = ExploreFailureClassifier.emptyFirstPage("测试源")
        assertTrue(message.contains("网络可能正常"))
        assertTrue(message.contains("ruleExplore"))
    }
}
