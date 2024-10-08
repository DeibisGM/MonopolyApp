package com.example.monopolymoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.monopolymoney.data.GameEvent
import com.example.monopolymoney.data.GameRoom
import com.example.monopolymoney.data.GameRoomRepository
import com.example.monopolymoney.data.Player
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DataViewModel private constructor(application: Application) : AndroidViewModel(application) {
    private val authViewModel: AuthViewModel = AuthViewModel(application)
    private val playerViewModel: PlayerViewModel = PlayerViewModel(application)
    private val gameRoomRepository = GameRoomRepository()

    // Auth State
    val authState: StateFlow<AuthViewModel.AuthState> = authViewModel.authState
    val userId: StateFlow<String?> = authViewModel.userId

    // GameRoom State
    private val _gameRoom = MutableStateFlow<GameRoom?>(null)
    val gameRoom: StateFlow<GameRoom?> = _gameRoom.asStateFlow()

    // Convenience accessors for GameRoom data
    val players: StateFlow<Map<String, Player>> = _gameRoom.map { it?.players ?: emptyMap() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val transactions: StateFlow<List<GameEvent>> = _gameRoom.map { it?.gameEvents ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentPlayer: StateFlow<String?> = _gameRoom.map { it?.currentPlayerId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val gameStarted: StateFlow<Boolean> = _gameRoom.map { it?.status == GameRoom.GameStatus.STARTED }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val roomCode: StateFlow<String?> = _gameRoom.map { it?.roomCode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val hostId: StateFlow<String?> = _gameRoom.map { it?.hostId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val gameStatus: StateFlow<GameRoom.GameStatus?> = _gameRoom.map { it?.status }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ==========================
    // Player Methods
    // ==========================
    val playerName = playerViewModel.playerName
    val profileImageResId = playerViewModel.profileImageResId
    val isNameSet = playerViewModel.isNameSet
    val isProfileImageSet = playerViewModel.isProfileImageSet

    fun setName(name: String) = playerViewModel.setName(name)
    fun setProfileImageResId(profileImageResId: Int) = playerViewModel.setProfileImageResId(profileImageResId)

    // ==========================
    // Auth Methods
    // ==========================
    fun loginUser(email: String, password: String) = authViewModel.loginUser(email, password)
    fun createUser(email: String, password: String) = authViewModel.createUser(email, password)
    fun signOut() = authViewModel.signOut()

    // ==========================
    // Room Management Methods
    // ==========================
    fun createRoom(playerName: String, profileImageResId: Int) {
        viewModelScope.launch {
            userId.value?.let { id ->
                val player = Player(id, playerName, 1500, profileImageResId, true)
                val newGameRoom = gameRoomRepository.createGameRoom(id, player)
                _gameRoom.value = newGameRoom
                observeGameRoom(newGameRoom.roomCode)
            }
        }
    }

    fun joinRoom(code: String, playerName: String, profileImageResId: Int) {
        viewModelScope.launch {
            try {
                userId.value?.let { id ->
                    val roomSnapshot = gameRoomRepository.getRoomSnapshot(code)
                    if (roomSnapshot != null) {
                        val currentRoom = GameRoom.fromMap(roomSnapshot)
                        if (currentRoom != null) {
                            val player = Player(id, playerName, 1500, profileImageResId, false)
                            val updatedRoom = currentRoom.copy(
                                players = currentRoom.players + (id to player)
                            )
                            gameRoomRepository.updateGameRoom(updatedRoom)
                            observeGameRoom(code)
                            _gameRoom.value = updatedRoom
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error appropriately
            }
        }
    }

    private fun observeGameRoom(code: String) {
        viewModelScope.launch {
            gameRoomRepository.observeGameRoom(code).collect { updatedGameRoom ->
                _gameRoom.value = updatedGameRoom
            }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                val updatedRoom = currentRoom.copy(status = GameRoom.GameStatus.STARTED)
                gameRoomRepository.updateGameRoom(updatedRoom)
            }
        }
    }

    fun endTurn() {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                val playerIds = currentRoom.players.keys.toList()
                val currentPlayerIndex = playerIds.indexOf(currentRoom.currentPlayerId)
                val nextPlayerIndex = (currentPlayerIndex + 1) % playerIds.size
                val updatedRoom = currentRoom.copy(currentPlayerId = playerIds[nextPlayerIndex])
                gameRoomRepository.updateGameRoom(updatedRoom)
            }
        }
    }

    fun leaveGame(isHost: Boolean = false) {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                userId.value?.let { currentUserId ->
                    val currentPlayer = currentRoom.players[currentUserId]
                    if (currentPlayer != null) {
                        val updatedPlayers = currentRoom.players - currentUserId

                        // Si no quedan más jugadores, termina el juego
                        if (updatedPlayers.isEmpty()) {
                            endGame()
                            return@launch
                        }

                        // Si el jugador que sale era el actual, pasar al siguiente
                        var nextPlayerId = currentRoom.currentPlayerId
                        if (currentUserId == currentRoom.currentPlayerId) {
                            val playerIds = updatedPlayers.keys.toList()
                            val currentIndex = playerIds.indexOf(currentUserId)
                            nextPlayerId = playerIds.getOrNull((currentIndex + 1) % playerIds.size)
                        }

                        // Si el host está saliendo, transferir el rol al siguiente jugador
                        val newHostId = if (isHost && updatedPlayers.isNotEmpty()) {
                            updatedPlayers.keys.first()
                        } else null

                        // Actualizar isHost para el nuevo host
                        val finalUpdatedPlayers = if (newHostId != null) {
                            updatedPlayers.mapValues { (playerId, player) ->
                                if (playerId == newHostId) player.copy(isHost = true)
                                else player.copy(isHost = false)
                            }
                        } else updatedPlayers

                        // Crear evento de salida del jugador
                        val playerLeftEvent = GameEvent.PlayerLeftEvent(
                            id = currentRoom.gameEvents.size,
                            playerId = currentUserId,
                            playerName = currentPlayer.name ?: "Unknown",
                            profileImageResId = currentPlayer.profileImageResId,
                            wasHost = isHost,
                            newHostId = newHostId
                        )

                        // Actualizar la sala
                        val updatedRoom = currentRoom.copy(
                            players = finalUpdatedPlayers,
                            hostId = newHostId ?: currentRoom.hostId,
                            currentPlayerId = nextPlayerId,
                            gameEvents = currentRoom.gameEvents + playerLeftEvent
                        )

                        gameRoomRepository.updateGameRoom(updatedRoom)
                        _gameRoom.value = null
                    }
                }
            }
        }
    }

    fun endGame() {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                val updatedRoom = currentRoom.copy(
                    status = GameRoom.GameStatus.FINISHED,
                    gameEvents = currentRoom.gameEvents + GameEvent.GameEndedEvent(
                        id = currentRoom.gameEvents.size,
                        finalBalances = currentRoom.players.mapValues { it.value.balance }
                    )
                )
                gameRoomRepository.updateGameRoom(updatedRoom)
                _gameRoom.value = null
            }
        }
    }


    // ==========================
    // Transaction Methods
    // ==========================
    fun makeTransaction(roomCode: String, fromPlayerId: String, toPlayerId: String, amount: Int) {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                val updatedPlayers = currentRoom.players.mapValues { (playerId, player) ->
                    when (playerId) {
                        fromPlayerId -> player.copy(balance = player.balance - amount)
                        toPlayerId -> player.copy(balance = player.balance + amount)
                        else -> player
                    }
                }

                val newTransaction = GameEvent.TransactionEvent(
                    id = currentRoom.gameEvents.size,
                    fromPlayerId = fromPlayerId,
                    toPlayerId = toPlayerId,
                    amount = amount
                )

                val updatedRoom = currentRoom.copy(
                    players = updatedPlayers,
                    gameEvents = currentRoom.gameEvents + newTransaction
                )

                gameRoomRepository.updateGameRoom(updatedRoom)
            }
        }
    }

    fun makeBankTransaction(roomCode: String, toPlayerId: String, amount: Int) {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                val updatedPlayers = currentRoom.players.mapValues { (playerId, player) ->
                    if (playerId == toPlayerId) {
                        player.copy(balance = player.balance + amount)
                    } else {
                        player
                    }
                }

                val newTransaction = GameEvent.TransactionEvent(
                    id = currentRoom.gameEvents.size,
                    fromPlayerId = "-1", // Bank ID
                    toPlayerId = toPlayerId,
                    amount = amount
                )

                val updatedRoom = currentRoom.copy(
                    players = updatedPlayers,
                    gameEvents = currentRoom.gameEvents + newTransaction
                )

                gameRoomRepository.updateGameRoom(updatedRoom)
            }
        }
    }

    // ==========================
    // Companion Object
    // ==========================
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
