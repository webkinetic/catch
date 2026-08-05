package com.catchapp.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catchapp.app.data.remote.ApiKeyStore
import com.catchapp.app.domain.CaptureStructurer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface KeyTestState {
    data object Idle : KeyTestState
    data object Testing : KeyTestState
    data class Result(val success: Boolean, val message: String) : KeyTestState
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
    private val structurer: CaptureStructurer
) : ViewModel() {

    fun hasKey(): Boolean = apiKeyStore.hasKey()

    fun currentKey(): String = apiKeyStore.getKey().orEmpty()

    fun saveKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotEmpty()) apiKeyStore.setKey(trimmed)
    }

    private val _testState = MutableStateFlow<KeyTestState>(KeyTestState.Idle)
    val testState: StateFlow<KeyTestState> = _testState.asStateFlow()

    /** Tests whatever is currently typed — doesn't require it to be saved first. */
    fun testKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            _testState.value = KeyTestState.Result(false, "Paste a key first.")
            return
        }

        _testState.value = KeyTestState.Testing
        viewModelScope.launch {
            val result = structurer.verifyKey(trimmed)
            _testState.value = result.fold(
                onSuccess = { KeyTestState.Result(true, "Key works — Gemini responded.") },
                onFailure = { KeyTestState.Result(false, it.message ?: "Something went wrong.") }
            )
        }
    }
}
