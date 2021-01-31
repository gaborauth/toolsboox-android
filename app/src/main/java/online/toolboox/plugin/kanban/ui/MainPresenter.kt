package online.toolboox.plugin.kanban.ui

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.toolboox.R
import online.toolboox.plugin.kanban.da.CardItem
import online.toolboox.ui.plugin.FragmentPresenter
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

/**
 * Kanban planner main presenter.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class MainPresenter @Inject constructor(
    private val gson: Gson
) : FragmentPresenter() {

    /**
     * Load card items from local storage.
     *
     * @param fragment the fragment
     */
    fun loadLocal(fragment: MainFragment) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val cardItems: MutableMap<UUID, CardItem> = mutableMapOf()
                try {
                    val kanbanDir = File(fragment.requireContext().filesDir, "kanban")
                    kanbanDir.mkdirs()

                    kanbanDir.list { d, n -> n.endsWith(".card.v1", true) }?.forEach {
                        Timber.i("Processing of cardItem: $it")
                        val cardItemJson = File(kanbanDir, it).readText(Charsets.UTF_8)
                        if (cardItemJson.isNotEmpty()) {
                            val cardItem = gson.fromJson(cardItemJson, CardItem::class.java)
                            cardItems[cardItem.id] = cardItem
                        }
                    }

                    withContext(Dispatchers.Main) { fragment.renderLoad(cardItems) }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.showError(e, R.string.something_happened_error) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Save a card item to the local storage.
     *
     * @param fragment the fragment
     * @param cardItem the card item to save
     */
    fun saveLocal(fragment: MainFragment, cardItem: CardItem) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val kanbanDir = File(fragment.requireContext().filesDir, "kanban")
                    kanbanDir.mkdirs()

                    val cardItemFile = File(kanbanDir, "${cardItem.id}.card.v1")
                    cardItemFile.writeText(gson.toJson(cardItem), Charsets.UTF_8)
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.showError(e, R.string.something_happened_error) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }
}
