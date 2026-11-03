package com.livesustainably.di

import com.livesustainably.data.repository.FirebaseSessionRepository
import com.livesustainably.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSessionRepository(repository: FirebaseSessionRepository): SessionRepository
}
