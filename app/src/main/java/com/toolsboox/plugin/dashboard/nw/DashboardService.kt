package com.toolsboox.plugin.dashboard.nw

import com.toolsboox.plugin.dashboard.da.Version
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET

/**
 * Dashboard service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface DashboardService {

    /**
     * Get the server API version.
     *
     * @return the server API version
     */
    @GET(value = "version.json")
    fun versionAsync(): Deferred<Response<Version>>
}
