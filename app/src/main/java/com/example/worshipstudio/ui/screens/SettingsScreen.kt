package com.example.worshipstudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.Membership
import com.example.worshipstudio.utils.AppTheme
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SettingsViewModel
import com.example.worshipstudio.viewmodel.TagViewModel

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
    authViewModel:     AuthViewModel,
    settingsViewModel: SettingsViewModel,
    tagViewModel:      TagViewModel,
    currentTheme:      AppTheme = AppTheme.NIGHTFALL,
    onThemeChange:     (AppTheme) -> Unit = {},
    onBack:            () -> Unit
) {
    val authState     by authViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val tagState      by tagViewModel.state.collectAsState()
    val isAdmin       = authState.role == "admin"

    // ── Section collapse states (all start closed) ────────────────────────────
    var profileExpanded  by remember { mutableStateOf(false) }
    var themeExpanded    by remember { mutableStateOf(false) }
    var membersExpanded  by remember { mutableStateOf(false) }
    var tagsExpanded     by remember { mutableStateOf(false) }
    var dangerExpanded   by remember { mutableStateOf(false) }

    // ── Tag management state ──────────────────────────────────────────────────
    var newTagName       by remember { mutableStateOf("") }

    // ── Dialog states ─────────────────────────────────────────────────────────
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var confirmChurchName  by remember { mutableStateOf("") }
    var showAddDialog      by remember { mutableStateOf(false) }
    var newMemberEmail     by remember { mutableStateOf("") }
    var newMemberName      by remember { mutableStateOf("") }
    var memberToRemove     by remember { mutableStateOf<Membership?>(null) }
    var memberToToggleRole by remember { mutableStateOf<Membership?>(null) }

    LaunchedEffect(settingsState.deleteSuccess) {
        if (settingsState.deleteSuccess) authViewModel.logout()
    }
    LaunchedEffect(authState.churchId, isAdmin) {
        if (isAdmin && authState.churchId.isNotEmpty()) {
            settingsViewModel.loadMembers(authState.churchId)
            tagViewModel.loadTags(authState.churchId)
        }
    }

    // ── Delete Church dialog ──────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; confirmChurchName = "" },
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
            title = { Text("Delete Church", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(buildAnnotatedString {
                        append("This permanently deletes ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("everything") }
                        append(" for \"")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(authState.churchId) }
                        append("\":\n\n• All members\n• All songs\n• All worship sets\n• All sessions\n\n")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)) { append("Cannot be undone.") }
                    }, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value         = confirmChurchName,
                        onValueChange = { confirmChurchName = it.lowercase() },
                        label         = { Text("Type \"${authState.churchId}\" to confirm") },
                        singleLine    = true,
                        isError       = confirmChurchName.isNotEmpty() && confirmChurchName != authState.churchId,
                        supportingText = {
                            if (confirmChurchName.isNotEmpty() && confirmChurchName != authState.churchId)
                                Text("Doesn't match", color = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick  = { showDeleteDialog = false; confirmChurchName = ""; settingsViewModel.deleteChurch(authState.churchId) },
                    enabled  = confirmChurchName == authState.churchId && !settingsState.isDeleting,
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false; confirmChurchName = "" }) { Text("Cancel") } }
        )
    }

    // ── Add Member dialog ─────────────────────────────────────────────────────
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newMemberEmail = ""; newMemberName = ""; settingsViewModel.clearError() },
            icon  = { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Add Member", fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Pre-add a member by email. They activate their account by registering via \"Join Church\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(value = newMemberName, onValueChange = { newMemberName = it },
                        label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newMemberEmail, onValueChange = { newMemberEmail = it },
                        label = { Text("Email") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth())
                    settingsState.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    onClick  = { settingsViewModel.addMember(newMemberEmail, newMemberName, authState.churchId); if (settingsState.actionError == null) { showAddDialog = false; newMemberEmail = ""; newMemberName = "" } },
                    enabled  = newMemberEmail.isNotBlank() && newMemberName.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; newMemberEmail = ""; newMemberName = ""; settingsViewModel.clearError() }) { Text("Cancel") } }
        )
    }

    // ── Remove Member dialog ──────────────────────────────────────────────────
    memberToRemove?.let { target ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            icon  = { Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Remove Member", fontWeight = FontWeight.Bold) },
            text  = { Text("Remove \"${target.displayName.ifBlank { target.email }}\" from ${authState.churchId}? They will lose access.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(onClick = { settingsViewModel.removeMember(target, authState.churchId); memberToRemove = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Cancel") } }
        )
    }

    // ── Toggle Role dialog ────────────────────────────────────────────────────
    memberToToggleRole?.let { target ->
        val isPromoting = target.role == "member"
        AlertDialog(
            onDismissRequest = { memberToToggleRole = null },
            icon = { Icon(if (isPromoting) Icons.Default.AdminPanelSettings else Icons.Default.Person, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text(if (isPromoting) "Promote to Admin" else "Demote to Member", fontWeight = FontWeight.Bold) },
            text  = { Text(if (isPromoting) "\"${target.displayName.ifBlank { target.email }}\" will become an admin and can manage members." else "\"${target.displayName.ifBlank { target.email }}\" will become a regular member.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(onClick = {
                    if (isPromoting) settingsViewModel.promoteToAdmin(target, authState.churchId)
                    else settingsViewModel.demoteToMember(target, authState.churchId)
                    memberToToggleRole = null
                }) { Text(if (isPromoting) "Promote" else "Demote") }
            },
            dismissButton = { TextButton(onClick = { memberToToggleRole = null }) { Text("Cancel") } }
        )
    }

    // ── Screen ────────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.padding(padding).fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Section 1: Profile / Church ───────────────────────────────────
            item {
                SettingsSection(
                    title    = "Profile",
                    icon     = Icons.Default.Person,
                    expanded = profileExpanded,
                    onToggle = { profileExpanded = !profileExpanded }
                ) {
                    SettingsInfoRow(label = "Name",   value = authState.displayName.ifBlank { "—" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsInfoRow(label = "Email",  value = authState.email.ifBlank { "—" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsInfoRow(label = "Church", value = authState.churchId.ifBlank { "—" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsInfoRow(label = "Role") {
                        RoleBadge(role = authState.role)
                    }
                }
            }

            // ── Section 2: Appearance ─────────────────────────────────────────
            item {
                SettingsSection(
                    title    = "Appearance",
                    icon     = Icons.Default.Palette,
                    expanded = themeExpanded,
                    onToggle = { themeExpanded = !themeExpanded }
                ) {
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            "App Theme",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding        = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(AppTheme.entries) { theme ->
                                ThemeCard(theme = theme, isSelected = theme == currentTheme, onClick = { onThemeChange(theme) })
                            }
                        }
                    }
                }
            }

            // ── Section 3: Members (admin only) ───────────────────────────────
            if (isAdmin) {
                item {
                    SettingsSection(
                        title    = "Members  ·  ${settingsState.members.size}",
                        icon     = Icons.Default.Group,
                        expanded = membersExpanded,
                        onToggle = { membersExpanded = !membersExpanded },
                        action   = {
                            FilledTonalIconButton(
                                onClick = { settingsViewModel.clearError(); showAddDialog = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, "Add member", modifier = Modifier.size(18.dp))
                            }
                        }
                    ) {
                        when {
                            settingsState.isLoading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            settingsState.members.isEmpty() -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No members yet. Tap + to add one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            else -> Column {
                                settingsState.members.forEachIndexed { index, member ->
                                    MemberItem(
                                        member        = member,
                                        currentUserId = authState.userId,
                                        onToggleRole  = { memberToToggleRole = member },
                                        onRemove      = { memberToRemove = member }
                                    )
                                    if (index < settingsState.members.lastIndex)
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                        settingsState.actionError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                    }
                }

                // ── Section 4: Tags ────────────────────────────────────────────
                item {
                    SettingsSection(
                        title    = "Tags  ·  ${tagState.tags.size}",
                        icon     = Icons.Default.LocalOffer,
                        expanded = tagsExpanded,
                        onToggle = { tagsExpanded = !tagsExpanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // ── Add tag row ───────────────────────────────────
                            Row(
                                modifier          = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value         = newTagName,
                                    onValueChange = { newTagName = it; tagViewModel.clearError() },
                                    label         = { Text("New tag name") },
                                    singleLine    = true,
                                    isError       = tagState.error != null,
                                    modifier      = Modifier.weight(1f),
                                    shape         = RoundedCornerShape(12.dp)
                                )
                                FilledTonalIconButton(
                                    onClick  = {
                                        if (newTagName.isNotBlank()) {
                                            tagViewModel.addTag(
                                                name      = newTagName.trim(),
                                                churchId  = authState.churchId,
                                                onSuccess = { newTagName = "" }
                                            )
                                        }
                                    },
                                    enabled  = newTagName.isNotBlank(),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, "Add tag")
                                }
                            }

                            // ── Duplicate / error message ─────────────────────
                            tagState.error?.let {
                                Text(
                                    it,
                                    color    = MaterialTheme.colorScheme.error,
                                    style    = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // ── Tag chips list ────────────────────────────────
                            if (tagState.tags.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                tagState.tags.forEach { tag ->
                                    Row(
                                        modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                tag.name,
                                                modifier  = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                style     = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color     = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        IconButton(
                                            onClick  = { tagViewModel.deleteTag(tag.id, authState.churchId) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Delete tag",
                                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No tags yet. Create one above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Section 5: Danger Zone ─────────────────────────────────────
                item {
                    SettingsSection(
                        title       = "Danger Zone",
                        icon        = Icons.Default.Warning,
                        expanded    = dangerExpanded,
                        onToggle    = { dangerExpanded = !dangerExpanded },
                        tint        = MaterialTheme.colorScheme.error,
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Deleting the church is permanent and removes all songs, sets, members and sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick  = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled  = !settingsState.isDeleting,
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border   = ButtonDefaults.outlinedButtonBorder(enabled = !settingsState.isDeleting)
                            ) {
                                if (settingsState.isDeleting) {
                                    CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Deleting…")
                                } else {
                                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Delete Church & All Data", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Collapsible section card ──────────────────────────────────────────────────
@Composable
private fun SettingsSection(
    title:       String,
    icon:        ImageVector,
    expanded:    Boolean,
    onToggle:    () -> Unit,
    tint:        Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    action:      @Composable (() -> Unit)? = null,
    content:     @Composable () -> Unit
) {
    val chevronAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")

    Surface(
        shape  = RoundedCornerShape(16.dp),
        color  = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    action?.invoke()
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp).rotate(chevronAngle)
                    )
                }
            }

            // Animated content
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider()
                    content()
                }
            }
        }
    }
}

// ── Info row (label + value text or custom trailing) ─────────────────────────
@Composable
private fun SettingsInfoRow(
    label:    String,
    value:    String = "",
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (trailing != null) trailing()
        else Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Member list tile ──────────────────────────────────────────────────────────
@Composable
private fun MemberItem(
    member:        Membership,
    currentUserId: String,
    onToggleRole:  () -> Unit,
    onRemove:      () -> Unit
) {
    val isYou         = member.userId == currentUserId
    val isPending     = member.isPending
    val isMemberAdmin = member.role == "admin"
    var menuExpanded  by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier         = Modifier.size(38.dp).clip(CircleShape)
                .background(if (isMemberAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (member.displayName.ifBlank { member.email }).firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = if (isMemberAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))

        // Name + email
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(member.displayName.ifBlank { member.email }, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isYou)    Text("· You",    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                if (isPending) Text("· Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(member.email, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))

        // Role badge
        RoleBadge(role = member.role)

        // ⋮ menu
        if (!isYou) {
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (!isPending) {
                        DropdownMenuItem(
                            text        = { Text(if (isMemberAdmin) "Make Member" else "Make Admin", color = MaterialTheme.colorScheme.primary) },
                            leadingIcon = { Icon(if (isMemberAdmin) Icons.Default.Person else Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                            onClick     = { menuExpanded = false; onToggleRole() }
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text        = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                        onClick     = { menuExpanded = false; onRemove() }
                    )
                }
            }
        } else {
            Spacer(Modifier.width(36.dp))
        }
    }
}

// ── Theme card ────────────────────────────────────────────────────────────────
@Composable
private fun ThemeCard(theme: AppTheme, isSelected: Boolean, onClick: () -> Unit) {
    val gradientColors = themeGradients[theme] ?: listOf(Color.Gray)
    val accent         = themeAccents[theme]   ?: Color.Gray
    val isDark         = theme == AppTheme.NIGHTFALL
    val textColor      = if (isDark) Color.White else Color(0xFF222222)
    // Memoize: Brush creation is expensive; theme doesn't change during scroll
    val gradientBrush  = remember(theme) { Brush.linearGradient(gradientColors) }

    Column(modifier = Modifier.width(110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(gradientBrush)
                .then(if (isSelected) Modifier.border(2.5.dp, accent, RoundedCornerShape(14.dp)) else Modifier)
                .clickable { onClick() }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(textColor.copy(alpha = 0.20f)))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = if (isDark) 0.15f else 0.60f))) {
                        Column(Modifier.padding(6.dp)) {
                            Box(Modifier.fillMaxWidth(0.7f).height(5.dp).clip(RoundedCornerShape(2.dp)).background(textColor.copy(alpha = 0.5f)))
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.fillMaxWidth(0.45f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(textColor.copy(alpha = 0.25f)))
                        }
                    }
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(accent.copy(alpha = 0.25f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("● Live", fontSize = 8.sp, color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (isSelected) {
                Box(Modifier.padding(8.dp).size(20.dp).clip(CircleShape).background(accent).align(Alignment.TopEnd), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, null, tint = if (isDark) Color.Black else Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(theme.displayName, style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        Text(theme.subtitle, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

// ── Role badge ────────────────────────────────────────────────────────────────
@Composable
private fun RoleBadge(role: String) {
    val isAdmin = role == "admin"
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            role.replaceFirstChar { it.uppercase() },
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = if (isAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
