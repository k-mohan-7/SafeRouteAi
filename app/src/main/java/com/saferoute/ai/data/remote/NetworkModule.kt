package com.saferoute.ai.data.remote

import com.saferoute.ai.BuildConfig
import com.saferoute.ai.data.local.SessionDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionDataStore: SessionDataStore): Interceptor = Interceptor { chain ->
        val userId = sessionDataStore.getUserIdBlocking()
        val request = if (userId != null) {
            chain.request().newBuilder()
                .header("X-User-Id", userId.toString())
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @Named("api")
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideApiService(@Named("api") retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    @Named("osrm")
    fun provideOsrmRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://router.project-osrm.org/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideOsrmService(@Named("osrm") retrofit: Retrofit): OsrmService =
        retrofit.create(OsrmService::class.java)
}
