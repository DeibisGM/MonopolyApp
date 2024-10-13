package com.example.monopolymoney.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.monopolymoney.data.GameEvent
import com.example.monopolymoney.data.GameRoom
import com.example.monopolymoney.data.GameRoomRepository
import com.example.monopolymoney.data.Player
import com.example.monopolymoney.data.User
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DataViewModel private constructor(application: Application) : AndroidViewModel(application) {

    // Dependencies
    val authViewModel: AuthViewModel = AuthViewModel(application)
    private val gameRoomRepository = GameRoomRepository()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    // State Management
    private val _gameRoom = MutableStateFlow<GameRoom?>(null)
    private val _showGameOverScreen = MutableStateFlow(false)
    private val _shouldNavigateToMain = MutableStateFlow(false)
    private var lastKnownRoomCode: String? = null

    // Public State Flows
    val authState: StateFlow<AuthViewModel.AuthState> = authViewModel.authState
    val user: StateFlow<User?> = authViewModel.user
    val gameRoom: StateFlow<GameRoom?> = _gameRoom.asStateFlow()
    val showGameOverScreen: StateFlow<Boolean> = _showGameOverScreen.asStateFlow()
    val shouldNavigateToMain: StateFlow<Boolean> = _shouldNavigateToMain.asStateFlow()

    // Derived State Flows
    val players: StateFlow<Map<String, Player>> = _gameRoom
        .map { it?.players ?: emptyMap() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val transactions: StateFlow<List<GameEvent>> = _gameRoom
        .map { it?.gameEvents ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentPlayer: StateFlow<String?> = _gameRoom
        .map { it?.currentPlayerId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val gameStarted: StateFlow<Boolean> = _gameRoom
        .map { it?.status == GameRoom.GameStatus.STARTED }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val roomCode: StateFlow<String?> = _gameRoom
        .map { it?.roomCode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val hostId: StateFlow<String?> = _gameRoom
        .map { it?.hostId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val gameStatus: StateFlow<GameRoom.GameStatus?> = _gameRoom
        .map { it?.status }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Room Management Methods
    fun createRoom() {
        viewModelScope.launch {
            user.value?.let { currentUser ->
                val player = Player(
                    currentUser.uuid,
                    currentUser.name,
                    1500,
                    currentUser.profileImageResId,
                    true
                )
                val newGameRoom = gameRoomRepository.createGameRoom(currentUser.uuid, player)
                _gameRoom.value = newGameRoom
                observeGameRoom(newGameRoom.roomCode)
            }
        }
    }

    fun joinRoom(code: String) {
        viewModelScope.launch {
            try {
                user.value?.let { currentUser ->
                    val roomSnapshot = gameRoomRepository.getRoomSnapshot(code)
                    roomSnapshot?.let { snapshot ->
                        GameRoom.fromMap(snapshot)?.let { currentRoom ->
                            val player = Player(
                                currentUser.uuid,
                                currentUser.name,
                                1500,
                                currentUser.profileImageResId,
                                false
                            )

                            val playerJoinEvent = GameEvent.PlayerJoinRoomEvent(
                                id = currentRoom.gameEvents.size,
                                playerId = currentUser.uuid,
                                playerName = currentUser.name,
                                profileImageResId = currentUser.profileImageResId,
                                isHost = false
                            )

                            val updatedEvents = if (currentRoom.status == GameRoom.GameStatus.STARTED) {
                                currentRoom.gameEvents + playerJoinEvent
                            } else {
                                currentRoom.gameEvents
                            }

                            val updatedRoom = currentRoom.copy(
                                players = currentRoom.players + (currentUser.uuid to player),
                                gameEvents = updatedEvents
                            )

                            gameRoomRepository.updateGameRoom(updatedRoom)
                            observeGameRoom(code)
                            _gameRoom.value = updatedRoom
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DataViewModel", "Error joining room: ${e.message}")
            }
        }
    }

    private fun observeGameRoom(code: String) {
        lastKnownRoomCode = code
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

    fun leaveGame() {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                user.value?.let { currentUser ->
                    handlePlayerLeaving(currentRoom, currentUser)
                }
            }
        }
    }

    private suspend fun handlePlayerLeaving(currentRoom: GameRoom, currentUser: User) {
        val currentPlayer = currentRoom.players[currentUser.uuid] ?: return
        val updatedPlayers = currentRoom.players - currentUser.uuid

        if (updatedPlayers.isEmpty()) {
            endGame()
            return
        }

        val nextPlayerId = determineNextPlayer(currentRoom, currentUser, updatedPlayers)
        val newHostId = determineNewHost(currentPlayer, updatedPlayers)
        val finalUpdatedPlayers = updatePlayersWithNewHost(updatedPlayers, newHostId)

        val playerLeftEvent = createPlayerLeftEvent(currentRoom, currentUser, currentPlayer, newHostId)

        val updatedRoom = currentRoom.copy(
            players = finalUpdatedPlayers,
            hostId = newHostId ?: currentRoom.hostId,
            currentPlayerId = nextPlayerId,
            gameEvents = currentRoom.gameEvents + playerLeftEvent
        )

        gameRoomRepository.updateGameRoom(updatedRoom)
        _gameRoom.value = null
    }

    private fun determineNextPlayer(currentRoom: GameRoom, currentUser: User, updatedPlayers: Map<String, Player>): String? {
        return if (currentUser.uuid == currentRoom.currentPlayerId) {
            val playerIds = updatedPlayers.keys.toList()
            val currentIndex = playerIds.indexOf(currentUser.uuid)
            playerIds.getOrNull((currentIndex + 1) % playerIds.size)
        } else {
            currentRoom.currentPlayerId
        }
    }

    private fun determineNewHost(currentPlayer: Player, updatedPlayers: Map<String, Player>): String? {
        return if (currentPlayer.isHost && updatedPlayers.isNotEmpty()) {
            updatedPlayers.keys.first()
        } else null
    }

    private fun updatePlayersWithNewHost(players: Map<String, Player>, newHostId: String?): Map<String, Player> {
        return if (newHostId != null) {
            players.mapValues { (playerId, player) ->
                player.copy(isHost = playerId == newHostId)
            }
        } else players
    }

    private fun createPlayerLeftEvent(
        currentRoom: GameRoom,
        currentUser: User,
        currentPlayer: Player,
        newHostId: String?
    ): GameEvent.PlayerLeftEvent {
        return GameEvent.PlayerLeftEvent(
            id = currentRoom.gameEvents.size,
            playerId = currentUser.uuid,
            playerName = currentPlayer.name ?: "Unknown",
            profileImageResId = currentPlayer.profileImageResId,
            wasHost = currentPlayer.isHost,
            newHostId = newHostId
        )
    }

    fun endGame() {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                val finalEvent = GameEvent.GameEndedEvent(
                    id = currentRoom.gameEvents.size,
                    finalBalances = currentRoom.players.mapValues { it.value.balance }
                )

                val updatedRoom = currentRoom.copy(
                    status = GameRoom.GameStatus.FINISHED,
                    gameEvents = currentRoom.gameEvents + finalEvent
                )

                gameRoomRepository.updateGameRoom(updatedRoom)
                _showGameOverScreen.value = true
            }
        }
    }

    fun leaveGameOverScreen() {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                user.value?.let { currentUser ->
                    val updatedPlayers = currentRoom.players - currentUser.uuid

                    if (updatedPlayers.isEmpty()) {
                        deleteGame()
                    } else {
                        val updatedRoom = currentRoom.copy(players = updatedPlayers)
                        gameRoomRepository.updateGameRoom(updatedRoom)
                    }

                    navigateToMain()
                }
            } ?: deleteGame()
        }
    }

    private fun navigateToMain() {
        _showGameOverScreen.value = false
        _shouldNavigateToMain.value = true
        _gameRoom.value = null
    }

    fun deleteGame() {
        viewModelScope.launch {
            val roomCodeToDelete = _gameRoom.value?.roomCode ?: lastKnownRoomCode
            roomCodeToDelete?.let {
                try {
                    database.child("rooms").child(it).removeValue().await()
                    _gameRoom.value = null
                    lastKnownRoomCode = null
                } catch (e: Exception) {
                    Log.e("DataViewModel", "Error deleting room: ${e.message}")
                }
            }
        }
    }

    fun resetNavigation() {
        _shouldNavigateToMain.value = false
    }

    // Transaction Methods
    fun makeTransaction(fromPlayerId: String, toPlayerId: String, amount: Int) {
        viewModelScope.launch {
            _gameRoom.value?.let { currentRoom ->
                val updatedPlayers = updatePlayerBalances(currentRoom.players, fromPlayerId, toPlayerId, amount)
                val newTransaction = createTransactionEvent(currentRoom.gameEvents.size, fromPlayerId, toPlayerId, amount)
                updateGameRoomWithTransaction(currentRoom, updatedPlayers, newTransaction)
            }
        }
    }

    fun makeBankTransaction(toPlayerId: String, amount: Int) {
        makeTransaction("-1", toPlayerId, amount)
    }

    private fun updatePlayerBalances(
        players: Map<String, Player>,
        fromPlayerId: String,
        toPlayerId: String,
        amount: Int
    ): Map<String, Player> {
        return players.mapValues { (playerId, player) ->
            when (playerId) {
                fromPlayerId -> player.copy(balance = player.balance - amount)
                toPlayerId -> player.copy(balance = player.balance + amount)
                else -> player
            }
        }
    }

    private fun createTransactionEvent(
        eventId: Int,
        fromPlayerId: String,
        toPlayerId: String,
        amount: Int
    ): GameEvent.TransactionEvent {
        return GameEvent.TransactionEvent(
            id = eventId,
            fromPlayerId = fromPlayerId,
            toPlayerId = toPlayerId,
            amount = amount
        )
    }

    private suspend fun updateGameRoomWithTransaction(
        currentRoom: GameRoom,
        updatedPlayers: Map<String, Player>,
        newTransaction: GameEvent.TransactionEvent
    ) {
        val updatedRoom = currentRoom.copy(
            players = updatedPlayers,
            gameEvents = currentRoom.gameEvents + newTransaction
        )
        gameRoomRepository.updateGameRoom(updatedRoom)
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