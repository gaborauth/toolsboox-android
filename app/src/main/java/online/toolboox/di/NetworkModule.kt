package online.toolboox.di

import com.google.gson.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import online.toolboox.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Retrofit DI module of network services.
 *
 * @author <a href="mailto:auth.gabor@gmail.com">Gábor AUTH</a>
 */
@Module
object NetworkModule {

    /**
     * Base URL of the service.
     */
    private const val SERVICE_BASE_URL = "https://api.toolboox.online/"

    /**
     * Provides the OkHttpClient.
     *
     * @return the client
     */
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val loggerInterceptor = HttpLoggingInterceptor()
        loggerInterceptor.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

        val retryInterceptor = Interceptor { chain ->
            val request: Request = chain.request()

            var response: Response = chain.proceed(request)
            if (response.isSuccessful) return@Interceptor response

            for (tryCount in 1..3) {
                Timber.i("Request is not successful ${response.code()}: retry attempt ($tryCount/3)")
                response = chain.proceed(request)

                if (response.isSuccessful) return@Interceptor response
            }

            return@Interceptor response
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggerInterceptor)
            .addInterceptor(retryInterceptor)
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
    @Singleton
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
     * Provides the Retrofit instance with plain converter.
     *
     * @param okHttpClient the OkHttpClient instance
     * @return the instance
     */
    @Provides
    @Singleton
    @Named("plain")
    fun providePlainRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(SERVICE_BASE_URL)
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
    @Singleton
    fun provideCoroutineRetrofit(gson: Gson, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(SERVICE_BASE_URL)
            .client(okHttpClient)
            .build()
    }
}
