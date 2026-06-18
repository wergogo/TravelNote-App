package com.wergogo.travelnote.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.R
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.ui.components.TnTextButton
import com.wergogo.travelnote.ui.theme.TravelnoteTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: LoginViewModel = viewModel { LoginViewModel(app.container.authRepository) }
    val coroutineScope = rememberCoroutineScope()

    LoginContent(
        email = vm.email,
        onEmailChange = vm::onEmailChange,
        password = vm.password,
        onPasswordChange = vm::onPasswordChange,
        errorMessage = vm.errorMessage,
        isLoading = vm.isLoading,
        onSignIn = {
            coroutineScope.launch {
                if (vm.login()) onLoginSuccess()
            }
        },
        onNavigateToRegister = onNavigateToRegister
    )
}

@Composable
internal fun LoginContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onSignIn: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = "TravelNote Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(Modifier.height(8.dp))

            // App name
            Text(
                text = "TravelNote",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(16.dp))

            PrimaryButton(
                text = "Sign In",
                enabled = !isLoading,
                onClick = onSignIn
            )

            Spacer(Modifier.height(8.dp))

            TnTextButton(
                text = "Don't have an account? Register",
                onClick = onNavigateToRegister
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    TravelnoteTheme {
        LoginContent(
            email = "user@example.com",
            onEmailChange = {},
            password = "password123",
            onPasswordChange = {},
            errorMessage = null,
            isLoading = false,
            onSignIn = {},
            onNavigateToRegister = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    TravelnoteTheme {
        LoginContent(
            email = "user@example.com",
            onEmailChange = {},
            password = "wrong",
            onPasswordChange = {},
            errorMessage = "Invalid email or password",
            isLoading = false,
            onSignIn = {},
            onNavigateToRegister = {}
        )
    }
}
