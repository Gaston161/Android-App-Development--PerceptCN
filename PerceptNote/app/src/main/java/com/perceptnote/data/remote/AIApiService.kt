// 📄 app/src/main/java/com/perceptnote/data/remote/AIApiService.kt
package com.perceptnote.data.remote

import com.perceptnote.data.remote.dto.MessageRequest
import com.perceptnote.data.remote.dto.MessageResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AIApiService {

    /**
     * Envoie un message à l'API Claude et retourne la réponse.
     * Les headers statiques (anthropic-version, content-type) sont définis ici.
     * La clé API (x-api-key) est injectée dynamiquement par l'interceptor OkHttp.
     */
    @Headers(
        "anthropic-version: 2023-06-01",
        "content-type: application/json"
    )
    @POST("v1/messages")
    suspend fun sendMessage(@Body request: MessageRequest): MessageResponse
}
