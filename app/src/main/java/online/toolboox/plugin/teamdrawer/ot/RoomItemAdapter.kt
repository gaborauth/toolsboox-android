package online.toolboox.plugin.teamdrawer.ot

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import online.toolboox.R
import online.toolboox.plugin.teamdrawer.da.RoomItem
import online.toolboox.utils.FluentDuration
import java.time.Instant

/**
 * Room item adapter.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
internal class RoomItemAdapter(
    private val context: Context,
    private val roomItems: List<RoomItem>,
    private val clickListener: OnItemClickListener
) : RecyclerView.Adapter<RoomItemAdapter.ViewHolder>() {

    /**
     * Required method for creating the ViewHolder objects.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return The newly created ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_teamdrawer_room, parent, false))
    }

    /**
     * Required method that binds the data to the ViewHolder.
     *
     * @param holder The ViewHolder into which the data should be put.
     * @param position The adapter position.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(roomItems[position], clickListener)
    }

    /**
     * Required method for determining the size of the data set.
     *
     * @return Size of the data set.
     */
    override fun getItemCount(): Int {
        return roomItems.size
    }

    /**
     * ViewHolder class that represents each row of data in the RecyclerView
     */
    internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.name)
        private val roomImage: ImageView = itemView.findViewById(R.id.roomImage)
        private val lastUpdatedText: TextView = itemView.findViewById(R.id.lastUpdated)

        fun bindTo(roomItem: RoomItem, clickListener: OnItemClickListener) {
            nameText.text = roomItem.name
            roomImage.setImageResource(roomItem.imageRes)
            lastUpdatedText.text = FluentDuration.convert(
                itemView.context,
                Instant.now().toEpochMilli() - roomItem.lastUpdated.time
            )

            itemView.setOnClickListener {
                clickListener.onItemClicked(roomItem)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClicked(roomItem: RoomItem)
    }
}
