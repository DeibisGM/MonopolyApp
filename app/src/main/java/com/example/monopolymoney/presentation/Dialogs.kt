package com.example.monopolymoney.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.monopolymoney.viewmodel.AuthViewModel

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

fun handleAction(
    context: android.content.Context,
    condition: Boolean,
    action: () -> Unit,
    message: String = "It's not your turn yet!"
) {
    if (condition) action()
    else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Aceptar")
            }
        },
        title = {
            Text("Error")
        },
        text = {
            Text(errorMessage)
        }
    )
}

@Composable
fun ResetEmailSentDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Aceptar")
            }
        },
        title = {
            Text("Correo enviado")
        },
        text = {
            Text("Se ha enviado un correo para restablecer la contraseña.")
        }
    )
}
@Composable
fun ForgotPasswordDialog(
    isLoggedIn: Boolean, // Nuevo parámetro
    onDismiss: () -> Unit,
    onSendResetEmail: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restablecer Contraseña", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                // Mensaje condicional basado en el valor de `isLoggedIn`
                Text(

                    text = if (isLoggedIn) {
                        "Se enviará un enlace a tu correo. Tu sesión se cerrará para que puedas iniciar sesión con la nueva contraseña."
                    } else {
                        "Por favor, ingresa tu correo electrónico para recibir un enlace de restablecimiento de contraseña."
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendResetEmail(email) },
                enabled = email.isNotBlank(),
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Enviar Enlace")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
