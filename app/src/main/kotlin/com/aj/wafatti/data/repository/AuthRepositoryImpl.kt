package com.aj.wafatti.data.repository

import android.util.Patterns
import com.aj.wafatti.core.common.Result
import com.aj.wafatti.core.utils.PhoneNumberNormalizer
import com.aj.wafatti.data.local.database.dao.UserDao
import com.aj.wafatti.data.local.database.entity.UserEntity
import com.aj.wafatti.data.local.datastore.UserPreferences
import com.aj.wafatti.domain.model.User
import com.aj.wafatti.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUserFlow: Flow<User?> = userPreferences.currentUserId.flatMapLatest { userId ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null || userId.isNullOrEmpty()) {
            flowOf(null)
        } else {
            userDao.getUserById(firebaseUser.uid).map {
                it?.toDomain() ?: User(
                    id = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                    email = firebaseUser.email ?: ""
                )
            }
        }
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Result.Error(Exception("Please enter your email address."))
        }
        if (password.isBlank()) {
            return Result.Error(Exception("Please enter your password."))
        }

        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user ?: return Result.Error(Exception("Authentication failed. User not found."))

            // Sync with Firestore profile users/{uid}
            var displayName = firebaseUser.displayName ?: trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            var phone = ""
            var normPhone = ""

            try {
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                if (userDoc.exists()) {
                    displayName = userDoc.getString("displayName") ?: displayName
                    phone = userDoc.getString("phoneNumber") ?: ""
                    normPhone = userDoc.getString("normalizedPhoneNumber") ?: PhoneNumberNormalizer.normalize(phone)
                } else {
                    // Legacy user profile creation
                    val userProfile = mapOf(
                        "uid" to firebaseUser.uid,
                        "displayName" to displayName,
                        "email" to trimmedEmail,
                        "phoneNumber" to phone,
                        "normalizedPhoneNumber" to normPhone,
                        "photoUrl" to null,
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(firebaseUser.uid).set(userProfile, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                // If Firestore profile fetch fails, continue with auth info
            }

            val user = User(
                id = firebaseUser.uid,
                displayName = displayName,
                email = trimmedEmail,
                phoneNumber = normPhone.ifBlank { phone },
                isOnline = true
            )
            userDao.insertUser(UserEntity.fromDomain(user))
            userPreferences.saveCurrentUserId(user.id)
            Result.Success(user)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(Exception("No account found with this email."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Error(Exception("Incorrect email or password."))
        } catch (e: FirebaseAuthException) {
            Result.Error(Exception(mapFirebaseError(e)))
        } catch (e: Exception) {
            Result.Error(Exception(e.localizedMessage ?: "Sign in failed. Please check your internet connection."))
        }
    }

    override suspend fun signUpWithEmail(displayName: String, email: String, phoneNumber: String, password: String): Result<User> {
        val trimmedEmail = email.trim()
        val trimmedName = displayName.trim()
        val trimmedPhone = phoneNumber.trim()
        val normalizedPhone = PhoneNumberNormalizer.normalize(trimmedPhone)

        if (trimmedName.isBlank()) {
            return Result.Error(Exception("Please enter your display name."))
        }
        if (trimmedEmail.isBlank()) {
            return Result.Error(Exception("Please enter your email address."))
        }
        if (trimmedPhone.isBlank()) {
            return Result.Error(Exception("Please enter your phone number."))
        }
        if (password.length < 6) {
            return Result.Error(Exception("Password must be at least 6 characters."))
        }

        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user ?: return Result.Error(Exception("Sign up failed. Please try again."))

            // Update Firebase Auth Display Name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(trimmedName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Save Firestore User Profile users/{uid}
            val userProfile = mapOf(
                "uid" to firebaseUser.uid,
                "displayName" to trimmedName,
                "email" to trimmedEmail,
                "phoneNumber" to trimmedPhone,
                "normalizedPhoneNumber" to normalizedPhone,
                "photoUrl" to null,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(firebaseUser.uid).set(userProfile).await()

            val user = User(
                id = firebaseUser.uid,
                displayName = trimmedName,
                email = trimmedEmail,
                phoneNumber = normalizedPhone,
                isOnline = true
            )
            userDao.insertUser(UserEntity.fromDomain(user))
            userPreferences.saveCurrentUserId(user.id)
            Result.Success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.Error(Exception("An account with this email already exists."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Error(Exception("Invalid email format."))
        } catch (e: FirebaseAuthException) {
            Result.Error(Exception(mapFirebaseError(e)))
        } catch (e: Exception) {
            Result.Error(Exception(e.localizedMessage ?: "Sign up failed. Please check your internet connection."))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Result.Error(Exception("Please enter your email address."))
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.Error(Exception("Please enter a valid email address."))
        }

        return try {
            firebaseAuth.sendPasswordResetEmail(trimmedEmail).await()
            Result.Success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(Exception("No account found with this email."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Error(Exception("Please enter a valid email address."))
        } catch (e: FirebaseAuthException) {
            Result.Error(Exception(mapFirebaseError(e)))
        } catch (e: Exception) {
            Result.Error(Exception(e.localizedMessage ?: "Failed to send reset link. Please check your internet connection."))
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            userPreferences.clearCurrentUserId()
        } catch (e: Exception) {
            userPreferences.clearCurrentUserId()
        }
    }

    private fun mapFirebaseError(e: FirebaseAuthException): String {
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address format."
            "ERROR_WRONG_PASSWORD" -> "Incorrect password."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email."
            "ERROR_USER_DISABLED" -> "This user account has been disabled."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Please try again later."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email/Password sign-in is not enabled in Firebase."
            else -> e.localizedMessage ?: "Authentication failed. Please try again."
        }
    }
}
