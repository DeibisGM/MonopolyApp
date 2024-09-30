package com.example.monopolymoney.presentation

import MonopolyViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.foundation.layout.size

private val My_yellow = Color(0xFFFFD67E)

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



//@Composable
//fun RepeatingBackgroundPattern(
//    spacing: Float = 12f,
//    backgroundColor: Color = Color(0xFF334051) // Color de fondo en HEX
//) {
//    val patternImage = ImageBitmap.imageResource(R.drawable.tile2)
//
//    Canvas(modifier = Modifier.fillMaxSize()) {
//        // Dibuja el color de fondo
//        drawRect(color = backgroundColor, size = size)
//
//        val imageWidth = patternImage.width.toFloat()
//        val imageHeight = patternImage.height.toFloat()
//
//        // Dibuja la imagen en un patrón repetido con espaciado
//        for (x in 0..(size.width / (imageWidth + spacing)).toInt()) {
//            for (y in 0..(size.height / (imageHeight + spacing)).toInt()) {
//                drawImage(
//                    image = patternImage,
//                    topLeft = Offset(
//                        x * (imageWidth + spacing),
//                        y * (imageHeight + spacing)
//                    )
//                )
//            }
//        }
//    }
//}

@Composable
fun MyScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFF141F23)
//                brush = Brush.verticalGradient(
//                    colors = listOf(
//                        Color(0xFF354359), // Color inicial del degradado (azul oscuro)
//                        Color(0xFF0F1118),  // Color final del degradado (azul claro)
//
//                    )
//                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f) // Esto permitirá que el contenido principal ocupe el espacio disponible
            ) {
                // Código de la parte superior
                TopSection()
                Spacer(modifier = Modifier.height(20.dp))

                // Contenido principal (Registro de eventos)
                EventLogSection(modifier = Modifier.weight(1f))
            }

            // Parte inferior con los botones anclados, sin padding
            BottomButtonsSection(modifier = Modifier.fillMaxWidth().padding(0.dp))
        }
    }
}

@Composable
fun TopSection() {
    val my_yellow = Color(0xFFFFD67E) // Por ejemplo, un color amarillo
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Imagen de la ciudad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 8f)
                .clip(RoundedCornerShape(30.dp))
                .border(0.5.dp, my_yellow, RoundedCornerShape(30.dp)) // Cambia el grosor y el color del borde según tus necesidades
        ) {
            Image(
                painter = painterResource(id = R.drawable.city2),
                contentDescription = "Pixel art city",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }


        // Espacio entre la imagen y la tarjeta
        Spacer(modifier = Modifier.height(16.dp))


        UserCard(
            avatarResId = R.drawable.faces01
        )


    }
}


@Composable
fun InfoCard(
    avatarResId: Int,
    isSpecialCard: Boolean, // Nuevo parámetro para indicar si es una tarjeta especial
    content: @Composable () -> Unit
) {
    val cardModifier = if (isSpecialCard) {
        Modifier
            .fillMaxWidth()
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),  // Arriba, opacidad 0%
                        Color.White.copy(alpha = 0f),  // Arriba, opacidad 0%
                        Color.White.copy(alpha = 0.15f)   // Abajo, opacidad 100%
                    )
                ),
                shape = RoundedCornerShape(40.dp) // Solo parte inferior
            )
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(40.dp),
        colors = if (isSpecialCard) {
            CardDefaults.cardColors(containerColor = Color.Transparent) // Fondo transparente
        } else {
            CardDefaults.cardColors(containerColor = Color.Transparent) // Color normal
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Image(
                painter = painterResource(id = avatarResId),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Custom content
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
            // Nombre y precio
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Casandra",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.multipixelregular)),
                    color = My_yellow
                )
                Spacer(modifier = Modifier.height(0.dp))
                Text(
                    text = "$1755",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.multipixelregular)),
                    color = Color(0xFF8AE78E)
                )
            }

            // Información de casas y edificios
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.house_icon),
                        contentDescription = "Houses",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "24",
                        color = Color.White,
                        fontFamily = FontFamily(Font(R.font.multipixelregular)),
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(0.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.building_icon),
                        contentDescription = "Buildings",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "15",
                        color = Color.White,
                        fontFamily = FontFamily(Font(R.font.multipixelregular)),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    fromUser: String,
    toUser: String,
    amount: Int,
    avatarResId: Int
) {
    InfoCard(
        avatarResId = avatarResId, isSpecialCard = true
    ) {
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
                Spacer(modifier = Modifier.height(0.dp))
                Text(
                    text = "$fromUser → $toUser",
                    color = Color.Gray,
                    fontFamily = FontFamily(Font(R.font.carisma500)),
                    fontSize = 16.sp
                )
            }
            Text(
                text = "$${amount.toInt()}",
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.carisma600)),
                color = Color(0xFFD2D2D2)
            )

        }
    }
}

@Composable
fun EventLogSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Title and line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Text(
//                text = "Registro de eventos",
//                fontWeight = FontWeight.Bold,
//                fontSize = 18.sp,
//                color = Color(0xFFFFD67E),
//                fontFamily = FontFamily(Font(R.font.carisma700)),
//                modifier = Modifier.padding(end = 16.dp)
//            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.5.dp)
                    .background(My_yellow)
            )
        }

        // List of transactions
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(transactionList) { transaction ->
                TransactionCard(
                    fromUser = transaction.fromUser,
                    toUser = transaction.toUser,
                    amount = transaction.amount,
                    avatarResId = transaction.avatarResId
                )
            }
        }
    }
}

// Data class to represent a transaction
data class Transaction(
    val fromUser: String,
    val toUser: String,
    val amount: Int,
    val avatarResId: Int
)

// Sample list of transactions
val transactionList = listOf(
    Transaction("Martha", "Deibis", 1250, R.drawable.faces01),
    Transaction("John", "Alice", 750, R.drawable.faces02),
    Transaction("Emma", "Lucas", 500, R.drawable.faces03),
    Transaction("Sophia", "Ethan", 1000, R.drawable.faces01),
    Transaction("Oliver", "Ava", 1500, R.drawable.faces02),
    Transaction("Martha", "Deibis", 1250, R.drawable.faces01),
    Transaction("John", "Alice", 750, R.drawable.faces02),
    Transaction("Emma", "Lucas", 500, R.drawable.faces03),
    Transaction("Sophia", "Ethan", 1000, R.drawable.faces01),
    Transaction("Oliver", "Ava", 1500, R.drawable.faces02)
)


@Composable
fun BottomButtonsSection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF273140), shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) // Redondeo en las esquinas superiores
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BottomButton(
            icon = Icons.Default.CreditCard,
            text = "Send",
            onClick = { /*TODO*/ }
        )
        BottomButton(
            icon = Icons.Default.Refresh,
            text = "Bank",
            onClick = { /*TODO*/ }
        )
        BottomButton(
            icon = Icons.Default.Close,
            text = "End",
            onClick = { /*TODO*/ }
        )
    }
}

@Composable
fun BottomButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 24.dp) // Aumenta el padding horizontal para más espacio a los lados
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.carisma500)),
                fontSize = 14.sp
            )
        }
    }
}














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
