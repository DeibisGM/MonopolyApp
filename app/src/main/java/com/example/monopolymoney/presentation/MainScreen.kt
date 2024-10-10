package com.example.monopolymoney.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monopolymoney.R
import com.example.monopolymoney.data.Player
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.monopolymoney.data.GameRoom
import com.example.monopolymoney.viewmodel.AuthViewModel
import com.example.monopolymoney.viewmodel.DataViewModel

private val MyYellow = Color(0xFFFFD67E)
private val GeneralPadding = 16.dp
private val ButtonHeight = 55.dp

@Composable
fun LobbyScreen(
    viewModel: DataViewModel,
    onNavigateToMoneyTransfer: () -> Unit,
    onNavigateToBankTransfer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val roomCode by viewModel.roomCode.collectAsState()
    val gameStarted by viewModel.gameStarted.collectAsState()
    val players by viewModel.players.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val user by viewModel.user.collectAsState()
    val userId = user?.uuid
    val hostId by viewModel.hostId.collectAsState()
    val gameStatus by viewModel.gameStatus.collectAsState()

    val isHost = userId == hostId
    val isMyTurn = currentPlayer == userId

            Box(modifier = Modifier.fillMaxSize()) {
                if (roomCode == null) {
                    IconButton(
                        onClick = { onNavigateToSettings() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(GeneralPadding)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.end),
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }

                when {
                    roomCode == null -> MainButtons(viewModel)
                    gameStatus == GameRoom.GameStatus.FINISHED -> GameEndScreen(viewModel, onNavigateToHome)
                    !gameStarted -> WaitingRoomScreen(viewModel, players, roomCode, isHost, onNavigateToHome)
                    else -> GameScreen(
                        viewModel = viewModel,
                        onNavigateToMoneyTransfer = onNavigateToMoneyTransfer,
                        onNavigateToBankTransfer = onNavigateToBankTransfer,
                        isMyTurn = isMyTurn,
                        isHost = isHost
                    )
                }
            }
        }

@Composable
fun WaitingRoomScreen(
    viewModel: DataViewModel,
    players: Map<String, Player>,
    roomCode: String?,
    isHost: Boolean,
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Waiting Room",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )


        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Room Code:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = roomCode ?: "N/A",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isHost) {
            Button(
                onClick = viewModel::startGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Start Game", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))


            Button(
                onClick = {
                    viewModel.deleteGame()
                    onNavigateToHome()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Cancel Game", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }

        Text(
            text = "Players:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(players.values.toList()) { player ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        player.name?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (player.isHost) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "(Host)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainButtons(viewModel: DataViewModel) {
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GeneralPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { showCreateRoomDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonHeight)
        ) {
            Text("Create Room")
        }
        Spacer(modifier = Modifier.height(GeneralPadding))
        Button(
            onClick = { showJoinRoomDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonHeight)
        ) {
            Text("Join Room")
        }
    }

    if (showCreateRoomDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateRoomDialog = false },
            onConfirm = {
                viewModel.createRoom()
                showCreateRoomDialog = false
            }
        )
    }
    if (showJoinRoomDialog) {
        JoinRoomDialog(
            onDismiss = { showJoinRoomDialog = false },
            onConfirm = { code ->
                viewModel.joinRoom(code)
                showJoinRoomDialog = false
            }
        )
    }
}

@Composable
fun GameEndScreen(viewModel: DataViewModel, onNavigateToHome: () -> Unit) {
    val players by viewModel.players.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141F23))
            .padding(GeneralPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Game Over!",
            fontSize = 24.sp,
            fontFamily = FontFamily(Font(R.font.carisma600)),
            color = MyYellow,
            modifier = Modifier.padding(vertical = 32.dp)
        )

        Text(
            text = "Final Balances",
            fontSize = 20.sp,
            fontFamily = FontFamily(Font(R.font.carisma500)),
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(players.values.toList()) { player ->
                PlayerFinalBalanceCard(player)
            }
        }

        Button(
            onClick = {
                onNavigateToHome()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Return to Main Menu")
        }
    }
}

@Composable
fun PlayerFinalBalanceCard(player: Player) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16252B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = player.profileImageResId),
                contentDescription = "Player avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = player.name ?: "Unknown",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.carisma500))
                )
                if (player.isHost) {
                    Text(
                        text = "Host",
                        color = MyYellow,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.carisma500))
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "$${player.balance}",
                color = MyYellow,
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.carisma600))
            )
        }
    }
}