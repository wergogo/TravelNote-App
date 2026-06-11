package com.wergogo.travelnote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.AppContainer
import androidx.compose.ui.platform.LocalContext
import com.wergogo.travelnote.ui.login.LoginScreen
import com.wergogo.travelnote.ui.splash.SplashScreen

@Composable
fun TravelNoteNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val container: AppContainer = app.container

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen { dest ->
                navController.navigate(dest.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        composable(Screen.Register.route) {
            // RegisterScreen — implemented in Task 23
            androidx.compose.material3.Text("Register")
        }
        composable(Screen.Home.route) {
            // HomeScreen — implemented in Task 25
            androidx.compose.material3.Text("Home")
        }
        composable(Screen.Profile.route) {
            // ProfileScreen — implemented in Task 24
            androidx.compose.material3.Text("Profile")
        }
        composable(Screen.CreateTrip.route) {
            // CreateTripScreen — implemented in Task 26
            androidx.compose.material3.Text("CreateTrip")
        }
        composable(Screen.JoinTrip.route) {
            // JoinTripScreen — implemented in Task 27
            androidx.compose.material3.Text("JoinTrip")
        }
        composable(
            route = Screen.TripOverview.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            // TripOverviewScreen — implemented in Task 28
            androidx.compose.material3.Text("TripOverview: $tripId")
        }
        composable(
            route = Screen.EditTrip.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            // EditTripScreen — implemented in Task 29
            androidx.compose.material3.Text("EditTrip: $tripId")
        }
        composable(
            route = Screen.DayDetail.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("dateKey") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val dateKey = backStackEntry.arguments?.getString("dateKey") ?: return@composable
            // DayDetailScreen — implemented in Task 32
            androidx.compose.material3.Text("DayDetail: $tripId / $dateKey")
        }
        composable(
            route = Screen.AddEvent.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("dateKey") { type = NavType.StringType },
                navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val dateKey = backStackEntry.arguments?.getString("dateKey") ?: return@composable
            val eventId = backStackEntry.arguments?.getString("eventId")
            // AddEventScreen — implemented in Task 33
            androidx.compose.material3.Text("AddEvent: $tripId / $dateKey")
        }
        composable(
            route = Screen.Members.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            // MembersScreen — implemented in Task 30
            androidx.compose.material3.Text("Members: $tripId")
        }
    }
}
