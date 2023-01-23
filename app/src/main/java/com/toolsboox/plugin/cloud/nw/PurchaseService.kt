package com.toolsboox.plugin.cloud.nw

import com.toolsboox.plugin.cloud.da.Purchase
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
     * @param purchase the purchase
     * @return the saved purchase
     */
    @POST(value = "purchase/update/{userId}")
    fun updateAsync(
        @Path("userId") userId: UUID,
        @Body purchase: Purchase
    ): Deferred<Response<Purchase>>
}
