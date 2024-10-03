package com.example.monopolymoney.data

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


data class Player(
    val id: String? = null,
    val name: String? = null,
    val balance: Int = 0,
    val profileImageResId: Int = 0 // Nuevo campo para el ID del recurso de la imagen de perfil
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "balance" to balance,
            "profileImageResId" to profileImageResId
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Player? {
            return try {
                Player(
                    id = map["id"] as? String,
                    name = map["name"] as? String,
                    balance = (map["balance"] as? Long)?.toInt() ?: 0,
                    profileImageResId = (map["profileImageResId"] as? Long)?.toInt() ?: 0
                )
            } catch (e: ClassCastException) {
                null
            }
        }
    }
}

data class Transaction(
    val id: Int = 0,
    val fromPlayerId: String = "", // Cambiado de fromPlayer a fromPlayerId
    val toPlayerId: String = "",   // Cambiado de toPlayer a toPlayerId
    val amount: Int = 0,
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "fromPlayerId" to fromPlayerId,
            "toPlayerId" to toPlayerId,
            "amount" to amount
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Transaction? {
            return try {
                Transaction(
                    id = (map["id"] as? Number)?.toInt() ?: 0,
                    fromPlayerId = map["fromPlayerId"] as String,
                    toPlayerId = map["toPlayerId"] as String,
                    amount = (map["amount"] as? Number)?.toInt() ?: 0
                )
            } catch (e: ClassCastException) {
                null
            }
        }
    }
}

data class TransactionDetails(
    val fromName: String = "",
    val toName: String = "",
    val amountText: String = "",
)

data class GameState(
    val hostId: String,           // Primero el host
    val players: List<Player>,    // Luego la lista de jugadores
    val status: String,           // Estado del juego (esperando o iniciado)
    val currentPlayer: String,    // El jugador actual
    val transactions: List<Transaction> // Finalmente las transacciones
) {
    // Convertir el estado del juego a un mapa con el orden correcto
    fun toMap(): Map<String, Any> {
        return mapOf(
            "hostId" to hostId,
            "players" to players.map { it.toMap() },
            "status" to status,
            "currentPlayer" to currentPlayer,
            "transactions" to transactions.map { it.toMap() }
        )
    }
}

@Parcelize
data class PlayerParcelable(
    val id: String? = null,
    val name: String? = null,
    val balance: Int = 0,
    val profileImageResId: Int = 0
) : Parcelable {
    fun toPlayer() = Player(id, name, balance, profileImageResId)
}

fun Player.toParcelable() = PlayerParcelable(id, name, balance, profileImageResId)

// Transaction.kt

@Parcelize
data class TransactionParcelable(
    val id: Int,
    val fromPlayerId: String,
    val toPlayerId: String,
    val amount: Int
) : Parcelable {
    fun toTransaction() = Transaction(id, fromPlayerId, toPlayerId, amount)
}

fun Transaction.toParcelable() = TransactionParcelable(id, fromPlayerId, toPlayerId, amount)