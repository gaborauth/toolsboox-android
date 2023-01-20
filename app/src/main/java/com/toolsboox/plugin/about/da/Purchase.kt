package com.toolsboox.plugin.about.da

import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
data class Purchase(
    val userId: UUID?,
    val orderId: String,
    val packageName: String,
    val productId: String,
    val purchaseTime: Date,
    val purchaseState: Int,
    val purchaseToken: String,
    val quantity: Int,
    val autoRenewing: Boolean,
    val acknowledged: Boolean
)
