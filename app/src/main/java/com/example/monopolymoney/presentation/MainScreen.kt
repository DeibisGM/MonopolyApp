package com.example.monopolymoney.presentation

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monopolymoney.R
import com.example.monopolymoney.data.Player
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import com.example.monopolymoney.data.GameEvent
import com.example.monopolymoney.data.GameRoom
import com.example.monopolymoney.viewmodel.DataViewModel

private val MyYellow = Color(0xFFFFD67E)
private val GeneralPadding = 16.dp
private val ButtonHeight = 55.dp

@Composable
fun LobbyScreen(
    viewModel: DataViewModel,
    onNavigateToMoneyTransfer: () -> Unit,
    onNavigateToBankTransfer: () -> Unit
) {
    val roomCode by viewModel.roomCode.collectAsState()
    val gameStarted by viewModel.gameStarted.collectAsState()
    val players by viewModel.players.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val user by viewModel.user.collectAsState()
    val userId = user?.uuid
    val hostId by viewModel.hostId.collectAsState()
    val gameStatus by viewModel.gameStatus.collectAsState()

    val isHost = userId == hostId
    val isMyTurn = currentPlayer == userId

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            roomCode == null -> MainButtons(viewModel)
            gameStatus == GameRoom.GameStatus.FINISHED -> GameEndScreen(viewModel)
            !gameStarted -> WaitingRoomScreen(viewModel, players, roomCode, isHost)
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
fun CreateRoomDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmación") },
        text = { Text("¿Estás seguro de que quieres crear una sala?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Sí, crear sala")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


@Composable
fun JoinRoomDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Room") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Room Code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.isNotBlank()
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WaitingRoomScreen(
    viewModel: DataViewModel,
    players: Map<String, Player>, // Now a Map
    roomCode: String?,
    isHost: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GeneralPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Waiting Room", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(GeneralPadding))
        Text("Room Code: $roomCode", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(32.dp))

        if (isHost) {
            Button(onClick = viewModel::startGame) {
                Text("Start Game")
            }
        }

        Text("Players:", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(players.values.toList()) { player -> // Iterate over values
                Text(
                    "${player.name} ${if (player.isHost) "(Host)" else ""}", // Access isHost from Player
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun GameScreen(
    viewModel: DataViewModel,
    onNavigateToMoneyTransfer: () -> Unit,
    onNavigateToBankTransfer: () -> Unit,
    isMyTurn: Boolean,
    isHost: Boolean
) {
    val context = LocalContext.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF141F23))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .padding(GeneralPadding)
                .weight(1f)) {
                TopSection(viewModel)
                Spacer(modifier = Modifier.height(20.dp))
                EventLogSection(modifier = Modifier.weight(1f), viewModel = viewModel)
            }
            BottomButtonsSection(
                modifier = Modifier.fillMaxWidth(),
                onNavigateToMoneyTransfer = {
                    if (isMyTurn) onNavigateToMoneyTransfer()
                    else Toast.makeText(context, "It's not your turn yet!", Toast.LENGTH_SHORT).show()
                },
                onNavigateToBankTransfer = {
                    if (isHost) onNavigateToBankTransfer()
                    else Toast.makeText(context, "Only the host can make bank transfers!", Toast.LENGTH_SHORT).show()
                },
                onEndTurn = {
                    if (isMyTurn) viewModel.endTurn()
                    else Toast.makeText(context, "It's not your turn yet!", Toast.LENGTH_SHORT).show()
                },
                isMyTurn = isMyTurn,
                isHost = isHost
            )
        }
    }
}

@Composable
fun TopSection(viewModel: DataViewModel) {
    val user by viewModel.user.collectAsState()
    val myId = user?.uuid
    val profileImageResId = user?.profileImageResId
    val name = user?.name
    val players by viewModel.players.collectAsState(initial = emptyMap())
    val roomCode by viewModel.roomCode.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    val playerBalance: String = players[myId]?.balance?.toString() ?: "0"
    val isHost: Boolean = players[myId]?.isHost ?: false

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GeneralPadding),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { showExitDialog = true }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.end),
                    contentDescription = "Exit",
                    tint = Color.Red.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "Room Code: $roomCode",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontFamily = FontFamily(Font(R.font.carisma500)),
                fontSize = 16.sp
            )
        }

        if (showExitDialog) {
            ExitGameDialog(
                isHost = isHost,
                onDismiss = { showExitDialog = false },
                onLeaveGame = {
                    viewModel.leaveGame()
                    showExitDialog = false
                },
                onEndGame = {
                    viewModel.endGame()
                    showExitDialog = false
                }
            )
        }

        // Rest of the TopSection content remains the same
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 8f)
                .clip(RoundedCornerShape(30.dp))
                .border(0.5.dp, MyYellow, RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.city2),
                contentDescription = "Pixel art city",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(GeneralPadding))

        if (profileImageResId != null) {
            UserCard(
                name = name?: "Player",
                balance = playerBalance,
                avatarResId = profileImageResId
            )
        }
    }
}

