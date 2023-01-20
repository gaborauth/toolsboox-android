package com.toolsboox.plugin.about.ui

import com.toolsboox.plugin.about.da.Purchase
import com.toolsboox.plugin.about.nw.PurchaseService
import com.toolsboox.ui.plugin.FragmentPresenter
import java.util.*
import javax.inject.Inject

/**
 * About presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class AboutPresenter @Inject constructor() : FragmentPresenter() {
    @Inject
    lateinit var purchaseService: PurchaseService

    /**
     * Update the purchase.
     *
     * @param fragment the fragment
     * @param userId the user ID
     * @param purchase the purchase
     * @return the saved purchase
     */
    fun update(fragment: AboutFragment, userId: UUID, purchase: Purchase) {
        coroutinesCallHelper(
            fragment,
            { purchaseService.updateAsync(userId, purchase) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.updateResult(body)
                        }
                    }

                    else -> fragment.somethingHappened(true)
                }
            },
            true
        )
    }
}
