package com.toolsboox.plugin.teamdrawer.ot

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toolsboox.R
import com.toolsboox.plugin.teamdrawer.da.NoteItem
import com.toolsboox.utils.FluentDuration
import java.time.Instant

/**
 * Note item adapter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
internal class NoteItemAdapter(
    private val context: Context,
    private val noteItems: List<NoteItem>,
    private val clickListener: OnItemClickListener
) : RecyclerView.Adapter<NoteItemAdapter.ViewHolder>() {

    /**
     * Required method for creating the ViewHolder objects.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return The newly created ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_teamdrawer_note, parent, false))
    }

    /**
     * Required method that binds the data to the ViewHolder.
     *
     * @param holder The ViewHolder into which the data should be put.
     * @param position The adapter position.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(noteItems[position], clickListener)
    }

    /**
     * Required method for determining the size of the data set.
     *
     * @return Size of the data set.
     */
    override fun getItemCount(): Int {
        return noteItems.size
    }

    /**
     * ViewHolder class that represents each row of data in the RecyclerView
     */
    internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.title)
        private val noteImage: ImageView = itemView.findViewById(R.id.noteImage)
        private val lastUpdatedText: TextView = itemView.findViewById(R.id.lastUpdated)

        fun bindTo(noteItem: NoteItem, clickListener: OnItemClickListener) {
            titleText.text = noteItem.title
            noteImage.setImageResource(noteItem.imageRes)
            lastUpdatedText.text = FluentDuration.convert(
                itemView.context,
                Instant.now().toEpochMilli() - noteItem.lastUpdated.time
            )

            itemView.setOnClickListener {
                clickListener.onItemClicked(noteItem)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClicked(noteItem: NoteItem)
    }
}
