package com.example.model

/**
 * UI State for AI-generated briefings.
 */
sealed class BriefingState {
    object Initial : BriefingState()
    object Loading : BriefingState()
    data class Success(val content: String) : BriefingState()
    data class Error(val message: String) : BriefingState()
}
