import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.monopolymoney.ui.theme.Shapes
import com.example.monopolymoney.ui.theme.Typographys

// Define your color palette for light and dark themes
private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFFBB86FC), // Replace with your colors
    secondary = Color(0xFF03DAC6),
)

private val LightColorPalette = lightColorScheme(
    primary = Color(0xFF6200EE), // Replace with your colors
    secondary = Color(0xFF03DAC6)
)

@Composable
fun MiAplicacionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors, // Use colorScheme instead of colors
        typography = Typographys,
        shapes = Shapes,
        content = content
    )
}
