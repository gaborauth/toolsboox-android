package com.toolsboox.ot

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.toolsboox.da.LocaleItem

/**
 * Locale item adapter class.
 *
 * @param context the context
 * @param resource the item resource
 * @param localeItems the locale items
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
internal class LocaleItemAdapter(
    context: Context,
    resource: Int,
    private val localeItems: List<LocaleItem>
) : ArrayAdapter<LocaleItem>(context, resource, localeItems) {

    /**
     * The filtered items.
     */
    private var filteredItems = localeItems

    /**
     * Returns with the count of items.
     *
     * @return the count
     */
    override fun getCount(): Int {
        return filteredItems.size
    }

    /**
     * Returns with the item of the index.
     *
     * @param index the index
     * @return the item
     */
    override fun getItem(index: Int): LocaleItem {
        return filteredItems[index]
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
        val item = filteredItems[position]
        view.text = item.toString()

        return view
    }

    /**
     * Create a view of the item.
     *
     * @param position the position
     * @param convertView the convert view
     * @param parent the parent view
     * @return the view
     */
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val item = filteredItems[position]
        view.text = item.toString()

        return view
    }

    /**
     * Create a filter of the adapter.
     *
     * @return the filter
     */
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (constraint == null) {
                    filterResults.values = localeItems
                    filterResults.count = localeItems.size
                } else {
                    val filteredList = mutableListOf<LocaleItem>()
                    val replacedConstraint = constraint.toString().replace("-", "")
                    localeItems.forEach {
                        if (it.languageTag.replace("-", "").startsWith(replacedConstraint, true)) {
                            filteredList.add(it)
                        }
                    }

                    filterResults.values = filteredList
                    filterResults.count = filteredList.size
                }
                return filterResults
            }

            override fun publishResults(contraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    filteredItems = results.values as List<LocaleItem>
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}