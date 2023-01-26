package com.toolsboox.nw

import com.toolsboox.da.Credential
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*
import java.util.*

/**
 * Credential service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface CredentialService {

    /**
     * Refresh the access token.
     *
     * @param refreshToken the acutal refresh token
     * @return the access token
     */
    @GET(value = "credential/accessToken")
    fun accessTokenAsync(
        @Header("Authorization") refreshToken: String
    ): Deferred<Response<String>>

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

    /**
     * Refresh the refresh token.
     *
     * @param refreshToken the acutal refresh token
     * @return the new refresh token
     */
    @GET(value = "credential/refreshToken")
    fun refreshTokenAsync(
        @Header("Authorization") refreshToken: String
    ): Deferred<Response<String>>
}
