package com.example.monopolymoney.presentation

import MonopolyViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.monopolymoney.data.Transaction
import com.example.monopolymoney.data.TransactionDetails
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

//private val My_yellow = Color(0xFFFFD67E)



private val GeneralPadding = 16.dp
private val ButtonHeight = 55.dp
private val CardPadding = 8.dp

@Composable
fun LobbyScreen(viewModel: MonopolyViewModel, onNavigateToMoneyTransfer: () -> Unit, onNavigateToBankTransfer: () -> Unit) {
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }

    val roomCode by viewModel.roomCode.collectAsState()
    val gameStarted by viewModel.gameStarted.collectAsState()
    val players by viewModel.players.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val hostId by viewModel.hostId.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            roomCode == null -> MainButtons(
                onCreateClick = { showCreateRoomDialog = true },
                onJoinClick = { showJoinRoomDialog = true },
                viewModel = viewModel
            )
            !gameStarted -> WaitingRoomScreen(viewModel, players, roomCode)
            else -> GameScreen(
                viewModel = viewModel,
                roomCode = roomCode!!,
                players = players,
                currentPlayer = currentPlayer,
                transactions = transactions,
                hostId = hostId ?: "",
                onNavigateToMoneyTransfer = onNavigateToMoneyTransfer,
                onNavigateToBankTransfer = onNavigateToBankTransfer
            )
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
}

@Composable
fun MainButtons(onCreateClick: () -> Unit, onJoinClick: () -> Unit, viewModel: MonopolyViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text( "")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onJoinClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(viewModel.playerName.value ?: "")
        }
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
fun WaitingRoomScreen(viewModel: MonopolyViewModel, players: List<Player>, roomCode: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Waiting Room", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Room Code: $roomCode", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Players:", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(players) { player ->
                Text("${player.name} (ID: ${player.id})", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.startGame() },
            modifier = Modifier.fillMaxWidth(),
            enabled = players.size >= 1
        ) {
            Text("Start Game")
        }
    }
}


// Función que mueve al jugador
fun makeMove(currentPosition: Float, steps: Int): Float {
    return currentPosition + steps.toFloat()
}

@Composable
fun GridGame(playerPosition: Float, onMovePlayer: (steps: Int) -> Unit) {
    val visibleGridSize = 4 // Tamaño visible de la cuadrícula
    val totalRows = 10 // Total de filas
    val totalCells = visibleGridSize * totalRows // Total de celdas

    var stepsToMove by remember { mutableStateOf(0) } // Número de pasos
    val timePerStep = 300 // Tiempo por paso (ms)

    // Estado animado de la posición del jugador
    val animatedPlayerPosition by animateFloatAsState(
        targetValue = playerPosition,
        animationSpec = tween(durationMillis = timePerStep * stepsToMove, easing = LinearEasing)
    )

    val playerImage = painterResource(id = R.drawable.house_icon)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val tileSize = maxWidth / visibleGridSize

        // Implementar scrollable para suavizar el desplazamiento
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .verticalScroll(scrollState)
        ) {
            Column {
                for (row in 0 until totalRows) {
                    Row {
                        for (col in 0 until visibleGridSize) {
                            val index = row * visibleGridSize + col

                            // Posición actual del jugador en la cuadrícula
                            val actualPlayerPosition = (animatedPlayerPosition % totalCells).toInt()
                            val isPlayerOnTile = actualPlayerPosition == index

                            Box(
                                modifier = Modifier
                                    .size(tileSize)
                                    .border(1.dp, Color.Black)
                            ) {
                                CustomBox(
                                    name = "Tile $index",
                                    amount = "${index * 100}",
                                    modifier = Modifier
                                        .background(if (index % 2 == 0) Color.Blue else Color.Green)
                                )

                                // Renderiza la imagen del jugador si está en esta tile
                                if (isPlayerOnTile) {
                                    Image(
                                        painter = playerImage,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(tileSize / 4)
                                            .align(Alignment.Center)
                                            .zIndex(1f) // Asegura que el jugador esté encima de la tile
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CustomBox(name: String, amount: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Rectángulo rojo en la parte superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color.Red)
                .align(Alignment.TopCenter)
        )

        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
                .background(Color.Black),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = amount,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

// Constants
val My_yellow = Color(0xFFFFD67E)

@Composable
fun MyScreen() {
    var playerPosition by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF141F23))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                //TopSection()
                Spacer(modifier = Modifier.height(20.dp))
                EventLogSection(modifier = Modifier.weight(1f))
            }
            BottomButtonsSection(
                modifier = Modifier.fillMaxWidth().padding(0.dp),
                onMovePlayer = { steps -> playerPosition = makeMove(playerPosition, steps) }
            )
        }
    }
}

@Composable
fun TopSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 8f)
                .clip(RoundedCornerShape(30.dp))
                .border(0.5.dp, My_yellow, RoundedCornerShape(30.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.city2),
                contentDescription = "Pixel art city",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        UserCard(avatarResId = R.drawable.faces01)
    }
}

