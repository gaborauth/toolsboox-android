package com.toolsboox.plugin.about.nw

import com.toolsboox.plugin.about.da.Purchase
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.*

/**
 * Purchase service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface PurchaseService {

    /**
     * Update the purchase.
     *
     * @param userId the user ID
     * @param purchaseJson the purchase JSON
     * @return the saved purchase JSON
     */
    @POST(value = "purchase/update/{userId}")
    fun updateAsync(
        @Path("userId") userId: UUID,
        @Body purchase: Purchase
    ): Deferred<Response<Purchase>>
}
