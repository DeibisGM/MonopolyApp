package com.example.monopolymoney.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.monopolymoney.data.AppDatabase
import com.example.monopolymoney.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getDatabase(application).userDao()

    fun createUserIfNotExists() {
        viewModelScope.launch(Dispatchers.IO) {
            val users = userDao.getAll()
            if (users.isEmpty()) {
                // Crear un usuario inicial con datos por defecto
                val newUser = User(uid = 4, firstName = null, lastName = null)
                userDao.insertAll(newUser)
            }
        }
    }
}