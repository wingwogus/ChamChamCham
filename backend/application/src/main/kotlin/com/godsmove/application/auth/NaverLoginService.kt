package com.godsmove.application.auth

import com.godsmove.application.exception.ErrorCode
import com.godsmove.domain.member.AuthProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NaverLoginService(
    private val naverProfileClient: NaverProfileClient,
    private val socialLoginSupport: SocialLoginSupport
) {
    fun login(command: AuthCommand.NaverLogin): AuthResult.Login {
        val profile = naverProfileClient.fetch(command.accessToken)

        return socialLoginSupport.login(
            provider = AuthProvider.NAVER,
            providerSubject = profile.subject,
            email = profile.email,
            emailRequiredErrorCode = ErrorCode.NAVER_EMAIL_REQUIRED,
            name = profile.name,
            phone = profile.phone,
            birthDate = profile.birthDate
        )
    }
}
