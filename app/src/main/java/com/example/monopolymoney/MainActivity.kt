package com.example.monopolymoney

import EmailVerificationScreen
import LoginScreen
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
import com.example.monopolymoney.presentation.*
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

sealed class Route {
    object Auth : Route() { override fun toString() = "auth" }
    object EmailVerification : Route() { override fun toString() = "email_verification" }
    object ProfileSetup : Route() { override fun toString() = "profile_setup" }
    object Main : Route() { override fun toString() = "main" }
    object MoneyTransfer : Route() { override fun toString() = "money_transfer" }
    object BankTransfer : Route() { override fun toString() = "bank_transfer" }
    object Settings : Route() { override fun toString() = "settings" }
}

@Composable
fun MonopolyApp(navController: NavHostController, viewModel: DataViewModel) {
    val authState by viewModel.authState.collectAsState()
    val registrationState by viewModel.authViewModel.registrationState.collectAsState()
    val showGameOverScreen by viewModel.showGameOverScreen.collectAsState()

    when (authState) {
        is AuthViewModel.AuthState.Loading -> {
            LoadingScreen(message = "Verificando sesión...")
        }
        is AuthViewModel.AuthState.Authenticated -> {
            val startDestination = when (registrationState) {
                AuthViewModel.RegistrationState.EmailVerificationSent -> Route.EmailVerification.toString()
                AuthViewModel.RegistrationState.NeedsProfile -> Route.ProfileSetup.toString()
                AuthViewModel.RegistrationState.Complete -> Route.Main.toString()
                else -> Route.Main.toString()
            }

            NavHost(navController = navController, startDestination = startDestination) {
                composable(Route.Auth.toString()) {
                    LoginScreen(
                        viewModel = viewModel.authViewModel,
                        onForgotPassword = { /* Implement if needed */ }
                    )
                }

                composable(Route.EmailVerification.toString()) {
                    EmailVerificationScreen(
                        viewModel = viewModel.authViewModel,
                        email = (viewModel.authViewModel.user.collectAsState().value?.email ?: ""),
                        onBackToLogin = {
                            viewModel.authViewModel.signOut()
                            navController.navigate(Route.Auth.toString()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Route.ProfileSetup.toString()) {
                    ProfileSetupScreen(viewModel = viewModel.authViewModel)
                }

                composable(Route.Main.toString()) {
                    if (showGameOverScreen) {
                        GameEndScreen(
                            viewModel = viewModel,
                            onNavigateToHome = {
                                viewModel.leaveGameOverScreen()
                            }
                        )
                    } else {
                        LobbyScreen(
                            viewModel = viewModel,
                            onNavigateToMoneyTransfer = { navController.navigate(Route.MoneyTransfer.toString()) },
                            onNavigateToBankTransfer = { navController.navigate(Route.BankTransfer.toString()) },
                            onNavigateToSettings = { navController.navigate(Route.Settings.toString()) },
                            onNavigateToHome = {
                                viewModel.leaveGameOverScreen()
                            }
                        )
                    }
                }

                composable(Route.MoneyTransfer.toString()) {
                    val players by viewModel.players.collectAsState()
                    val currentPlayer by viewModel.currentPlayer.collectAsState()
                    val roomCode by viewModel.roomCode.collectAsState()
                    val hostId by viewModel.hostId.collectAsState()
                    val user by viewModel.user.collectAsState()
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

                composable(Route.BankTransfer.toString()) {
                    val players by viewModel.players.collectAsState()
                    val currentPlayer by viewModel.currentPlayer.collectAsState()
                    val roomCode by viewModel.roomCode.collectAsState()
                    val hostId by viewModel.hostId.collectAsState()
                    val user by viewModel.user.collectAsState()
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

                composable(Route.Settings.toString()) {
                    SettingsScreen(
                        viewModel = viewModel.authViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
        is AuthViewModel.AuthState.Unauthenticated -> {
            NavHost(navController = navController, startDestination = Route.Auth.toString()) {
                composable(Route.Auth.toString()) {
                    LoginScreen(
                        viewModel = viewModel.authViewModel,
                        onForgotPassword = { /* Implement if needed */ }
                    )
                }
            }
        }
        is AuthViewModel.AuthState.Error -> {
            LaunchedEffect(Unit) {
                navController.navigate(Route.Auth.toString()) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        AuthViewModel.AuthState.ResetEmailSent -> TODO()
    }
}