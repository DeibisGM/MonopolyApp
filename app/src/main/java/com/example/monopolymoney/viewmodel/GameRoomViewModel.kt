package com.example.monopolymoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.monopolymoney.data.Player
import com.example.monopolymoney.data.Transaction
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// GameRoomViewModel.kt
class GameRoomViewModel(application: Application) : AndroidViewModel(application) {
    private val database: FirebaseDatabase = Firebase.database
    private val roomsRef: DatabaseReference = database.getReference("rooms")

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _currentPlayer = MutableStateFlow<String?>(null)
    val currentPlayer: StateFlow<String?> = _currentPlayer

    private val _hostId = MutableStateFlow<String?>(null)
    val hostId: StateFlow<String?> = _hostId

    private val _gameStarted = MutableStateFlow(false)
    val gameStarted: StateFlow<Boolean> = _gameStarted

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var hostIdValueEventListener: ValueEventListener? = null

    private var playerValueEventListener: ValueEventListener? = null
    private var transactionValueEventListener: ValueEventListener? = null
    private var currentPlayerValueEventListener: ValueEventListener? = null
    private var gameStatusValueEventListener: ValueEventListener? = null

    private var transactionIdCounter = 0

    init {
        viewModelScope.launch {
            roomCode.collect { newCode ->
                newCode?.let { code ->
                    listenToRoomUpdates(code)
                }
            }
        }
    }

