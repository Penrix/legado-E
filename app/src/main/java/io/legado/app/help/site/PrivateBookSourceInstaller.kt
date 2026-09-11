package io.legado.app.help.site

import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import splitties.init.appCtx

/**
 * Installs App-managed sources directly into Sigma's normal BookSource database.
 *
 * Two managed families are intentionally separated:
 * - Penrix 内置: site adapters we maintain as part of the private App.
 * - Penrix 社区精选: community rules we have reviewed, repaired and pinned.
 *
 * Public/manual imports still use SourceHelp.insertBookSource(). Managed assets use collision-
 * resistant source keys so they never overwrite a user's own source for the same website.
 */
object PrivateBookSourceInstaller {

    const val BUILTIN_GROUP = "Penrix 内置"
    const val CURATED_GROUP = "Penrix 社区精选"

    private data class ManagedAsset(
        val path: String,
        val group: String,
        val keyMarker: String
    )

    private val assets = listOf(
        ManagedAsset("privateSites/bookSources/twkan-pure.json", BUILTIN_GROUP, "penrix_builtin="),
        ManagedAsset("privateSites/bookSources/69shuba-pure.json", BUILTIN_GROUP, "penrix_builtin="),
        ManagedAsset("privateSites/bookSources/bachashuku-pure.json", BUILTIN_GROUP, "penrix_builtin="),
        ManagedAsset("privateSites/bookSources/diyibanzhu-pure.json", BUILTIN_GROUP, "penrix_builtin="),
        ManagedAsset("privateSites/bookSources/uaa-pure.json", BUILTIN_GROUP, "penrix_builtin="),
        ManagedAsset("privateSites/communityCurated/uukanshu-curated.json", CURATED_GROUP, "penrix_curated="),
        ManagedAsset("privateSites/communityCurated/quanben-io-curated.json", CURATED_GROUP, "penrix_curated="),
        ManagedAsset("privateSites/communityCurated/cool18-curated.json", CURATED_GROUP, "penrix_curated=")
    )

    fun installOrUpdate() {
        assets.forEach { asset ->
            val source = load(asset.path) ?: return@forEach
            if (!isManaged(source, asset)) return@forEach

            val existing = appDb.bookSourceDao.getBookSource(source.bookSourceUrl)
            if (existing != null) {
                // App owns parser/network rules; the user owns enable/explore/order preferences.
                source.enabled = existing.enabled
                source.enabledExplore = existing.enabledExplore
                source.customOrder = existing.customOrder
            }
            appDb.bookSourceDao.insert(source)
        }
    }

    private fun load(assetPath: String): BookSource? {
        return runCatching {
            val json = appCtx.assets.open(assetPath).bufferedReader().use { it.readText() }
            GSON.fromJsonObject<BookSource>(json).getOrThrow()
        }.getOrNull()
    }

    private fun isManaged(source: BookSource, asset: ManagedAsset): Boolean {
        val hasExpectedGroup = source.bookSourceGroup
            ?.split(',')
            ?.any { it.trim() == asset.group } == true
        return hasExpectedGroup && source.bookSourceUrl.contains(asset.keyMarker)
    }
}
