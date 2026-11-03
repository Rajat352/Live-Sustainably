package com.livesustainably.di

import com.livesustainably.data.remote.AuthRemoteDataSource
import com.livesustainably.data.remote.FirebaseAuthRemoteDataSource
import com.livesustainably.data.remote.FirebaseInitialUserDataSource
import com.livesustainably.data.remote.InitialUserDataSource
import com.livesustainably.data.repository.AuthRepositoryImpl
import com.livesustainably.data.repository.FirebaseActivityRepository
import com.livesustainably.data.repository.FirebaseFeedRepository
import com.livesustainably.data.repository.FirebaseFriendsRepository
import com.livesustainably.data.repository.FirebaseProfileRepository
import com.livesustainably.domain.repository.ActivityRepository
import com.livesustainably.domain.repository.FeedRepository
import com.livesustainably.domain.repository.FriendsRepository
import com.livesustainably.domain.repository.ProfileRepository
import com.livesustainably.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindActivityRepository(repository: FirebaseActivityRepository): ActivityRepository

    @Binds
    abstract fun bindFeedRepository(repository: FirebaseFeedRepository): FeedRepository

    @Binds
    abstract fun bindFriendsRepository(repository: FirebaseFriendsRepository): FriendsRepository

    @Binds
    abstract fun bindProfileRepository(repository: FirebaseProfileRepository): ProfileRepository

    @Binds
    abstract fun bindAuthRepository(repository: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(source: FirebaseAuthRemoteDataSource): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindInitialUserDataSource(source: FirebaseInitialUserDataSource): InitialUserDataSource
}
