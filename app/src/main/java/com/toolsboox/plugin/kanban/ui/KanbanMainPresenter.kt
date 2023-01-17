package com.toolsboox.plugin.kanban.ui

import android.os.Environment
import com.squareup.moshi.Moshi
import com.toolsboox.R
import com.toolsboox.plugin.kanban.da.v1.CardItem
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.*
import java.nio.file.Files
import java.util.*
import javax.inject.Inject

/**
 * Kanban planner main presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class KanbanMainPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

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
                    migrateData(fragment)

                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                    val path = File(rootPath, "kanban")

                    path.list { d, n -> n.endsWith("-card-v1.json", true) }?.forEach { fileName ->
                        Timber.i("Processing of cardItem: $fileName")

                        val item = File(path, fileName)
                        if (item.exists()) {
                            FileReader(item).use { fileReader ->
                                Timber.i("Try to load from ${item.name}")
                                if (item.absolutePath.endsWith("-v1.json")) {
                                    moshi.adapter(CardItem::class.java)
                                        .fromJson(fileReader.readText())?.let { cardItem ->
                                            Timber.i("Card item: ${cardItem.id}")
                                            cardItems[cardItem.id] = cardItem
                                        }
                                }
                            }
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
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                    val path = File(rootPath, "kanban")
                    path.mkdirs()

                    PrintWriter(FileWriter(File(path, "${cardItem.id}-card-v1.json"))).use {
                        val adapter = moshi.adapter(CardItem::class.java)
                        it.write(adapter.toJson(cardItem))
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.showError(e, R.string.something_happened_error) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Migrate cards from old folder to new folder.
     *
     * @param fragment the kanban main fragment
     */
    private fun migrateData(fragment: KanbanMainFragment) {
        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        val path = File(rootPath, "kanban")
        path.mkdirs()

        val kanbanDir = File(fragment.requireContext().filesDir, "kanban")
        kanbanDir.mkdirs()
        if (File(kanbanDir, "migrated").exists()) return

        kanbanDir.list { d, n -> n.endsWith(".card.v1", true) }?.forEach {
            Timber.i("Migrating cardItem: $it")
            val source = File(kanbanDir, it).toPath()
            val destination = File(path, it.replace(".card.v1", "-card-v1.json")).toPath()
            Files.move(source, destination)
            Timber.i("Migrated: $it")
        }

        File(kanbanDir, "migrated").createNewFile()
    }
}
