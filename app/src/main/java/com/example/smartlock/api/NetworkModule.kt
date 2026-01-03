package com.example.smartlock.api

import android.content.Context
import android.util.Log
import com.example.smartlock.di.AuthRetrofit
import com.example.smartlock.di.RefreshRetrofit
import com.example.smartlock.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    private const val BASE_URL = "https://smartkey-t7rg.onrender.com/"

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): Interceptor{
        return Interceptor { chain ->
            Log.d("AUTH_DEBUG", "=== INTERCEPTOR BẮT ĐẦU ===")

            val request = chain.request()
            val token = tokenManager.getAccessToken()

            Log.d("AUTH_DEBUG", "URL: ${request.url}")
            Log.d("AUTH_DEBUG", "Token: $token")

            val newRequest = if(token != null){
                request.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                Log.e("AUTH_DEBUG", "⚠TOKEN NULL!")
                request
            }

            val response = chain.proceed(newRequest)
            Log.d("AUTH_DEBUG", "Response Code: ${response.code}")

            if (response.code == 401) {
                Log.e("AUTH_DEBUG", "LỖI 401 - Token không hợp lệ hoặc hết hạn")
            }

            response
        }
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor{
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    @RefreshRetrofit
    fun provideRefreshRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideRefreshApi(): RefreshApi =
        provideRefreshRetrofit().create(RefreshApi::class.java)

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenManager: TokenManager,
        refreshApi: RefreshApi
    ): TokenAuthenticator = TokenAuthenticator(tokenManager, refreshApi)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        authenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    @Provides
    @Singleton
    @AuthRetrofit
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideSmartLockApiService(@AuthRetrofit retrofit: Retrofit): SmartLockApiService {
        return retrofit.create(SmartLockApiService::class.java)
    }
}