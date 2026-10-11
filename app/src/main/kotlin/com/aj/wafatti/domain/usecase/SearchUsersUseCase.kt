package com.aj.wafatti.domain.usecase

import com.aj.wafatti.core.common.Result
import com.aj.wafatti.domain.model.User
import com.aj.wafatti.domain.repository.UserRepository
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(query: String): Result<List<User>> {
        return if (query.isBlank()) {
            Result.Success(emptyList())
        } else {
            userRepository.searchUsers(query)
        }
    }
}
