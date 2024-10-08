package com.example.monopolymoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monopolymoney.data.GameRoom
import com.example.monopolymoney.data.GameRoomRepository
import com.example.monopolymoney.data.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class GameRoomViewModel(private val repository: GameRoomRepository) : ViewModel() {
//    private val _gameRoom = MutableStateFlow<GameRoom?>(null)
//    val gameRoom: StateFlow<GameRoom?> = _gameRoom.asStateFlow()
//
//    fun observeGameRoom(roomCode: String) {
//        viewModelScope.launch {
//            repository.observeGameRoom(roomCode).collect { updatedGameRoom ->
//                _gameRoom.value = updatedGameRoom
//            }
//        }
//    }
//
//    fun createRoom(hostId: String, hostPlayer: Player) {
//        viewModelScope.launch {
//            val newGameRoom = repository.createGameRoom(hostId, hostPlayer)
//            _gameRoom.value = newGameRoom
//        }
//    }
//
//    fun joinRoom(playerId: String, player: Player) {
//        viewModelScope.launch {
//            val currentGameRoom = _gameRoom.value ?: return@launch
//            val updatedPlayers = currentGameRoom.players + (playerId to player)
//            val updatedGameRoom = currentGameRoom.copy(players = updatedPlayers)
//            repository.updateGameRoom(updatedGameRoom)
//        }
//    }
//
//    fun startGame() {
//        viewModelScope.launch {
//            val currentGameRoom = _gameRoom.value ?: return@launch
//            val updatedGameRoom = currentGameRoom.copy(status = GameRoom.GameStatus.STARTED)
//            repository.updateGameRoom(updatedGameRoom)
//        }
//    }
//
//    fun endTurn() {
//        viewModelScope.launch {
//            val currentGameRoom = _gameRoom.value ?: return@launch
//            val playerIds = currentGameRoom.players.keys.toList()
//            val currentPlayerIndex = playerIds.indexOf(currentGameRoom.currentPlayerId)
//            val nextPlayerIndex = (currentPlayerIndex + 1) % playerIds.size
//            val updatedGameRoom = currentGameRoom.copy(currentPlayerId = playerIds[nextPlayerIndex])
//            repository.updateGameRoom(updatedGameRoom)
//        }
//    }
//
//    fun makeTransaction(fromPlayerId: String, toPlayerId: String, amount: Int) {
//        viewModelScope.launch {
//            val currentGameRoom = _gameRoom.value ?: return@launch
//            val updatedPlayers = currentGameRoom.players.mapValues { (playerId, player) ->
//                when (playerId) {
//                    fromPlayerId -> player.copy(balance = player.balance - amount)
//                    toPlayerId -> player.copy(balance = player.balance + amount)
//                    else -> player
//                }
//            }
//            val newTransaction = Transaction(
//                id = currentGameRoom.transactions.size,
//                fromPlayerId = fromPlayerId,
//                toPlayerId = toPlayerId,
//                amount = amount
//            )
//            val updatedTransactions = currentGameRoom.transactions + newTransaction
//            val updatedGameRoom = currentGameRoom.copy(
//                players = updatedPlayers,
//                transactions = updatedTransactions
//            )
//            repository.updateGameRoom(updatedGameRoom)
//        }
//    }

}