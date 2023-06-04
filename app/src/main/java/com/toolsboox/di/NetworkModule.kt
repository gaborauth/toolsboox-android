package com.toolsboox.di

import android.content.SharedPreferences
import com.google.gson.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.toolsboox.BuildConfig
import com.toolsboox.ProductConstants
import com.toolsboox.ot.DateJsonAdapter
import com.toolsboox.ot.LocaleJsonAdapter
import com.toolsboox.ot.UUIDJsonAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Named

/**
 * Retrofit DI module of network services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
object NetworkModule {

    /**
     * Base URL of GitHub raw.
     */
    const val GITHUB_BASE_URL = "https://gaborauth.github.io/toolsboox/"

    /**
     * Provides the OkHttpClient.
     *
     * @return the client
     */
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val loggerInterceptor = HttpLoggingInterceptor()
        loggerInterceptor.level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

        return OkHttpClient.Builder()
            .addInterceptor(loggerInterceptor)
            .connectTimeout(10000, TimeUnit.MILLISECONDS)
            .writeTimeout(10000, TimeUnit.MILLISECONDS)
            .readTimeout(10000, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * Provides the OkHttpClient with access token header.
     *
     * @return the client
     */
    @Provides
    @Named("accessToken")
    fun provideOkHttpClientWithAccessToken(sharedPreferences: SharedPreferences): OkHttpClient {
        val loggerInterceptor = HttpLoggingInterceptor()
        loggerInterceptor.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

        val bearerInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()

            sharedPreferences.getString("accessToken", null)?.let { accessToken ->
                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
            }

            return@Interceptor chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggerInterceptor)
            .addInterceptor(bearerInterceptor)
            .connectTimeout(10000, TimeUnit.MILLISECONDS)
            .writeTimeout(10000, TimeUnit.MILLISECONDS)
            .readTimeout(10000, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * Provides GSON instance.
     *
     * @return the instance
     */
    @Provides
    fun provideGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(
            Date::class.java,
            JsonDeserializer { j, _, _ -> Date(j.asLong) })
        gsonBuilder.registerTypeAdapter(
            Date::class.java,
            JsonSerializer<Date> { s, _, _ -> JsonPrimitive(s.time) })
        return gsonBuilder.create()
    }

    /**
     * Provides the Moshi JSON instance.
     *
     * @return the moshi instance
     */
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(LocaleJsonAdapter())
            .add(DateJsonAdapter())
            .add(UUIDJsonAdapter())
            .build()
    }

    /**
     * Provides the Retrofit instance with plain converter.
     *
     * @param okHttpClient the OkHttpClient instance
     * @return the instance
     */
    @Provides
    @Named("plain")
    fun providePlainRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(ProductConstants.SERVICE_BASE_URL)
            .client(okHttpClient)
            .build()
    }

    /**
     * Provides the Retrofit instance with GSON converter of GitHub RAW.
     *
     * @param okHttpClient the OkHttpClient instance
     * @return the instance
     */
    @Provides
    @Named("gitHubRaw")
    fun provideGitHubRawRetrofit(gson: Gson, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(GITHUB_BASE_URL)
            .client(okHttpClient)
            .build()
    }

    /**
     * Provides the Retrofit instance with GSON converter with access token header.
     *
     * @param okHttpClient the OkHttpClient instance
     * @return the instance
     */
    @Provides
    @Named("accessToken")
    fun provideGsonRetrofitWithAccessToken(gson: Gson, @Named("accessToken") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(ProductConstants.SERVICE_BASE_URL)
            .client(okHttpClient)
            .build()
    }

    /**
     * Provides the Retrofit instance with GSON converter.
     *
     * @param okHttpClient the OkHttpClient instance
     * @return the instance
     */
    @Provides
    fun provideGsonRetrofit(gson: Gson, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(ProductConstants.SERVICE_BASE_URL)
            .client(okHttpClient)
            .build()
    }
}
