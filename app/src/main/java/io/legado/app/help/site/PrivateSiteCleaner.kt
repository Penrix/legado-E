package io.legado.app.help.site

import android.webkit.JavascriptInterface
import io.legado.app.utils.ChineseUtils
import java.net.URI

/**
 * Personal, user-facing WebView cleanup layer.
 *
 * Keep this deliberately separate from the book-source runtime. Source requests, Rhino,
 * java.ajax and background/source WebViews must not pass through this cleaner.
 */
object PrivateSiteCleaner {

    const val JS_BRIDGE_NAME = "PenrixSite"

    private val twkanHosts = setOf("twkan.com", "www.twkan.com")

    private val twkanBlockedHostSuffixes = setOf(
        "googletagmanager.com",
        "google-analytics.com",
        "doubleclick.net",
        "connect.facebook.net",
        "facebook.com"
    )

    fun isTwkan(url: String?): Boolean = hostOf(url) in twkanHosts

    fun isTwkanChapter(url: String?): Boolean {
        if (!isTwkan(url)) return false
        val path = runCatching { URI(url).path.orEmpty() }.getOrDefault("")
        return path.startsWith("/txt/")
    }

    fun shouldApply(url: String?, sourceVerification: Boolean): Boolean {
        return !sourceVerification && isTwkan(url)
    }

    fun shouldBlockRequest(
        pageUrl: String?,
        requestUrl: String?,
        sourceVerification: Boolean
    ): Boolean {
        if (!shouldApply(pageUrl, sourceVerification)) return false
        val requestHost = hostOf(requestUrl) ?: return false
        return twkanBlockedHostSuffixes.any { suffix ->
            requestHost == suffix || requestHost.endsWith(".$suffix")
        }
    }

    fun scriptFor(url: String?, sourceVerification: Boolean): String? {
        if (!shouldApply(url, sourceVerification)) return null
        return when {
            isTwkanChapter(url) -> twkanChapterPureScript
            else -> twkanGeneralCleanupScript
        }
    }

    class Bridge {
        /** Minimal bridge: no network, file, cookie, login or source access. */
        @JavascriptInterface
        fun t2s(content: String): String = ChineseUtils.t2s(content)
    }

    private fun hostOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return runCatching { URI(url).host?.lowercase() }.getOrNull()
    }

    private val twkanGeneralCleanupScript = """
        (() => {
          document.querySelectorAll(
            'iframe[src*="facebook.com"], iframe[src*="googletagmanager.com"], .adsbygoogle, ins.adsbygoogle'
          ).forEach(el => el.remove());
        })();
    """.trimIndent()

    /**
     * TWKAN chapter pages have stable content containers used by current community sources:
     * #txtcontent0, #txtcontent and .txtnav. Build a clean shell from that content instead of
     * trying to maintain a long blacklist of every sidebar/recommendation/comment widget.
     */
    private val twkanChapterPureScript = """
        (() => {
          if (window.__penrixTwkanPureApplied) return;
          window.__penrixTwkanPureApplied = true;

          const content = document.querySelector('#txtcontent0, #txtcontent, .txtnav');
          if (!content) return;

          content.querySelectorAll('script, iframe, ins, .adsbygoogle').forEach(el => el.remove());

          const promoPatterns = [
            /[記记]住本站域名/i,
            /GOOGLE\s*搜索\s*TWKAN/i,
            /把本網(?:站)?分享到/i,
            /把本网站分享到/i,
            /台[灣湾]小說網.*twkan\.com/i,
            /台[灣湾]小说网.*twkan\.com/i
          ];

          const isPromo = text => {
            const normalized = (text || '').replace(/\s+/g, ' ').trim();
            if (!normalized || normalized.length > 180) return false;
            return promoPatterns.some(re => re.test(normalized));
          };

          Array.from(content.querySelectorAll('*')).reverse().forEach(el => {
            if (el.children.length === 0 && isPromo(el.textContent)) {
              el.remove();
            }
          });

          Array.from(content.childNodes).forEach(node => {
            if (node.nodeType === Node.TEXT_NODE && isPromo(node.textContent)) {
              node.remove();
            }
          });

          const titleNode = document.querySelector('h1');
          const bridge = window.${JS_BRIDGE_NAME};
          if (bridge && typeof bridge.t2s === 'function') {
            content.innerHTML = bridge.t2s(content.innerHTML);
            if (titleNode) titleNode.textContent = bridge.t2s(titleNode.textContent || '');
          }

          const titleText = (titleNode?.textContent || document.title || '').trim();
          const cleanContent = content.cloneNode(true);

          const navCandidates = Array.from(document.querySelectorAll('a')).filter(a => {
            const text = (a.textContent || '').replace(/\s+/g, '').trim();
            return /^(上一章|下一章|目錄|目录)$/.test(text);
          });
          const seen = new Set();
          const navLinks = navCandidates.filter(a => {
            const key = `${(a.textContent || '').trim()}|${a.href}`;
            if (seen.has(key)) return false;
            seen.add(key);
            return true;
          }).slice(-3).map(a => a.cloneNode(true));

          const style = document.createElement('style');
          style.textContent = `
            html, body { background: #fff !important; color: #222 !important; }
            body { margin: 0; padding: 0; }
            #penrix-twkan-pure { max-width: 46rem; margin: 0 auto; padding: 24px 18px 40px; }
            #penrix-twkan-pure h1 { font-size: 1.35rem; line-height: 1.5; margin: 0 0 24px; }
            #penrix-twkan-content { font-size: 1.12rem; line-height: 1.9; overflow-wrap: anywhere; }
            #penrix-twkan-content p, #penrix-twkan-content div { margin: 0 0 0.9em; }
            #penrix-twkan-content img { max-width: 100%; height: auto; }
            #penrix-twkan-nav { display: flex; justify-content: space-between; gap: 12px; margin-top: 32px; }
            #penrix-twkan-nav a { flex: 1; text-align: center; padding: 10px 6px; text-decoration: none; }
          `;

          const root = document.createElement('main');
          root.id = 'penrix-twkan-pure';
          if (titleText) {
            const h1 = document.createElement('h1');
            h1.textContent = titleText;
            root.appendChild(h1);
          }
          cleanContent.id = 'penrix-twkan-content';
          root.appendChild(cleanContent);

          if (navLinks.length) {
            const nav = document.createElement('nav');
            nav.id = 'penrix-twkan-nav';
            navLinks.forEach(a => nav.appendChild(a));
            root.appendChild(nav);
          }

          document.body.replaceChildren(root);
          document.head.appendChild(style);
          if (titleText) document.title = titleText;
        })();
    """.trimIndent()
}
