package io.legado.app.help.site

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateSiteAdBlockTest {

    @Test
    fun `missav blocks current third party promotions but keeps first party requests`() {
        val page = "https://missav.ws/dm252/id"
        assertTrue(
            PrivateSiteCleaner.shouldBlockRequest(
                page,
                "https://go.mayzaent.com/ad/frame",
                sourceVerification = false
            )
        )
        assertTrue(
            PrivateSiteCleaner.shouldBlockRequest(
                page,
                "https://bit.ly/example-promo",
                sourceVerification = false
            )
        )
        assertFalse(
            PrivateSiteCleaner.shouldBlockRequest(
                page,
                "https://missav.ws/video/cover.jpg",
                sourceVerification = false
            )
        )
    }

    @Test
    fun `hanime blocks erodalabs promotion without blocking its own media`() {
        val page = "https://hanime1.me/"
        assertTrue(
            PrivateSiteCleaner.shouldBlockRequest(
                page,
                "https://l.erodalabs.com/promo",
                sourceVerification = false
            )
        )
        assertFalse(
            PrivateSiteCleaner.shouldBlockRequest(
                page,
                "https://vdownload.hembed.com/video/poster.jpg",
                sourceVerification = false
            )
        )
    }

    @Test
    fun `verification bypass disables private ad filtering`() {
        assertFalse(
            PrivateSiteCleaner.shouldBlockRequest(
                "https://www.69shuba.com/",
                "https://doubleclick.net/ad",
                sourceVerification = true
            )
        )
    }

    @Test
    fun `site cleanup script includes profile specific rules`() {
        val missav = PrivateSiteCleaner.scriptFor("https://missav.ws/", false)
        val hanime = PrivateSiteCleaner.scriptFor("https://hanime1.me/", false)
        assertNotNull(missav)
        assertNotNull(hanime)
        assertTrue(missav!!.contains("mayzaent.com"))
        assertTrue(missav.contains("bit.ly"))
        assertTrue(hanime!!.contains("erodalabs.com"))
        assertTrue(missav.contains("MutationObserver"))
    }
}
