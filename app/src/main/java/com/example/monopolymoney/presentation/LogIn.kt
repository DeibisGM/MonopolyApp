import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.monopolymoney.R
import com.example.monopolymoney.viewmodel.AuthViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onForgotPassword: () -> Unit,
    //onEmailVerificationNeeded: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login to Monopoly Money",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            isError = email.isEmpty()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = password.isEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onForgotPassword,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot Password?")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.loginUser(email, password)
                } else {
                    feedbackMessage = "Please enter both email and password."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Log In")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.createUser(email, password)
                    //onEmailVerificationNeeded()
                } else {
                    feedbackMessage = "Please enter both email and password."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Sign Up")
        }

        if (authState is AuthViewModel.AuthState.Error) {
            Text(
                text = (authState as AuthViewModel.AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        feedbackMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun EmailVerificationScreen(
    viewModel: AuthViewModel,
    email: String,
    onBackToLogin: () -> Unit
) {
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Please verify your email",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "We've sent a verification email to $email. Please check your inbox and click the verification link.",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = {
                viewModel.verifyEmail()
                feedbackMessage = "Checking email verification status..."
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("I've verified my email")
        }
        TextButton(
            onClick = {
                viewModel.resendVerificationEmail()
                feedbackMessage = "Verification email resent. Please check your inbox."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resend verification email")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                feedbackMessage = null
                viewModel.signOut()
                onBackToLogin()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Login")
        }

        feedbackMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
) {
    var name by remember { mutableStateOf(viewModel.user.value?.name ?: "") }
    var selectedImageResId by remember { mutableStateOf(viewModel.user.value?.profileImageResId ?: R.drawable.faces01) }

    val profileImages = listOf(
        R.drawable.faces01, R.drawable.faces02,
        R.drawable.faces03, R.drawable.faces04,
        R.drawable.faces05, R.drawable.faces06,
        R.drawable.faces07, R.drawable.faces08,
        R.drawable.faces09, R.drawable.faces10,
        R.drawable.faces11, R.drawable.faces12,
        R.drawable.faces13, R.drawable.faces14,
        R.drawable.faces15, R.drawable.faces16,
        R.drawable.faces17
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Confirm Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select your profile image:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            items(profileImages) { imageResId ->
                val isSelected = selectedImageResId == imageResId
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = "Profile image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { selectedImageResId = imageResId }
                        .alpha(if (!isSelected) 0.5f else 1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.updateUserProfile(name, selectedImageResId)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = name.isNotBlank() && selectedImageResId != 0
        ) {
            Text(text = "Confirm Profile")
        }
    }
}