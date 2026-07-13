package com.example.worshipstudio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.utils.CredentialsStore
import com.example.worshipstudio.viewmodel.AuthViewModel

private enum class LoginMode { SIGN_IN, JOIN_CHURCH, NEW_CHURCH, FORGOT_PASSWORD }

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens — dark mint palette
// ─────────────────────────────────────────────────────────────────────────────
private val Mint          = Color(0xFF4EE0A0)
private val MintDeep      = Color(0xFF1E7A52)
private val OnMint        = Color(0xFF06281A)
private val BgTop         = Color(0xFF0D1311)
private val BgBottom      = Color(0xFF070B09)
private val CardBg        = Color(0xFF111814)
private val FieldBg       = Color(0xFF1A231E)
private val SubtleBorder  = Color(0x14FFFFFF)
private val FieldBorder   = Color(0x1FFFFFFF)
private val TextPrimary   = Color(0xFFF2F5F3)
private val TextSecondary = Color(0xFF98A39D)
private val ErrorRed      = Color(0xFFFF6B6B)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        // Faint mint glow behind the card
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(420.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Mint.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Local state for forgot-password feedback
            var resetSent  by remember { mutableStateOf(false) }
            var resetError by remember { mutableStateOf<String?>(null) }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(28.dp),
                color    = CardBg,
                border   = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Logo + title (shared across modes) ─────────────────
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Mint, MintDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "WorshipSync",
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (mode) {
                            LoginMode.SIGN_IN         -> "Welcome back"
                            LoginMode.JOIN_CHURCH     -> "Join a church"
                            LoginMode.NEW_CHURCH      -> "Register a new church"
                            LoginMode.FORGOT_PASSWORD -> "Reset your password"
                        },
                        fontSize = 16.sp,
                        color    = TextSecondary
                    )
                    Spacer(Modifier.height(28.dp))

                    // ── Animated mode content ───────────────────────────────
                    AnimatedContent(
                        targetState = mode,
                        transitionSpec = {
                            val forward = targetState != LoginMode.SIGN_IN
                            (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { if (forward) -it else it } + fadeOut())
                        },
                        label = "login_mode"
                    ) { currentMode ->
                        Column {
                            // Back link for sub-screens
                            if (currentMode != LoginMode.SIGN_IN) {
                                BackRow {
                                    mode = LoginMode.SIGN_IN
                                    viewModel.clearError()
                                    resetSent  = false
                                    resetError = null
                                }
                                Spacer(Modifier.height(20.dp))
                            }

                            when (currentMode) {

                                // ── Sign In ───────────────────────────────
                                LoginMode.SIGN_IN -> {
                                    LabeledField(
                                        label       = "Email Address",
                                        value       = email,
                                        onChange    = { email = it },
                                        placeholder = "you@example.com",
                                        leadingIcon = Icons.Outlined.Email,
                                        keyboardType = KeyboardType.Email
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    PasswordField(
                                        value    = password,
                                        onChange = { password = it },
                                        visible  = pwVisible,
                                        onToggle = { pwVisible = !pwVisible },
                                        onForgot = {
                                            mode = LoginMode.FORGOT_PASSWORD
                                            viewModel.clearError()
                                        }
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    ChurchField(churchId, { churchId = it })

                                    Spacer(Modifier.height(8.dp))
                                    RememberMeRow(rememberMe, { rememberMe = it }) {
                                        CredentialsStore.clear(context)
                                        rememberMe = false
                                        email = ""; password = ""; churchId = ""
                                    }
                                    ErrorRow(state.error)
                                    Spacer(Modifier.height(12.dp))

                                    MintButton(
                                        label     = "Login",
                                        isLoading = state.isLoading,
                                        enabled   = !state.isLoading && email.isNotBlank()
                                                && password.isNotBlank() && churchId.isNotBlank(),
                                        showArrow = true
                                    ) { viewModel.login(email, password, churchId) }

                                    if (state.slowNetwork && state.isLoading) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            "Still connecting… your Wi-Fi may be blocking " +
                                            "Google servers. Mobile data is usually faster.",
                                            fontSize   = 12.sp,
                                            lineHeight = 17.sp,
                                            color      = Color(0xFFE8C468),
                                            textAlign  = TextAlign.Center,
                                            modifier   = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(Modifier.height(28.dp))
                                    SectionDivider("NEW HERE?")
                                    Spacer(Modifier.height(20.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OptionTile(
                                            label    = "Join Church",
                                            icon     = Icons.Outlined.Groups,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            mode = LoginMode.JOIN_CHURCH
                                            viewModel.clearError()
                                        }
                                        OptionTile(
                                            label    = "New Church",
                                            icon     = Icons.Outlined.Church,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            mode = LoginMode.NEW_CHURCH
                                            viewModel.clearError()
                                        }
                                    }
                                }

                                // ── Forgot Password ───────────────────────
                                LoginMode.FORGOT_PASSWORD -> {
                                    if (resetSent) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Outlined.MarkEmailRead,
                                                contentDescription = null,
                                                tint = Mint,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                "Check your email",
                                                fontWeight = FontWeight.Bold,
                                                fontSize   = 19.sp,
                                                color      = TextPrimary
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                "A password reset link has been sent to $email.\n\n" +
                                                "After resetting via the link, come back and sign in. " +
                                                "You will be prompted to set a new permanent password.",
                                                fontSize  = 13.sp,
                                                lineHeight = 19.sp,
                                                color     = TextSecondary,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(Modifier.height(24.dp))
                                            MintButton(label = "Back to Sign In") {
                                                mode      = LoginMode.SIGN_IN
                                                resetSent = false
                                            }
                                        }
                                    } else {
                                        Text(
                                            "Enter your registered email and church name. " +
                                            "We'll send a reset link to your inbox.",
                                            fontSize   = 13.sp,
                                            lineHeight = 19.sp,
                                            color      = TextSecondary
                                        )
                                        Spacer(Modifier.height(22.dp))

                                        LabeledField(
                                            label       = "Email Address",
                                            value       = email,
                                            onChange    = { email = it },
                                            placeholder = "you@example.com",
                                            leadingIcon = Icons.Outlined.Email,
                                            keyboardType = KeyboardType.Email
                                        )
                                        Spacer(Modifier.height(18.dp))
                                        ChurchField(churchId, { churchId = it })

                                        if (resetError != null) {
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                resetError!!,
                                                color    = ErrorRed,
                                                fontSize = 13.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        Spacer(Modifier.height(22.dp))

                                        MintButton(
                                            label     = "Send Reset Link",
                                            isLoading = state.isLoading,
                                            enabled   = !state.isLoading
                                                    && email.isNotBlank()
                                                    && churchId.isNotBlank()
                                        ) {
                                            resetError = null
                                            viewModel.requestPasswordReset(
                                                email     = email,
                                                churchId  = churchId,
                                                onSuccess = { resetSent = true },
                                                onError   = { resetError = it }
                                            )
                                        }
                                    }
                                }

                                // ── Join Church ───────────────────────────
                                LoginMode.JOIN_CHURCH -> {
                                    Text(
                                        "Join as a member of an existing church. " +
                                        "Ask your admin for the exact church name.",
                                        fontSize   = 13.sp,
                                        lineHeight = 19.sp,
                                        color      = TextSecondary
                                    )
                                    Spacer(Modifier.height(22.dp))

                                    LabeledField(
                                        label       = "Your Name",
                                        value       = displayName,
                                        onChange    = { displayName = it },
                                        placeholder = "e.g. Alex Johnson",
                                        leadingIcon = Icons.Outlined.Person
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    LabeledField(
                                        label       = "Email Address",
                                        value       = email,
                                        onChange    = { email = it },
                                        placeholder = "you@example.com",
                                        leadingIcon = Icons.Outlined.Email,
                                        keyboardType = KeyboardType.Email
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    PasswordField(
                                        value    = password,
                                        onChange = { password = it },
                                        visible  = pwVisible,
                                        onToggle = { pwVisible = !pwVisible }
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    ChurchField(
                                        churchId, { churchId = it },
                                        helperText = "Church name is case-insensitive"
                                    )

                                    ErrorRow(state.error)
                                    Spacer(Modifier.height(22.dp))

                                    MintButton(
                                        label     = "Join as Member",
                                        isLoading = state.isLoading,
                                        enabled   = !state.isLoading && email.isNotBlank()
                                                && password.isNotBlank() && churchId.isNotBlank()
                                                && displayName.isNotBlank(),
                                        showArrow = true
                                    ) { viewModel.registerAsMember(email, password, churchId, displayName) }
                                }

                                // ── New Church ────────────────────────────
                                LoginMode.NEW_CHURCH -> {
                                    Text(
                                        "Create a new church. You will be the admin.",
                                        fontSize   = 13.sp,
                                        lineHeight = 19.sp,
                                        color      = TextSecondary
                                    )
                                    Spacer(Modifier.height(22.dp))

                                    ChurchField(
                                        value      = churchId,
                                        onChange   = { churchId = it },
                                        label      = "Church Name",
                                        helperText = "Must be unique · stored in lowercase"
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    LabeledField(
                                        label       = "Your Name",
                                        value       = displayName,
                                        onChange    = { displayName = it },
                                        placeholder = "e.g. Alex Johnson",
                                        leadingIcon = Icons.Outlined.Person
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    LabeledField(
                                        label       = "Email Address",
                                        value       = email,
                                        onChange    = { email = it },
                                        placeholder = "you@example.com",
                                        leadingIcon = Icons.Outlined.Email,
                                        keyboardType = KeyboardType.Email
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    PasswordField(
                                        value    = password,
                                        onChange = { password = it },
                                        visible  = pwVisible,
                                        onToggle = { pwVisible = !pwVisible }
                                    )

                                    ErrorRow(state.error)
                                    Spacer(Modifier.height(22.dp))

                                    MintButton(
                                        label     = "Create Church",
                                        isLoading = state.isLoading,
                                        enabled   = !state.isLoading && email.isNotBlank()
                                                && password.isNotBlank() && churchId.isNotBlank()
                                                && displayName.isNotBlank(),
                                        showArrow = true
                                    ) { viewModel.registerNewChurch(email, password, churchId, displayName) }

                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "You will automatically become the admin.",
                                        fontSize  = 12.sp,
                                        color     = TextSecondary.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier  = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BackRow(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onBack,
            shape   = RoundedCornerShape(12.dp),
            color   = FieldBg,
            border  = BorderStroke(1.dp, FieldBorder)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.padding(8.dp).size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Back to Sign In",
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            color      = TextSecondary
        )
    }
}

@Composable
private fun SectionDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = SubtleBorder)
        Text(
            label,
            fontSize      = 12.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 2.sp,
            color         = TextSecondary,
            modifier      = Modifier.padding(horizontal = 14.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = SubtleBorder)
    }
}

@Composable
private fun OptionTile(
    label:    String,
    icon:     ImageVector,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        color    = FieldBg,
        border   = BorderStroke(1.dp, FieldBorder)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 15.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Mint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

@Composable
private fun MintButton(
    label:     String,
    isLoading: Boolean = false,
    enabled:   Boolean = true,
    showArrow: Boolean = false,
    onClick:   () -> Unit
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Mint,
            contentColor           = OnMint,
            disabledContainerColor = Mint.copy(alpha = 0.25f),
            disabledContentColor   = OnMint.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color    = OnMint,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (showArrow) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            error,
            color    = ErrorRed,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
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
            Checkbox(
                checked = checked,
                onCheckedChange = onChange,
                colors = CheckboxDefaults.colors(
                    checkedColor   = Mint,
                    checkmarkColor = OnMint,
                    uncheckedColor = TextSecondary
                )
            )
            Text("Remember me", color = TextPrimary, fontSize = 14.sp)
        }
        if (checked) {
            TextButton(onClick = onForget) {
                Text("Forget saved", fontSize = 12.sp, color = ErrorRed)
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
    LabeledField(
        label       = label,
        value       = value,
        onChange    = { onChange(it.lowercase()) },
        placeholder = "your church name",
        leadingIcon = Icons.Outlined.Church,
        helperText  = helperText.ifEmpty { null }
    )
}

@Composable
private fun PasswordField(
    value:    String,
    onChange: (String) -> Unit,
    visible:  Boolean,
    onToggle: () -> Unit,
    onForgot: (() -> Unit)? = null
) {
    LabeledField(
        label                = "Password",
        value                = value,
        onChange             = onChange,
        placeholder          = "••••••••",
        leadingIcon          = Icons.Outlined.Lock,
        keyboardType         = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon         = {
            IconButton(onClick = onToggle) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                    tint = TextSecondary
                )
            }
        },
        labelTrailing = onForgot?.let {
            {
                Text(
                    "Forgot Password?",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Mint
                )
            }
        },
        onLabelTrailingClick = onForgot
    )
}

@Composable
private fun LabeledField(
    label:                String,
    value:                String,
    onChange:             (String) -> Unit,
    placeholder:          String,
    leadingIcon:          ImageVector,
    keyboardType:         KeyboardType         = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon:         @Composable (() -> Unit)? = null,
    helperText:           String? = null,
    labelTrailing:        @Composable (() -> Unit)? = null,
    onLabelTrailingClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary
            )
            if (labelTrailing != null) {
                if (onLabelTrailingClick != null) {
                    TextButton(
                        onClick        = onLabelTrailingClick,
                        contentPadding = PaddingValues(0.dp),
                        modifier       = Modifier.height(24.dp)
                    ) { labelTrailing() }
                } else {
                    labelTrailing()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value                = value,
            onValueChange        = onChange,
            placeholder          = { Text(placeholder, color = TextSecondary.copy(alpha = 0.7f)) },
            leadingIcon          = {
                Icon(leadingIcon, contentDescription = null, tint = TextSecondary)
            },
            trailingIcon         = trailingIcon,
            keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            singleLine           = true,
            modifier             = Modifier.fillMaxWidth(),
            shape                = RoundedCornerShape(16.dp),
            colors               = OutlinedTextFieldDefaults.colors(
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                focusedBorderColor      = Mint.copy(alpha = 0.75f),
                unfocusedBorderColor    = FieldBorder,
                cursorColor             = Mint,
                focusedContainerColor   = FieldBg,
                unfocusedContainerColor = FieldBg,
                focusedLeadingIconColor = Mint,
                unfocusedLeadingIconColor = TextSecondary
            )
        )
        if (helperText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                helperText,
                fontSize = 11.sp,
                color    = TextSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
