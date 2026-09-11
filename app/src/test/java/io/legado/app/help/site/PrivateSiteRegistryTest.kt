package io.legado.app.help.site

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateSiteRegistryTest {

    @Test
    fun `stable site identity is independent from rotating domain`() {
        assertEquals("diyibanzhu", PrivateSiteRegistry.profileFor("https://diyibanzhu.me/")?.id)
        assertEquals("diyibanzhu", PrivateSiteRegistry.profileFor("https://m.diyibanzhu.quest/")?.id)
        assertEquals("diyibanzhu", PrivateSiteRegistry.profileFor("https://111bz.cc/5_5379/")?.id)
        assertNull(PrivateSiteRegistry.profileFor("https://diyibanzhu.me.example.org/"))
    }

    @Test
    fun `bachashuku mirrors share one identity`() {
        assertEquals("bachashuku", PrivateSiteRegistry.profileFor("https://8xsk.com/")?.id)
        assertEquals("bachashuku", PrivateSiteRegistry.profileFor("https://8xsk.org/")?.id)
        assertEquals("bachashuku", PrivateSiteRegistry.profileFor("https://www.bachashuku.org/book/718.html")?.id)
        assertTrue(PrivateSiteRegistry.bachashuku.requiresLogin)
    }

    @Test
    fun `content kinds and access attributes are explicit`() {
        assertEquals(PrivateSiteKind.FORUM, PrivateSiteRegistry.cool18.kind)
        assertEquals(PrivateSiteKind.NAVIGATION, PrivateSiteRegistry.thePornDude.kind)
        assertTrue(PrivateSiteRegistry.uaa.requiresLogin)
        assertTrue(PrivateSiteRegistry.hotupub.requiresLogin)
        assertTrue(PrivateSiteRegistry.hotupub.usesCredits)
        assertTrue(PrivateSiteRegistry.missav.regionSensitive)
        assertTrue(PrivateSiteRegistry.uaa.adult)
        assertFalse(PrivateSiteRegistry.twkan.adult)
    }

    @Test
    fun `selected long tail novel sites are registered`() {
        assertEquals("69shuba", PrivateSiteRegistry.profileFor("https://www.69shuba.com/book/1.htm")?.id)
        assertEquals("uukan", PrivateSiteRegistry.profileFor("https://uukanshu.cc/book/example")?.id)
        assertEquals("uukan", PrivateSiteRegistry.profileFor("https://www.uukan.org/chapter/example")?.id)
        assertEquals("quanben", PrivateSiteRegistry.profileFor("https://www.quanben.io/n/example/1.html")?.id)
    }

    @Test
    fun `viral porn is intentionally not registered yet`() {
        assertNull(PrivateSiteRegistry.profileFor("https://viralporn.com/"))
    }
}
