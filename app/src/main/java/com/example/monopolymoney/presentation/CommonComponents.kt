package com.example.monopolymoney.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MyYellow = Color(0xFFFFD67E)
private val GeneralPadding = 16.dp

@Composable
fun LoadingScreen(message: String = "Cargando...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF151414)),
        contentAlignment = Alignment.Center
    ) {
        val colors = listOf(MyYellow, Color(0xFFFFC057), Color(0xFFFF9F43), Color(0xFFFF8534))
        val infiniteTransition = rememberInfiniteTransition(label = "")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = ""
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..1) {
                        val dotPosition = if (row == 0) col else 3 - col
                        val distance = (dotPosition - progress + 4) % 4
                        val index1 = distance.toInt()
                        val index2 = (index1 + 1) % colors.size
                        val interpolation = distance - index1

                        val color = lerp(colors[index1], colors[index2], interpolation)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
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

@Composable
fun UserCard(name: String, balance: String, avatarResId: Int) {
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