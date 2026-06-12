package com.wergogo.travelnote.ui.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.ui.components.TnTextButton
import com.wergogo.travelnote.ui.theme.TravelnoteTheme
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: RegisterViewModel = viewModel { RegisterViewModel(app.container.authRepository) }
    val coroutineScope = rememberCoroutineScope()

    RegisterContent(
        nickname = vm.nickname,
        onNicknameChange = vm::onNicknameChange,
        email = vm.email,
        onEmailChange = vm::onEmailChange,
        password = vm.password,
        onPasswordChange = vm::onPasswordChange,
        confirmPassword = vm.confirmPassword,
        onConfirmPasswordChange = vm::onConfirmPasswordChange,
        errorMessage = vm.errorMessage,
        isLoading = vm.isLoading,
        onCreateAccount = {
            coroutineScope.launch {
                if (vm.register()) onRegisterSuccess()
            }
        },
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
internal fun RegisterContent(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onCreateAccount: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppTopBar("Create Account") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            PrimaryButton(
                text = "Create Account",
                enabled = !isLoading,
                onClick = onCreateAccount
            )

            TnTextButton(
                text = "Already have an account? Sign In",
                onClick = onNavigateToLogin
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    TravelnoteTheme {
        RegisterContent(
            nickname = "John Doe",
            onNicknameChange = {},
            email = "john@example.com",
            onEmailChange = {},
            password = "password123",
            onPasswordChange = {},
            confirmPassword = "password123",
            onConfirmPasswordChange = {},
            errorMessage = null,
            isLoading = false,
            onCreateAccount = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenErrorPreview() {
    TravelnoteTheme {
        RegisterContent(
            nickname = "John Doe",
            onNicknameChange = {},
            email = "john@example.com",
            onEmailChange = {},
            password = "pass",
            onPasswordChange = {},
            confirmPassword = "different",
            onConfirmPasswordChange = {},
            errorMessage = "Passwords do not match",
            isLoading = false,
            onCreateAccount = {},
            onNavigateToLogin = {}
        )
    }
}
