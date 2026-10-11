package com.aj.wafatti.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aj.wafatti.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String,
    val phoneNumber: String = "",
    val avatarUrl: String?,
    val isOnline: Boolean,
    val lastSeen: Long,
    val bio: String,
    val savedName: String? = null
) {
    fun toDomain(): User = User(
        id = id,
        displayName = displayName,
        email = email,
        phoneNumber = phoneNumber,
        avatarUrl = avatarUrl,
        isOnline = isOnline,
        lastSeen = lastSeen,
        bio = bio,
        savedName = savedName
    )

    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            displayName = user.displayName,
            email = user.email,
            phoneNumber = user.phoneNumber,
            avatarUrl = user.avatarUrl,
            isOnline = user.isOnline,
            lastSeen = user.lastSeen,
            bio = user.bio,
            savedName = user.savedName
        )
    }
}
