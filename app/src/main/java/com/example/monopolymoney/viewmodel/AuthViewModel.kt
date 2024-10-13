package com.example.monopolymoney.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.monopolymoney.data.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    sealed class RegistrationState {
        object None : RegistrationState()
        object EmailVerificationSent : RegistrationState()
        object EmailVerified : RegistrationState()
        object NeedsProfile : RegistrationState()
        object Complete : RegistrationState()
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("users")
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("UserCredentials", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.None)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    init {
        verifyStoredCredentials()
    }

    fun resetAuthState() {
        _authState.value = AuthState.Unauthenticated
        _registrationState.value = RegistrationState.None
        _user.value = null
        clearUserCredentials()
    }

    fun verifyStoredCredentials() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val storedUuid = sharedPreferences.getString("uuid", null)

            if (storedUuid != null) {
                try {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        // Recargar el usuario para obtener el estado de verificación más reciente
                        firebaseUser.reload().await()

                        val user = getUserFromDatabase(storedUuid)
                        if (user != null) {
                            _user.value = user

                            if (!firebaseUser.isEmailVerified) {
                                _registrationState.value = RegistrationState.EmailVerificationSent
                                _authState.value = AuthState.Authenticated(firebaseUser)
                            } else if (user.name.isBlank() || user.profileImageResId == 0) {
                                _registrationState.value = RegistrationState.NeedsProfile
                                _authState.value = AuthState.Authenticated(firebaseUser)
                            } else {
                                _registrationState.value = RegistrationState.Complete
                                _authState.value = AuthState.Authenticated(firebaseUser)
                            }
                        } else {
                            handleInvalidCredentials()
                        }
                    } else {
                        handleInvalidCredentials()
                    }
                } catch (e: Exception) {
                    handleInvalidCredentials()
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private suspend fun handleSilentAuth(uuid: String) {
        try {
            // Aquí podrías implementar una autenticación silenciosa con Firebase
            // Por ahora, simplemente marcamos como no autenticado
            handleInvalidCredentials()
        } catch (e: Exception) {
            handleInvalidCredentials()
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null && user.email != null) {
                    // Re-authenticate user before changing password
                    val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                    user.reauthenticate(credential).await()

                    // Change password
                    user.updatePassword(newPassword).await()
                    _authState.value = AuthState.Authenticated(user)
                }
            } catch (e: Exception) {
                when (e) {
                    is FirebaseAuthWeakPasswordException ->
                        _authState.value =
                            AuthState.Error("Password should be at least 6 characters")

                    is FirebaseAuthInvalidCredentialsException ->
                        _authState.value = AuthState.Error("Current password is incorrect")

                    else -> _authState.value =
                        AuthState.Error(e.message ?: "Failed to change password")
                }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null) {
                    // Delete user data from database
                    database.child(user.uid).removeValue().await()

                    // Delete Firebase Auth account
                    user.delete().await()

                    // Clear local data
                    clearUserCredentials()
                    _user.value = null
                    _registrationState.value = RegistrationState.None
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to delete account")
            }
        }
    }

    private fun handleInvalidCredentials() {
        clearUserCredentials()
        _user.value = null
        _registrationState.value = RegistrationState.None
        _authState.value = AuthState.Unauthenticated
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // Recargar el usuario para obtener el estado de verificación más reciente
                    firebaseUser.reload().await()

                    val user = getUserFromDatabase(firebaseUser.uid)
                    if (user != null) {
                        _user.value = user
                        saveUserUuid(firebaseUser.uid)

                        if (!firebaseUser.isEmailVerified) {
                            _registrationState.value = RegistrationState.EmailVerificationSent
                            _authState.value = AuthState.Authenticated(firebaseUser)
                        } else if (user.name.isBlank() || user.profileImageResId == 0) {
                            _registrationState.value = RegistrationState.NeedsProfile
                            _authState.value = AuthState.Authenticated(firebaseUser)
                        } else {
                            _registrationState.value = RegistrationState.Complete
                            _authState.value = AuthState.Authenticated(firebaseUser)
                        }
                    } else {
                        _authState.value = AuthState.Error("User data not found")
                    }
                }
            } catch (e: Exception) {
                _authState.value = when (e) {
                    is FirebaseAuthInvalidUserException ->
                        AuthState.Error("User does not exist. Please register.")
                    else -> AuthState.Error(e.message ?: "Authentication failed")
                }
            }
        }
    }


    fun createUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // Send email verification
                    firebaseUser.sendEmailVerification().await()

                    // Create user in database but mark as unverified
                    val newUser = User(
                        uuid = firebaseUser.uid,
                        email = email,
                        name = "",
                        profileImageResId = 0,
                        isEmailVerified = false
                    )
                    createUserInDatabase(newUser)

                    // Update states
                    _user.value = newUser
                    _registrationState.value = RegistrationState.EmailVerificationSent
                    _authState.value = AuthState.Authenticated(firebaseUser)

                    // Save UUID but mark as unverified
                    saveUserUuid(firebaseUser.uid, false)
                } else {
                    _authState.value = AuthState.Error("User creation failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "User creation failed")
            }
        }
    }

    fun verifyEmail() {
        viewModelScope.launch {
            try {
                auth.currentUser?.let { firebaseUser ->
                    firebaseUser.reload().await()
                    if (firebaseUser.isEmailVerified) {
                        // Update user in database
                        _user.value?.let { currentUser ->
                            val updatedUser = currentUser.copy(isEmailVerified = true)
                            updateUserInDatabase(updatedUser)
                            _user.value = updatedUser
                        }

                        // Update registration state
                        _registrationState.value = RegistrationState.NeedsProfile

                        // Update auth state
                        _authState.value = AuthState.Authenticated(firebaseUser)

                        // Update shared preferences
                        saveUserUuid(firebaseUser.uid, true)
                    } else {
                        _authState.value = AuthState.Error("Email not verified yet")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to verify email")
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                auth.currentUser?.let { firebaseUser ->
                    firebaseUser.sendEmailVerification().await()
                    _registrationState.value = RegistrationState.EmailVerificationSent
                }
            } catch (e: Exception) {
                _authState.value =
                    AuthState.Error(e.message ?: "Failed to resend verification email")
            }
        }
    }

    private fun saveUserUuid(uuid: String, isVerified: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putString("uuid", uuid)
        editor.putBoolean("isEmailVerified", isVerified)
        editor.apply()
    }

    private suspend fun createUserInDatabase(user: User) {
        try {
            database.child(user.uuid).setValue(user.toMap()).await()
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Failed to create user in database")
        }
    }

    fun updateUserProfile(name: String, profileImageResId: Int) {
        viewModelScope.launch {
            try {
                _user.value?.let { currentUser ->
                    val updatedUser = currentUser.copy(
                        name = name,
                        profileImageResId = profileImageResId,
                        isEmailVerified = true
                    )

                    // Update in database
                    database.child(updatedUser.uuid).updateChildren(updatedUser.toMap()).await()

                    // Update local state
                    _user.value = updatedUser

                    // Update registration state if needed
                    if (name.isNotBlank() && profileImageResId != 0) {
                        _registrationState.value = RegistrationState.Complete
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to update profile")
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.ResetEmailSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    private suspend fun getUserFromDatabase(uuid: String): User? {
        return try {
            val snapshot = database.child(uuid).get().await()
            snapshot.getValue<Map<String, Any?>>()?.let { User.fromMap(it) }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun updateUserInDatabase(user: User) {
        database.child(user.uuid).updateChildren(user.toMap()).await()
    }

    private fun saveUserUuid(uuid: String) {
        val editor = sharedPreferences.edit()
        editor.putString("uuid", uuid)
        editor.apply()
    }

    private fun clearUserCredentials() {
        val editor = sharedPreferences.edit()
        editor.remove("uuid")
        editor.apply()
    }

    fun signOut() {
        auth.signOut()
        clearUserCredentials()
        _user.value = null
        _authState.value = AuthState.Unauthenticated
    }

    sealed class AuthState {
        object Unauthenticated : AuthState()
        object Loading : AuthState()
        data class Authenticated(val firebaseUser: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
        object ResetEmailSent : AuthState()
    }
}