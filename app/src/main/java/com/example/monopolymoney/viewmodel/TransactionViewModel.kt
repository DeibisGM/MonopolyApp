package com.example.monopolymoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel


// TransactionViewModel.kt
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
//    private val database: FirebaseDatabase = Firebase.database
//    private val roomsRef: DatabaseReference = database.getReference("rooms")
//
//    fun makeTransaction(roomCode: String, fromPlayerId: String, toPlayerId: String, amount: Int, players: List<Player>, transactions: List<Transaction>) {
//        viewModelScope.launch {
//            val updatedPlayers = players.map { player ->
//                when (player.id) {
//                    fromPlayerId -> player.copy(balance = player.balance - amount)
//                    toPlayerId -> player.copy(balance = player.balance + amount)
//                    else -> player
//                }
//            }
//
//            val newTransaction = Transaction(
//                id = transactions.size,
//                fromPlayerId = fromPlayerId,
//                toPlayerId = toPlayerId,
//                amount = amount
//            )
//
//            val updatedTransactions = transactions + newTransaction
//
//            val gameState = GameState(
//                hostId = players.firstOrNull()?.id ?: "",
//                players = updatedPlayers,
//                status = "started",
//                currentPlayer = fromPlayerId,
//                transactions = updatedTransactions
//            )
//
//            updateRoomInFirebase(roomCode, gameState)
//        }
//    }
//
//    fun makeBankTransaction(roomCode: String, toPlayerId: String, amount: Int,
//                            players: List<Player>, transactions: List<Transaction>, currentPlayer: String) {
//        viewModelScope.launch {
//            val updatedPlayers = players.map { player ->
//                if (player.id == toPlayerId) {
//                    player.copy(balance = player.balance + amount)
//                } else {
//                    player
//                }
//            }
//
//            val newTransaction = Transaction(
//                id = transactions.size,
//                fromPlayerId = "-1",
//                toPlayerId = toPlayerId,
//                amount = amount
//            )
//
//            val updatedTransactions = transactions + newTransaction
//
//            val gameState = GameState(
//                hostId = players.firstOrNull()?.id ?: "",
//                players = updatedPlayers,
//                status = "started",
//                currentPlayer = currentPlayer,
//                transactions = updatedTransactions
//            )
//
//            updateRoomInFirebase(roomCode, gameState)
//        }
//    }
//
//    private suspend fun updateRoomInFirebase(roomCode: String, gameState: GameState) {
//        roomsRef.child(roomCode).setValue(gameState.toMap()).await()
//    }
}
