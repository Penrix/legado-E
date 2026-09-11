package io.legado.app.help.site

import java.net.URI

/**
 * Domain selection helpers for built-in private sites.
 *
 * The stable identity is [PrivateSiteProfile.id], never a hostname. A site may expose more
 * than one known entry point and rotating-domain sites may learn additional candidates later.
 * Learned candidates are deliberately not trusted here: callers must verify the returned page
 * with [fingerprintMatches] before persisting or preferring a new domain.
 */
object PrivateSiteDomainResolver {

    fun candidateUrls(
        profile: PrivateSiteProfile,
        preferredUrl: String? = null,
        learnedUrls: Collection<String> = emptyList()
    ): List<String> {
        return buildList {
            normalizeHttpsUrl(preferredUrl)?.let(::add)
            learnedUrls.mapNotNull(::normalizeHttpsUrl).forEach(::add)
            profile.entryUrls.mapNotNull(::normalizeHttpsUrl).forEach(::add)
        }.distinct()
    }

    fun fingerprintMatches(
        profile: PrivateSiteProfile,
        pageTitle: String?,
        pageText: String?
    ): Boolean {
        if (profile.fingerprintTerms.isEmpty()) return true
        val haystack = buildString {
            append(pageTitle.orEmpty())
            append('\n')
            append(pageText.orEmpty())
        }
        return profile.fingerprintTerms.any { haystack.contains(it, ignoreCase = true) }
    }

    fun rootUrl(url: String?): String? {
        val normalized = normalizeHttpsUrl(url) ?: return null
        return runCatching {
            val uri = URI(normalized)
            "${uri.scheme}://${uri.host.lowercase()}/"
        }.getOrNull()
    }

    private fun normalizeHttpsUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            val uri = URI(url.trim())
            if (!uri.scheme.equals("https", ignoreCase = true)) return@runCatching null
            val host = uri.host?.lowercase() ?: return@runCatching null
            URI("https", uri.userInfo, host, uri.port, uri.path.ifBlank { "/" }, uri.query, null)
                .toString()
        }.getOrNull()
    }
}
