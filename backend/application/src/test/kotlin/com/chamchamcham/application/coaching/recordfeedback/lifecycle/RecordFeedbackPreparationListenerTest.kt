package com.chamchamcham.application.coaching.recordfeedback.lifecycle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Async

class RecordFeedbackPreparationListenerTest {
    @Test
    fun `record feedback preparation listener is the only async entry point`() {
        val preparationOn = RecordFeedbackPreparationListener::class.java.getDeclaredMethod(
            "on",
            RecordFeedbackPreparationRequested::class.java,
        )
        val generationOn = RecordFeedbackGenerationListener::class.java.getDeclaredMethod(
            "on",
            RecordFeedbackGenerationRequested::class.java,
        )

        assertThat(preparationOn.isAnnotationPresent(Async::class.java)).isTrue()
        assertThat(generationOn.isAnnotationPresent(Async::class.java)).isFalse()
    }
}