@Composable
fun InfoCard(
    avatarResId: Int,
    isSpecialCard: Boolean,
    content: @Composable () -> Unit
) {
    val cardModifier = if (isSpecialCard) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(Color.Transparent)

    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = avatarResId),
                contentDescription = "Avatar",
                modifier = Modifier.size(44.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            content()
        }
    }
}

@Composable
fun UserCard(avatarResId: Int) {
    InfoCard(avatarResId, isSpecialCard = false) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Casandra",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.multipixelregular)),
                    color = My_yellow
                )
                Text(
                    text = "$1755",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.multipixelregular)),
                    color = Color(0xFF8AE78E)
                )
            }
            Column(horizontalAlignment = Alignment.Start) {
                PropertyRow(R.drawable.house_icon, "24")
                PropertyRow(R.drawable.building_icon, "15")
            }
        }
    }
}

@Composable
fun PropertyRow(iconResId: Int, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(1.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count,
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.multipixelregular)),
            fontSize = 16.sp
        )
    }
}

@Composable
fun TransactionCard(
    fromUser: String,
    toUser: String,
    amount: Int,
    avatarResId: Int
) {
    InfoCard(avatarResId = avatarResId, isSpecialCard = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Transaction",
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.carisma600)),
                    color = My_yellow
                )
                Text(
                    text = "$fromUser → $toUser",
                    color = Color.Gray,
                    fontFamily = FontFamily(Font(R.font.carisma500)),
                    fontSize = 16.sp
                )
            }
            Text(
                text = "$$amount",
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.carisma600)),
                color = Color(0xFFD2D2D2)
            )
        }
    }
}

