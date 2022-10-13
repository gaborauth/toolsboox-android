package com.toolsboox.plugin.templates.ot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.toolsboox.databinding.ListItemCommunityTemplateBinding
import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.templates.da.CommunityTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL


/**
 * Community template item adapter for recycler view.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CommunityTemplatesItemAdapter(
    val context: Context?,
    private var items: List<CommunityTemplate>,
    private val listener: (CommunityTemplate) -> Unit
) : RecyclerView.Adapter<CommunityTemplatesItemAdapter.ViewHolder>() {

    /**
     * OnCreateViewHolder hook.
     *
     * @param parent the parent
     * @param viewType the view field
     * @return the ViewHolder instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemCommunityTemplateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        return ViewHolder(binding)
    }

    /**
     * OnBindViewHolder hook.
     *
     * @param holder the view holder
     * @param position the position
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position], listener)

    /**
     * The number of the items.
     *
     * @return number of items
     */
    override fun getItemCount() = items.size

    /**
     * Update the content.
     *
     * @param newItems the new items
     */
    fun update(newItems: List<CommunityTemplate>) {
        DiffUtil.calculateDiff(CommunityTemplatesDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    /**
     * DiffUtil callback of the list.
     *
     * @author <a href="mailto:gabor.auth@iotguru.live">Gábor AUTH</a>
     */
    class CommunityTemplatesDiffUtilCallback(
        private var oldCommunityTemplates: List<CommunityTemplate>,
        private var newCommunityTemplates: List<CommunityTemplate>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldCommunityTemplates.size
        override fun getNewListSize(): Int = newCommunityTemplates.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldCommunityTemplates[oldItemPosition].name == newCommunityTemplates[newItemPosition].name
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return false
        }
    }

    /**
     * ViewHolder instance.
     *
     * @param binding the binding
     */
    class ViewHolder(val binding: ListItemCommunityTemplateBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind the item with the listener.
         *
         * @param item the item
         * @param listener the listener
         */
        fun bind(item: CommunityTemplate, listener: (CommunityTemplate) -> Unit) = with(itemView) {
            binding.name.text = item.name
            binding.description.text = item.description
            binding.category.text = item.category

            setOnClickListener { listener(item) }

            val thumbnailCacheDir = File(context.cacheDir, "communityTemplates")
            thumbnailCacheDir.mkdirs()

            val thumbnailCache = File(thumbnailCacheDir, item.thumbnailUri)
            if (thumbnailCache.exists()) {
                binding.thumbnail.setImageBitmap(BitmapFactory.decodeFile(thumbnailCache.absolutePath))
            } else {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val url = URL(NetworkModule.GITHUB_BASE_URL + "communityTemplates/" + item.thumbnailUri)
                        val bitmap = BitmapFactory.decodeStream(url.content as InputStream)
                        withContext(Dispatchers.Main) {
                            binding.thumbnail.setImageBitmap(bitmap)
                        }
                        FileOutputStream(thumbnailCache).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    } catch (t: Throwable) {
                        Timber.e(t)
                    }
                }
            }

            binding.iconStar.setOnClickListener {
                binding.iconStar.visibility = View.INVISIBLE
                binding.iconStarred.visibility = View.VISIBLE
            }
            binding.iconStarred.setOnClickListener {
                binding.iconStar.visibility = View.VISIBLE
                binding.iconStarred.visibility = View.INVISIBLE
            }
        }
    }
}
