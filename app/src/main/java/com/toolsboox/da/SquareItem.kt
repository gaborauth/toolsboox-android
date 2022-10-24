package com.toolsboox.da

import android.os.Bundle

/**
 * Square item data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class SquareItem(
    val title: String,
    val imageRes: Int,
    val actionId: Int = 0,
    val bundle: Bundle
)
