package online.toolboox.ot

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * Square item linear layout.
 *
 * @param context the context
 * @param attrs the attributes
 * @param defStyle the default style
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class SquareLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    /**
     * Set square measurement.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
