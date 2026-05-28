package com.denis.padron.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.denis.padron.data.PadronViewModel

@Composable
fun AppNavigation() {
    val navController  = rememberNavController()
    val padronViewModel: PadronViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToTsje = { navController.navigate("tsje") },
                onNavigateToAnr  = { navController.navigate("anr")  },
                onNavigateToPlra = { navController.navigate("plra") }
            )
        }
        composable("tsje") {
            TsjeScreen(viewModel=padronViewModel, onBack={ navController.popBackStack() })
        }
        composable("anr") {
            AnrScreen(onBack={ navController.popBackStack() })
        }
        composable("plra") {
            PlraScreen(viewModel=padronViewModel, onBack={ navController.popBackStack() })
        }
    }
}
