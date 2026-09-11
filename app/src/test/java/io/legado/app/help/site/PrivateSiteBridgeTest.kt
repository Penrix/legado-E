package io.legado.app.help.site

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateSiteBridgeTest {

    @Test
    fun `only TWKAN user pages receive Android bridge`() {
        assertTrue(
            PrivateSiteCleaner.shouldInstallBridge(
                "https://twkan.com/txt/1/2",
                sourceVerification = false
            )
        )
        assertFalse(
            PrivateSiteCleaner.shouldInstallBridge(
                "https://www.xvideos.com/",
                sourceVerification = false
            )
        )
        assertFalse(
            PrivateSiteCleaner.shouldInstallBridge(
                "https://www.uaa.com/",
                sourceVerification = false
            )
        )
    }

    @Test
    fun `verification pages never receive private bridge`() {
        assertFalse(
            PrivateSiteCleaner.shouldInstallBridge(
                "https://twkan.com/txt/1/2",
                sourceVerification = true
            )
        )
    }
}
