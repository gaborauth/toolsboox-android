package online.toolboox.ot

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

/**
 * Instant drop-down autocomplete text view.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class AutoCompleteTextView : AppCompatAutoCompleteTextView {

    /**
     * The default constructor.
     *
     * @param context the context
     */
    constructor(context: Context) : super(context)

    /**
     * The default constructor.
     *
     * @param context the context
     * @param attributeSet the attribute set
     */
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    /**
     * The default constructor.
     *
     * @param context the context
     * @param attributeSet the attribute set
     */
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    /**
     * Always true filter.
     */
    override fun enoughToFilter(): Boolean = true

    /**
     * Always filtering focus change.
     */
    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (filter == null) return

        if (focused) {
            performFiltering(text, 0)
        }
    }
}
