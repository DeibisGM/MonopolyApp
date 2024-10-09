package com.example.monopolymoney.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.monopolymoney.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    // Agregar nuevo estado para el registro
    sealed class RegistrationState {
        object None : RegistrationState()
        object NeedsProfile : RegistrationState()
        object Complete : RegistrationState()
    }
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("users")
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("UserCredentials", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.None)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    init {
        verifyStoredCredentials()
    }


    private fun verifyStoredCredentials() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val storedUuid = sharedPreferences.getString("uuid", null)

            if (storedUuid != null) {
                try {
                    val user = getUserFromDatabase(storedUuid)
                    if (user != null) {
                        // Si encontramos al usuario en la base de datos
                        _user.value = user

                        // Verificar si el usuario necesita completar su perfil
                        if (user.name.isBlank() || user.profileImageResId == 0) {
                            _registrationState.value = RegistrationState.NeedsProfile
                        } else {
                            _registrationState.value = RegistrationState.Complete
                        }

                        // Intentar obtener el FirebaseUser actual
                        auth.currentUser?.let { firebaseUser ->
                            _authState.value = AuthState.Authenticated(firebaseUser)
                        } ?: run {
                            // Si no hay FirebaseUser, intentar una autenticación silenciosa
                            handleSilentAuth(storedUuid)
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
                    val user = getUserFromDatabase(firebaseUser.uid)
                    if (user != null) {
                        _user.value = user
                        saveUserUuid(firebaseUser.uid)

                        if (user.name.isBlank() || user.profileImageResId == 0) {
                            _registrationState.value = RegistrationState.NeedsProfile
                        } else {
                            _registrationState.value = RegistrationState.Complete
                        }

                        _authState.value = AuthState.Authenticated(firebaseUser)
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
                    val newUser = User(
                        uuid = firebaseUser.uid,
                        email = email,
                        name = "",
                        profileImageResId = 0
                    )
                    // Primero actualizar el estado de registro
                    _registrationState.value = RegistrationState.NeedsProfile
                    // Luego crear el usuario en la base de datos
                    createUserInDatabase(newUser)
                    _user.value = newUser
                    saveUserUuid(firebaseUser.uid)
                    // Finalmente actualizar el estado de autenticación
                    _authState.value = AuthState.Authenticated(firebaseUser)
                } else {
                    _authState.value = AuthState.Error("User creation failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "User creation failed")
            }
        }
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
            _user.value?.let { currentUser ->
                val updatedUser = currentUser.copy(
                    name = name,
                    profileImageResId = profileImageResId
                )
                try {
                    updateUserInDatabase(updatedUser)
                    _user.value = updatedUser
                    // Indicar que el registro está completo
                    _registrationState.value = RegistrationState.Complete
                } catch (e: Exception) {
                    _authState.value = AuthState.Error("Failed to update profile")
                }
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
    }
}
