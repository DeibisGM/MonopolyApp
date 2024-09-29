package com.example.monopolymoney.data

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


data class Player(
    val id: String? = null, // Cambiado a null por defecto para reflejar el tipo String?
    val name: String? = null, // Cambiado a null por defecto para reflejar el tipo String?
    val balance: Int = 0
) {
    fun toMap(): Map<String, Any?> { // Cambiado a Any? para permitir valores nulos
        return mapOf(
            "id" to id,
            "name" to name,
            "balance" to balance
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Player? { // Cambiado a Any? para permitir valores nulos
            return try {
                Player(
                    id = map["id"] as? String, // Se mantiene como String?
                    name = map["name"] as? String, // Se mantiene como String?
                    balance = (map["balance"] as? Long)?.toInt() ?: 0
                )
            } catch (e: ClassCastException) {
                // Manejo de error si el tipo es incorrecto
                null
            }
        }
    }
}



data class Transaction(
    val id: Int = 0,
    val fromPlayer: String = "", // -1 para el banco
    val toPlayer: String = "",
    val amount: Int = 0,
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "fromPlayer" to fromPlayer,
            "toPlayer" to toPlayer,
            "amount" to amount
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Transaction? {
            return try {
                Transaction(
                    id = (map["id"] as? Number)?.toInt() ?: 0,
                    fromPlayer = map["fromPlayer"] as String,
                    toPlayer = map["toPlayer"] as String,
                    amount = (map["amount"] as? Number)?.toInt() ?: 0
                )
            } catch (e: ClassCastException) {
                // Manejo de error si el tipo es incorrecto
                null
            }
        }
    }}

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
    val id: String? = null, // Cambiado a String? con valor por defecto null
    val name: String? = null, // Cambiado a String? con valor por defecto null
    val balance: Int = 0
) : Parcelable {
    fun toPlayer() = Player(id, name, balance) // Conversión a Player con String?
}

fun Player.toParcelable() = PlayerParcelable(id, name, balance) // Conversión a PlayerParcelable

// Transaction.kt

@Parcelize
data class TransactionParcelable(
    val id: Int,
    val fromPlayer: String,
    val toPlayer: String,
    val amount: Int
) : Parcelable {
    fun toTransaction() = Transaction(id, fromPlayer, toPlayer, amount)
}

fun Transaction.toParcelable() = TransactionParcelable(id, fromPlayer, toPlayer, amount)