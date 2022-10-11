package com.toolsboox.plugin.templates.nw

import kotlinx.coroutines.Deferred
import com.toolsboox.plugin.templates.da.CommunityTemplate
import retrofit2.Response
import retrofit2.http.GET

/**
 * Templates service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface TemplatesService {
    /**
     * List the community templates.
     *
     * @return the list of templates
     */
    @GET(value = "communityTemplates.json")
    fun listAsync(
    ): Deferred<Response<List<CommunityTemplate>>>
}
