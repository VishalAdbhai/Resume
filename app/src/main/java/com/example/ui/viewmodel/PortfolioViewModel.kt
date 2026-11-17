package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ResumeProfile
import com.example.data.model.ResumeTemplate
import com.example.data.model.UiState
import com.example.data.repository.ResumeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PortfolioViewModel(
    private val repository: ResumeRepository,
    private val currentUserId: String
) : ViewModel() {

    val resumesState: StateFlow<UiState<List<ResumeProfile>>> = repository.observeResumes(currentUserId)
        .map<List<ResumeProfile>, UiState<List<ResumeProfile>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing resumes", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load your resumes."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    fun createResume(
        title: String,
        template: ResumeTemplate,
        initialFullName: String,
        initialEmail: String,
        onCreated: (String) -> Unit
    ) {
        if (_isMutating.value) return
        _isMutating.value = true
        viewModelScope.launch {
            val cleanTitle = title.trim().ifBlank { "My Resume" }.take(100)
            val newResume = ResumeProfile(
                userId = currentUserId,
                title = cleanTitle,
                templateId = template.name,
                fullName = initialFullName.trim().take(120),
                email = initialEmail.trim().take(150)
            )
            repository.createResume(currentUserId, newResume)
                .onSuccess { createdId ->
                    _isMutating.value = false
                    onCreated(createdId)
                }
                .onFailure { err ->
                    _isMutating.value = false
                    _bannerMessage.value = err.localizedMessage ?: "Unable to create resume."
                }
        }
    }

    fun duplicateResume(source: ResumeProfile) {
        if (_isMutating.value) return
        _isMutating.value = true
        viewModelScope.launch {
            val copyTitle = "${source.title.take(90)} (Copy)"
            repository.duplicateResume(currentUserId, source, copyTitle)
                .onSuccess {
                    _isMutating.value = false
                    _bannerMessage.value = "Created \"$copyTitle\""
                }
                .onFailure { err ->
                    _isMutating.value = false
                    _bannerMessage.value = err.localizedMessage ?: "Unable to duplicate resume."
                }
        }
    }

    fun deleteResume(resumeId: String, onDeletedLocally: (String) -> Unit = {}) {
        if (_isMutating.value) return
        _isMutating.value = true
        viewModelScope.launch {
            repository.deleteResume(currentUserId, resumeId)
                .onSuccess {
                    onDeletedLocally(resumeId)
                    _isMutating.value = false
                    _bannerMessage.value = "Resume deleted"
                }
                .onFailure { err ->
                    _isMutating.value = false
                    _bannerMessage.value = err.localizedMessage ?: "Unable to delete resume."
                }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    private companion object {
        const val TAG = "PortfolioVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
