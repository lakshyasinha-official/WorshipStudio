package com.example.worshipstudio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.R
import com.example.worshipstudio.utils.CredentialsStore
import com.example.worshipstudio.viewmodel.AuthViewModel

private enum class LoginMode { SIGN_IN, JOIN_CHURCH, NEW_CHURCH }

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val state   by viewModel.state.collectAsState()

    val saved = remember { CredentialsStore.load(context) }

    var mode        by remember { mutableStateOf(LoginMode.SIGN_IN) }
    var email       by remember { mutableStateOf(saved?.email    ?: "") }
    var password    by remember { mutableStateOf(saved?.password ?: "") }
    var churchId    by remember { mutableStateOf(saved?.churchId ?: "") }
    var displayName by remember { mutableStateOf("") }
    var rememberMe  by remember { mutableStateOf(saved != null) }
    var pwVisible   by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedIn, state.churchId) {
        if (state.isLoggedIn && state.churchId.isNotEmpty()) {
            if (rememberMe) CredentialsStore.save(context, email, password, churchId)
            else            CredentialsStore.clear(context)
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background ────────────────────────────────────────────────────────
        Image(
            painter            = painterResource(R.drawable.church_bg),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize().blur(2.dp)
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.62f), Color.Black.copy(alpha = 0.82f))
                )
            )
        )

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text("WorshipSync", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                "Worship together, in sync",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.70f)
            )
            Spacer(Modifier.height(28.dp))

            // ── Animated card ─────────────────────────────────────────────────
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    val forward = targetState != LoginMode.SIGN_IN
                    (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it else it } + fadeOut())
                },
                label = "login_mode"
            ) { currentMode ->
                Surface(
                    modifier       = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)),
                    shape          = RoundedCornerShape(28.dp),
                    color          = Color.White.copy(alpha = 0.14f),
                    tonalElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {

                        // Back arrow for sub-screens
                        if (currentMode != LoginMode.SIGN_IN) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { mode = LoginMode.SIGN_IN; viewModel.clearError() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (currentMode == LoginMode.JOIN_CHURCH) "Join a Church"
                                    else "Register New Church",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 18.sp,
                                    color      = Color.White
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        when (currentMode) {

                            // ── Sign In ───────────────────────────────────────
                            LoginMode.SIGN_IN -> {
                                Text(
                                    "Welcome back",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 20.sp,
                                    color      = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Sign in with your email, password and church name.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                                Spacer(Modifier.height(20.dp))

                                GlassTextField(email, { email = it }, "Email", KeyboardType.Email)
                                Spacer(Modifier.height(12.dp))
                                PasswordField(password, { password = it }, pwVisible) { pwVisible = !pwVisible }
                                Spacer(Modifier.height(12.dp))
                                ChurchField(churchId, { churchId = it })

                                Spacer(Modifier.height(16.dp))
                                RememberMeRow(rememberMe, { rememberMe = it }) {
                                    CredentialsStore.clear(context)
                                    rememberMe = false
                                    email = ""; password = ""; churchId = ""
                                }
                                ErrorRow(state.error)
                                Spacer(Modifier.height(8.dp))

                                Button(
                                    onClick  = { viewModel.login(email, password, churchId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled  = !state.isLoading && email.isNotBlank()
                                            && password.isNotBlank() && churchId.isNotBlank()
                                ) {
                                    if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp))
                                    else Text("Sign In", fontWeight = FontWeight.Bold)
                                }

                                Spacer(Modifier.height(20.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.20f))
                                Spacer(Modifier.height(16.dp))

                                // Links to sub-screens
                                Text(
                                    "New here?",
                                    style  = MaterialTheme.typography.labelMedium,
                                    color  = Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick  = { mode = LoginMode.JOIN_CHURCH; viewModel.clearError() },
                                    modifier = Modifier.fillMaxWidth(),
                                    border   = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                    colors   = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Join a Church")
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick  = { mode = LoginMode.NEW_CHURCH; viewModel.clearError() },
                                    modifier = Modifier.fillMaxWidth(),
                                    border   = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                    colors   = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Register a New Church")
                                }
                            }

                            // ── Join Church ───────────────────────────────────
                            LoginMode.JOIN_CHURCH -> {
                                Text(
                                    "Join as a member of an existing church.\nAsk your admin for the exact church name.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                                Spacer(Modifier.height(20.dp))

                                GlassTextField(displayName, { displayName = it }, "Your Name")
                                Spacer(Modifier.height(12.dp))
                                GlassTextField(email, { email = it }, "Email", KeyboardType.Email)
                                Spacer(Modifier.height(12.dp))
                                PasswordField(password, { password = it }, pwVisible) { pwVisible = !pwVisible }
                                Spacer(Modifier.height(12.dp))
                                ChurchField(churchId, { churchId = it }, helperText = "Church name is case-insensitive")

                                ErrorRow(state.error)
                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick  = { viewModel.registerAsMember(email, password, churchId, displayName) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled  = !state.isLoading && email.isNotBlank()
                                            && password.isNotBlank() && churchId.isNotBlank()
                                            && displayName.isNotBlank()
                                ) {
                                    if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp))
                                    else Text("Join as Member", fontWeight = FontWeight.Bold)
                                }
                            }

                            // ── New Church ────────────────────────────────────
                            LoginMode.NEW_CHURCH -> {
                                Text(
                                    "Create a new church. You will be the admin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                                Spacer(Modifier.height(20.dp))

                                ChurchField(
                                    value      = churchId,
                                    onChange   = { churchId = it },
                                    label      = "Church Name",
                                    helperText = "Must be unique · stored in lowercase"
                                )
                                Spacer(Modifier.height(12.dp))
                                GlassTextField(displayName, { displayName = it }, "Your Name")
                                Spacer(Modifier.height(12.dp))
                                GlassTextField(email, { email = it }, "Email", KeyboardType.Email)
                                Spacer(Modifier.height(12.dp))
                                PasswordField(password, { password = it }, pwVisible) { pwVisible = !pwVisible }

                                ErrorRow(state.error)
                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick  = { viewModel.registerNewChurch(email, password, churchId, displayName) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled  = !state.isLoading && email.isNotBlank()
                                            && password.isNotBlank() && churchId.isNotBlank()
                                            && displayName.isNotBlank()
                                ) {
                                    if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp))
                                    else Text("Create Church", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "You will automatically become the admin.",
                                    style     = MaterialTheme.typography.labelSmall,
                                    color     = Color.White.copy(alpha = 0.50f),
                                    textAlign = TextAlign.Center,
                                    modifier  = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorRow(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(error, color = Color(0xFFFF6B6B),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RememberMeRow(checked: Boolean, onChange: (Boolean) -> Unit, onForget: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChange)
            Text("Remember me", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
        if (checked) {
            TextButton(onClick = onForget) {
                Text("Forget saved", style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF6B6B))
            }
        }
    }
}

@Composable
private fun ChurchField(
    value:      String,
    onChange:   (String) -> Unit,
    label:      String = "Church Name",
    helperText: String = ""
) {
    GlassTextField(
        value         = value,
        onValueChange = { onChange(it.lowercase()) },
        label         = label,
        trailingIcon  = { Icon(Icons.Default.Church, null, tint = Color.White.copy(alpha = 0.7f)) }
    )
    if (helperText.isNotEmpty()) {
        Spacer(Modifier.height(3.dp))
        Text(helperText, style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.50f), modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, visible: Boolean, onToggle: () -> Unit) {
    GlassTextField(
        value                = value,
        onValueChange        = onChange,
        label                = "Password",
        keyboardType         = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon         = {
            IconButton(onClick = onToggle) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    )
}

@Composable
private fun GlassTextField(
    value:                String,
    onValueChange:        (String) -> Unit,
    label:                String,
    keyboardType:         KeyboardType         = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon:         @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon         = trailingIcon,
        singleLine           = true,
        modifier             = Modifier.fillMaxWidth(),
        shape                = RoundedCornerShape(14.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Color.White,
            focusedLabelColor       = Color.White.copy(alpha = 0.9f),
            unfocusedLabelColor     = Color.White.copy(alpha = 0.65f),
            focusedBorderColor      = Color.White.copy(alpha = 0.8f),
            unfocusedBorderColor    = Color.White.copy(alpha = 0.35f),
            cursorColor             = Color.White,
            focusedContainerColor   = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
        )
    )
}
