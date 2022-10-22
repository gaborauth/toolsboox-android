package com.toolsboox.plugin.kanban.ui

import com.google.gson.Gson
import com.toolsboox.R
import com.toolsboox.plugin.kanban.da.CardItem
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

/**
 * Kanban planner main presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class KanbanMainPresenter @Inject constructor() : FragmentPresenter() {

    @Inject
    lateinit var gson: Gson

    /**
     * Load card items from local storage.
     *
     * @param fragment the fragment
     */
    fun loadLocal(fragment: KanbanMainFragment) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val cardItems: MutableMap<UUID, CardItem> = mutableMapOf()
                try {
                    val kanbanDir = File(fragment.requireContext().filesDir, "kanban")
                    kanbanDir.mkdirs()

                    //kanbanDir.list().forEach {
                    //    val item = File(kanbanDir, it)
                    //    item.delete()
                    //    Timber.i("Item $it deleted.")
                    //}

                    kanbanDir.list { d, n -> n.endsWith(".card.v1", true) }?.forEach {
                        Timber.i("Processing of cardItem: $it")
                        val cardItemJson = File(kanbanDir, it).readText(Charsets.UTF_8)
                        if (cardItemJson.isNotEmpty()) {
                            val cardItem = gson.fromJson(cardItemJson, CardItem::class.java)
                            Timber.i("Card item: ${cardItem.lane}")
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
    fun saveLocal(fragment: KanbanMainFragment, cardItem: CardItem) {
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