package com.toolsboox.plugin.calendar.nw

import com.toolsboox.plugin.calendar.da.v1.CalendarSyncItem
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

/**
 * Calendar service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface CalendarService {

    /**
     * Retrieve the encrypted credential content.
     *
     * @return the stored credential content
     */
    @GET(value = "calendarItem/authenticate")
    fun authenticateGetAsync(): Deferred<Response<String>>

    /**
     * Store the encrypted credential content.
     *
     * @param data the JSON encoded credential content
     * @return the stored credential content
     */
    @POST(value = "calendarItem/authenticate")
    fun authenticatePostAsync(
        @Body data: String
    ): Deferred<Response<String>>

    /**
     * List the cloud calendar items.
     *
     * @return the list of calendar sync items
     */
    @GET(value = "calendarItem/list")
    fun listAsync(): Deferred<Response<List<CalendarSyncItem>>>

    /**
     * Upload an encrypted calendar page.
     *
     * @param path the path of the file
     * @param baseName the base name of the file
     * @param version the version of the file
     * @param encoded the encoded data
     * @return the saved calendar sync item
     */
    @POST(value = "calendarItem/update/{path}/{baseName}/{version}")
    fun updateAsync(
        @Path("path") path: String,
        @Path("baseName") baseName: String,
        @Path("version") version: String,
        @Body encoded: String
    ): Deferred<Response<CalendarSyncItem>>
}