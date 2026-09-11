package io.legado.app.help.site

import java.net.URI

enum class PrivateSiteKind {
    NOVEL,
    FORUM,
    VIDEO,
    NAVIGATION
}

data class PrivateSiteProfile(
    val id: String,
    val displayName: String,
    val startUrl: String,
    val kind: PrivateSiteKind,
    val rootDomains: Set<String>,
    val entryUrls: Set<String> = setOf(startUrl),
    val fingerprintTerms: Set<String> = emptySet(),
    val adult: Boolean = false,
    val requiresLogin: Boolean = false,
    val usesCredits: Boolean = false,
    val regionSensitive: Boolean = false,
    val blockedHostSuffixes: Set<String> = emptySet(),
    val domRemoveSelectors: Set<String> = emptySet()
) {
    fun matchesHost(host: String?): Boolean {
        val normalized = host?.lowercase() ?: return false
        return rootDomains.any { root ->
            normalized == root || normalized.endsWith(".$root")
        }
    }
}

/** Stable identities and presentation-only cleanup rules for selected private sites. */
object PrivateSiteRegistry {

    private val commonTrackerHosts = setOf(
        "googletagmanager.com",
        "google-analytics.com",
        "doubleclick.net",
        "googlesyndication.com"
    )

    private val commonAdultAdHosts = setOf(
        "trafficjunky.net",
        "trafficfactory.biz",
        "trafficfactory.com",
        "exoclick.com",
        "exosrv.com",
        "juicyads.com",
        "plugrush.com",
        "ero-advertising.com",
        "hilltopads.net",
        "propellerads.com",
        "onclicka.com",
        "adsterra.com",
        "monetag.com",
        "tsyndicate.com",
        "craktraffic.com",
        "adnium.com",
        "popads.net",
        "popcash.net"
    )

    private val commonAdSelectors = setOf(
        "ins.adsbygoogle",
        ".adsbygoogle",
        "[data-ad-client]",
        "[data-ad-slot]",
        "[id^='google_ads_']",
        "iframe[name^='google_ads']"
    )

    private val adultVideoOverlaySelectors = setOf(
        "[class*='popunder']",
        "[id*='popunder']",
        "[class*='popup-ad']",
        "[id*='popup-ad']",
        "[class*='ad-overlay']",
        "[id*='ad-overlay']"
    )

    val twkan = PrivateSiteProfile(
        id = "twkan",
        displayName = "TWKAN",
        startUrl = "https://twkan.com/?t=1",
        kind = PrivateSiteKind.NOVEL,
        rootDomains = setOf("twkan.com"),
        fingerprintTerms = setOf("TWKAN", "台湾小说", "台灣小說"),
        blockedHostSuffixes = commonTrackerHosts + setOf(
            "connect.facebook.net",
            "facebook.com"
        ),
        domRemoveSelectors = commonAdSelectors
    )

    val shuba69 = PrivateSiteProfile(
        id = "69shuba",
        displayName = "69书吧",
        startUrl = "https://www.69shuba.com/",
        kind = PrivateSiteKind.NOVEL,
        rootDomains = setOf("69shuba.com"),
        fingerprintTerms = setOf("69书吧"),
        blockedHostSuffixes = commonTrackerHosts,
        domRemoveSelectors = commonAdSelectors
    )

    val uukan = PrivateSiteProfile(
        id = "uukan",
        displayName = "UU看书",
        startUrl = "https://uukanshu.cc/",
        kind = PrivateSiteKind.NOVEL,
        rootDomains = setOf("uukanshu.cc", "uukan.org"),
        entryUrls = setOf("https://uukanshu.cc/", "https://www.uukan.org/"),
        fingerprintTerms = setOf("UU看书"),
        blockedHostSuffixes = commonTrackerHosts,
        domRemoveSelectors = commonAdSelectors
    )

    val quanben = PrivateSiteProfile(
        id = "quanben",
        displayName = "全本小说网",
        startUrl = "https://www.quanben.io/",
        kind = PrivateSiteKind.NOVEL,
        rootDomains = setOf("quanben.io"),
        fingerprintTerms = setOf("全本"),
        blockedHostSuffixes = commonTrackerHosts,
        domRemoveSelectors = commonAdSelectors
    )

    val diyibanzhu = PrivateSiteProfile(
        id = "diyibanzhu",
        displayName = "第一版主",
        startUrl = "https://m.diyibanzhu.me/",
        kind = PrivateSiteKind.NOVEL,
        rootDomains = setOf(
            "diyibanzhu.me",
            "diyibanzhu.quest",
            "111bz.cc"
        ),
        entryUrls = setOf(
            "https://m.diyibanzhu.me/",
            "https://diyibanzhu.me/",
            "https://diyibanzhu.quest/",
            "https://111bz.cc/"
        ),
        fingerprintTerms = setOf("第一版主"),
        adult = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts + setOf(
            "5mgrgsc.cn",
            "6uzxtlv.cn",
            "holmesmind.com"
        ),
        domRemoveSelectors = commonAdSelectors + setOf(
            "iframe[src*='5mgrgsc.cn']",
            "a[href*='5mgrgsc.cn']"
        )
    )

    val bachashuku = PrivateSiteProfile(
        id = "bachashuku",
        displayName = "八叉书库",
        startUrl = "https://bachashuku.org/",
        kind = PrivateSiteKind.NOVEL,
        rootDomains = setOf("8xsk.com", "8xsk.org", "bachashuku.org"),
        entryUrls = setOf(
            "https://bachashuku.org/",
            "https://8xsk.com/",
            "https://8xsk.org/"
        ),
        fingerprintTerms = setOf("八叉书库", "八叉"),
        adult = true,
        requiresLogin = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts,
        domRemoveSelectors = commonAdSelectors
    )

