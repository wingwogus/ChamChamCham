package com.chamchamcham.application.policy

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PolicyCardTextGenerator {
    fun eligibilitySummary(text: String?): String {
        val source = text.orEmpty()
        return when {
            source.contains("친환경인증") || source.contains("친환경 인증") -> "친환경 인증 농업인"
            source.contains("청년") -> "청년 농업인"
            source.contains("귀농") -> "귀농 예정 농업인"
            source.contains("농업경영정보") -> "경영정보 등록 농업인"
            source.isBlank() -> "상세 자격 확인"
            else -> compact(source, "상세 자격 확인")
        }
    }

    fun benefitSummary(text: String?): String {
        val source = text.orEmpty()
        return when {
            source.contains("직불금") -> "인증단계별 직불금"
            source.contains("시설") -> "재배시설 지원"
            source.contains("교육") -> "교육 프로그램 지원"
            source.contains("융자") -> "정책자금 융자"
            source.isBlank() -> "상세 지원 확인"
            else -> compact(source, "상세 지원 확인")
        }
    }

    fun periodLabel(start: LocalDate?, end: LocalDate?, notice: String?): String {
        val sourceNotice = notice?.trim()?.takeIf(String::isNotEmpty)
        val label = when {
            start != null && end != null -> "${start.formatFull()}~${end.formatMonthDay()}"
            sourceNotice != null -> sourceNotice
            end != null -> "${end.formatFull()}까지"
            else -> "접수기관문의"
        }
        return compact(label, "접수기관문의")
    }

    private fun compact(text: String, fallback: String): String {
        val normalized = text.replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) {
            return fallback
        }
        return normalized.take(MAX_CARD_CHARS)
    }

    private fun LocalDate.formatFull(): String = "%04d.%02d.%02d".format(year, monthValue, dayOfMonth)

    private fun LocalDate.formatMonthDay(): String = "%02d.%02d".format(monthValue, dayOfMonth)

    private companion object {
        const val MAX_CARD_CHARS = 19
    }
}
