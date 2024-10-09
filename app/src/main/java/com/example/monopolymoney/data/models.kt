package com.example.monopolymoney.data

import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Player(
    val id: String? = null,
    val name: String? = null,
    val balance: Int = 0,
    val profileImageResId: Int = 0,
    val isHost: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "balance" to balance,
            "profileImageResId" to profileImageResId,
            "isHost" to isHost
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Player? {
            return try {
                Player(
                    id = map["id"] as? String,
                    name = map["name"] as? String,
                    balance = (map["balance"] as? Long)?.toInt() ?: 0,
                    profileImageResId = (map["profileImageResId"] as? Long)?.toInt() ?: 0,
                    isHost = map["isHost"] as? Boolean ?: false
                )
            } catch (e: ClassCastException) {
                null
            }
        }
    }
}

data class User(
    val uuid: String = "",
    val email: String = "",
    val name: String = "",
    val profileImageResId: Int = 0,
    val isEmailVerified: Boolean = false
) {
    // Constructor sin argumentos requerido para Firebase
    constructor() : this("", "", "", 0)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uuid" to uuid,
            "email" to email,
            "name" to name,
            "profileImageResId" to profileImageResId,
            "isEmailVerified" to isEmailVerified
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): User {
            return User(
                uuid = map["uuid"] as? String ?: "",
                email = map["email"] as? String ?: "",
                name = map["name"] as? String ?: "",
                profileImageResId = (map["profileImageResId"] as? Long)?.toInt() ?: 0,
                isEmailVerified = (map["isEmailVerified"] as? Boolean) ?: false
            )
        }
    }
}

sealed class GameEvent {
    data class TransactionEvent(
        val id: Int = 0,
        val fromPlayerId: String = "",
        val toPlayerId: String = "",
        val amount: Int = 0,
        val timestamp: Long = System.currentTimeMillis()
    ) : GameEvent() {
        fun toMap(): Map<String, Any> = mapOf(
            "type" to "TRANSACTION",
            "id" to id,
            "fromPlayerId" to fromPlayerId,
            "toPlayerId" to toPlayerId,
            "amount" to amount,
            "timestamp" to timestamp
        )
    }

    data class PlayerLeftEvent(
        val id: Int = 0,
        val playerId: String = "",
        val playerName: String = "",
        val profileImageResId: Int = 0,
        val wasHost: Boolean = false,
        val newHostId: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : GameEvent() {
        fun toMap(): Map<String, Any> = mapOf(
            "type" to "PLAYER_LEFT",
            "id" to id,
            "playerId" to playerId,
            "playerName" to playerName,
            "profileImageResId" to profileImageResId,
            "wasHost" to wasHost,
            "newHostId" to (newHostId ?: ""),
            "timestamp" to timestamp
        )
    }

    data class GameEndedEvent(
        val id: Int = 0,
        val finalBalances: Map<String, Int> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    ) : GameEvent() {
        fun toMap(): Map<String, Any> = mapOf(
            "type" to "GAME_ENDED",
            "id" to id,
            "finalBalances" to finalBalances,
            "timestamp" to timestamp
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): GameEvent? {
            return when (map["type"] as? String) {
                "TRANSACTION" -> TransactionEvent(
                    id = (map["id"] as? Number)?.toInt() ?: 0,
                    fromPlayerId = map["fromPlayerId"] as String,
                    toPlayerId = map["toPlayerId"] as String,
                    amount = (map["amount"] as? Number)?.toInt() ?: 0,
                    timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                "PLAYER_LEFT" -> PlayerLeftEvent(
                    id = (map["id"] as? Number)?.toInt() ?: 0,
                    playerId = map["playerId"] as String,
                    playerName = map["playerName"] as String,
                    profileImageResId = (map["profileImageResId"] as? Number)?.toInt() ?: 0,
                    wasHost = map["wasHost"] as Boolean,
                    newHostId = map["newHostId"] as? String,
                    timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                "GAME_ENDED" -> GameEndedEvent(
                    id = (map["id"] as? Number)?.toInt() ?: 0,
                    finalBalances = (map["finalBalances"] as? Map<String, Number>)?.mapValues { it.value.toInt() } ?: emptyMap(),
                    timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                else -> null

            }
        }
    }


}

data class GameRoom(
    val roomCode: String,
    val hostId: String,
    val players: Map<String, Player> = mapOf(),
    val gameEvents: List<GameEvent> = listOf(),
    val currentPlayerId: String? = null,
    val status: GameStatus = GameStatus.WAITING
) {
    enum class GameStatus {
        WAITING, STARTED, FINISHED
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "roomCode" to roomCode,
        "hostId" to hostId,
        "players" to players.mapValues { it.value.toMap() },
        "gameEvents" to gameEvents.map {
            when (it) {
                is GameEvent.TransactionEvent -> it.toMap()
                is GameEvent.PlayerLeftEvent -> it.toMap()
                is GameEvent.GameEndedEvent -> it.toMap()
            }
        },
        "currentPlayerId" to currentPlayerId,
        "status" to status.name
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): GameRoom? {
            return try {
                GameRoom(
                    roomCode = map["roomCode"] as String,
                    hostId = map["hostId"] as String,
                    players = (map["players"] as? Map<String, Map<String, Any?>>)?.mapValues { Player.fromMap(it.value)!! } ?: mapOf(),
                    gameEvents = (map["gameEvents"] as? List<Map<String, Any?>>)?.mapNotNull { GameEvent.fromMap(it) } ?: listOf(),
                    currentPlayerId = map["currentPlayerId"] as? String,
                    status = GameStatus.valueOf(map["status"] as String)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

class GameRoomRepository {
    private val database: FirebaseDatabase = Firebase.database
    private val roomsRef: DatabaseReference = database.getReference("rooms")

    fun observeGameRoom(roomCode: String): Flow<GameRoom?> = callbackFlow {
        val listener = roomsRef.child(roomCode).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameRoom = snapshot.getValue<Map<String, Any?>>()?.let { GameRoom.fromMap(it) }
                trySend(gameRoom)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose { roomsRef.child(roomCode).removeEventListener(listener) }
    }

    suspend fun updateGameRoom(gameRoom: GameRoom) {
        roomsRef.child(gameRoom.roomCode).setValue(gameRoom.toMap()).await()
    }

    suspend fun createGameRoom(hostId: String, hostPlayer: Player): GameRoom {
        val roomCode = generateUniqueCode()
        val gameRoom = GameRoom(
            roomCode = roomCode,
            hostId = hostId,
            players = mapOf(hostId to hostPlayer),
            currentPlayerId = hostId
        )
        updateGameRoom(gameRoom)
        return gameRoom
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

    suspend fun getRoomSnapshot(code: String): Map<String, Any?>? {
        return try {
            val snapshot = roomsRef.child(code).get().await()
            snapshot.getValue<Map<String, Any?>>()
        } catch (e: Exception) {
            null
        }
    }
}
