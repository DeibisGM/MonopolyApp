package com.example.monopolymoney.presentation

import MonopolyViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.monopolymoney.data.Player
import com.example.monopolymoney.data.Transaction
import com.example.monopolymoney.data.TransactionDetails

private val CardCornerRadius = 16.dp
private val ButtonHeight = 50.dp



@Composable
fun PlayerItem(player: Player) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(CardCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            player.name?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text(
                text = "$${player.balance}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun BalanceCard(player: Player) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Primer recuadro con borde y fondo transparente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .clip(RoundedCornerShape(16.dp)) // Forma redondeada
                .border(1.dp, Color(0xFFD1D4F6), RoundedCornerShape(16.dp)) // Grosor y color del borde
                .background(Color.Transparent) // Fondo transparente
        ) {
            // Contenido del cuadro
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize() //
            ) {
                Spacer(modifier = Modifier.weight(1f)) // Espacio ajustado para centrar el texto
            }
        }

        // Caja para el texto con fondo
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter) // Alinear en la parte superior central
                .offset(y = (-16).dp) // Ajustar el offset para centrar el texto en la parte superior
                .background(Color(0xFF121217))
                .padding(horizontal = 16.dp) // Espaciado alrededor del texto
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically // Alinear verticalmente el texto
            ) {
                Text(
                    text = "TU BALANCE ",
                    color = Color.White,
                    fontWeight = FontWeight.Normal, // Texto en normal
                    fontSize = 24.sp
                )
                Text(
                    text = "$${player.balance}", // Usar el balance del jugador
                    color = Color.White,
                    fontWeight = FontWeight.Bold, // Número en negrita
                    fontSize = 24.sp
                )
            }
        }

        // Segundo recuadro sin borde
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp)) // Forma redondeada
                .background(Color(0xFFD1D4F6))
                .align(Alignment.BottomCenter) // Alinear el segundo recuadro en la parte inferior
        )
    }
}


@Composable
fun AlignedIconsWithText(
    centerText: String,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp), // Padding de 16 dp a los lados
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Espacio entre los íconos
    ) {
        Icon(
            Icons.Filled.Home,
            contentDescription = "Home",
            modifier = Modifier
                .size(26.dp)
                .clickable { onLeftClick() } // Click en el ícono de la izquierda
        )

        Text(
            text = centerText,
            modifier = Modifier
                .clickable { /* Acción al hacer clic en el texto */ }
                .padding(horizontal = 8.dp), // Padding alrededor del texto
            style = MaterialTheme.typography.bodyMedium, fontSize = 16.sp
        )

        Icon(
            Icons.Filled.Favorite,
            contentDescription = "Favorite",
            modifier = Modifier
                .size(26.dp)
                .clickable { onRightClick() } // Click en el ícono de la derecha
        )
    }
}



@Composable
fun TransactionList(
    players: List<Player>,
    transactions: List<Transaction>
) {
    Column() {
        Text(
            text = "Transacciones",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(8.dp).padding(start = 24.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
        ) {
            items(transactions) { transaction ->
                TransactionItem(transaction = transaction, players = players)
            }
        }
    }}

@Composable
fun TransactionButtons(
    onSendClick: () -> Unit,
    onMiddleClick: () -> Unit = {}, // Default to empty lambda if not needed
    onCloseClick: () -> Unit,
    showMiddleButton: Boolean = true,
    isSendEnabled: Boolean = true,
    isMiddleEnabled: Boolean = true,
    isCloseEnabled: Boolean = true,
    modifier: Modifier = Modifier // Permitir modificar el layout desde afuera
) {
    // Definir colores
    val backgroundColor = Color(0xFF121217)
    val buttonColorDark = Color(0xFF29292E)
    val buttonColorAccent = Color(0xFFD1D4F6)
    val strokeColor = Color.Gray
    val strokeWidth = 0.5.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransactionButton(
                onClick = onSendClick,
                text = "Enviar dinero",
                icon = Icons.Default.ArrowForward,
                containerColor = buttonColorDark,
                modifier = Modifier.weight(1f),
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                enabled = isSendEnabled,
                iconPosition = IconPosition.END
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (showMiddleButton) {
                TransactionButton(
                    onClick = onMiddleClick,
                    icon = Icons.Default.AccountBalance,
                    containerColor = buttonColorDark,
                    modifier = Modifier.size(48.dp),
                    strokeColor = strokeColor,
                    strokeWidth = strokeWidth,
                    enabled = isMiddleEnabled
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            TransactionButton(
                onClick = onCloseClick,
                icon = Icons.Default.Close,
                containerColor = buttonColorAccent,
                modifier = Modifier.size(48.dp),
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                enabled = isCloseEnabled
            )
        }
    }
}

enum class IconPosition { START, END }

@Composable
fun TransactionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier,
    text: String? = null,
    strokeColor: Color = Color.Transparent,
    strokeWidth: Dp = 0.dp,
    enabled: Boolean = true,
    iconPosition: IconPosition = IconPosition.START
) {
    val contentColor = Color.White
    val buttonAlpha = if (enabled) 1f else 0.3f

    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .alpha(buttonAlpha)
            .border(
                width = strokeWidth,
                color = strokeColor,
                shape = RoundedCornerShape(16.dp)
            )
            ,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (iconPosition == IconPosition.START) {
                Icon(
                    imageVector = icon,
                    contentDescription = text ?: "Button action",
                    modifier = Modifier.size(24.dp)
                )
            }
            text?.let {
                Text(text)
            }
            if (iconPosition == IconPosition.END) {
                Icon(
                    imageVector = icon,
                    contentDescription = text ?: "Button action",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, players: List<Player>) {
    val (fromName, toName, amountText) = transactionDetails(transaction, players)

    // Main card containing the transaction details
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(width = 0.7.dp, color = Color(0xFF3F3F43), shape = RoundedCornerShape(12.dp)), // Borde gris
        shape = RoundedCornerShape(12.dp), // Rounded corners like the image
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2E)) // Color de fondo
    ) {
        // Row containing both the transaction info and amount
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info about transaction (names and arrow)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon depending on the transaction type
                Icon(
                    if (transaction.toPlayer != "-1") Icons.Default.Home else Icons.Default.ArrowUpward, // Adjust icon for home
                    contentDescription = null,
                    modifier = Modifier.size(24.dp), // Adjust size as needed
                    tint = MaterialTheme.colorScheme.onSurfaceVariant // Icon tint as per the image color
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Text showing "from → to" players
                Text(
                    text = "$fromName → $toName",
                    style = TextStyle(
                        fontSize = 16.sp, // Especifica el tamaño aquí
                        color = Color(0xFFE3E3E3) // Especifica el color aquí
                    )
                )
            }

            // Amount text styled to look bold and prominent
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // Set the color as seen in the image
                )
            )
        }
    }
}

