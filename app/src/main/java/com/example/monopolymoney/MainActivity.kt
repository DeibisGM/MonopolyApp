package com.example.monopolymoney

import LoginScreen
import MonopolyViewModel
import MonopolyViewModelFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.monopolymoney.ui.theme.Shapes
import com.example.monopolymoney.ui.theme.Typographys
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.monopolymoney.presentation.LobbyScreen
import com.example.monopolymoney.presentation.MoneyTransferScreen
import com.example.monopolymoney.presentation.MyScreen
import com.example.monopolymoney.viewmodels.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MonopolyViewModel
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.analytics

        // Use ViewModelFactory to create MonopolyViewModel
        val factory = MonopolyViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[MonopolyViewModel::class.java]

        userViewModel.createUserIfNotExists()

        savedInstanceState?.let {
            viewModel.restoreState(it)
        }

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
                    //MonopolyApp(navController, viewModel)
                    MyScreen();
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
fun MonopolyApp(navController: NavHostController, viewModel: MonopolyViewModel) {
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
            val myPlayerId by viewModel.playerId.collectAsState()

            myPlayerId?.let { id ->
                MoneyTransferScreen(
                    players = players,
                    myPlayerId = id,
                    onTransactionComplete = { amount, toPlayerId ->
                        viewModel.makeTransaction(id, toPlayerId, amount)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                    isBankMode = false
                )
            } ?: run {
                Text("Error: Player ID not found")
            }
        }
        composable("bankTransfer") {
            val players by viewModel.players.collectAsState()
            val myPlayerId by viewModel.playerId.collectAsState()

            myPlayerId?.let { id ->
                MoneyTransferScreen(
                    players = players,
                    myPlayerId = id,
                    onTransactionComplete = { amount, toPlayerId ->
                        viewModel.makeBankTransaction(toPlayerId, amount)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                    isBankMode = true
                )
            } ?: run {
                Text("Error: Player ID not found")
            }
        }
    }

// Observar el estado de autenticación y navegar en consecuencia
    when (authState) {
        is AuthState.Authenticated -> {
            if (viewModel.getNameState()) {
                // Navegar a la pantalla principal solo si el nombre no está establecido
                if (navController.currentBackStackEntry?.destination?.route != "main") {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            } else {
                // Si el nombre ya está establecido, podrías navegar a otra pantalla si es necesario
                // Por ejemplo:
                // if (navController.currentBackStackEntry?.destination?.route != "home") {
                //     navController.navigate("home") {
                //         popUpTo("auth") { inclusive = true }
                //     }
                // }
            }
        }
        is AuthState.Unauthenticated -> {
            // Navegar a la pantalla de autenticación si no está autenticado
            if (navController.currentBackStackEntry?.destination?.route != "auth") {
                navController.navigate("auth") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        }
        else -> {
            // Manejar otros estados si es necesario
        }
    }
}