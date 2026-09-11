package io.legado.app.help.site

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateSiteCleanerTest {

    @Test
    fun `matches only real TWKAN hosts`() {
        assertTrue(PrivateSiteCleaner.isTwkan("https://twkan.com/txt/93323/58783896"))
        assertTrue(PrivateSiteCleaner.isTwkan("https://www.twkan.com/book/93323.html"))
        assertFalse(PrivateSiteCleaner.isTwkan("https://twkan.com.example.org/txt/1/2"))
        assertFalse(PrivateSiteCleaner.isTwkan("https://example.org/?next=https://twkan.com"))
    }

    @Test
    fun `recognizes chapter pages`() {
        assertTrue(PrivateSiteCleaner.isTwkanChapter("https://twkan.com/txt/93323/58783896"))
        assertFalse(PrivateSiteCleaner.isTwkanChapter("https://twkan.com/book/93323.html"))
    }

    @Test
    fun `never applies during source verification`() {
        val url = "https://twkan.com/txt/93323/58783896"
        assertFalse(PrivateSiteCleaner.shouldApply(url, sourceVerification = true))
        assertNull(PrivateSiteCleaner.scriptFor(url, sourceVerification = true))
        assertFalse(
            PrivateSiteCleaner.shouldBlockRequest(
                url,
                "https://www.googletagmanager.com/gtm.js",
                sourceVerification = true
            )
        )
    }

    @Test
    fun `blocks known TWKAN third party ad and tracking hosts only in user browser`() {
        val page = "https://twkan.com/txt/93323/58783896"
        assertTrue(
            PrivateSiteCleaner.shouldBlockRequest(
                page,
                "https://www.googletagmanager.com/gtm.js?id=GTM-TEST",
                sourceVerification = false
            )
        )
        assertTrue(
            PrivateSiteCleaner.shouldBlockRequest(
                page,
                "https://connect.facebook.net/zh_TW/sdk.js",
                sourceVerification = false
            )
        )
        assertFalse(
            PrivateSiteCleaner.shouldBlockRequest(
                "https://example.org/",
                "https://www.googletagmanager.com/gtm.js",
                sourceVerification = false
            )
        )
        assertFalse(
            PrivateSiteCleaner.shouldBlockRequest(
                page,
                "https://twkan.com/static/site.js",
                sourceVerification = false
            )
        )
    }

    @Test
    fun `chapter pages receive pure reading script with unicode normalization`() {
        val script = requireNotNull(
            PrivateSiteCleaner.scriptFor(
                "https://twkan.com/txt/93323/58783896",
                sourceVerification = false
            )
        )
        assertTrue(script.contains("normalize('NFKC')"))
        assertTrue(script.contains("69shux"))
    }
}
