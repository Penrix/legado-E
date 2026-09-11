package io.legado.app.help.site

import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.ScriptHandler
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URLDecoder

/**
 * Full filter-engine bridge for managed private sites.
 *
 * adblock-rust is the primary network/cosmetic/scriptlet engine. PrivateSiteCleaner remains only
 * as a site-specific correction layer for known leftovers and product-specific presentation.
 */
object PrivateAdBlock {

    const val JS_BRIDGE_NAME = "PenrixAdBlock"
    private const val FILTERS_ASSET = "privateSites/adblock/filters.txt"
    private const val RESOURCES_ASSET = "privateSites/adblock/resources.json"

    @Volatile
    private var initialized = false

    @Volatile
    private var activeManagedPageUrl: String = ""

    private val nativeLoaded: Boolean = runCatching {
        System.loadLibrary("penrix_adblock")
        true
    }.getOrDefault(false)

    @JvmStatic
    private external fun nativeInitialize(filters: String, resources: String): Boolean

    @JvmStatic
    private external fun nativeIsReady(): Boolean

    @JvmStatic
    private external fun nativeNetworkDecision(
        url: String,
        sourceUrl: String,
        requestType: String,
        method: String
    ): String

    @JvmStatic
    private external fun nativeCosmeticResources(url: String): String

    @JvmStatic
    private external fun nativeHiddenSelectors(
        classesJson: String,
        idsJson: String,
        exceptionsJson: String
    ): String

    fun ensureInitialized(context: Context): Boolean {
        if (initialized) return true
        if (!nativeLoaded) return false
        synchronized(this) {
            if (initialized) return true
            initialized = runCatching {
                val filters = context.assets.open(FILTERS_ASSET).bufferedReader().use { it.readText() }
                val resources = context.assets.open(RESOURCES_ASSET).bufferedReader().use { it.readText() }
                nativeInitialize(filters, resources) && nativeIsReady()
            }.getOrDefault(false)
            return initialized
        }
    }

    fun isReady(): Boolean = initialized && nativeLoaded && runCatching { nativeIsReady() }.getOrDefault(false)

    fun setActivePage(url: String?) {
        activeManagedPageUrl = if (PrivateSiteRegistry.profileFor(url) != null) url.orEmpty() else ""
    }

    fun clearActivePage(url: String?) {
        if (url.isNullOrBlank() || activeManagedPageUrl == url) activeManagedPageUrl = ""
    }

