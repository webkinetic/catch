package com.catchapp.app.ui.onboarding

import androidx.lifecycle.ViewModel
import com.catchapp.app.data.remote.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    fun hasKey(): Boolean = apiKeyStore.hasKey()

    fun currentKey(): String = apiKeyStore.getKey().orEmpty()

    fun saveKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotEmpty()) apiKeyStore.setKey(trimmed)
    }
}
