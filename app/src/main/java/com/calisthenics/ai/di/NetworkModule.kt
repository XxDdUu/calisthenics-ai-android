package com.calisthenics.ai.di

import android.util.Log
import com.calisthenics.ai.BuildConfig
import com.calisthenics.ai.data.remote.api.AuthInterceptor
import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.api.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "CalisthenicsHttp"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val diagnosticInterceptor = Interceptor { chain ->
            val req = chain.request()
            val startMs = System.currentTimeMillis()
            try {
                val res = chain.proceed(req)
                val elapsedMs = System.currentTimeMillis() - startMs
                if (req.url.encodedPath.contains("/admin/ai") || elapsedMs > 5000L || !res.isSuccessful) {
                    Log.i(TAG, "${req.method} ${req.url.encodedPath} -> ${res.code} (${elapsedMs}ms)")
                }
                res
            } catch (ex: Exception) {
                val elapsedMs = System.currentTimeMillis() - startMs
                Log.w(TAG, "${req.method} ${req.url.encodedPath} failed after ${elapsedMs}ms: ${ex.javaClass.simpleName}")
                throw ex
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(diagnosticInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCalisthenicsApi(retrofit: Retrofit): CalisthenicsApi {
        return retrofit.create(CalisthenicsApi::class.java)
    }
}
