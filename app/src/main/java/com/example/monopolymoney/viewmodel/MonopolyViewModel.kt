import android.app.Application
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monopolymoney.data.GameState
import com.example.monopolymoney.data.Player
import com.example.monopolymoney.data.PlayerParcelable
import com.example.monopolymoney.data.Transaction
import com.example.monopolymoney.data.TransactionParcelable
import com.example.monopolymoney.data.toParcelable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class MonopolyViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _userInfo = MutableStateFlow<FirebaseUser?>(null)
    val userInfo: StateFlow<FirebaseUser?> = _userInfo

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    // SharedPreferences para almacenar las credenciales
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("UserCredentials", Context.MODE_PRIVATE)

    // Variables para almacenar las credenciales recuperadas
    private var savedEmail: String? = null
    private var savedPassword: String? = null
    private var savedId: String? = null
    private var savedName: String? = null

    // Firebase references and listeners
    private val database: FirebaseDatabase = Firebase.database
    private val roomsRef: DatabaseReference = database.getReference("rooms")

    private var playerValueEventListener: ValueEventListener? = null
    private var transactionValueEventListener: ValueEventListener? = null
    private var currentPlayerValueEventListener: ValueEventListener? = null
    private var gameStatusValueEventListener: ValueEventListener? = null

    // StateFlow variables to track app state
    private val _playerId = MutableStateFlow<String?>(null)
    val playerId: StateFlow<String?> get() = _playerId

    private val _playerName = MutableStateFlow<String?>(null)
    val playerName: StateFlow<String?> get() = _playerName

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _currentPlayer = MutableStateFlow<String?>(null)
    val currentPlayer: StateFlow<String?> = _currentPlayer

    private val _gameStarted = MutableStateFlow(false)
    val gameStarted: StateFlow<Boolean> = _gameStarted

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode

    private val _hostId = MutableStateFlow<String?>(null)
    val hostId: StateFlow<String?> = _hostId

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isNameSet = MutableStateFlow(false)
    val isNameSet: StateFlow<Boolean> = _isNameSet

    fun setName(name: String) {
        val editor = sharedPreferences.edit()

        _playerName.value = name
        editor.putString("name", name)
        editor.apply()
        _isNameSet.value = true
        savedName = name
    }

    fun getNameState(): Boolean {
        return _isNameSet.value
    }

    private var transactionIdCounter = 0

    init {
//        // Inicializa las variables
//        _playerName.value = null
//        _isNameSet.value = false

        // Cargar las credenciales de usuario
        loadUserCredentials()

        // Verificar si hay un nombre guardado y actualizar _isNameSet
        savedName = sharedPreferences.getString("name", null)
        if (savedName != null) {
            _playerName.value = savedName
            _isNameSet.value = true
        } else {
            _isNameSet.value = false
        }

        verifyStoredCredentials()

        viewModelScope.launch {
            roomCode.collect { newCode ->
                newCode?.let { code -> listenToRoomUpdates(code) }
            }
        }
    }


    private fun verifyStoredCredentials() {
        viewModelScope.launch {
            val email = sharedPreferences.getString("email", null)
            val password = sharedPreferences.getString("password", null)

            if (email != null && password != null) {
                _hostId.value = sharedPreferences.getString("id", null)
                _playerId.value = sharedPreferences.getString("id", null)
                _authState.value = AuthState.Loading
                try {

                    auth.signInWithEmailAndPassword(email, password).await()
                    val user = auth.currentUser
                    if (user != null) {

                        try {
                            user.reload().await()
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

    fun getSavedEmail(): String? {
        return savedEmail
    }

    fun getSavedPassword(): String? {
        return savedPassword
    }

    private fun handleInvalidCredentials() {

        clearUserCredentials()
        _authState.value = AuthState.Unauthenticated
    }

    private fun clearUserCredentials() {
        val editor = sharedPreferences.edit()
        editor.remove("email")
        editor.remove("password")
        editor.apply()
    }

    private fun loadUserCredentials() {
        savedEmail = sharedPreferences.getString("email", null)
        savedPassword = sharedPreferences.getString("password", null)
        savedId = sharedPreferences.getString("id", null)
        savedName = sharedPreferences.getString("name", null)
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val user = auth.currentUser
                if (user != null) {
                    val id = user.uid
                    _authState.value = AuthState.Authenticated(user)

                    saveUserCredentials(email, password, id)

                } else {
                    _authState.value = AuthState.Error("Authentication failed")
                }
            } catch (e: Exception) {
                // Si el usuario no existe (FirebaseAuthInvalidUserException), devolver un error específico
                if (e is FirebaseAuthInvalidUserException) {
                    _authState.value = AuthState.Error("User does not exist. Please register.")
                } else {
                    // Para otros tipos de errores, muestra un mensaje genérico o el mensaje del error
                    _authState.value = AuthState.Error(e.message ?: "Authentication failed")
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
        editor.putString("id", id)
        editor.putString("name", savedName)
        editor.apply()

        _hostId.value = id
        _playerId.value = id
        loadUserCredentials()
    }

    fun signOut() {
        auth.signOut()
        clearUserCredentials()
        _authState.value = AuthState.Unauthenticated
    }

    fun goOffline() {
        database.goOffline()

    }

    // Listen to real-time updates for a specific game room in Firebase
    private fun listenToRoomUpdates(roomCode: String) {
        removePreviousListeners(roomCode)

        // Listener to track changes in player list
        val newPlayerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val playerList = snapshot.children.mapNotNull { it.getValue(Player::class.java) }
                _players.value = playerList
            }

            override fun onCancelled(error: DatabaseError) {
                _error.value = "Error loading players: ${error.message}"
            }
        }

        // Listener to track changes in transactions
        val newTransactionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val transactionList =
                    snapshot.children.mapNotNull { it.getValue(Transaction::class.java) }
                _transactions.value = transactionList
            }

            override fun onCancelled(error: DatabaseError) {
                _error.value = "Error loading transactions: ${error.message}"
            }
        }

        // Listener to track changes in the current player
        val newCurrentPlayerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _currentPlayer.value = snapshot.getValue(String::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                _error.value = "Error loading current player: ${error.message}"
            }
        }

        // Listener to track changes in game status (e.g., "started" or "waiting")
        val newGameStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _gameStarted.value = snapshot.getValue(String::class.java) == "started"
            }

            override fun onCancelled(error: DatabaseError) {
                _error.value = "Error loading game status: ${error.message}"
            }
        }

        // Attach new listeners to Firebase references
        roomsRef.child(roomCode).apply {
            child("players").addValueEventListener(newPlayerListener)
            child("transactions").addValueEventListener(newTransactionListener)
            child("currentPlayer").addValueEventListener(newCurrentPlayerListener)
            child("status").addValueEventListener(newGameStatusListener)
        }

        // Store listeners to remove later if needed
        playerValueEventListener = newPlayerListener
        transactionValueEventListener = newTransactionListener
        currentPlayerValueEventListener = newCurrentPlayerListener
        gameStatusValueEventListener = newGameStatusListener
    }

    // Clean up previous listeners to avoid memory leaks or redundant listeners
    private fun removePreviousListeners(roomCode: String) {
        playerValueEventListener?.let {
            roomsRef.child(roomCode).child("players").removeEventListener(it)
        }
        transactionValueEventListener?.let {
            roomsRef.child(roomCode).child("transactions").removeEventListener(it)
        }
        currentPlayerValueEventListener?.let {
            roomsRef.child(roomCode).child("currentPlayer").removeEventListener(it)
        }
        gameStatusValueEventListener?.let {
            roomsRef.child(roomCode).child("status").removeEventListener(it)
        }
    }

    // Crear una transacción entre jugadores y actualizar Firebase
    fun makeTransaction(fromPlayerId: String, toPlayerId: String, amount: Int) {
        viewModelScope.launch {
            val updatedPlayers = _players.value.map { player ->
                when (player.id) {
                    fromPlayerId -> player.copy(balance = player.balance - amount)
                    toPlayerId -> player.copy(balance = player.balance + amount)
                    else -> player
                }
            }
            _players.value = updatedPlayers

            val newTransaction = Transaction(
                id = transactionIdCounter++,
                fromPlayer = fromPlayerId,
                toPlayer = toPlayerId,
                amount = amount
            )
            _transactions.value = _transactions.value + newTransaction

            _roomCode.value?.let { roomCode ->
                val gameState = GameState(
                    hostId = _players.value.firstOrNull()?.id
                        ?: "",  // El host es el primer jugador
                    players = _players.value,
                    status = if (_gameStarted.value) "started" else "waiting",
                    currentPlayer = _currentPlayer.value ?: "",
                    transactions = _transactions.value
                )

                updateRoomInFirebase(roomCode, gameState)
            }
        }
    }

    fun makeBankTransaction(toPlayerId: String, amount: Int) {
        viewModelScope.launch {
            // Update the player's balance
            val updatedPlayers = _players.value.map { player ->
                if (player.id == toPlayerId) {
                    player.copy(balance = player.balance + amount)
                } else {
                    player
                }
            }
            _players.value = updatedPlayers

            // Create a new transaction
            val newTransaction = Transaction(
                id = transactionIdCounter++,
                fromPlayer = "-1", // Use "-1" to represent the bank
                toPlayer = toPlayerId,
                amount = amount
            )
            _transactions.value = _transactions.value + newTransaction

            // Update Firebase with the new game state
            _roomCode.value?.let { roomCode ->
                val gameState = GameState(
                    hostId = _players.value.firstOrNull()?.id ?: "",
                    players = _players.value,
                    status = if (_gameStarted.value) "started" else "waiting",
                    currentPlayer = _currentPlayer.value ?: "",
                    transactions = _transactions.value
                )

                updateRoomInFirebase(roomCode, gameState)
            }
        }
    }

    // Start the game and assign the first player
    fun startGame() {
        if (_players.value.isNotEmpty()) {
            _gameStarted.value = true
            _currentPlayer.value = _players.value.first().id

            _roomCode.value?.let {
                viewModelScope.launch {
                    updateRoomStatusInFirebase(it, "started")
                    updateCurrentPlayerInFirebase(it, _currentPlayer)
                }
            }
        }
    }

    // Move to the next player's turn
    fun endTurn() {
        val currentIndex = _players.value.indexOfFirst { it.id == _currentPlayer.value }
        val nextIndex = (currentIndex + 1) % _players.value.size
        _currentPlayer.value = _players.value[nextIndex].id

        _roomCode.value?.let {
            viewModelScope.launch { updateCurrentPlayerInFirebase(it, _currentPlayer) }
        }
    }

    fun createRoom() {
        val id = playerId
        val player = Player(id = id.value, name = playerName.value, balance = 1500)

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val code = generateUniqueCode()
                val room = hashMapOf(
                    "hostId" to id.value,
                    "players" to listOf(player.toMap()),
                    "status" to "waiting",
                    "currentPlayer" to "",
                    "transactions" to emptyList<Map<String, Any>>()
                )

                roomsRef.child(code).setValue(room).await()
                _hostId.value = id.value
                _roomCode.value = code
                _players.value = listOf(player)
            } catch (e: Exception) {
                _error.value = "Failed to create room: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinRoom(code: String) {
        val id = savedId ?: return
        val player = Player(id = id, name = playerName.toString(), balance = 1500)

        viewModelScope.launch {
            try {
                val snapshot = roomsRef.child(code).get().await()
                if (!snapshot.exists()) {
                    _error.value = "Room not found"
                    return@launch
                }

                val room = snapshot.value as? Map<String, Any>
                if (room != null) {
                    val playersData = room["players"] as? List<Map<String, Any>> ?: emptyList()
                    if (playersData.size >= 4) {
                        _error.value = "Room is full"
                        return@launch
                    }

                    val playerList = playersData.mapNotNull { map -> Player.fromMap(map) }.toMutableList()
                    playerList.add(player)

                    roomsRef.child(code).child("players").setValue(playerList.map { it.toMap() }).await()
                    _roomCode.value = code
                }
            } catch (e: Exception) {
                _error.value = "Failed to join room: ${e.message}"
            }
        }
    }

    // Genera un código único de 6 dígitos y verifica su unicidad en Firebase
    private suspend fun generateUniqueCode(): String {
        var code: String
        var exists: Boolean

        do {
            code = (1..6).map { (0..9).random() }.joinToString("")
            exists = checkIfRoomExists(code)
        } while (exists)

        return code
    }

    // Verifica si el código de la sala ya existe en Firebase
    private suspend fun checkIfRoomExists(code: String): Boolean {
        return try {
            val snapshot = roomsRef.child(code).get().await()
            snapshot.exists() // Devuelve true si el código de la sala ya existe
        } catch (e: Exception) {
            false // En caso de error, asumimos que no existe
        }
    }

    // Update the room status in Firebase
    private suspend fun updateRoomStatusInFirebase(roomCode: String, status: String) {
        roomsRef.child(roomCode).child("status").setValue(status).await()
    }

    // Update the current player in Firebase
    private suspend fun updateCurrentPlayerInFirebase(
        roomCode: String,
        currentPlayer: StateFlow<String?>
    ) {
        roomsRef.child(roomCode).child("currentPlayer").setValue(currentPlayer.value).await()
    }

    // Update the room data in Firebase
    private suspend fun updateRoomInFirebase(roomCode: String, gameState: GameState) {
        roomsRef.child(roomCode).setValue(gameState.toMap()).await()
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up listeners to avoid memory leaks
        _roomCode.value?.let { removePreviousListeners(it) }
    }

    fun saveState(outState: Bundle) {
        outState.putString("roomCode", _roomCode.value)
        outState.putString("playerId", _playerId.value)
        outState.putBoolean("gameStarted", _gameStarted.value)
        outState.putString("currentPlayer", _currentPlayer.value)
        outState.putParcelableArrayList(
            "players",
            ArrayList(_players.value.map { it.toParcelable() })
        )
        outState.putParcelableArrayList(
            "transactions",
            ArrayList(_transactions.value.map { it.toParcelable() })
        )
    }

    fun restoreState(savedInstanceState: Bundle) {
        _roomCode.value = savedInstanceState.getString("roomCode")
        _playerId.value = savedInstanceState.getString("playerId")
        _gameStarted.value = savedInstanceState.getBoolean("gameStarted")
        _currentPlayer.value = savedInstanceState.getString("currentPlayer")
        _players.value = savedInstanceState.getParcelableArrayList<PlayerParcelable>("players")
            ?.map { it.toPlayer() } ?: emptyList()
        _transactions.value =
            savedInstanceState.getParcelableArrayList<TransactionParcelable>("transactions")
                ?.map { it.toTransaction() } ?: emptyList()

        // Re-establish Firebase listeners
        _roomCode.value?.let { listenToRoomUpdates(it) }
    }

}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

// Factory for creating MonopolyViewModel
class MonopolyViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonopolyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MonopolyViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}