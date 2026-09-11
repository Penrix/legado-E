package io.legado.app.help.site

import java.net.URI

enum class PrivateSiteKind {
    NOVEL,
    VIDEO,
    NAVIGATION
}

data class PrivateSiteProfile(
    val id: String,
    val displayName: String,
    val startUrl: String,
    val kind: PrivateSiteKind,
    val rootDomains: Set<String>,
    val blockedHostSuffixes: Set<String> = emptySet()
) {
    fun matchesHost(host: String?): Boolean {
        val normalized = host?.lowercase() ?: return false
        return rootDomains.any { root ->
            normalized == root || normalized.endsWith(".$root")
        }
    }
}

/**
 * Stable identities for the user's built-in private sites.
 *
 * A site identity is deliberately not tied to one exact hostname so regional/language
 * subdomains can share one profile. Frequently rotating domains should be handled by a
 * separate domain-resolution layer instead of being hard-coded here.
 */
object PrivateSiteRegistry {

    private val commonTrackerHosts = setOf(
        "googletagmanager.com",
        "google-analytics.com",
        "doubleclick.net",
        "googlesyndication.com"
    )

    private val commonAdultAdHosts = setOf(
        "trafficjunky.net",
        "exoclick.com",
        "exosrv.com",
        "juicyads.com",
        "popads.net",
        "popcash.net"
    )

    val twkan = PrivateSiteProfile(
        id = "twkan",
        displayName = "TWKAN",
        startUrl = "https://twkan.com/",
        kind = PrivateSiteKind.NOVEL,
        rootDomains = setOf("twkan.com"),
        blockedHostSuffixes = commonTrackerHosts + setOf(
            "connect.facebook.net",
            "facebook.com"
        )
    )

    val hanime1 = PrivateSiteProfile(
        id = "hanime1",
        displayName = "Hanime1",
        startUrl = "https://hanime1.me/",
        kind = PrivateSiteKind.VIDEO,
        rootDomains = setOf("hanime1.me"),
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts
    )

    val pornhub = PrivateSiteProfile(
        id = "pornhub",
        displayName = "Pornhub",
        startUrl = "https://jp.pornhub.com/",
        kind = PrivateSiteKind.VIDEO,
        rootDomains = setOf("pornhub.com"),
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts
    )

    val xvideos = PrivateSiteProfile(
        id = "xvideos",
        displayName = "XVideos",
        startUrl = "https://www.xvideos.com/",
        kind = PrivateSiteKind.VIDEO,
        rootDomains = setOf("xvideos.com"),
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts
    )

    val missav = PrivateSiteProfile(
        id = "missav",
        displayName = "MissAV",
        startUrl = "https://missav.ws/",
        kind = PrivateSiteKind.VIDEO,
        rootDomains = setOf("missav.ws"),
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts
    )

    val profiles: List<PrivateSiteProfile> = listOf(
        twkan,
        hanime1,
        pornhub,
        xvideos,
        missav
    )

    fun profileFor(url: String?): PrivateSiteProfile? {
        val host = hostOf(url) ?: return null
        return profiles.firstOrNull { it.matchesHost(host) }
    }

    fun hostOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return runCatching { URI(url).host?.lowercase() }.getOrNull()
    }
}
