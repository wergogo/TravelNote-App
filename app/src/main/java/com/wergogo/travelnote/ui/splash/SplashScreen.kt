package com.wergogo.travelnote.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.R
import com.wergogo.travelnote.TravelNoteApplication
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
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = "TravelNote Logo",
            modifier = Modifier.size(160.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "TravelNote",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    TravelnoteTheme {
        SplashContent()
    }
}
