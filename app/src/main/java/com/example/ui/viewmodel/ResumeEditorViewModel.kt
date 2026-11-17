package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.EducationItem
import com.example.data.model.ExperienceItem
import com.example.data.model.PdfPageSize
import com.example.data.model.ProjectItem
import com.example.data.model.ResumeAccent
import com.example.data.model.ResumeProfile
import com.example.data.model.ResumeTemplate
import com.example.data.model.UiState
import com.example.data.repository.ResumeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ResumeEditorViewModel(
    private val repository: ResumeRepository,
    private val currentUserId: String,
    private val resumeId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ResumeProfile>>(UiState.Loading)
    val uiState: StateFlow<UiState<ResumeProfile>> = _uiState.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        observeSelectedResume()
    }

    private fun observeSelectedResume() {
        viewModelScope.launch {
            repository.observeResume(currentUserId, resumeId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing resume $resumeId", rootError)
                    _uiState.value = UiState.Error(rootError.message ?: "Failed to load resume.")
                }
                .collect { remote ->
                    if (remote == null) {
                        _uiState.value = UiState.Error("Resume not found or was deleted.")
                    } else if (!_hasUnsavedChanges.value || _uiState.value !is UiState.Success) {
                        _uiState.value = UiState.Success(remote)
                    }
                }
        }
    }

    private fun updateDraft(transform: (ResumeProfile) -> ResumeProfile) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(transform(current))
        _hasUnsavedChanges.value = true
    }

    fun updateTitle(title: String) {
        updateDraft { it.copy(title = title.take(100)) }
    }

    fun updateBasics(
        fullName: String,
        headline: String,
        email: String,
        phone: String,
        location: String,
        website: String,
        linkedIn: String
    ) {
        updateDraft {
            it.copy(
                fullName = fullName.take(120),
                headline = headline.take(150),
                email = email.take(150),
                phone = phone.take(50),
                location = location.take(120),
                website = website.take(250),
                linkedIn = linkedIn.take(250)
            )
        }
    }

    fun updateSummary(summary: String) {
        updateDraft { it.copy(summary = summary.take(2000)) }
    }

    fun updateShowPhotoInPdf(show: Boolean) {
        updateDraft { it.copy(showPhotoInPdf = show) }
    }

    fun upsertExperience(item: ExperienceItem) {
        updateDraft { draft ->
            val existingIdx = draft.experiences.indexOfFirst { it.entryId == item.entryId }
            val updated = if (existingIdx >= 0) {
                draft.experiences.toMutableList().apply { set(existingIdx, item) }
            } else {
                if (draft.experiences.size >= 30) draft.experiences else listOf(item) + draft.experiences
            }
            draft.copy(experiences = updated)
        }
    }

    fun deleteExperience(entryId: String) {
        updateDraft { draft ->
            draft.copy(experiences = draft.experiences.filterNot { it.entryId == entryId })
        }
    }

    fun moveExperience(entryId: String, delta: Int) {
        updateDraft { draft ->
            val list = draft.experiences.toMutableList()
            val idx = list.indexOfFirst { it.entryId == entryId }
            val target = idx + delta
            if (idx >= 0 && target in list.indices) {
                val moved = list.removeAt(idx)
                list.add(target, moved)
            }
            draft.copy(experiences = list)
        }
    }

    fun upsertEducation(item: EducationItem) {
        updateDraft { draft ->
            val existingIdx = draft.educations.indexOfFirst { it.entryId == item.entryId }
            val updated = if (existingIdx >= 0) {
                draft.educations.toMutableList().apply { set(existingIdx, item) }
            } else {
                if (draft.educations.size >= 20) draft.educations else listOf(item) + draft.educations
            }
            draft.copy(educations = updated)
        }
    }

    fun deleteEducation(entryId: String) {
        updateDraft { draft ->
            draft.copy(educations = draft.educations.filterNot { it.entryId == entryId })
        }
    }

    fun moveEducation(entryId: String, delta: Int) {
        updateDraft { draft ->
            val list = draft.educations.toMutableList()
            val idx = list.indexOfFirst { it.entryId == entryId }
            val target = idx + delta
            if (idx >= 0 && target in list.indices) {
                val moved = list.removeAt(idx)
                list.add(target, moved)
            }
            draft.copy(educations = list)
        }
    }

    fun addSkill(rawSkill: String) {
        val clean = rawSkill.trim()
        if (clean.isEmpty()) return
        updateDraft { draft ->
            if (draft.skills.size >= 50 || draft.skills.any { it.equals(clean, ignoreCase = true) }) {
                draft
            } else {
                draft.copy(skills = draft.skills + clean)
            }
        }
    }

    fun removeSkill(skill: String) {
        updateDraft { draft ->
            draft.copy(skills = draft.skills.filterNot { it == skill })
        }
    }

    fun upsertProject(item: ProjectItem) {
        updateDraft { draft ->
            val existingIdx = draft.projects.indexOfFirst { it.entryId == item.entryId }
            val updated = if (existingIdx >= 0) {
                draft.projects.toMutableList().apply { set(existingIdx, item) }
            } else {
                if (draft.projects.size >= 30) draft.projects else listOf(item) + draft.projects
            }
            draft.copy(projects = updated)
        }
    }

    fun deleteProject(entryId: String) {
        updateDraft { draft ->
            draft.copy(projects = draft.projects.filterNot { it.entryId == entryId })
        }
    }

    fun updateStyleSettings(
        template: ResumeTemplate? = null,
        accent: ResumeAccent? = null,
        pageSize: PdfPageSize? = null,
        showPhotoInPdf: Boolean? = null,
        autoSave: Boolean = true
    ) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        val updated = current.copy(
            templateId = template?.name ?: current.templateId,
            accentId = accent?.name ?: current.accentId,
            pageSize = pageSize?.name ?: current.pageSize,
            showPhotoInPdf = showPhotoInPdf ?: current.showPhotoInPdf
        )
        _uiState.value = UiState.Success(updated)
        _hasUnsavedChanges.value = true
        if (autoSave) {
            saveChanges(showSavedToast = false)
        }
    }

    fun saveChanges(showSavedToast: Boolean = true, onSaved: () -> Unit = {}) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            repository.updateResume(currentUserId, resumeId, current)
                .onSuccess {
                    _isSaving.value = false
                    _hasUnsavedChanges.value = false
                    if (showSavedToast) {
                        _statusMessage.value = "Saved to cloud"
                    }
                    onSaved()
                }
                .onFailure { err ->
                    _isSaving.value = false
                    _statusMessage.value = err.localizedMessage ?: "Failed to save resume."
                }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private companion object {
        const val TAG = "ResumeEditorVM"
    }
}
