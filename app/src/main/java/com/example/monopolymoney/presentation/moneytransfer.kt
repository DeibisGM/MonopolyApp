package com.example.monopolymoney.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monopolymoney.data.Player

private val ButtonShape = RoundedCornerShape(12.dp)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF57D55B),
    secondary = Color(0xFF363636),
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF212121),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightGrey = Color(0xFFB0B0B0)
private val green = Color(0xFF57D55B)

@Composable
fun MoneyTransferScreen(
    players: List<Player>,
    myPlayerId: String,
    onTransactionComplete: (amount: Int, toPlayerId: String) -> Unit,
    onCancel: () -> Unit,
    isBankMode: Boolean = false,
    isHost: Boolean = false,
    isMyTurn: Boolean = false
) {
    var amount by remember { mutableStateOf("0") }
    var isInitialValue by remember { mutableStateOf(true) }
    var selectedRecipient by remember { mutableStateOf<Player?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Find the current player object
    val currentPlayerObject = players.find { it.id == myPlayerId }

    // Determine if the transaction should be allowed
    val canMakeTransaction = when {
        isBankMode -> isHost // Host can always use bank
        else -> isMyTurn // Regular transfers only on your turn
    }

    // Filter recipients based on mode and permissions
    val availableRecipients = when {
        isBankMode -> players // Bank can transfer to anyone, including self
        else -> players.filter { it.id != myPlayerId } // Can't transfer to self in normal mode
    }

    MaterialTheme(colorScheme = DarkColorScheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TopBar(
                        onCancel = onCancel,
                        isBankMode = isBankMode,
                        //isEnabled = canMakeTransaction
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RecipientSelector(
                        selectedRecipient = selectedRecipient,
                        players = availableRecipients,
                        enabled = canMakeTransaction,
                        onRecipientSelected = { selectedRecipient = it }
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    AmountDisplay(amount)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isBankMode) {
                            Text(
                                "Bank Balance: Unlimited",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            currentPlayerObject?.let {
                                PaymentSourceCard(it.balance)
                            }
                        }

                        NumberPad(
                            onNumberClick = { digit ->
                                if (isInitialValue) {
                                    amount = ""
                                    isInitialValue = false
                                }
                                if (amount.length < 6) amount += digit
                            },
                            onDeleteClick = {
                                if (amount.isNotEmpty()) {
                                    amount = amount.dropLast(1)
                                }
                                if (amount.isEmpty()) {
                                    amount = "0"
                                    isInitialValue = true
                                }
                            },
                            //enabled = canMakeTransaction
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SendButton(
                            enabled = canMakeTransaction && selectedRecipient != null && amount.toIntOrNull() ?: 0 > 0,
                            onSendMoney = {
                                handleTransaction(
                                    currentPlayerObject,
                                    selectedRecipient,
                                    amount.toIntOrNull() ?: 0,
                                    onTransactionComplete,
                                    isBankMode
                                ) { error ->
                                    errorMessage = error
                                    showErrorDialog = true
                                }
                            },
                            isBankMode = isBankMode
                        )

                        if (!canMakeTransaction) {
                            Text(
                                text = if (isBankMode) "Only the host can make bank transfers"
                                else "You can only make transfers during your turn",
                                color = Color.Red,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (showErrorDialog) {
                            AlertDialog(
                                onDismissRequest = { showErrorDialog = false },
                                title = { Text("Transaction Failed") },
                                text = { Text(errorMessage) },
                                confirmButton = {
                                    Button(onClick = { showErrorDialog = false }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipientSelector(
    selectedRecipient: Player?,
    players: List<Player>,
    enabled: Boolean,
    onRecipientSelected: (Player) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { if (enabled) expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (enabled) Color(0xFF363636) else Color(0xFF262626),
                contentColor = if (enabled) Color.White else Color.Gray
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = enabled
        ) {
            Text(
                selectedRecipient?.name ?: "Select Recipient",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontSize = 18.sp
            )
//            Icon(
//                imageVector = Icons.Filled.ArrowDropDown,
//                contentDescription = "Dropdown Arrow"
//            )
        }

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF363636))
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { player.name?.let { Text(it, color = Color.White) } },
                    onClick = {
                        onRecipientSelected(player)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = Color.White
                    )
                )
            }
        }
    }
}

private fun handleTransaction(
    currentPlayer: Player?,
    selectedRecipient: Player?,
    amount: Int,
    onTransactionComplete: (amount: Int, toPlayerId: String) -> Unit,
    isBankMode: Boolean,
    onError: (String) -> Unit
) {
    if (selectedRecipient == null) {
        onError("Please select a recipient.")
        return
    }

    if (amount <= 0) {
        onError("Please enter a valid amount.")
        return
    }

    if (!isBankMode) {
        if (currentPlayer == null) {
            onError("Current player not found.")
            return
        }

        if (amount > currentPlayer.balance) {
            onError("Insufficient funds. Your balance is $${currentPlayer.balance}.")
            return
        }
    }

    selectedRecipient.id?.let { onTransactionComplete(amount, it) }
}

@Composable
fun TopBar(onCancel: () -> Unit, isBankMode: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onCancel) {
            Text("<", fontSize = 24.sp, color = Color.White)
        }
        Text(
            if (isBankMode) "Bank Transfer" else "Send Money",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        IconButton(onClick = { /* Handle info click */ }) {
            Text("?", fontSize = 24.sp, color = Color.White)
        }
    }
}

@Composable
fun RecipientSelector(selectedRecipient: Player?, players: List<Player>, onRecipientSelected: (Player) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF363636),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                selectedRecipient?.name ?: "Select Recipient",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontSize = 18.sp
            )
//            Icon(
//                imageVector = Icons.Filled.ArrowDropDown,
//                contentDescription = "Dropdown Arrow"
//            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF363636))
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { player.name?.let { Text(it, color = Color.White) } },
                    onClick = {
                        onRecipientSelected(player)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = Color.White
                    )
                )
            }
        }
    }
}


@Composable
fun AmountDisplay(amount: String) {
    Text(
        "$$amount",
        fontSize = 60.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        textAlign = TextAlign.Center,
        color = Color.White
    )
}

// Muestra el balance del jugador actual
@Composable
fun PaymentSourceCard(balance: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Balance: $$balance",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun NumberPad(onNumberClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "⌫")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        numbers.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { number ->
                    Button(
                        onClick = {
                            if (number == "⌫") onDeleteClick()
                            else onNumberClick(number)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp)
                            .height(55.dp), // Increased height
                        shape = ButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(number, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SendButton(enabled: Boolean, onSendMoney: () -> Unit, isBankMode: Boolean) {
    Button(
        onClick = onSendMoney,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) green else MaterialTheme.colorScheme.secondary,
            contentColor = Color.White
        )
    ) {
        Text(
            if (isBankMode) "Transfer from Bank" else "Send Money",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}