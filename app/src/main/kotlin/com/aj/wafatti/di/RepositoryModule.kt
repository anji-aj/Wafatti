package com.aj.wafatti.di

import com.aj.wafatti.data.repository.AuthRepositoryImpl
import com.aj.wafatti.data.repository.ChatRepositoryImpl
import com.aj.wafatti.data.repository.UserRepositoryImpl
import com.aj.wafatti.domain.repository.AuthRepository
import com.aj.wafatti.domain.repository.ChatRepository
import com.aj.wafatti.domain.repository.UserRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
