package com.example.monopolymoney.presentation

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monopolymoney.R
import com.example.monopolymoney.data.GameEvent
import com.example.monopolymoney.data.Player
import com.example.monopolymoney.viewmodel.DataViewModel

private val MyYellow = Color(0xFFFFD67E)
private val GeneralPadding = 16.dp

@Composable
fun GameScreen(
    viewModel: DataViewModel,
    onNavigateToMoneyTransfer: () -> Unit,
    onNavigateToBankTransfer: () -> Unit,
    isMyTurn: Boolean,
    isHost: Boolean
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141F23))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(GeneralPadding)
                    .weight(1f)
            ) {
                TopSection(viewModel)
                Spacer(modifier = Modifier.height(20.dp))
                EventLogSection(modifier = Modifier.weight(1f), viewModel = viewModel)
            }
            BottomButtonsSection(
                modifier = Modifier.fillMaxWidth(),
                onNavigateToMoneyTransfer = { handleAction(context, isMyTurn, onNavigateToMoneyTransfer) },
                onNavigateToBankTransfer = { handleAction(context, isHost, onNavigateToBankTransfer, "Only the host can make bank transfers!") },
                onEndTurn = { handleAction(context, isMyTurn, viewModel::endTurn) },
                isMyTurn = isMyTurn,
                isHost = isHost
            )
        }
    }
}

@Composable
fun TopSection(viewModel: DataViewModel) {
    val user by viewModel.user.collectAsState()
    val players by viewModel.players.collectAsState(initial = emptyMap())
    val roomCode by viewModel.roomCode.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    val myId = user?.uuid
    val playerBalance = players[myId]?.balance?.toString() ?: "0"
    val isHost = players[myId]?.isHost ?: false

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        roomCode?.let {
            TopBar(
                roomCode = it,
                onExitClick = { showExitDialog = true }
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

        CityImage()
        Spacer(modifier = Modifier.height(GeneralPadding))

        user?.profileImageResId?.let { avatarResId ->
            UserCard(
                name = user?.name ?: "Player",
                balance = playerBalance,
                avatarResId = avatarResId
            )
        }
    }
}

@Composable
fun TopBar(roomCode: String, onExitClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GeneralPadding),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onExitClick) {
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
}

@Composable
fun CityImage() {
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
}

@Composable
fun EventLogSection(modifier: Modifier = Modifier, viewModel: DataViewModel) {
    val gameEvents by viewModel.transactions.collectAsState()
    val players by viewModel.players.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Divider(color = MyYellow, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 20.dp))

        if (gameEvents.isEmpty()) {
            EmptyEventLog()
        } else {
            EventList(gameEvents, players)
        }
    }
}

@Composable
fun EmptyEventLog() {
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
}

@Composable
fun EventList(gameEvents: List<GameEvent>, players: Map<String, Player>) {
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
                is GameEvent.PlayerJoinRoomEvent -> PlayerJoinEventCard(event)
                is GameEvent.PlayerLeftEvent -> PlayerLeftEventCard(event)
                is GameEvent.GameEndedEvent -> {} // Handle game ended event if needed
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
        modifier = Modifier.fillMaxWidth(),
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
fun GameEventCard(
    profileImageResId: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    wasHost: Boolean = false,
    newHostId: String? = null,
    titleColor: Color = Color.Black
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = profileImageResId),
            contentDescription = "Player Avatar",
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.carisma600)),
                color = titleColor
            )
            Text(
                text = message,
                fontFamily = FontFamily(Font(R.font.carisma500)),
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PlayerLeftEventCard(event: GameEvent.PlayerLeftEvent) {
    GameEventCard(
        profileImageResId = event.profileImageResId,
        title = "Player Left",
        message = "${event.playerName} has exited the game", // Evita repetir 'left the game'
        wasHost = event.wasHost,
        newHostId = event.newHostId,
        titleColor = Color.Red.copy(alpha = 0.7f) // Rojo más tenue para eventos negativos
    )
}

@Composable
fun PlayerJoinEventCard(event: GameEvent.PlayerJoinRoomEvent) {
    GameEventCard(
        profileImageResId = event.profileImageResId,
        title = "Player Joined",
        message = "${event.playerName} has entered the room", // Texto más amigable
        titleColor = Color.Green.copy(alpha = 0.8f) // Verde para eventos positivos
    )
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