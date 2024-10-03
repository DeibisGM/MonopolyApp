package com.example.monopolymoney.viewmodel

import AuthState

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.StateFlow

class DataViewModel private constructor(application: Application) : AndroidViewModel(application) {
    private val authViewModel: AuthViewModel = AuthViewModel(application)
    private val gameRoomViewModel: GameRoomViewModel = GameRoomViewModel(application)
    private val playerViewModel: PlayerViewModel = PlayerViewModel(application)
    private val transactionViewModel: TransactionViewModel = TransactionViewModel(application)

    // Auth State
    val authState: StateFlow<AuthState> = authViewModel.authState

    // User ID
    val userId: StateFlow<String?> = authViewModel.userId

    // GameRoom State
    val players = gameRoomViewModel.players
    val transactions = gameRoomViewModel.transactions
    val currentPlayer = gameRoomViewModel.currentPlayer
    val gameStarted = gameRoomViewModel.gameStarted
    val roomCode = gameRoomViewModel.roomCode
    val hostId = gameRoomViewModel.hostId
    val error = gameRoomViewModel.error

    // Player State
    val playerName = playerViewModel.playerName
    val profileImageResId = playerViewModel.profileImageResId
    val isNameSet = playerViewModel.isNameSet
    val isProfileImageSet = playerViewModel.isProfileImageSet

    // Auth Functions
    fun loginUser(email: String, password: String) = authViewModel.loginUser(email, password)
    fun createUser(email: String, password: String) = authViewModel.createUser(email, password)
    fun signOut() = authViewModel.signOut()

    fun createRoom(playerName: String, profileImageResId: Int) {
        userId.value?.let { id ->
            gameRoomViewModel.createRoom(id, playerName, profileImageResId)
        }
    }

    fun joinRoom(code: String, playerName: String, profileImageResId: Int) {
        userId.value?.let { id ->
            gameRoomViewModel.joinRoom(code, id, playerName, profileImageResId)
        }
    }

    fun startGame() = gameRoomViewModel.startGame()
    fun endTurn() = gameRoomViewModel.endTurn()

    // Player Functions
    fun setName(name: String) = playerViewModel.setName(name)
    fun setProfileImageResId(profileImageResId: Int) = playerViewModel.setProfileImageResId(profileImageResId)

    // Transaction Functions
    fun makeTransaction(roomCode: String, fromPlayerId: String, toPlayerId: String, amount: Int) {
        transactionViewModel.makeTransaction(
            roomCode,
            fromPlayerId,
            toPlayerId,
            amount,
            players.value,
            transactions.value
        )
    }

    fun makeBankTransaction(roomCode: String, toPlayerId: String, amount: Int) {
        currentPlayer.value?.let {
            transactionViewModel.makeBankTransaction(
                roomCode,
                toPlayerId,
                amount,
                players.value,
                transactions.value,
                it
            )
        }
    }

    companion object {
        @Volatile
        private var instance: DataViewModel? = null

        fun getInstance(application: Application): DataViewModel {
            return instance ?: synchronized(this) {
                instance ?: DataViewModel(application).also { instance = it }
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            if (modelClass.isAssignableFrom(DataViewModel::class.java)) {
                return getInstance(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}