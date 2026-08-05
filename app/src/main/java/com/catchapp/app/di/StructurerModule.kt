package com.catchapp.app.di

import com.catchapp.app.data.remote.gemini.GeminiCaptureStructurer
import com.catchapp.app.domain.CaptureStructurer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the CaptureStructurer interface to the Gemini implementation. This
 * is the one line that would change to swap providers later — nothing that
 * calls CaptureStructurer needs to know or care.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StructurerModule {

    @Binds
    @Singleton
    abstract fun bindCaptureStructurer(impl: GeminiCaptureStructurer): CaptureStructurer
}
