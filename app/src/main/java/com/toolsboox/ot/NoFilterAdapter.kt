package com.toolsboox.ot

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/**
 * No filter adapter class.
 *
 * @param context the context
 * @param resource the item resource
 * @param items the items
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
internal class NoFilterAdapter(
    context: Context,
    resource: Int,
    private val items: List<String>
) : ArrayAdapter<String>(context, resource, items) {

    /**
     * Create a filter of the adapter.
     *
     * @return the filter
     */
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                filterResults.values = items
                filterResults.count = items.size
                return filterResults
            }

            override fun publishResults(contraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }
}