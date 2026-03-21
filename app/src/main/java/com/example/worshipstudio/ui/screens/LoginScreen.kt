package com.example.worshipstudio.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.R
import com.example.worshipstudio.utils.CredentialsStore
import com.example.worshipstudio.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val state   by viewModel.state.collectAsState()

    // ── Pre-fill from saved credentials ───────────────────────────────────────
    val saved = remember { CredentialsStore.load(context) }

    var email          by remember { mutableStateOf(saved?.email    ?: "") }
    var password       by remember { mutableStateOf(saved?.password ?: "") }
    var churchId       by remember { mutableStateOf(saved?.churchId ?: "") }
    var role           by remember { mutableStateOf("member") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var rememberMe     by remember { mutableStateOf(saved != null) }
    var passwordVisible by remember { mutableStateOf(false) }

    // ── Navigate on successful login ──────────────────────────────────────────
    LaunchedEffect(state.isLoggedIn, state.churchId) {
        if (state.isLoggedIn && state.churchId.isNotEmpty()) {
            if (rememberMe) CredentialsStore.save(context, email, password, churchId)
            else            CredentialsStore.clear(context)
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Blurred church background ─────────────────────────────────────────
        Image(
            painter            = painterResource(R.drawable.church_bg),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .fillMaxSize()
                .blur(2.dp)
        )

        // ── Dark gradient scrim so text stays readable ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
        )

        // ── Login card ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App title
            Text(
                "WorshipSync",
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isRegisterMode) "Create Account" else "Sign In",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.80f)
            )
            Spacer(Modifier.height(32.dp))

            // ── Frosted glass card ────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.15f),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // ── Email ─────────────────────────────────────────────────
                    GlassTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = "Email",
                        keyboardType  = KeyboardType.Email
                    )
                    Spacer(Modifier.height(12.dp))

                    // ── Password with eye toggle ──────────────────────────────
                    GlassTextField(
                        value                = password,
                        onValueChange        = { password = it },
                        label                = "Password",
                        keyboardType         = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                              else                  PasswordVisualTransformation(),
                        trailingIcon         = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector        = if (passwordVisible) Icons.Default.VisibilityOff
                                                         else                 Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide" else "Show",
                                    tint               = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))

                    // ── Church ID ─────────────────────────────────────────────
                    GlassTextField(
                        value         = churchId,
                        onValueChange = { churchId = it },
                        label         = "Church ID"
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Same email can belong to multiple churches",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // ── Role (register only) ──────────────────────────────────
                    if (isRegisterMode) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Role: ", color = Color.White,
                                style = MaterialTheme.typography.bodyMedium)
                            RadioButton(selected = role == "member",
                                onClick = { role = "member" })
                            Text("Member", color = Color.White)
                            Spacer(Modifier.width(16.dp))
                            RadioButton(selected = role == "admin",
                                onClick = { role = "admin" })
                            Text("Admin", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
                    Spacer(Modifier.height(8.dp))

                    // ── Remember Me row ───────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Checkbox(
                                checked         = rememberMe,
                                onCheckedChange = { checked ->
                                    rememberMe = checked
                                    if (!checked) CredentialsStore.clear(context)
                                }
                            )
                            Text(
                                "Remember me",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (CredentialsStore.isRemembered(context) && !isRegisterMode) {
                            TextButton(onClick = {
                                CredentialsStore.clear(context)
                                rememberMe = false
                                email = ""; password = ""; churchId = ""
                            }) {
                                Text(
                                    "Forget saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF6B6B)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Error ─────────────────────────────────────────────────
                    state.error?.let {
                        Text(
                            it,
                            color  = Color(0xFFFF6B6B),
                            style  = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // ── Login / Register button ───────────────────────────────
                    Button(
                        onClick = {
                            if (isRegisterMode) viewModel.register(email, password, churchId, role)
                            else                viewModel.login(email, password, churchId)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled  = !state.isLoading &&
                                   email.isNotBlank() &&
                                   password.isNotBlank() &&
                                   churchId.isNotBlank()
                    ) {
                        if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Text(if (isRegisterMode) "Register" else "Login")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(
                    if (isRegisterMode) "Already have an account? Sign In"
                    else                "No account? Register",
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Reusable glass-style text field ──────────────────────────────────────────
@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val colors = OutlinedTextFieldDefaults.colors(
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
    androidx.compose.material3.OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon         = trailingIcon,
        singleLine           = true,
        modifier             = Modifier.fillMaxWidth(),
        shape                = RoundedCornerShape(14.dp),
        colors               = colors
    )
}
