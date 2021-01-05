package online.toolboox.ot

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import online.toolboox.R
import online.toolboox.da.SquareItem

internal class DashboardItemAdapter(
    private val context: Context,
    private val squareItems: List<SquareItem>,
    private val clickListener: OnItemClickListener
) : RecyclerView.Adapter<DashboardItemAdapter.ViewHolder>() {

    /**
     * Required method for creating the ViewHolder objects.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return The newly created ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_square, parent, false))
    }

    /**
     * Required method that binds the data to the ViewHolder.
     *
     * @param holder The ViewHolder into which the data should be put.
     * @param position The adapter position.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(squareItems[position], clickListener)
    }

    /**
     * Required method for determining the size of the data set.
     *
     * @return Size of the data set.
     */
    override fun getItemCount(): Int {
        return squareItems.size
    }

    /**
     * ViewHolder class that represents each row of data in the RecyclerView
     */
    internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.title)
        private val titleImage: ImageView = itemView.findViewById(R.id.titleImage)

        fun bindTo(squareItem: SquareItem, clickListener: OnItemClickListener) {
            titleText.text = squareItem.title
            titleImage.setImageResource(squareItem.imageRes)

            itemView.setOnClickListener {
                clickListener.onItemClicked(squareItem)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClicked(squareItem: SquareItem)
    }
}
