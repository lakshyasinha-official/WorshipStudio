package com.example.worshipstudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.Membership
import com.example.worshipstudio.utils.AppTheme
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SettingsViewModel

// ── Gradient colours for each theme card preview ──────────────────────────────
private val themeGradients = mapOf(
    AppTheme.NIGHTFALL  to listOf(Color(0xFF0A0A18), Color(0xFF2D0060), Color(0xFF5D0080), Color(0xFFAA00AA)),
    AppTheme.DAWN_MIST  to listOf(Color(0xFFFDF6EE), Color(0xFFFAE8D8), Color(0xFFF2D9E8), Color(0xFFE8D5F0)),
    AppTheme.HOLY_LIGHT to listOf(Color(0xFFFAFCFF), Color(0xFFE8F3FF), Color(0xFFD6EAFF), Color(0xFFE9F5F0)),
    AppTheme.SANCTUARY  to listOf(Color(0xFFFDFBF7), Color(0xFFEEF5EE), Color(0xFFE4EEF7), Color(0xFFEDE8F6))
)

private val themeAccents = mapOf(
    AppTheme.NIGHTFALL  to Color(0xFFBB86FC),
    AppTheme.DAWN_MIST  to Color(0xFFC4836C),
    AppTheme.HOLY_LIGHT to Color(0xFF3A7FC1),
    AppTheme.SANCTUARY  to Color(0xFF7A8CCC)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    currentTheme: AppTheme = AppTheme.NIGHTFALL,
    onThemeChange: (AppTheme) -> Unit = {},
    onBack: () -> Unit
) {
    val authState     by authViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val isAdmin       = authState.role == "admin"

    LaunchedEffect(authState.churchId, isAdmin) {
        if (isAdmin && authState.churchId.isNotEmpty()) {
            settingsViewModel.loadMembers(authState.churchId)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── Church info header ─────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color    = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(
                            "Church",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            authState.churchId.ifBlank { "—" },
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Your role:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            RoleBadge(role = authState.role)
                        }
                    }
                }
                HorizontalDivider()
            }

            // ── Theme picker ───────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "App Theme",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Changes the background and colour palette across the whole app",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))

                LazyRow(
                    contentPadding    = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(AppTheme.entries) { theme ->
                        ThemeCard(
                            theme     = theme,
                            isSelected = theme == currentTheme,
                            onClick   = { onThemeChange(theme) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
            }

            // ── Members section (admin only) ───────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                if (isAdmin) {
                    Text(
                        "Church Members",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            if (!isAdmin) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Nothing else here yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Contact your church admin for changes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                when {
                    settingsState.isLoading -> {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    }
                    settingsState.members.isEmpty() -> {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No members found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        items(settingsState.members, key = { it.userId + it.churchId }) { member ->
                            MemberItem(member = member)
                        }
                    }
                }
            }
        }
    }
}

// ── Theme card ────────────────────────────────────────────────────────────────
@Composable
private fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val gradientColors = themeGradients[theme] ?: listOf(Color.Gray)
    val accent         = themeAccents[theme]   ?: Color.Gray
    val isDark         = theme == AppTheme.NIGHTFALL
    val textColor      = if (isDark) Color.White else Color(0xFF222222)

    Column(
        modifier            = Modifier.width(110.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(gradientColors))
                .then(
                    if (isSelected) Modifier.border(
                        width = 3.dp,
                        color = accent,
                        shape = RoundedCornerShape(16.dp)
                    ) else Modifier
                )
                .clickable { onClick() }
        ) {
            // Mini UI mockup inside the card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Mock top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(textColor.copy(alpha = 0.20f))
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Mock song card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = if (isDark) 0.15f else 0.60f))
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Box(
                                Modifier.fillMaxWidth(0.7f).height(5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(textColor.copy(alpha = 0.5f))
                            )
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier.fillMaxWidth(0.45f).height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(textColor.copy(alpha = 0.25f))
                            )
                        }
                    }
                    // Mock live chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accent.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("● Live", fontSize = 8.sp, color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Selected checkmark
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint     = if (isDark) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            theme.displayName,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color      = if (isSelected) MaterialTheme.colorScheme.primary
                         else            MaterialTheme.colorScheme.onSurface
        )
        Text(
            theme.subtitle,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
    }
}

// ── Member list tile ──────────────────────────────────────────────────────────
@Composable
private fun MemberItem(member: Membership) {
    ListItem(
        headlineContent   = {
            Text(member.displayName.ifBlank { member.email }, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(
                member.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = { RoleBadge(role = member.role) }
    )
    HorizontalDivider()
}

// ── Role badge chip ───────────────────────────────────────────────────────────
@Composable
private fun RoleBadge(role: String) {
    val isAdmin = role == "admin"
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isAdmin) MaterialTheme.colorScheme.primaryContainer
                else         MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text       = role.replaceFirstChar { it.uppercase() },
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = if (isAdmin) MaterialTheme.colorScheme.onPrimaryContainer
                         else         MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
