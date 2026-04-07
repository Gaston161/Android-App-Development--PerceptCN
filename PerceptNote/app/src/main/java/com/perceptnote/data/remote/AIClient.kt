// 📄 data/remote/AIClient.kt — VERSION OPENROUTER (gratuit)
package com.perceptnote.data.remote

import com.perceptnote.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AIClient {

    // OpenRouter — compatible avec le format Claude/OpenAI
    private const val BASE_URL = "https://openrouter.ai/api/v1/"

    private val apiKeyInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            // OpenRouter utilise Bearer token (pas x-api-key)
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENROUTER_API_KEY}")
            // Optionnel mais recommandé par OpenRouter
            .addHeader("HTTP-Referer", "com.perceptnote")
            .addHeader("X-Title", "PerceptNote")
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiKeyInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun create(): AIApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AIApiService::class.java)
}