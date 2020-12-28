package online.toolboox.plugin.dashboard.ot

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import online.toolboox.R
import online.toolboox.plugin.dashboard.da.DashboardItem

internal class DashboardItemAdapter(
    private val context: Context,
    private val dashboardItems: List<DashboardItem>,
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
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_dashboard, parent, false))
    }

    /**
     * Required method that binds the data to the ViewHolder.
     *
     * @param holder The ViewHolder into which the data should be put.
     * @param position The adapter position.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(dashboardItems[position], clickListener)
    }

    /**
     * Required method for determining the size of the data set.
     *
     * @return Size of the data set.
     */
    override fun getItemCount(): Int {
        return dashboardItems.size
    }

    /**
     * ViewHolder class that represents each row of data in the RecyclerView
     */
    internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.title)
        private val titleImage: ImageView = itemView.findViewById(R.id.titleImage)

        fun bindTo(dashboardItem: DashboardItem, clickListener: OnItemClickListener) {
            titleText.text = dashboardItem.title
            titleImage.setImageResource(dashboardItem.imageRes)

            itemView.setOnClickListener {
                clickListener.onItemClicked(dashboardItem)
            }
        }
    }

    interface OnItemClickListener{
        fun onItemClicked(dashboardItem: DashboardItem)
    }
}
