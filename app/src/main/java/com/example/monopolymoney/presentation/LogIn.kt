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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monopolymoney.R
import com.example.monopolymoney.presentation.ForgotPasswordDialog
import com.example.monopolymoney.presentation.SystemAwareScreen
import com.example.monopolymoney.ui.theme.MyColors
import com.example.monopolymoney.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onCreateAccount: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    SystemAwareScreen(backgroundColor = Color(0xFF252525)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Welcome back!",
                fontFamily = FontFamily(Font(R.font.carisma600)),
                fontSize = 24.sp,
                color = MyColors.white,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Use your credentials to log in",
                fontFamily = FontFamily(Font(R.font.carisma500)),
                fontSize = 18.sp,
                color = MyColors.white,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    val image = if (passwordVisible) R.drawable.end else R.drawable.bank
                    IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.padding(end = 8.dp).size(32.dp)) {
                        Icon(painter = painterResource(id = image), tint = MyColors.white, contentDescription = null)
                    }
                }
            )


            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { showForgotPasswordDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot Password?",
                    fontFamily = FontFamily(Font(R.font.carisma500)),
                    fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.loginUser(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = "Log In",
                    fontFamily = FontFamily(Font(R.font.carisma600)),
                    fontSize = 16.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    color = MyColors.white.copy(alpha = 0.3f)
                )
                Text(
                    text = "OR",
                    color = MyColors.white.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    color = MyColors.white.copy(alpha = 0.3f)
                )
            }

            // Botón para iniciar sesión con Google
            OutlinedButton(
                onClick = onCreateAccount//onGoogleSignIn
                ,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.end), contentDescription = null) // Icono de Google
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Sign in with Google",
                    fontFamily = FontFamily(Font(R.font.carisma600)),
                    fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(100.dp))

            TextButton(
                onClick = onCreateAccount,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("New here? Sign up!",
                    fontFamily = FontFamily(Font(R.font.carisma600)),
                    fontSize = 16.sp,)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (authState is AuthViewModel.AuthState.Error) {
                Text(
                    text = (authState as AuthViewModel.AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (showForgotPasswordDialog) {
                ForgotPasswordDialog(
                    isLoggedIn = false,
                    onDismiss = { showForgotPasswordDialog = false },
                    onSendResetEmail = { email ->
                        viewModel.sendPasswordResetEmail(email)
                        showForgotPasswordDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun CreateAccountScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    SystemAwareScreen(backgroundColor = Color(0xFF252525)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MyColors.white,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    val image = if (passwordVisible) R.drawable.end else R.drawable.bank
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(painter = painterResource(id = image), tint = MyColors.white, contentDescription = null)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.createUser(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = "Create Account")
            }


            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Already have an account? Log in")
            }

            if (authState is AuthViewModel.AuthState.Error) {
                Text(
                    text = (authState as AuthViewModel.AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
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

    SystemAwareScreen(backgroundColor = Color(0xFF252525)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
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
}

@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
) {
    var name by remember { mutableStateOf(viewModel.user.value?.name ?: "") }
    var selectedImageResId by remember { mutableStateOf(viewModel.user.value?.profileImageResId ?: R.drawable.faces01) }

    val profileImages = getProfileImages()

    SystemAwareScreen(backgroundColor = Color(0xFF252525)) {
        ProfileSetupContent(
            name = name,
            onNameChange = { name = it },
            profileImages = profileImages,
            selectedImageResId = selectedImageResId,
            onImageSelected = { selectedImageResId = it },
            onConfirmClick = {
                viewModel.updateUserProfile(name, selectedImageResId)
            }
        )
    }
}

@Composable
fun ProfileSetupContent(
    name: String,
    onNameChange: (String) -> Unit,
    profileImages: List<Int>,
    selectedImageResId: Int,
    onImageSelected: (Int) -> Unit,
    onConfirmClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
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
            onValueChange = onNameChange,
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select your profile image:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ProfileImageGrid(
            profileImages = profileImages,
            selectedImageResId = selectedImageResId,
            onImageSelected = onImageSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConfirmClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = name.isNotBlank() && selectedImageResId != 0
        ) {
            Text(text = "Confirm Profile")
        }
    }
}

@Composable
fun ProfileImageGrid(
    profileImages: List<Int>,
    selectedImageResId: Int,
    onImageSelected: (Int) -> Unit
) {
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
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.4f
                        ),
                        shape = CircleShape
                    )
                    .clickable { onImageSelected(imageResId) }
                    .alpha(if (!isSelected) 0.5f else 1f)
            )
        }
    }
}

fun getProfileImages(): List<Int> {
    return listOf(
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
}
