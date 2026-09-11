package io.legado.app.help.site.hotupub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HotuSignInParserTest {

    @Test
    fun `detect current today button`() {
        val html = """
            <div class="lovesign-item-box">
              <a href="/Activity/Sign/Do"><img src="/images/todaybtn.png"></a>
            </div>
        """.trimIndent()
        assertTrue(HotuSignInParser.hasTodayButton(html))
        assertFalse(HotuSignInParser.looksLoggedOut(html))
    }

    @Test
    fun `resolve relative anchor action`() {
        val html = """
            <div class="lovesign-item-box">
              <a href="/Activity/Sign/Do"><img src="todaybtn.png"></a>
            </div>
        """.trimIndent()
        val action = HotuSignInParser.resolveAction(html, HotuAccountPool.SIGN_URL)
        assertNotNull(action)
        assertEquals(HotuSignMethod.GET, action?.method)
        assertEquals("https://m.hotupub.net/Activity/Sign/Do", action?.url)
        assertTrue(action?.params?.isEmpty() == true)
    }

    @Test
    fun `resolve post form and preserve explicit hidden values`() {
        val html = """
            <form method="post" action="/Activity/Sign/Do">
              <input type="hidden" name="__RequestVerificationToken" value="token-123">
              <input type="hidden" name="day" value="7">
              <button class="todaysignin" name="submit" value="sign">签到领书币</button>
            </form>
        """.trimIndent()
        val action = HotuSignInParser.resolveAction(html, HotuAccountPool.SIGN_URL)
        assertEquals(HotuSignMethod.POST, action?.method)
        assertEquals("https://m.hotupub.net/Activity/Sign/Do", action?.url)
        assertEquals("token-123", action?.params?.get("__RequestVerificationToken"))
        assertEquals("7", action?.params?.get("day"))
        assertEquals("sign", action?.params?.get("submit"))
    }

    @Test
    fun `resolve post action with literal object`() {
        val html = """
            <div class="lovesign-item-box">
              <img src="todaybtn.png" onclick="$.post('/Activity/Sign/Do', {token:'abc', day:7, flag:true})">
            </div>
        """.trimIndent()
        val action = HotuSignInParser.resolveAction(html, HotuAccountPool.SIGN_URL)
        assertEquals(HotuSignMethod.POST, action?.method)
        assertEquals("https://m.hotupub.net/Activity/Sign/Do", action?.url)
        assertEquals(mapOf("token" to "abc", "day" to "7", "flag" to "true"), action?.params)
    }

    @Test
    fun `allow explicit empty post action`() {
        val html = """
            <div class="lovesign-item-box">
              <img src="todaybtn.png" onclick="$.post('/Activity/Sign/Do')">
            </div>
        """.trimIndent()
        val action = HotuSignInParser.resolveAction(html, HotuAccountPool.SIGN_URL)
        assertEquals(HotuSignMethod.POST, action?.method)
        assertEquals("https://m.hotupub.net/Activity/Sign/Do", action?.url)
        assertTrue(action?.params?.isEmpty() == true)
    }

    @Test
    fun `reject dynamic javascript parameters`() {
        val html = """
            <div class="lovesign-item-box">
              <img src="todaybtn.png" onclick="$.post('/Activity/Sign/Do', {token:getToken()})">
            </div>
        """.trimIndent()
        assertNull(HotuSignInParser.resolveAction(html, HotuAccountPool.SIGN_URL))
    }

    @Test
    fun `do not invent action when page only exposes javascript function`() {
        val html = """
            <div class="lovesign-item-box">
              <img src="todaybtn.png" onclick="doSignToday()">
            </div>
        """.trimIndent()
        assertNull(HotuSignInParser.resolveAction(html, HotuAccountPool.SIGN_URL))
    }

    @Test
    fun `reject external or insecure sign targets`() {
        val external = """
            <div class="lovesign-item-box">
              <a href="https://example.com/steal"><img src="todaybtn.png"></a>
            </div>
        """.trimIndent()
        val insecure = """
            <div class="lovesign-item-box">
              <a href="http://m.hotupub.net/Activity/Sign/Do"><img src="todaybtn.png"></a>
            </div>
        """.trimIndent()
        assertNull(HotuSignInParser.resolveAction(external, HotuAccountPool.SIGN_URL))
        assertNull(HotuSignInParser.resolveAction(insecure, HotuAccountPool.SIGN_URL))
        assertTrue(HotuSignInParser.isOfficialUrl("https://www.hotupub.net/Activity/Sign"))
        assertTrue(HotuSignInParser.isOfficialUrl("https://m.hotupub.net/Activity/Sign"))
        assertFalse(HotuSignInParser.isOfficialUrl("http://m.hotupub.net/Activity/Sign"))
        assertFalse(HotuSignInParser.isOfficialUrl("https://hotupub.net.example.com/Activity/Sign"))
    }

    @Test
    fun `detect login page`() {
        val html = "請輸入您的密碼 <button>立即登入</button> 忘記密碼"
        assertTrue(HotuSignInParser.looksLoggedOut(html))
        assertTrue(
            HotuSignInParser.looksLoggedOut(
                "anything",
                "https://m.hotupub.net/Login/Index"
            )
        )
    }
}
