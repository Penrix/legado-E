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

    fun isTwkan(url: String?): Boolean = PrivateSiteRegistry.twkan.matchesHost(hostOf(url))

    fun isManagedSite(url: String?): Boolean = PrivateSiteRegistry.profileFor(url) != null

    fun isTwkanChapter(url: String?): Boolean {
        if (!isTwkan(url)) return false
        val path = runCatching { URI(url).path.orEmpty() }.getOrDefault("")
        return path.startsWith("/txt/")
    }

    fun shouldApply(url: String?, sourceVerification: Boolean): Boolean {
        return !sourceVerification && isManagedSite(url)
    }

    /** The JavaScript bridge exists only for TWKAN's traditional-to-simplified conversion. */
    fun shouldInstallBridge(url: String?, sourceVerification: Boolean): Boolean {
        return !sourceVerification && isTwkan(url)
    }

    fun shouldBlockRequest(
        pageUrl: String?,
        requestUrl: String?,
        sourceVerification: Boolean
    ): Boolean {
        if (!shouldApply(pageUrl, sourceVerification)) return false
        val profile = PrivateSiteRegistry.profileFor(pageUrl) ?: return false
        val requestHost = hostOf(requestUrl) ?: return false
        return profile.blockedHostSuffixes.any { suffix ->
            requestHost == suffix || requestHost.endsWith(".$suffix")
        }
    }

    fun scriptFor(url: String?, sourceVerification: Boolean): String? {
        if (!shouldApply(url, sourceVerification)) return null
        val profile = PrivateSiteRegistry.profileFor(url) ?: return null
        return when {
            isTwkanChapter(url) -> twkanChapterPureScript
            else -> cleanupScript(profile)
        }
    }

    class Bridge {
        /** Minimal bridge: no network, file, cookie, login or source access. */
        @JavascriptInterface
        fun t2s(content: String): String = ChineseUtils.t2s(content)
    }

    private fun hostOf(url: String?): String? = PrivateSiteRegistry.hostOf(url)

    /**
     * Build cleanup JavaScript from the selected site's own profile.
     *
     * Four layers are used:
     * 1. WebView request interception blocks known third-party ad/tracker hosts before loading.
     * 2. This script removes matching DOM nodes and site-specific ad selectors.
     * 3. MutationObserver repeats cleanup for ads injected after page load.
     * 4. Click/window.open guards suppress navigation to known ad hosts without blocking normal links.
     */
    private fun cleanupScript(profile: PrivateSiteProfile): String {
        val blockedHosts = profile.blockedHostSuffixes
            .sorted()
            .joinToString(",") { jsString(it) }
        val selectors = profile.domRemoveSelectors
            .sorted()
            .joinToString(",") { jsString(it) }
        val observerKey = "__penrixCleaner_${profile.id.replace(Regex("[^A-Za-z0-9_]"), "_")}" 

        return """
            (() => {
              const blockedHostSuffixes = [$blockedHosts];
              const removeSelectors = [$selectors];

              const blockedUrl = value => {
                if (!value) return false;
                try {
                  const host = new URL(value, location.href).hostname.toLowerCase();
                  return blockedHostSuffixes.some(suffix =>
                    host === suffix || host.endsWith('.' + suffix)
                  );
                } catch (_) {
                  return false;
                }
              };

              const removeBySelector = root => {
                if (!root || !root.querySelectorAll) return;
                removeSelectors.forEach(selector => {
                  try {
                    root.querySelectorAll(selector).forEach(el => el.remove());
                  } catch (_) {}
                });
              };

              const clean = root => {
                if (!root || !root.querySelectorAll) return;
                root.querySelectorAll(
                  'iframe[src], script[src], img[src], source[src], video[poster], a[href]'
                ).forEach(el => {
                  const value = el.getAttribute('src') ||
                    el.getAttribute('href') ||
                    el.getAttribute('poster');
                  if (blockedUrl(value)) el.remove();
                });
                removeBySelector(root);
              };

              clean(document);

              if (!window.$observerKey) {
                document.addEventListener('click', event => {
                  const target = event.target;
                  const anchor = target && target.closest ? target.closest('a[href]') : null;
                  if (anchor && blockedUrl(anchor.href)) {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                  }
                }, true);

                const nativeOpen = window.open ? window.open.bind(window) : null;
                if (nativeOpen && !window.__penrixNativeWindowOpen) {
                  window.__penrixNativeWindowOpen = nativeOpen;
                  window.open = function(url, ...args) {
                    if (blockedUrl(url)) return null;
                    return nativeOpen(url, ...args);
                  };
                }

                const observer = new MutationObserver(records => {
                  records.forEach(record => {
                    if (record.type === 'attributes') {
                      const node = record.target;
                      const value = node.getAttribute?.('src') ||
                        node.getAttribute?.('href') ||
                        node.getAttribute?.('poster');
                      if (blockedUrl(value)) {
                        node.remove?.();
                        return;
                      }
                      clean(node);
                      return;
                    }
                    record.addedNodes.forEach(node => {
                      if (node.nodeType !== Node.ELEMENT_NODE) return;
                      const value = node.getAttribute?.('src') ||
                        node.getAttribute?.('href') ||
                        node.getAttribute?.('poster');
                      if (blockedUrl(value)) {
                        node.remove();
                        return;
                      }
                      clean(node);
                    });
                  });
                });
                observer.observe(document.documentElement, {
                  childList: true,
                  subtree: true,
                  attributes: true,
                  attributeFilter: ['src', 'href', 'poster']
                });
                window.$observerKey = observer;
              }
            })();
        """.trimIndent()
    }

    private fun jsString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return "\"$escaped\""
    }

    /**
     * TWKAN chapter pages have stable content containers used by current community sources:
     * #txtcontent0, #txtcontent and .txtnav. Build a clean shell from that content instead of
     * trying to maintain a long blacklist of every sidebar/recommendation/comment widget.
     */
    private val twkanChapterPureScript = """
        (() => {
          if (window.__penrixTwkanPureApplied) return;

          const content = document.querySelector('#txtcontent0, #txtcontent, .txtnav');
          if (!content) return;
          window.__penrixTwkanPureApplied = true;

          content.querySelectorAll('script, iframe, ins, .adsbygoogle').forEach(el => el.remove());

          const promoPatterns = [
            /[記记]住本站域名/i,
            /GOOGLE\s*搜索\s*TWKAN/i,
            /把本網(?:站)?分享到/i,
            /把本网站分享到/i,
            /台[灣湾]小說網.*twkan\.com/i,
            /台[灣湾]小说网.*twkan\.com/i,
            /^(?:www\.)?(?:twkan|69shux)\.com$/i
          ];

          const normalizeText = text => {
            const raw = String(text || '');
            try {
              return raw.normalize('NFKC');
            } catch (_) {
              return raw;
            }
          };

          const isPromo = text => {
            const normalized = normalizeText(text).replace(/\s+/g, ' ').trim();
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
            const key = (a.textContent || '').trim() + '|' + a.href;
            if (seen.has(key)) return false;
            seen.add(key);
            return true;
          }).slice(-3).map(a => a.cloneNode(true));

          const style = document.createElement('style');
          style.textContent = `
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
