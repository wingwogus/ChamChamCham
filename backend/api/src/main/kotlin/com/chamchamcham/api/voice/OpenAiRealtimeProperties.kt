package com.chamchamcham.api.voice

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "openai.realtime")
data class OpenAiRealtimeProperties(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val voice: String,
    val connectTimeoutMillis: Int,
    val readTimeoutMillis: Int,
    /** client_secret 만료(초). 대화 시간 한도(app.voice-session)와 함께 세션이 무한정 살아있지 않게 한다. */
    val expiresAfterSeconds: Int = 360,
)
