//
//  LoginResponseDTO.swift
//  ChamChamCham
//
//  Created by iyungui on 7/3/26.
//

import Foundation

struct LoginResponseDTO: Decodable, Sendable {
    let accessToken: String
    let refreshToken: String
    let member: MemberProfileResponseDTO
    let onboarding: OnboardingResponseDTO
}

enum OnboardingStatusDTO: String, Decodable, Sendable {
    case required = "REQUIRED"
    case complete = "COMPLETE"
}

struct OnboardingResponseDTO: Decodable, Sendable {
    let status: OnboardingStatusDTO
    let missingFields: [String]
}
