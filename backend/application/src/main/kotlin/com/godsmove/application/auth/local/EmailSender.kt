package com.godsmove.application.auth.local

interface EmailSender {
    fun sendVerificationCode(email: String, code: String)
}
