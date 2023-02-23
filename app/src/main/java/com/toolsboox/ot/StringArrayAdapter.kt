package com.toolsboox.ot

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Data class to string array adapter class.
 *
 * @param context the context
 * @param resource the item resource
 * @param items the items
 * @param itemToString function to map the item to string to display
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
open class StringArrayAdapter<T>(
    context: Context,
    resource: Int,
    private val items: List<T>,
    val itemToString: (T) -> String
) : ArrayAdapter<T>(context, resource, items) {

    /**
     * Returns with the count of items.
     *
     * @return the count
     */
    override fun getCount(): Int {
        return items.size
    }

    /**
     * Returns with the item of the index.
     *
     * @param index the index
     * @return the item
     */
    override fun getItem(index: Int): T {
        return items[index]
    }

    /**
     * Create a view of the item.
     *
     * @param position the position
     * @param convertView the convert view
     * @param parent the parent view
     * @return the view
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val item = items[position]
        view.text = itemToString(item)

        return view
    }
}