    private fun listenToRoomUpdates(roomCode: String) {
        removePreviousListeners(roomCode)

        val newPlayerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val playerList = snapshot.children.mapNotNull { it.getValue(Player::class.java) }
                _players.value = playerList
            }
            override fun onCancelled(error: DatabaseError) {
                _error.value = "Error loading players: ${error.message}"
            }
        }

        val newTransactionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val transactionList = snapshot.children.mapNotNull { it.getValue(Transaction::class.java) }
                _transactions.value = transactionList
            }
            override fun onCancelled(error: DatabaseError) {
                _error.value = "Error loading transactions: ${error.message}"
            }
        }

        val newCurrentPlayerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _currentPlayer.value = snapshot.getValue(String::class.java)
            }
            override fun onCancelled(error: DatabaseError) {
                _error.value = "Error loading current player: ${error.message}"
            }
        }

        val newGameStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _gameStarted.value = snapshot.getValue(String::class.java) == "started"
            }
            override fun onCancelled(error: DatabaseError) {
                _error.value = "Error loading game status: ${error.message}"
            }
        }

        roomsRef.child(roomCode).apply {
            child("players").addValueEventListener(newPlayerListener)
            child("transactions").addValueEventListener(newTransactionListener)
            child("currentPlayer").addValueEventListener(newCurrentPlayerListener)
            child("status").addValueEventListener(newGameStatusListener)
        }

        playerValueEventListener = newPlayerListener
        transactionValueEventListener = newTransactionListener
        currentPlayerValueEventListener = newCurrentPlayerListener
        gameStatusValueEventListener = newGameStatusListener
    }

    private fun removePreviousListeners(roomCode: String) {
        playerValueEventListener?.let { roomsRef.child(roomCode).child("players").removeEventListener(it) }
        transactionValueEventListener?.let { roomsRef.child(roomCode).child("transactions").removeEventListener(it) }
        currentPlayerValueEventListener?.let { roomsRef.child(roomCode).child("currentPlayer").removeEventListener(it) }
        gameStatusValueEventListener?.let { roomsRef.child(roomCode).child("status").removeEventListener(it) }
    }

    fun createRoom(playerId: String, playerName: String, profileImageResId: Int) {
        val player = Player(id = playerId, name = playerName, balance = 1500, profileImageResId = profileImageResId)
        viewModelScope.launch {
            try {
                val code = generateUniqueCode()
                val room = hashMapOf(
                    "hostId" to playerId,
                    "players" to listOf(player.toMap()),
                    "status" to "waiting",
                    "currentPlayer" to playerId,
                    "transactions" to emptyList<Map<String, Any>>()
                )
                roomsRef.child(code).setValue(room).await()
                _roomCode.value = code
                _hostId.value = playerId // Establecer el hostId al crear la sala
                _players.value = listOf(player)

                // Agregar listener para el hostId
                addHostIdListener(code)
            } catch (e: Exception) {
                _error.value = "Failed to create room: ${e.message}"
            }
        }
    }

    fun joinRoom(code: String, playerId: String, playerName: String, profileImageResId: Int) {
        val player = Player(id = playerId, name = playerName, balance = 1500, profileImageResId = profileImageResId)
        viewModelScope.launch {
            try {
                val snapshot = roomsRef.child(code).get().await()
                if (!snapshot.exists()) {
                    _error.value = "Room not found"
                    return@launch
                }

                val room = snapshot.value as? Map<*, *>
                val existingHostId = room?.get("hostId") as? String

                if (existingHostId != null) {
                    _hostId.value = existingHostId // Establecer el hostId al unirse a la sala
                }

                val playersData = room?.get("players") as? List<Map<String, Any>> ?: emptyList()
                if (playersData.size >= 4) {
                    _error.value = "Room is full"
                    return@launch
                }

                val playerList = playersData.mapNotNull { Player.fromMap(it) }.toMutableList()
                playerList.add(player)

                roomsRef.child(code).child("players").setValue(playerList.map { it.toMap() }).await()
                _roomCode.value = code
                _players.value = playerList

                // Agregar listener para el hostId
                addHostIdListener(code)
            } catch (e: Exception) {
                _error.value = "Failed to join room: ${e.message}"
            }
        }
    }

    private fun addHostIdListener(roomCode: String) {
        hostIdValueEventListener?.let {
            roomsRef.child(roomCode).child("hostId").removeEventListener(it)
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _hostId.value = snapshot.getValue(String::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                _error.value = "Error loading host ID: ${error.message}"
            }
        }

        roomsRef.child(roomCode).child("hostId").addValueEventListener(listener)
        hostIdValueEventListener = listener
    }

    fun startGame() {
        viewModelScope.launch {
            val code = _roomCode.value
            if (code != null) {
                try {
                    // Actualizamos el estado de la sala a "started"
                    roomsRef.child(code).child("status").setValue("started").await()
                    _gameStarted.value = true
                } catch (e: Exception) {
                    _error.value = "Failed to start game: ${e.message}"
                }
            } else {
                _error.value = "Room code is null"
            }
        }
    }

    fun endTurn() {
        viewModelScope.launch {
            val code = _roomCode.value
            val currentPlayerId = _currentPlayer.value
            val playersList = _players.value

            if (code != null && currentPlayerId != null && playersList.isNotEmpty()) {
                // Encontrar el índice del jugador actual
                val currentIndex = playersList.indexOfFirst { it.id == currentPlayerId }

                if (currentIndex != -1) {
                    // Obtener el índice del siguiente jugador (circularmente)
                    val nextIndex = (currentIndex + 1) % playersList.size
                    val nextPlayerId = playersList[nextIndex].id

                    try {
                        // Actualizar el jugador actual en Firebase
                        roomsRef.child(code).child("currentPlayer").setValue(nextPlayerId).await()
                        _currentPlayer.value = nextPlayerId // Actualizamos localmente el jugador actual
                    } catch (e: Exception) {
                        _error.value = "Failed to update current player: ${e.message}"
                    }
                }
            } else {
                _error.value = "Room code, current player, or players list is null"
            }
        }
    }



    private suspend fun generateUniqueCode(): String {
        var code: String
        var exists: Boolean
        do {
            code = (1..6).map { (0..9).random() }.joinToString("")
            exists = checkIfRoomExists(code)
        } while (exists)
        return code
    }

    private suspend fun checkIfRoomExists(code: String): Boolean {
        return try {
            val snapshot = roomsRef.child(code).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        _roomCode.value?.let { code ->
            hostIdValueEventListener?.let {
                roomsRef.child(code).child("hostId").removeEventListener(it)
            }
        }
    }
}