@Composable
fun EventLogSection(modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {
        // Yellow line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.5.dp)
                    .background(My_yellow)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(transactionList) { transaction ->
                Column {
                    TransactionCard(
                        fromUser = transaction.fromUser,
                        toUser = transaction.toUser,
                        amount = transaction.amount,
                        avatarResId = transaction.avatarResId
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(
                        color = Color.Gray.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun BottomButtonsSection(
    modifier: Modifier = Modifier,
    onMovePlayer: (steps: Int) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF16252B), shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .padding(vertical = 15.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BottomButton(drawableIcon = R.drawable.send, text = "Send", onClick = {})
        BottomButton(drawableIcon = R.drawable.bank, text = "Bank", onClick = {})
        BottomButton(drawableIcon = R.drawable.end, text = "End", onClick = { onMovePlayer(3) })
    }
}

@Composable
fun BottomButton(
    drawableIcon: Int? = null,
    vectorIcon: ImageVector? = null,
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                drawableIcon != null -> Image(
                    painter = painterResource(id = drawableIcon),
                    contentDescription = text,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(32.dp)
                )
                vectorIcon != null -> Icon(
                    imageVector = vectorIcon,
                    contentDescription = text,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

data class Transaction(
    val fromUser: String,
    val toUser: String,
    val amount: Int,
    val avatarResId: Int
)

val transactionList = listOf(
    Transaction("Martha", "Deibis", 1250, R.drawable.faces01),
    Transaction("John", "Alice", 750, R.drawable.faces02),
    Transaction("Guadalupe", "Luca", 500, R.drawable.faces03),
    Transaction("Sophia", "Ethan", 1000, R.drawable.faces01),
    Transaction("Oliver", "Ava", 1500, R.drawable.faces02),
    Transaction("Martha", "Deibis", 1250, R.drawable.faces01),
    Transaction("Deibis", "Alice", 750, R.drawable.faces02),
    Transaction("Jason", "Lucas", 500, R.drawable.faces03),
    Transaction("Sophia", "Ethan", 1000, R.drawable.faces01),
    Transaction("Oliver", "Ava", 1500, R.drawable.faces02),
    Transaction("Emma", "Lucas", 500, R.drawable.faces03),
    Transaction("Sophia", "Ethan", 1000, R.drawable.faces01),
    Transaction("Oliver", "Ava", 1500, R.drawable.faces02),
    Transaction("Martha", "Deibis", 1250, R.drawable.faces01),
    Transaction("John", "Alice", 750, R.drawable.faces02),
    Transaction("Guadalupe", "Luca", 500, R.drawable.faces03),
    Transaction("Sophia", "Ethan", 1000, R.drawable.faces01),
    Transaction("Oliver", "Ava", 1500, R.drawable.faces02),
    Transaction("Martha", "Deibis", 1250, R.drawable.faces01),
    Transaction("Deibis", "Alice", 750, R.drawable.faces02),
    Transaction("Jason", "Lucas", 500, R.drawable.faces03),
    Transaction("Sophia", "Ethan", 1000, R.drawable.faces01),
    Transaction("Oliver", "Ava", 1500, R.drawable.faces02),
    Transaction("Emma", "Lucas", 500, R.drawable.faces03),
    Transaction("Sophia", "Ethan", 1000, R.drawable.faces01),
    Transaction("Oliver", "Ava", 1500, R.drawable.faces02)
)

// Note: The makeMove function is not defined in the provided code.
// You'll need to implement this function based on your game logic.











@Composable
fun GameScreen(
    viewModel: MonopolyViewModel,
    roomCode: String,
    players: List<Player>,
    currentPlayer: String?,
    transactions: List<Transaction>,
    hostId: String,  // Agregar este parámetro
    onNavigateToMoneyTransfer: () -> Unit, // Add this parameter
    onNavigateToBankTransfer: () -> Unit  // New parameter
) {
    val myPlayerId by viewModel.playerId.collectAsState()
    val myPlayer = players.find { it.id == myPlayerId }
    var showMoneyTransferScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            AlignedIconsWithText(
                centerText = ("Code: " + roomCode + " - Name: " + viewModel.playerName.value) ?: "",
                onLeftClick = { /* Action for left icon click */ },
                onRightClick = { /* Action for right icon click */ }
            )
            Spacer(modifier = Modifier.height(24.dp))

            myPlayer?.let { player ->
                BalanceCard(player)
            }

            TransactionList(players, transactions)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            TransactionButtons(
                onSendClick = onNavigateToMoneyTransfer,
                onMiddleClick = onNavigateToBankTransfer,  // Updated to use new function
                onCloseClick = { viewModel.endTurn() },
                showMiddleButton = myPlayerId == hostId,
                isSendEnabled = currentPlayer == myPlayerId,
                isMiddleEnabled = true,
                isCloseEnabled = currentPlayer == myPlayerId,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }

}

// Función 'transactionDetails' usando colores del esquema
@Composable
fun transactionDetails(transaction: Transaction, players: List<Player>): TransactionDetails {
    val fromName = if (transaction.fromPlayer == "-1") "Bank" else players.find { it.id == transaction.fromPlayer }?.name ?: "Unknown"
    val toName = if (transaction.toPlayer == "-1") "Bank" else players.find { it.id == transaction.toPlayer }?.name ?: "Unknown"

    val isReceiving = transaction.toPlayer != "-1"
    val amountText = if (isReceiving) "-$${transaction.amount}" else "+$${transaction.amount}"

    return TransactionDetails(fromName, toName, amountText)
}
