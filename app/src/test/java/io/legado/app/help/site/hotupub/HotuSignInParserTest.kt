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
    }

    @Test
    fun `resolve post action from onclick`() {
        val html = """
            <div class="lovesign-item-box">
              <img src="todaybtn.png" onclick="$.post('/Activity/Sign/Do')">
            </div>
        """.trimIndent()
        val action = HotuSignInParser.resolveAction(html, HotuAccountPool.SIGN_URL)
        assertEquals(HotuSignMethod.POST, action?.method)
        assertEquals("https://m.hotupub.net/Activity/Sign/Do", action?.url)
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