@Composable
fun ExitGameDialog(
    isHost: Boolean,
    onDismiss: () -> Unit,
    onLeaveGame: () -> Unit,
    onEndGame: () -> Unit
) {
    if (isHost) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Exit Options") },
            text = { Text("Do you want to leave the game or end it for everyone?") },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onLeaveGame,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.8f)
                        )
                    ) {
                        Text("Leave Game")
                    }
                    Button(
                        onClick = onEndGame,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("End Game for All")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Leave Game") },
            text = { Text("Are you sure you want to leave the game?") },
            confirmButton = {
                Button(
                    onClick = onLeaveGame,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Yes, Leave Game")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun UserCard(name: String, balance:String, avatarResId: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = avatarResId),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = "$$balance",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MyYellow
            )
        }
    }
}

@Composable
fun EventLogSection(modifier: Modifier = Modifier, viewModel: DataViewModel) {
    val gameEvents by viewModel.transactions.collectAsState()
    val players by viewModel.players.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Divider(color = MyYellow, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 20.dp))

        if (gameEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GeneralPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events yet",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = true
            ) {
                items(gameEvents) { event ->
                    Divider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    when (event) {
                        is GameEvent.TransactionEvent -> TransactionEventCard(event, players)
                        is GameEvent.PlayerLeftEvent -> PlayerLeftEventCard(event)
                        is GameEvent.GameEndedEvent -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionEventCard(transaction: GameEvent.TransactionEvent, players: Map<String, Player>) {
    val fromUser = if (transaction.fromPlayerId == "-1") "Bank" else players[transaction.fromPlayerId]?.name ?: "Unknown"
    val toUser = if (transaction.toPlayerId == "-1") "Bank" else players[transaction.toPlayerId]?.name ?: "Unknown"
    val avatarResId = players[transaction.fromPlayerId]?.profileImageResId ?: R.drawable.default_avatar

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = avatarResId),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(GeneralPadding))
        Column {
            Text(
                text = "Transaction",
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.carisma600)),
                color = MyYellow
            )

            Text(
                text = "$fromUser → $toUser",
                fontFamily = FontFamily(Font(R.font.carisma500)),
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$${transaction.amount}",
            fontFamily = FontFamily(Font(R.font.carisma600)),
            fontSize = 16.sp,
            color = Color(0xFFD2D2D2)
        )
    }
}


@Composable
fun GameEndScreen(viewModel: DataViewModel) {
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
            onClick = { //viewModel.clearGameRoom()
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

@Composable
fun PlayerLeftEventCard(event: GameEvent.PlayerLeftEvent) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = event.profileImageResId),
            contentDescription = "Player Avatar",
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(GeneralPadding))
        Column {
            Text(
                text = "Player Left",
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.carisma600)),
                color = Color.Red.copy(alpha = 0.7f)
            )

            Text(
                text = "${event.playerName} left the game",
                fontFamily = FontFamily(Font(R.font.carisma500)),
                color = Color.Gray,
                fontSize = 16.sp
            )

            if (event.wasHost && event.newHostId != null) {
                Text(
                    text = "New host assigned",
                    fontFamily = FontFamily(Font(R.font.carisma500)),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun BottomButtonsSection(
    modifier: Modifier = Modifier,
    onNavigateToMoneyTransfer: () -> Unit,
    onNavigateToBankTransfer: () -> Unit,
    onEndTurn: () -> Unit,
    isMyTurn: Boolean,
    isHost: Boolean
) {
    Row(
        modifier = modifier
            .background(
                Color(0xFF16252B),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            )
            .padding(vertical = 20.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BottomButton(
            drawableIcon = R.drawable.send,
            onClick = onNavigateToMoneyTransfer,
            isActive = isMyTurn
        )
        BottomButton(
            drawableIcon = R.drawable.bank,
            onClick = onNavigateToBankTransfer,
            isActive = isHost
        )
        BottomButton(
            drawableIcon = R.drawable.end,
            onClick = onEndTurn,
            isActive = isMyTurn
        )
    }
}

@Composable
fun BottomButton(
    drawableIcon: Int,
    onClick: () -> Unit,
    isActive: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isActive, onClick = onClick)
            .padding(horizontal = 24.dp)
            .alpha(if (isActive) 1f else 0.5f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = drawableIcon),
                contentDescription = "image",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}