package io.legado.app.help.site

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateSiteDomainResolverTest {

    @Test
    fun `candidate URLs prefer local preference then learned then bundled fallbacks`() {
        val urls = PrivateSiteDomainResolver.candidateUrls(
            profile = PrivateSiteRegistry.diyibanzhu,
            preferredUrl = "https://m.diyibanzhu.quest/",
            learnedUrls = listOf("https://new.example/path", "http://insecure.example/")
        )

        assertEquals("https://m.diyibanzhu.quest/", urls.first())
        assertTrue(urls.contains("https://new.example/path"))
        assertFalse(urls.any { it.startsWith("http://") })
        assertTrue(urls.contains("https://diyibanzhu.me/"))
        assertTrue(urls.contains("https://111bz.cc/"))
    }

    @Test
    fun `rotating domain must match site fingerprint before trust`() {
        val profile = PrivateSiteRegistry.diyibanzhu
        assertTrue(
            PrivateSiteDomainResolver.fingerprintMatches(
                profile,
                pageTitle = "第一版主小说网",
                pageText = "欢迎阅读"
            )
        )
        assertFalse(
            PrivateSiteDomainResolver.fingerprintMatches(
                profile,
                pageTitle = "Domain for sale",
                pageText = "Buy this domain"
            )
        )
    }

    @Test
    fun `root URL drops path query and fragment`() {
        assertEquals(
            "https://www.uaa.com/",
            PrivateSiteDomainResolver.rootUrl("https://www.uaa.com/novel/123?from=test#chapter")
        )
    }
}
