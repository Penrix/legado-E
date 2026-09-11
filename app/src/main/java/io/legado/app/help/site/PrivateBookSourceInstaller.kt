package io.legado.app.help.site

import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import splitties.init.appCtx

/**
 * Installs App-managed book sources without going through public/community source import.
 *
 * This is intentionally separate from SourceHelp.insertBookSource(): Sigma's public import path
 * keeps all of its original behaviour, including its 18+ filtering. Private built-ins are assets
 * shipped by this App and use collision-resistant source keys so they do not overwrite a user's
 * community source for the same website.
 */
object PrivateBookSourceInstaller {

    const val MANAGED_GROUP = "Penrix 内置"
    private const val MANAGED_KEY_MARKER = "penrix_builtin="

    private val assetPaths = listOf(
        "privateSites/bookSources/twkan-pure.json",
        "privateSites/bookSources/69shuba-pure.json",
        "privateSites/bookSources/bachashuku-pure.json",
        "privateSites/bookSources/diyibanzhu-pure.json"
    )

    fun installOrUpdate() {
        assetPaths.forEach { assetPath ->
            val source = load(assetPath) ?: return@forEach
            if (!isManaged(source)) return@forEach

            val existing = appDb.bookSourceDao.getBookSource(source.bookSourceUrl)
            if (existing != null) {
                // Rule updates belong to the App; user-facing enable/order choices belong to user.
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

    private fun isManaged(source: BookSource): Boolean {
        return source.bookSourceGroup
            ?.split(',')
            ?.any { it.trim() == MANAGED_GROUP } == true &&
                source.bookSourceUrl.contains(MANAGED_KEY_MARKER)
    }
}
