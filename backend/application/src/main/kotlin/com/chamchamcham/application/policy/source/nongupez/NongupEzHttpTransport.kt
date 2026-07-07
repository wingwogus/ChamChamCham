package com.chamchamcham.application.policy.source.nongupez

fun interface NongupEzHttpTransport {
    fun post(path: String, form: Map<String, String>): String
}