    val uaa = PrivateSiteProfile(
        id = "uaa",
        displayName = "UAA",
        startUrl = "https://www.uaa.com/",
        kind = PrivateSiteKind.NOVEL,
        rootDomains = setOf("uaa.com"),
        fingerprintTerms = setOf("UAA", "有爱爱"),
        adult = true,
        requiresLogin = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts,
        domRemoveSelectors = commonAdSelectors
    )

    val hotupub = PrivateSiteProfile(
        id = "hotupub",
        displayName = "河图文化",
        startUrl = "https://www.hotupub.net/home/index",
        kind = PrivateSiteKind.NOVEL,
        rootDomains = setOf("hotupub.net"),
        fingerprintTerms = setOf("河图", "河圖"),
        adult = true,
        requiresLogin = true,
        usesCredits = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts,
        domRemoveSelectors = commonAdSelectors
    )

    val cool18 = PrivateSiteProfile(
        id = "cool18",
        displayName = "禁忌书屋 / Cool18",
        startUrl = "https://www.cool18.com/bbs4/index.php",
        kind = PrivateSiteKind.FORUM,
        rootDomains = setOf("cool18.com"),
        fingerprintTerms = setOf("Cool18", "禁忌书屋"),
        adult = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts,
        domRemoveSelectors = commonAdSelectors + setOf(
            "iframe[src*='ad']",
            "[class*='advert']",
            "[id*='advert']"
        )
    )

    val hanime1 = PrivateSiteProfile(
        id = "hanime1",
        displayName = "Hanime1",
        startUrl = "https://hanime1.me/",
        kind = PrivateSiteKind.VIDEO,
        rootDomains = setOf("hanime1.me"),
        fingerprintTerms = setOf("Hanime1"),
        adult = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts + setOf(
            "erodalabs.com",
            "impactserving.com"
        ),
        domRemoveSelectors = commonAdSelectors + adultVideoOverlaySelectors + setOf(
            "a[href*='erodalabs.com']",
            "a[href*='impactserving.com']",
            "iframe[src*='impactserving.com']"
        )
    )

    val pornhub = PrivateSiteProfile(
        id = "pornhub",
        displayName = "Pornhub",
        startUrl = "https://jp.pornhub.com/",
        kind = PrivateSiteKind.VIDEO,
        rootDomains = setOf("pornhub.com"),
        fingerprintTerms = setOf("Pornhub"),
        adult = true,
        regionSensitive = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts,
        domRemoveSelectors = commonAdSelectors + adultVideoOverlaySelectors
    )

    val xvideos = PrivateSiteProfile(
        id = "xvideos",
        displayName = "XVideos",
        startUrl = "https://www.xvideos.com/",
        kind = PrivateSiteKind.VIDEO,
        rootDomains = setOf("xvideos.com"),
        fingerprintTerms = setOf("XVideos"),
        adult = true,
        regionSensitive = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts,
        domRemoveSelectors = commonAdSelectors + adultVideoOverlaySelectors
    )

    val missav = PrivateSiteProfile(
        id = "missav",
        displayName = "MissAV",
        startUrl = "https://missav.ws/",
        kind = PrivateSiteKind.VIDEO,
        rootDomains = setOf("missav.ws", "missav.com"),
        fingerprintTerms = setOf("MissAV"),
        adult = true,
        regionSensitive = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts + setOf(
            "mayzaent.com",
            "bit.ly",
            "myavlive.com",
            "creative.myavlive.com",
            "creative.live.missav.com",
            "trackwilltrk.com",
            "rmhfrtnd.com",
            "ktkjmp.com",
            "gsjln04hd.com",
            "cashewsforlife208.com",
            "phloxsub73ulata.com",
            "xxxjmp.com"
        ),
        domRemoveSelectors = commonAdSelectors + adultVideoOverlaySelectors + setOf(
            "iframe[src*='mayzaent.com']",
            "a[href*='bit.ly']",
            "a[href*='myavlive.com']",
            "a[href*='trackwilltrk.com']",
            "a[href*='rmhfrtnd.com']",
            "a[href*='ktkjmp.com']",
            "iframe[src*='creative.live.missav.com']"
        )
    )

    val thePornDude = PrivateSiteProfile(
        id = "theporndude",
        displayName = "The Porn Dude",
        startUrl = "https://theporndude.com/zh",
        kind = PrivateSiteKind.NAVIGATION,
        rootDomains = setOf("theporndude.com"),
        fingerprintTerms = setOf("Porn Dude", "ThePornDude"),
        adult = true,
        blockedHostSuffixes = commonTrackerHosts + commonAdultAdHosts,
        domRemoveSelectors = commonAdSelectors
    )

    val profiles: List<PrivateSiteProfile> = listOf(
        twkan,
        shuba69,
        uukan,
        quanben,
        diyibanzhu,
        bachashuku,
        uaa,
        hotupub,
        cool18,
        hanime1,
        pornhub,
        xvideos,
        missav,
        thePornDude
    )

    fun profileById(id: String?): PrivateSiteProfile? {
        if (id.isNullOrBlank()) return null
        return profiles.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    fun profileFor(url: String?): PrivateSiteProfile? {
        val host = hostOf(url) ?: return null
        return profiles.firstOrNull { it.matchesHost(host) }
    }

    fun hostOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return runCatching { URI(url).host?.lowercase() }.getOrNull()
    }
}
