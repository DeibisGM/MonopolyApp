package com.example.monopolymoney.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.monopolymoney.viewmodel.DataViewModel

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
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onLeaveGame,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Text("Leave Game")
                    }
                    Button(
                        onClick = onEndGame,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("End Game for All")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Yes, Leave Game")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}

fun handleAction(context: android.content.Context, condition: Boolean, action: () -> Unit, message: String = "It's not your turn yet!") {
    if (condition) action()
    else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}