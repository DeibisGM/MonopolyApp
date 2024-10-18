package com.example.monopolymoney

import CreateAccountScreen
import EmailVerificationScreen
import LoginScreen
import ProfileSetupScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.monopolymoney.presentation.ErrorDialog
import com.example.monopolymoney.presentation.GameEndScreen
import com.example.monopolymoney.presentation.LoadingScreen
import com.example.monopolymoney.presentation.LobbyScreen
import com.example.monopolymoney.presentation.MoneyTransferScreen
import com.example.monopolymoney.presentation.ResetEmailSentDialog
import com.example.monopolymoney.presentation.SettingsScreen
import com.example.monopolymoney.ui.theme.MonopolyMoneyTheme
import com.example.monopolymoney.ui.theme.MyColors
import com.example.monopolymoney.viewmodel.AuthViewModel
import com.example.monopolymoney.viewmodel.DataViewModel


class MainActivity : ComponentActivity() {
    private lateinit var viewModel: DataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        viewModel = ViewModelProvider(this, DataViewModel.Factory(application))[DataViewModel::class.java]

        setContent {
            MonopolyMoneyTheme {
                Box(modifier = Modifier.fillMaxSize().background(MyColors.background)) {
                    val navController = rememberNavController()
                    LoadingScreen()
                    //MonopolyApp(navController, viewModel)
                }
            }
        }

    }
}


sealed class Route {
    object Auth : Route() {
        override fun toString() = "auth"
    }

    object EmailVerification : Route() {
        override fun toString() = "email_verification"
    }

    object ProfileSetup : Route() {
        override fun toString() = "profile_setup"
    }

    object Main : Route() {
        override fun toString() = "main"
    }

    object MoneyTransfer : Route() {
        override fun toString() = "money_transfer"
    }

    object BankTransfer : Route() {
        override fun toString() = "bank_transfer"
    }

    object Settings : Route() {
        override fun toString() = "settings"
    }

    object CreateAccount : Route() {
        override fun toString() = "create_account"
    }
}

@Composable
fun MonopolyApp(navController: NavHostController, viewModel: DataViewModel) {
    val authState by viewModel.authState.collectAsState()
    val registrationState by viewModel.authViewModel.registrationState.collectAsState()
    val showGameOverScreen by viewModel.showGameOverScreen.collectAsState()

    when (authState) {
        is AuthViewModel.AuthState.Loading -> LoadingScreen(message = "Verificando sesión...")

        is AuthViewModel.AuthState.Authenticated -> {
            HandleAuthenticatedState(
                navController = navController,
                viewModel = viewModel,
                registrationState = registrationState,
                showGameOverScreen = showGameOverScreen
            )
        }

        is AuthViewModel.AuthState.Unauthenticated -> {
            UnauthenticatedNavHost(navController = navController, viewModel = viewModel.authViewModel)
        }

        is AuthViewModel.AuthState.Error -> {
            val errorMessage = (authState as AuthViewModel.AuthState.Error).message
            ErrorDialog(
                errorMessage = errorMessage,
                onDismiss = { viewModel.authViewModel.resetAuthState() }
            )
        }

        AuthViewModel.AuthState.ResetEmailSent -> {
            ResetEmailSentDialog(
                onDismiss = { viewModel.authViewModel.resetAuthState() }
            )
        }
    }
    }


@Composable
fun HandleAuthenticatedState(
    navController: NavHostController,
    viewModel: DataViewModel,
    registrationState: AuthViewModel.RegistrationState,
    showGameOverScreen: Boolean
) {
    when (registrationState) {
        AuthViewModel.RegistrationState.EmailVerificationSent -> {
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

        AuthViewModel.RegistrationState.NeedsProfile -> {
            ProfileSetupScreen(viewModel = viewModel.authViewModel)
        }

        AuthViewModel.RegistrationState.Complete -> {
            if (showGameOverScreen) {
                GameEndScreen(
                    viewModel = viewModel,
                    onNavigateToHome = { viewModel.leaveGameOverScreen() }
                )
            } else {
                MainNavHost(navController = navController, viewModel = viewModel)
            }
        }

        else -> {
            navController.navigate(Route.Auth.toString()) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}

@Composable
fun UnauthenticatedNavHost(navController: NavHostController, viewModel: AuthViewModel) {
    NavHost(navController = navController, startDestination = Route.Auth.toString()) {
        composable(Route.Auth.toString()) {
            LoginScreen(
                viewModel = viewModel,
                onCreateAccount = { navController.navigate(Route.CreateAccount.toString()) }
            )
        }

        composable(Route.CreateAccount.toString()) {
            CreateAccountScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainNavHost(navController: NavHostController, viewModel: DataViewModel) {
    NavHost(navController = navController, startDestination = Route.Main.toString()) {
        composable(Route.Main.toString()) {
            LobbyScreen(
                viewModel = viewModel,
                onNavigateToMoneyTransfer = { navController.navigate(Route.MoneyTransfer.toString()) },
                onNavigateToBankTransfer = { navController.navigate(Route.BankTransfer.toString()) },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.toString()) {
                        popUpTo(Route.Main.toString()) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToHome = { viewModel.leaveGameOverScreen() }
            )
        }

        composable(Route.MoneyTransfer.toString()) {
            MoneyTransferScreenWrapper(navController, viewModel, isBankMode = false)
        }

        composable(Route.BankTransfer.toString()) {
            MoneyTransferScreenWrapper(navController, viewModel, isBankMode = true)
        }

        composable(Route.Settings.toString()) {
            SettingsScreen(
                viewModel = viewModel.authViewModel,
                onNavigateBack = {
                    navController.popBackStack(Route.Main.toString(), inclusive = false)
                }
            )
        }
    }
}

@Composable
fun MoneyTransferScreenWrapper(navController: NavHostController, viewModel: DataViewModel, isBankMode: Boolean) {
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
                if (isBankMode) {
                    viewModel.makeBankTransaction(toPlayerId, amount)
                } else {
                    viewModel.makeTransaction(userId, toPlayerId, amount)
                }
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() },
            isBankMode = isBankMode,
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