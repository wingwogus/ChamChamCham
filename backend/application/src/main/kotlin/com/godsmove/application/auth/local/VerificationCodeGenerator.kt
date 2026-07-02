package com.godsmove.application.auth.local

interface VerificationCodeGenerator {
    fun generate(): String
}
