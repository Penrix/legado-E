package io.legado.app.help.site

import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import splitties.init.appCtx

/**
 * Installs App-managed private site adapters directly into Sigma's normal BookSource database.
 *
 * Community sources are only research inputs for discovering useful websites and parser ideas.
 * Once a website is selected, its rules live here as a Penrix-maintained private adapter.
 *
 * Manual imports still use SourceHelp.insertBookSource(). Private assets use collision-resistant
 * source keys so they never overwrite a user's own source for the same website.
 */
object PrivateBookSourceInstaller {

    const val MANAGED_GROUP = "Penrix 内置"
    private const val MANAGED_KEY_MARKER = "penrix_builtin="

    private val assetPaths = listOf(
        "privateSites/bookSources/twkan-pure.json",
        "privateSites/bookSources/69shuba-pure.json",
        "privateSites/bookSources/bachashuku-pure.json",
        "privateSites/bookSources/diyibanzhu-pure.json",
        "privateSites/bookSources/uaa-pure.json",
        "privateSites/bookSources/uukanshu-pure.json",
        "privateSites/bookSources/quanben-io-pure.json",
        "privateSites/bookSources/cool18-pure.json",
        "privateSites/bookSources/hotupub-pure.json"
    )

    fun installOrUpdate() {
        assetPaths.forEach { assetPath ->
            val source = load(assetPath) ?: return@forEach
            if (!isManaged(source)) return@forEach

            val existing = appDb.bookSourceDao.getBookSource(source.bookSourceUrl)
            if (existing != null) {
                // App owns parser/network rules; the user owns enable/explore/order preferences.
                source.enabled = existing.enabled
                source.enabledExplore = existing.enabledExplore
                source.customOrder = existing.customOrder
            }
            appDb.bookSourceDao.insert(source)
        }
        PrivateReadingDefaults.applyToExistingBooks()
    }

    private fun load(assetPath: String): BookSource? {
        return runCatching {
            val json = appCtx.assets.open(assetPath).bufferedReader().use { it.readText() }
            GSON.fromJsonObject<BookSource>(json).getOrThrow()
        }.getOrNull()
    }

    private fun isManaged(source: BookSource): Boolean {
        val hasManagedGroup = source.bookSourceGroup
            ?.split(',')
            ?.any { it.trim() == MANAGED_GROUP } == true
        return hasManagedGroup && source.bookSourceUrl.contains(MANAGED_KEY_MARKER)
    }
}
