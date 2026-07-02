package com.godsmove.application.auth

interface NaverProfileClient {
    fun fetch(accessToken: String): NaverProfile
}
