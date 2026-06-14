package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BriefingRepository
import com.example.model.BriefingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing AI Briefing states.
 * Incorporates BriefingRepository for dual-path (Local Nano / Cloud Gemini) logic.
 */
class BriefingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val db = AppDatabase.getDatabase(application)
    private val repository = BriefingRepository(application.applicationContext, db)

    private val _briefingState = MutableStateFlow<BriefingState>(BriefingState.Initial)
    val briefingState = _briefingState.asStateFlow()

    /**
     * Triggers a briefing generation for a given prompt.
     * Manages Loading -> Success/Error state transitions.
     */
    fun fetchBriefing(prompt: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _briefingState.value = BriefingState.Loading
            try {
                val result = repository.getBriefingAction(prompt, forceRefresh)
                // We consider "無法取得" as a soft error handled by the success state if it's literal,
                // but let's check for specific failure indicators.
                if (result.contains("發生未知錯誤") || result.contains("無法取得 AI 摘要")) {
                    _briefingState.value = BriefingState.Error(result)
                } else {
                    _briefingState.value = BriefingState.Success(result)
                }
            } catch (e: Exception) {
                _briefingState.value = BriefingState.Error(e.message ?: "發生未知錯誤")
            }
        }
    }

    /**
     * Clears the current briefing state.
     */
    fun resetState() {
        _briefingState.value = BriefingState.Initial
    }
}
