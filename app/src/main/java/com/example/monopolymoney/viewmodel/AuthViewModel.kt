package com.example.monopolymoney.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("UserCredentials", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId

    init {
        verifyStoredCredentials()
    }

    private fun verifyStoredCredentials() {
        viewModelScope.launch {
            val email = sharedPreferences.getString("email", null)
            val password = sharedPreferences.getString("password", null)
            val storedUserId = sharedPreferences.getString("userId", null)

            _userId.value = storedUserId

            if (email != null && password != null) {
                _authState.value = AuthState.Loading
                try {
                    auth.signInWithEmailAndPassword(email, password).await()
                    val user = auth.currentUser
                    if (user != null) {
                        try {
                            user.reload().await()
                            _userId.value = user.uid
                            _authState.value = AuthState.Authenticated(user)
                        } catch (e: Exception) {
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

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val user = auth.currentUser
                if (user != null) {
                    val id = user.uid
                    _userId.value = id
                    _authState.value = AuthState.Authenticated(user)
                    saveUserCredentials(email, password, id)
                } else {
                    _authState.value = AuthState.Error("Authentication failed")
                }
            } catch (e: Exception) {
                _authState.value = if (e is FirebaseAuthInvalidUserException) {
                    AuthState.Error("User does not exist. Please register.")
                } else {
                    AuthState.Error(e.message ?: "Authentication failed")
                }
            }
        }
    }

    fun createUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                val newUser = auth.currentUser
                if (newUser != null) {
                    val id = newUser.uid
                    _userId.value = id
                    _authState.value = AuthState.Authenticated(newUser)
                    saveUserCredentials(email, password, id)
                } else {
                    _authState.value = AuthState.Error("User creation failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "User creation failed")
            }
        }
    }

    private fun saveUserCredentials(email: String, password: String, id: String) {
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.putString("userId", id)
        editor.apply()
    }

    private fun handleInvalidCredentials() {
        clearUserCredentials()
        _userId.value = null
        _authState.value = AuthState.Unauthenticated
    }

    private fun clearUserCredentials() {
        val editor = sharedPreferences.edit()
        editor.remove("email")
        editor.remove("password")
        editor.remove("userId")
        editor.apply()
    }

    fun signOut() {
        auth.signOut()
        clearUserCredentials()
        _userId.value = null
        _authState.value = AuthState.Unauthenticated
    }

    sealed class AuthState {
        object Unauthenticated : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
