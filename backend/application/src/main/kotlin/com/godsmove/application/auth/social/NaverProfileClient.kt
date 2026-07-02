package com.godsmove.application.auth.social

interface NaverProfileClient {
    fun fetch(accessToken: String): NaverProfile
}
