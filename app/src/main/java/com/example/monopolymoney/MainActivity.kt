package com.example.monopolymoney

import LoginScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.monopolymoney.presentation.LobbyScreen
import com.example.monopolymoney.presentation.MoneyTransferScreen
import com.example.monopolymoney.ui.theme.Shapes
import com.example.monopolymoney.ui.theme.Typographys
import com.example.monopolymoney.viewmodel.AuthViewModel
import com.example.monopolymoney.viewmodel.DataViewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: DataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.analytics

        // Initialize DataViewModel using its Factory
        viewModel = ViewModelProvider(
            this,
            DataViewModel.Factory(application)
        )[DataViewModel::class.java]

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(),
                typography = Typographys,
                shapes = Shapes
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MonopolyApp(navController, viewModel)
                }
            }
        }
    }
}

@Composable
fun MonopolyApp(navController: NavHostController, viewModel: DataViewModel) {
    val authState by viewModel.authState.collectAsState()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            LobbyScreen(
                viewModel = viewModel,
                onNavigateToMoneyTransfer = { navController.navigate("moneyTransfer") },
                onNavigateToBankTransfer = { navController.navigate("bankTransfer") }
            )
        }

        composable("moneyTransfer") {
            val players by viewModel.players.collectAsState()
            val currentPlayer by viewModel.currentPlayer.collectAsState()
            val roomCode by viewModel.roomCode.collectAsState()
            val hostId by viewModel.hostId.collectAsState()
            val userId by viewModel.userId.collectAsState()

            currentPlayer?.let { playerId ->
                roomCode?.let { code ->
                    MoneyTransferScreen(
                        players = players,
                        myPlayerId = userId ?: "",
                        onTransactionComplete = { amount, toPlayerId ->
                            viewModel.makeTransaction(code, playerId, toPlayerId, amount)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() },
                        isBankMode = false,
                        isHost = userId == hostId,
                        isMyTurn = userId == currentPlayer
                    )
                }
            } ?: Text("Error: Room or player information not found")
        }

        composable("bankTransfer") {
            val players by viewModel.players.collectAsState()
            val currentPlayer by viewModel.currentPlayer.collectAsState()
            val roomCode by viewModel.roomCode.collectAsState()
            val hostId by viewModel.hostId.collectAsState()
            val userId by viewModel.userId.collectAsState()

            currentPlayer?.let { playerId ->
                roomCode?.let { code ->
                    MoneyTransferScreen(
                        players = players,
                        myPlayerId = userId ?: "",
                        onTransactionComplete = { amount, toPlayerId ->
                            viewModel.makeBankTransaction(code, toPlayerId, amount)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() },
                        isBankMode = true,
                        isHost = userId == hostId,
                        isMyTurn = userId == currentPlayer
                    )
                }
            } ?: Text("Error: Room or player information not found")
        }
    }

    // Handle navigation based on auth state
    when (authState) {
        is AuthViewModel.AuthState.Authenticated -> {
            if (viewModel.isNameSet.collectAsState().value) {
                if (navController.currentBackStackEntry?.destination?.route != "main") {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            }
        }
        is AuthViewModel.AuthState.Unauthenticated -> {
            if (navController.currentBackStackEntry?.destination?.route != "auth") {
                navController.navigate("auth") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        }
        else -> { /* Handle other states */ }
    }
}