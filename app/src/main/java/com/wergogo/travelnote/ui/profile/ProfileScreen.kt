package com.wergogo.travelnote.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.AvatarPicker
import com.wergogo.travelnote.ui.components.BottomNavTab
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.ui.components.SecondaryButton
import com.wergogo.travelnote.ui.components.TnTextButton
import com.wergogo.travelnote.ui.components.TravelNoteBottomNav
import com.wergogo.travelnote.ui.theme.TravelnoteTheme
import com.wergogo.travelnote.util.DefaultImages
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onChangePassword: () -> Unit,
    onTripsClick: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: ProfileViewModel = viewModel {
        ProfileViewModel(app.container.authRepository, app.container.profileRepository)
    }
    val coroutineScope = rememberCoroutineScope()
    val profile by vm.profile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAvatarPicker by remember { mutableStateOf(false) }

    // Show success message via Snackbar
    LaunchedEffect(vm.successMessage) {
        vm.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.successMessage = null
        }
    }

    if (showAvatarPicker) {
        AvatarPicker(
            selected = profile?.avatarId ?: "avatar_01",
            onSelect = { key ->
                showAvatarPicker = false
                coroutineScope.launch { vm.changeAvatar(key) }
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    ProfileContent(
        avatarId = profile?.avatarId ?: "avatar_01",
        editNickname = vm.editNickname,
        onNicknameChange = { vm.editNickname = it },
        errorMessage = vm.errorMessage,
        isLoading = vm.isLoading,
        onAvatarEdit = { showAvatarPicker = true },
        onSave = { coroutineScope.launch { vm.saveNickname() } },
        onLogout = {
            vm.logout()
            onLogout()
        },
        onChangePassword = onChangePassword,
        onTripsClick = onTripsClick,
        snackbarHostState = snackbarHostState
    )
}

@Composable
internal fun ProfileContent(
    avatarId: String,
    editNickname: String,
    onNicknameChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onAvatarEdit: () -> Unit,
    onSave: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onTripsClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = { AppTopBar("Profile") },
        bottomBar = {
            TravelNoteBottomNav(
                selected = BottomNavTab.PROFILE,
                onTripsClick = onTripsClick,
                onProfileClick = { /* already on profile */ }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Avatar section
            Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    painter = painterResource(id = DefaultImages.avatar(avatarId)),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                SmallFloatingActionButton(
                    onClick = onAvatarEdit,
                    modifier = Modifier.size(28.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Avatar",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = editNickname,
                onValueChange = onNicknameChange,
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            PrimaryButton(
                text = "Save Changes",
                onClick = onSave,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(4.dp))

            TnTextButton(
                text = "Change Password",
                onClick = onChangePassword
            )

            Spacer(modifier = Modifier.height(8.dp))

            SecondaryButton(
                text = "Sign Out",
                onClick = onLogout
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    TravelnoteTheme {
        ProfileContent(
            avatarId = "avatar_03",
            editNickname = "Jane Traveler",
            onNicknameChange = {},
            errorMessage = null,
            isLoading = false,
            onAvatarEdit = {},
            onSave = {},
            onChangePassword = {},
            onLogout = {},
            onTripsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenErrorPreview() {
    TravelnoteTheme {
        ProfileContent(
            avatarId = "avatar_01",
            editNickname = "",
            onNicknameChange = {},
            errorMessage = "Display name cannot be empty",
            isLoading = false,
            onAvatarEdit = {},
            onSave = {},
            onChangePassword = {},
            onLogout = {},
            onTripsClick = {}
        )
    }
}