    fun installServiceWorkerClient() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST)
        ) {
            return
        }
        runCatching {
            ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(
                object : ServiceWorkerClientCompat() {
                    override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                        val pageUrl = activeManagedPageUrl
                        if (pageUrl.isBlank()) return null
                        return intercept(pageUrl, request)
                    }
                }
            )
        }
    }

    fun installDocumentStartScript(
        webView: WebView,
        url: String?,
        sourceVerification: Boolean
    ): ScriptHandler? {
        if (sourceVerification || !isReady()) return null
        val profile = PrivateSiteRegistry.profileFor(url) ?: return null
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return null
        val origins = buildSet {
            profile.rootDomains.forEach { domain ->
                add("https://$domain")
                add("https://*.$domain")
                add("http://$domain")
                add("http://*.$domain")
            }
        }
        return runCatching {
            WebViewCompat.addDocumentStartJavaScript(webView, DOCUMENT_START_SCRIPT, origins)
        }.getOrNull()
    }

    fun intercept(pageUrl: String?, request: WebResourceRequest): WebResourceResponse? {
        if (!isReady() || PrivateSiteRegistry.profileFor(pageUrl) == null) return null
        val requestUrl = request.url?.toString().orEmpty()
        if (requestUrl.isBlank()) return null
        val requestType = inferRequestType(request)
        val decision = networkDecision(
            requestUrl,
            pageUrl.orEmpty(),
            requestType,
            request.method.orEmpty().ifBlank { "GET" }
        ) ?: return null

        if (decision.block) {
            decision.redirect?.let { redirect ->
                dataUrlResponse(redirect)?.let { return it }
            }
            return emptyResponse(requestType)
        }
        return null
    }

    fun shouldBlockNavigation(pageUrl: String?, targetUrl: String?): Boolean {
        if (!isReady() || targetUrl.isNullOrBlank() || PrivateSiteRegistry.profileFor(pageUrl) == null) {
            return false
        }
        return networkDecision(targetUrl, pageUrl.orEmpty(), "document", "GET")?.block == true
    }

    fun rewrittenNavigation(pageUrl: String?, targetUrl: String?): String? {
        if (!isReady() || targetUrl.isNullOrBlank() || PrivateSiteRegistry.profileFor(pageUrl) == null) {
            return null
        }
        val decision = networkDecision(targetUrl, pageUrl.orEmpty(), "document", "GET") ?: return null
        if (decision.block) return null
        return decision.rewrittenUrl?.takeIf { it.isNotBlank() && it != targetUrl }
    }

    private fun networkDecision(
        url: String,
        sourceUrl: String,
        requestType: String,
        method: String
    ): NetworkDecision? {
        return runCatching {
            val json = JSONObject(nativeNetworkDecision(url, sourceUrl, requestType, method))
            if (!json.has("block")) return@runCatching null
            NetworkDecision(
                block = json.optBoolean("block", false),
                redirect = json.optString("redirect").takeUnless { it.isBlank() || it == "null" },
                rewrittenUrl = json.optString("rewritten_url").takeUnless { it.isBlank() || it == "null" }
            )
        }.getOrNull()
    }

    private fun inferRequestType(request: WebResourceRequest): String {
        if (request.isForMainFrame) return "document"
        val url = request.url?.toString()?.lowercase().orEmpty()
        val accept = request.requestHeaders["Accept"]?.lowercase().orEmpty()
        return when {
            accept.contains("text/css") || url.substringBefore('?').endsWith(".css") -> "stylesheet"
            accept.contains("javascript") || url.substringBefore('?').let { it.endsWith(".js") || it.endsWith(".mjs") } -> "script"
            accept.startsWith("image/") || Regex("\\.(png|jpe?g|gif|webp|svg|avif|ico)(?:[?#]|$)").containsMatchIn(url) -> "image"
            accept.startsWith("font/") || Regex("\\.(woff2?|ttf|otf)(?:[?#]|$)").containsMatchIn(url) -> "font"
            accept.startsWith("video/") || accept.startsWith("audio/") || Regex("\\.(mp4|webm|m3u8|mp3|m4a|aac|ts)(?:[?#]|$)").containsMatchIn(url) -> "media"
            accept.contains("text/html") -> "subdocument"
            accept.contains("json") || accept.contains("xml") -> "xmlhttprequest"
            else -> "other"
        }
    }

    private fun emptyResponse(requestType: String): WebResourceResponse {
        val mime = when (requestType) {
            "script" -> "application/javascript"
            "stylesheet" -> "text/css"
            "image" -> "image/gif"
            "font" -> "font/woff2"
            "media" -> "application/octet-stream"
            else -> "text/plain"
        }
        return WebResourceResponse(mime, "utf-8", ByteArrayInputStream(ByteArray(0)))
    }

    /** adblock-rust returns redirect replacements as data: URLs. */
    private fun dataUrlResponse(dataUrl: String): WebResourceResponse? {
        if (!dataUrl.startsWith("data:")) return null
        return runCatching {
            val comma = dataUrl.indexOf(',')
            if (comma <= 5) return@runCatching null
            val metadata = dataUrl.substring(5, comma)
            val payload = dataUrl.substring(comma + 1)
            val base64 = metadata.endsWith(";base64", ignoreCase = true)
            val mime = metadata.substringBefore(';').ifBlank { "text/plain" }
            val bytes = if (base64) {
                Base64.decode(payload, Base64.DEFAULT)
            } else {
                URLDecoder.decode(payload, "UTF-8").toByteArray(Charsets.UTF_8)
            }
            WebResourceResponse(mime, if (mime.startsWith("text/") || mime.contains("javascript")) "utf-8" else null, ByteArrayInputStream(bytes))
        }.getOrNull()
    }

    class Bridge {
        @JavascriptInterface
        fun cosmeticResources(url: String): String {
            if (!isReady()) return "{}"
            return runCatching { nativeCosmeticResources(url) }.getOrDefault("{}")
        }

        @JavascriptInterface
        fun hiddenSelectors(classesJson: String, idsJson: String, exceptionsJson: String): String {
            if (!isReady()) return "[]"
            return runCatching {
                nativeHiddenSelectors(classesJson, idsJson, exceptionsJson)
            }.getOrDefault("[]")
        }
    }

    private data class NetworkDecision(
        val block: Boolean,
        val redirect: String?,
        val rewrittenUrl: String?
    )

    /**
     * Runs before page JavaScript. It applies adblock-rust's hostname cosmetics, scriptlets,
     * procedural/action rules and the generic class/id rules requested by the engine.
     */
    private val DOCUMENT_START_SCRIPT = """
        (() => {
          if (window.__penrixAdblockDocumentStart) return;
          window.__penrixAdblockDocumentStart = true;
          const bridge = window.$JS_BRIDGE_NAME;
          if (!bridge) return;

          const safeJson = (raw, fallback) => {
            try { return JSON.parse(String(raw || '')); } catch (_) { return fallback; }
          };
          const resources = safeJson(bridge.cosmeticResources(location.href), {});
          const exceptions = Array.isArray(resources.exceptions) ? resources.exceptions : [];
          let styleNode = null;
          let genericStyleNode = null;

          const ensureStyle = (id, css) => {
            if (!css) return null;
            let node = document.getElementById(id);
            if (!node) {
              node = document.createElement('style');
              node.id = id;
              (document.head || document.documentElement || document).appendChild(node);
            }
            node.textContent += css;
            return node;
          };

          const hideSelectors = Array.isArray(resources.hide_selectors) ? resources.hide_selectors : [];
          const initialCss = hideSelectors.map(s => {
            try { document.querySelector(s); return s + '{display:none!important;}'; }
            catch (_) { return ''; }
          }).join('\n');
          const installInitialCss = () => { styleNode = ensureStyle('__penrix_adblock_css', initialCss) || styleNode; };
          if (document.documentElement) installInitialCss();
          else document.addEventListener('DOMContentLoaded', installInitialCss, { once: true });

          if (resources.injected_script) {
            try { (0, eval)(String(resources.injected_script)); } catch (_) {}
          }

          const makeMatcher = raw => {
            const text = String(raw || '');
            if (text.length > 2 && text[0] === '/' && text.lastIndexOf('/') > 0) {
              const end = text.lastIndexOf('/');
              try { return value => new RegExp(text.slice(1, end), text.slice(end + 1)).test(String(value || '')); }
              catch (_) {}
            }
            return value => String(value || '').includes(text);
          };

          const unique = nodes => Array.from(new Set(nodes.filter(Boolean)));
          const query = (root, selector) => {
            try { return Array.from((root || document).querySelectorAll(selector)); } catch (_) { return []; }
          };

          const runProcedural = encoded => {
            const rule = safeJson(encoded, null);
            if (!rule || !Array.isArray(rule.selector) || !rule.selector.length) return;
            let nodes = [];
            for (let index = 0; index < rule.selector.length; index++) {
              const op = rule.selector[index] || {};
              const type = String(op.type || '');
              const arg = op.arg == null ? '' : String(op.arg);
              if (type === 'css-selector') {
                if (!nodes.length && index === 0) nodes = query(document, arg);
                else nodes = unique(nodes.flatMap(node => query(node, arg)));
              } else if (type === 'has-text') {
                const match = makeMatcher(arg);
                nodes = nodes.filter(node => match(node.textContent));
              } else if (type === 'min-text-length') {
                const min = parseInt(arg, 10) || 0;
                nodes = nodes.filter(node => String(node.textContent || '').length >= min);
              } else if (type === 'matches-path') {
                const match = makeMatcher(arg);
                if (!match(location.pathname + location.search)) nodes = [];
              } else if (type === 'matches-css' || type === 'matches-css-before' || type === 'matches-css-after') {
                const colon = arg.indexOf(':');
                if (colon < 1) { nodes = []; continue; }
                const property = arg.slice(0, colon).trim();
                const match = makeMatcher(arg.slice(colon + 1).trim());
                const pseudo = type === 'matches-css-before' ? '::before' : type === 'matches-css-after' ? '::after' : null;
                nodes = nodes.filter(node => {
                  try { return match(getComputedStyle(node, pseudo).getPropertyValue(property)); }
                  catch (_) { return false; }
                });
              } else if (type === 'matches-attr') {
                const equal = arg.indexOf('=');
                if (equal > 0) {
                  const nameMatch = makeMatcher(arg.slice(0, equal));
                  const valueMatch = makeMatcher(arg.slice(equal + 1));
                  nodes = nodes.filter(node => Array.from(node.attributes || []).some(attr => nameMatch(attr.name) && valueMatch(attr.value)));
                } else {
                  const match = makeMatcher(arg);
                  nodes = nodes.filter(node => Array.from(node.attributes || []).some(attr => match(attr.name + '=' + attr.value)));
                }
              } else if (type === 'upward') {
                const count = parseInt(arg, 10);
                nodes = unique(nodes.map(node => {
                  if (Number.isFinite(count) && count > 0) {
                    let current = node;
                    for (let i = 0; i < count && current; i++) current = current.parentElement;
                    return current;
                  }
                  try { return node.closest(arg); } catch (_) { return null; }
                }));
              } else if (type === 'xpath') {
                nodes = unique(nodes.flatMap(node => {
                  const out = [];
                  try {
                    const result = document.evaluate(arg, node, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
                    let item;
                    while ((item = result.iterateNext())) if (item.nodeType === Node.ELEMENT_NODE) out.push(item);
                  } catch (_) {}
                  return out;
                }));
              }
              if (!nodes.length) break;
            }

            const action = rule.action || null;
            nodes.forEach(node => {
              try {
                if (!action) {
                  node.style.setProperty('display', 'none', 'important');
                  return;
                }
                const type = String(action.type || '');
                const arg = action.arg == null ? '' : String(action.arg);
                if (type === 'remove') node.remove();
                else if (type === 'style') node.style.cssText += ';' + arg;
                else if (type === 'remove-attr') node.removeAttribute(arg);
                else if (type === 'remove-class') node.classList.remove(arg);
                else node.style.setProperty('display', 'none', 'important');
              } catch (_) {}
            });
          };

          const procedural = Array.isArray(resources.procedural_actions) ? resources.procedural_actions : [];
          const runAllProcedural = () => procedural.forEach(runProcedural);

          const seenClasses = new Set();
          const seenIds = new Set();
          const scan = root => {
            const nodes = [];
            if (root && root.nodeType === Node.ELEMENT_NODE) nodes.push(root);
            if (root && root.querySelectorAll) nodes.push(...root.querySelectorAll('[class],[id]'));
            const classes = [];
            const ids = [];
            nodes.forEach(node => {
              if (node.id && !seenIds.has(node.id)) { seenIds.add(node.id); ids.push(node.id); }
              if (node.classList) Array.from(node.classList).forEach(name => {
                if (name && !seenClasses.has(name)) { seenClasses.add(name); classes.push(name); }
              });
            });
            if (!classes.length && !ids.length) return;
            const selectors = safeJson(
              bridge.hiddenSelectors(JSON.stringify(classes), JSON.stringify(ids), JSON.stringify(exceptions)),
              []
            );
            if (!Array.isArray(selectors) || !selectors.length) return;
            const css = selectors.map(s => {
              try { document.querySelector(s); return s + '{display:none!important;}'; }
              catch (_) { return ''; }
            }).join('\n');
            genericStyleNode = ensureStyle('__penrix_adblock_generic_css', css) || genericStyleNode;
          };

          const startDomFiltering = () => {
            installInitialCss();
            scan(document);
            runAllProcedural();
            const target = document.documentElement;
            if (!target) return;
            const observer = new MutationObserver(records => {
              records.forEach(record => {
                if (record.type === 'attributes') scan(record.target);
                record.addedNodes.forEach(node => scan(node));
              });
              runAllProcedural();
            });
            observer.observe(target, {
              childList: true,
              subtree: true,
              attributes: true,
              attributeFilter: ['class', 'id']
            });
            window.__penrixAdblockObserver = observer;
          };

          if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', startDomFiltering, { once: true });
          } else {
            startDomFiltering();
          }
        })();
    """.trimIndent()
}
