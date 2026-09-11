package io.legado.app.help.site

import io.legado.app.constant.PageAnim
import io.legado.app.data.appDb
import io.legado.app.help.book.isImage

/**
 * Reading defaults for App-managed private text sources.
 *
 * The default is applied only when a book has no per-book page animation preference yet.
 * Once the user explicitly selects another animation, that per-book choice wins permanently.
 */
object PrivateReadingDefaults {

    private const val MANAGED_KEY_MARKER = "penrix_builtin="

    fun applyToExistingBooks() {
        appDb.bookDao.webBooks.forEach { book ->
            if (
                !book.isImage &&
                book.origin.contains(MANAGED_KEY_MARKER) &&
                book.readConfig?.pageAnim == null
            ) {
                book.setPageAnim(PageAnim.scrollPageAnim)
                appDb.bookDao.update(book)
            }
        }
    }
}
