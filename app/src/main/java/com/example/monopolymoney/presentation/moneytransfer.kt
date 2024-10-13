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

@Composable
fun MoneyTransferScreen(
    players: Map<String, Player>,
    myPlayerId: String,
    onTransactionComplete: (amount: Int, toPlayerId: String) -> Unit,
    onCancel: () -> Unit,
    isBankMode: Boolean = false,
    isHost: Boolean = false,
    isMyTurn: Boolean = false
) {
    var amount by remember { mutableStateOf("0") }
    var selectedRecipient by remember { mutableStateOf<Player?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val currentPlayerObject = players[myPlayerId]
    val canMakeTransaction = if (isBankMode) isHost else isMyTurn
    val availableRecipients = if (isBankMode) players.values.toList() else players.values.filter { it.id != myPlayerId }

    MaterialTheme(colorScheme = DarkColorScheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TransactionCard(
                amount = amount,
                selectedRecipient = selectedRecipient,
                players = availableRecipients,
                canMakeTransaction = canMakeTransaction,
                onRecipientSelected = { selectedRecipient = it },
                onCancel = onCancel
            )

            BottomSection(
                currentPlayerObject = currentPlayerObject,
                isBankMode = isBankMode,
                amount = amount,
                onAmountChange = { amount = it },
                canMakeTransaction = canMakeTransaction,
                selectedRecipient = selectedRecipient,
                onTransactionComplete = onTransactionComplete,
                showErrorDialog = showErrorDialog,
                errorMessage = errorMessage,
                onErrorDismiss = { showErrorDialog = false },
                onTransactionError = { error ->
                    errorMessage = error
                    showErrorDialog = true
                }
            )
        }
    }
}

@Composable
fun BottomSection(
    currentPlayerObject: Player?,
    isBankMode: Boolean,
    amount: String,
    onAmountChange: (String) -> Unit,
    canMakeTransaction: Boolean,
    selectedRecipient: Player?,
    onTransactionComplete: (Int, String) -> Unit,
    showErrorDialog: Boolean,
    errorMessage: String,
    onErrorDismiss: () -> Unit,
    onTransactionError: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DisplayBalance(isBankMode, currentPlayerObject)
            NumberPad(
                onNumberClick = { digit ->
                    val newAmount = if (amount == "0") digit else amount + digit
                    onAmountChange(newAmount)
                },
                onDeleteClick = {
                    val newAmount = amount.dropLast(1)
                    onAmountChange(newAmount.ifEmpty { "0" })
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            SendButton(
                enabled = canMakeTransaction && selectedRecipient != null && (amount.toIntOrNull()
                    ?: 0) > 0,
                onSendMoney = {
                    selectedRecipient?.id?.let { recipientId ->
                        onTransactionComplete(amount.toInt(), recipientId)
                    } ?: onTransactionError("Invalid recipient")
                },
                isBankMode = isBankMode
            )

            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = onErrorDismiss,
                    title = { Text("Transaction Failed") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        Button(onClick = onErrorDismiss) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    amount: String,
    selectedRecipient: Player?,
    players: List<Player>,
    canMakeTransaction: Boolean,
    onRecipientSelected: (Player) -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            TopBar(onCancel = onCancel, isBankMode = false)
            Spacer(modifier = Modifier.height(16.dp))
            RecipientSelector(
                selectedRecipient = selectedRecipient,
                players = players,
                enabled = canMakeTransaction,
                onRecipientSelected = onRecipientSelected
            )
            Spacer(modifier = Modifier.height(25.dp))
            AmountDisplay(amount)
        }
    }
}

@Composable
fun DisplayBalance(isBankMode: Boolean, currentPlayerObject: Player?) {
    Text(
        text = if (isBankMode) "Bank Balance: Unlimited" else "Balance: $${currentPlayerObject?.balance ?: 0}",
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientSelector(
    selectedRecipient: Player?,
    players: List<Player>,
    enabled: Boolean,
    onRecipientSelected: (Player) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedRecipient?.name ?: "Select Recipient",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                disabledTextColor = if (enabled) Color.White else Color.Gray
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { Text(player.name ?: "", color = Color.White) },
                    onClick = {
                        onRecipientSelected(player)
                        expanded = false
                    }
                )
            }
        }
    }
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

@Composable
fun NumberPad(onNumberClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "⌫")
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        numbers.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { number ->
                    Button(
                        onClick = {
                            if (number == "⌫") onDeleteClick() else onNumberClick(number)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp)
                            .height(55.dp),
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
            containerColor = if (enabled) Color(0xFF57D55B) else MaterialTheme.colorScheme.secondary,
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