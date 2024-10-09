package com.example.monopolymoney

import AuthScreen
import ProfileSetupScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.monopolymoney.presentation.SettingsScreen
import com.example.monopolymoney.ui.theme.Shapes
import com.example.monopolymoney.ui.theme.Typographys
import com.example.monopolymoney.viewmodel.AuthViewModel
import com.example.monopolymoney.viewmodel.DataViewModel
import com.google.firebase.analytics.FirebaseAnalytics

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: DataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAnalytics.getInstance(this)

        viewModel = ViewModelProvider(this, DataViewModel.Factory(application))[DataViewModel::class.java]

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
    val user by viewModel.user.collectAsState()
    val registrationState by viewModel.authViewModel.registrationState.collectAsState()

    LaunchedEffect(authState, registrationState) {
        when {
            authState is AuthViewModel.AuthState.Authenticated && registrationState == AuthViewModel.RegistrationState.NeedsProfile -> {
                if (navController.currentDestination?.route != "profile_setup") {
                    navController.navigate("profile_setup") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            }
            authState is AuthViewModel.AuthState.Authenticated && registrationState == AuthViewModel.RegistrationState.Complete -> {
                if (navController.currentDestination?.route != "main") {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            }
            authState is AuthViewModel.AuthState.Unauthenticated -> {
                if (navController.currentDestination?.route != "auth") {
                    navController.navigate("auth") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(viewModel = viewModel.authViewModel)
        }

        composable("profile_setup") {
            ProfileSetupScreen(viewModel = viewModel.authViewModel)
        }

        composable("main") {
            val players by viewModel.players.collectAsState()
            val currentPlayer by viewModel.currentPlayer.collectAsState()
            val roomCode by viewModel.roomCode.collectAsState()
            val hostId by viewModel.hostId.collectAsState()

            if (user != null) {
                LobbyScreen(
                    viewModel = viewModel,
                    onNavigateToMoneyTransfer = { navController.navigate("moneyTransfer") },
                    onNavigateToBankTransfer = { navController.navigate("bankTransfer") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("auth") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }

        composable("moneyTransfer") {
            val players by viewModel.players.collectAsState()
            val currentPlayer by viewModel.currentPlayer.collectAsState()
            val roomCode by viewModel.roomCode.collectAsState()
            val hostId by viewModel.hostId.collectAsState()
            val userId = user?.uuid

            if (currentPlayer != null && roomCode != null && userId != null) {
                MoneyTransferScreen(
                    players = players,
                    myPlayerId = userId,
                    onTransactionComplete = { amount, toPlayerId ->
                        viewModel.makeTransaction(userId, toPlayerId, amount)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                    isBankMode = false,
                    isHost = userId == hostId,
                    isMyTurn = userId == currentPlayer
                )
            } else {
                Text("Error: Room or player information not found")
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel.authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("bankTransfer") {
            val players by viewModel.players.collectAsState()
            val currentPlayer by viewModel.currentPlayer.collectAsState()
            val roomCode by viewModel.roomCode.collectAsState()
            val hostId by viewModel.hostId.collectAsState()
            val userId = user?.uuid

            if (currentPlayer != null && roomCode != null && userId != null) {
                MoneyTransferScreen(
                    players = players,
                    myPlayerId = userId,
                    onTransactionComplete = { amount, toPlayerId ->
                        viewModel.makeBankTransaction(toPlayerId, amount)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                    isBankMode = true,
                    isHost = userId == hostId,
                    isMyTurn = userId == currentPlayer
                )
            } else {
                Text("Error: Room or player information not found")
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}
