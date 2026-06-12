package com.wergogo.travelnote.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import androidx.compose.ui.tooling.preview.Preview
import com.wergogo.travelnote.ui.navigation.Screen
import com.wergogo.travelnote.ui.theme.TravelnoteTheme

@Composable
fun SplashScreen(onNavigate: (Screen) -> Unit) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: SplashViewModel = viewModel { SplashViewModel(app.container.authRepository) }

    val destination by vm.destination.collectAsState()

    LaunchedEffect(destination) {
        destination?.let { onNavigate(it) }
    }

    SplashContent()
}

@Composable
internal fun SplashContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    TravelnoteTheme {
        SplashContent()
    }
}
