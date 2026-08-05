package com.catchapp.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catchapp.app.data.CaptureRepository
import com.catchapp.app.data.local.CaptureEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CaptureRepository
) : ViewModel() {

    private val captureId: Long = checkNotNull(savedStateHandle["captureId"])

    val capture: StateFlow<CaptureEntity?> = repository.observeById(captureId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun confirm() {
        viewModelScope.launch { repository.confirmCapture(captureId) }
    }

    fun discard() {
        viewModelScope.launch { repository.discardCapture(captureId) }
    }

    fun delete() {
        viewModelScope.launch { repository.deleteCapture(captureId) }
    }

    fun retry() {
        viewModelScope.launch { repository.retryStructuring(captureId) }
    }
}
