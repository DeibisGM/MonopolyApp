package com.example.monopolymoney.presentation
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.monopolymoney.R
import com.example.monopolymoney.ui.theme.MyColors
import com.example.monopolymoney.viewmodel.AuthViewModel

@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    var showProfileImageDialog by remember { mutableStateOf(false) }
    var showChangeNameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = user) {
        if (user == null) {
            onNavigateBack()
        }
    }

    SystemAwareScreen(backgroundColor = Color(0xFF141F23)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.end),
                        tint = MyColors.white,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MyColors.white
                )
                Spacer(modifier = Modifier.width(48.dp)) // For visual balance
            }

            // Profile Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = user?.profileImageResId ?: R.drawable.faces01
                        ),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable { showProfileImageDialog = true }
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = user?.name ?: "Unknown User",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            // Settings Options
            SettingsOption(
                icon = R.drawable.end,
                title = "Change Name",
                onClick = { showChangeNameDialog = true }
            )
            SettingsOption(
                icon = R.drawable.end,
                title = "Change Profile Picture",
                onClick = { showProfileImageDialog = true }
            )
            SettingsOption(
                icon = R.drawable.end,
                title = "Change Password",
                onClick = { showChangePasswordDialog = true }
            )
            SettingsOption(
                icon = R.drawable.end,
                title = "Sign Out",
                onClick = {
                    viewModel.signOut()
                    onNavigateBack()
                }
            )
            SettingsOption(
                icon = R.drawable.end,
                title = "Delete Account",
                textColor = MaterialTheme.colorScheme.error,
                onClick = { showDeleteAccountDialog = true }
            )
        }

        // Dialogs
        if (showProfileImageDialog) {
            ProfileImageDialog(
                currentImageResId = user?.profileImageResId ?: R.drawable.faces01,
                onDismiss = { showProfileImageDialog = false },
                onImageSelected = { resId ->
                    viewModel.updateUserProfile(user?.name ?: "", resId)
                    showProfileImageDialog = false
                }
            )
        }

        if (showChangeNameDialog) {
            ChangeNameDialog(
                currentName = user?.name ?: "",
                onDismiss = { showChangeNameDialog = false },
                onNameChanged = { newName ->
                    viewModel.updateUserProfile(
                        newName,
                        user?.profileImageResId ?: R.drawable.faces01
                    )
                    showChangeNameDialog = false
                }
            )
        }



        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                isLoggedIn = true,
                onDismiss = { showForgotPasswordDialog = false },
                onSendResetEmail = { email ->
                    viewModel.sendPasswordResetEmail(email)
                    showForgotPasswordDialog = false
                }
            )
        }

        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                onDismiss = { showChangePasswordDialog = false },
                onPasswordChanged = { currentPassword, newPassword ->
                    viewModel.changePassword(currentPassword, newPassword)
                    showChangePasswordDialog = false
                },
                onForgotPassword = {
                    // Show a dialog to enter email
                    showForgotPasswordDialog = true
                    showChangePasswordDialog = false
                }
            )
        }

        if (showDeleteAccountDialog) {
            DeleteAccountDialog(
                onDismiss = { showDeleteAccountDialog = false },
                onConfirmDelete = {
                    viewModel.deleteAccount()
                }
            )
        }
    }
}

@Composable
fun SettingsOption(
    icon: Int,
    title: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp) // Establece un tamaño fijo para el ícono
            )
            Spacer(modifier = Modifier.width(8.dp)) // Aumenté el espaciado para que el texto no quede muy pegado
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}


@Composable
fun ProfileImageDialog(
    currentImageResId: Int,
    onDismiss: () -> Unit,
    onImageSelected: (Int) -> Unit
) {
    val profileImages = listOf(
        R.drawable.faces01, R.drawable.faces02, R.drawable.faces03,
        R.drawable.faces04, R.drawable.faces05, R.drawable.faces06,
        R.drawable.faces07, R.drawable.faces08, R.drawable.faces09,
        R.drawable.faces10, R.drawable.faces11, R.drawable.faces12,
        R.drawable.faces13, R.drawable.faces14, R.drawable.faces15,
        R.drawable.faces16, R.drawable.faces17
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Profile Picture") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(profileImages) { imageResId ->
                    val isSelected = currentImageResId == imageResId
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = "Profile image option",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { onImageSelected(imageResId) }
                            .alpha(if (!isSelected) 0.7f else 1f),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onNameChanged: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Name") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New Name") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onNameChanged(newName) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onPasswordChanged: (String, String) -> Unit,
    onForgotPassword: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val passwordFields = listOf(
        PasswordFieldData(
            label = "Current Password",
            password = currentPassword,
            onPasswordChange = { currentPassword = it }
        ),
        PasswordFieldData(
            label = "New Password",
            password = newPassword,
            onPasswordChange = { newPassword = it }
        ),
        PasswordFieldData(
            label = "Confirm New Password",
            password = confirmPassword,
            onPasswordChange = { confirmPassword = it }
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column {
                passwordFields.forEach { fieldData ->
                    PasswordField(
                        label = fieldData.label,
                        password = fieldData.password,
                        onPasswordChange = fieldData.onPasswordChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onForgotPassword,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?")
                }
            }
        },
        confirmButton = {
            val isPasswordValid = currentPassword.isNotBlank() &&
                    newPassword.isNotBlank() &&
                    newPassword == confirmPassword &&
                    newPassword.length >= 6

            TextButton(
                onClick = { onPasswordChanged(currentPassword, newPassword) },
                enabled = isPasswordValid
            ) {
                Text("Change Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PasswordField(
    label: String,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(label) },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    painter = painterResource(
                        id = if (showPassword) R.drawable.bank else R.drawable.end
                    ),
                    contentDescription = if (showPassword) "Hide password" else "Show password"
                )
            }
        }
    )
}

data class PasswordFieldData(
    val label: String,
    val password: String,
    val onPasswordChange: (String) -> Unit
)


@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account") },
        text = {
            Text("Are you sure you want to delete your account? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}