package com.catch.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catch.app.data.CaptureRepository
import com.catch.app.data.local.CaptureEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    repository: CaptureRepository
) : ViewModel() {

    val captures: StateFlow<List<CaptureEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
