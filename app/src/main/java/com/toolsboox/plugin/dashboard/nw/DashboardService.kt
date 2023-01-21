package com.toolsboox.plugin.dashboard.nw

import com.toolsboox.plugin.dashboard.da.Version
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Dashboard service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface DashboardService {


    /**
     * Get a value of the parameter by key.
     *
     * @param key the key
     * @return the value of the parameter
     */
    @GET(value = "dashboard/parameter/{key}")
    fun parameterAsync(
        @Path("key") key: String
    ): Deferred<Response<String>>

    /**
     * Get the server API version.
     *
     * @return the server API version
     */
    @GET(value = "dashboard/v2/version/{currentVersion}")
    fun versionAsync(
        @Path("currentVersion") currentVersion: String
    ): Deferred<Response<Version>>
}
