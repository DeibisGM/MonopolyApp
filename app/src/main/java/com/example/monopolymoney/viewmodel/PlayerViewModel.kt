package com.example.monopolymoney.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// PlayerViewModel.kt
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("UserCredentials", Context.MODE_PRIVATE)

    private val _playerName = MutableStateFlow<String?>(null)
    val playerName: StateFlow<String?> = _playerName

    private val _profileImageResId = MutableStateFlow(0)
    val profileImageResId: StateFlow<Int> = _profileImageResId

    private val _isNameSet = MutableStateFlow(false)
    val isNameSet: StateFlow<Boolean> = _isNameSet

    private val _isProfileImageSet = MutableStateFlow(false)
    val isProfileImageSet: StateFlow<Boolean> = _isProfileImageSet

    init {
        val savedName = sharedPreferences.getString("names", null)
        if (savedName != null) {
            _playerName.value = savedName
            _isNameSet.value = true
        }
        _profileImageResId.value = sharedPreferences.getInt("profileImageResId", 0)
        _isProfileImageSet.value = _profileImageResId.value != 0
    }

    fun setName(name: String) {
        val editor = sharedPreferences.edit()
        _playerName.value = name
        editor.putString("names", name)
        editor.apply()
        _isNameSet.value = true
    }

    fun setProfileImageResId(profileImageResId: Int) {
        val editor = sharedPreferences.edit()
        _profileImageResId.value = profileImageResId
        editor.putInt("profileImageResId", profileImageResId)
        editor.apply()
        _isProfileImageSet.value = true
    }
}