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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.AvatarPicker
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.ui.components.SecondaryButton
import com.wergogo.travelnote.util.DefaultImages
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: ProfileViewModel = viewModel {
        ProfileViewModel(app.container.authRepository, app.container.profileRepository)
    }
    val coroutineScope = rememberCoroutineScope()
    val profile by vm.profile.collectAsState()

    var showAvatarPicker by remember { mutableStateOf(false) }

    if (showAvatarPicker) {
        AvatarPicker(
            selectedKey = profile?.avatarId ?: "avatar_01",
            onSelect = { key ->
                showAvatarPicker = false
                coroutineScope.launch { vm.changeAvatar(key) }
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    Scaffold(
        topBar = { AppTopBar("Profile") }
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
                    painter = painterResource(id = DefaultImages.avatar(profile?.avatarId)),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                SmallFloatingActionButton(
                    onClick = { showAvatarPicker = true },
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
                value = vm.editNickname,
                onValueChange = { vm.editNickname = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (vm.errorMessage != null) {
                Text(
                    text = vm.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            PrimaryButton(
                text = "Save Changes",
                onClick = { coroutineScope.launch { vm.saveNickname() } },
                enabled = !vm.isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            SecondaryButton(
                text = "Sign Out",
                onClick = {
                    coroutineScope.launch {
                        vm.logout()
                        onLogout()
                    }
                }
            )
        }
    }
}
