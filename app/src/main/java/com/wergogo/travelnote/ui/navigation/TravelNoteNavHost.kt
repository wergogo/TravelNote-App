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
import com.wergogo.travelnote.ui.register.RegisterScreen
import com.wergogo.travelnote.ui.profile.ProfileScreen
import com.wergogo.travelnote.ui.splash.SplashScreen
import com.wergogo.travelnote.ui.home.HomeScreen
import com.wergogo.travelnote.ui.createtrip.CreateTripScreen
import com.wergogo.travelnote.ui.jointrip.JoinTripScreen
import com.wergogo.travelnote.ui.tripoverview.TripOverviewScreen
import com.wergogo.travelnote.ui.edittrip.EditTripScreen
import com.wergogo.travelnote.ui.members.MembersScreen
import com.wergogo.travelnote.ui.daydetail.DayDetailScreen

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
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onTripClick = { tripId ->
                    navController.navigate(Screen.TripOverview.route(tripId))
                },
                onCreateTrip = { navController.navigate(Screen.CreateTrip.route) },
                onJoinTrip = { navController.navigate(Screen.JoinTrip.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.CreateTrip.route) {
            CreateTripScreen(
                onTripCreated = { tripId ->
                    navController.navigate(Screen.TripOverview.route(tripId)) {
                        popUpTo(Screen.CreateTrip.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.JoinTrip.route) {
            JoinTripScreen(
                onJoinSuccess = { tripId ->
                    navController.navigate(Screen.TripOverview.route(tripId)) {
                        popUpTo(Screen.JoinTrip.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TripOverview.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            TripOverviewScreen(
                tripId = tripId,
                onDayClick = { dateKey ->
                    navController.navigate(Screen.DayDetail.route(tripId, dateKey))
                },
                onEditTrip = { navController.navigate(Screen.EditTrip.route(tripId)) },
                onMembersClick = { navController.navigate(Screen.Members.route(tripId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditTrip.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            EditTripScreen(
                tripId = tripId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
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
            DayDetailScreen(
                tripId = tripId,
                dateKey = dateKey,
                onAddEvent = { navController.navigate(Screen.AddEvent.route(tripId, dateKey)) },
                onEditEvent = { eventId -> navController.navigate(Screen.AddEvent.route(tripId, dateKey, eventId)) },
                onAddExpense = { navController.navigate("expense/$tripId/$dateKey") },
                onAddNote = { navController.navigate("note/$tripId/$dateKey") },
                onBack = { navController.popBackStack() }
            )
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
            MembersScreen(
                tripId = tripId,
                onBack = { navController.popBackStack() },
                onTripLeft = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
