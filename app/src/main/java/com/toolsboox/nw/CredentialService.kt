package com.toolsboox.nw

import com.toolsboox.da.Credential
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.*

/**
 * Credential service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface CredentialService {

    /**
     * Sign up.
     *
     * @param hash the MD5 hash of the password
     * @return the user ID
     */
    @POST(value = "credential/create")
    fun createAsync(
        @Body hash: String
    ): Deferred<Response<Credential>>

    /**
     * Log in.
     *
     * @param userId the user ID
     * @param hash the MD5 hash of the password
     * @return the refresh token
     */
    @POST(value = "credential/login/{userId}")
    fun loginAsync(
        @Path("userId") userId: UUID,
        @Body hash: String
    ): Deferred<Response<String>>
}
