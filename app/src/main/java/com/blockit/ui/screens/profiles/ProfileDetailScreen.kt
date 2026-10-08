package com.blockit.ui.screens.profiles

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockit.domain.matcher.DomainMatcher
import com.blockit.domain.model.BlockMode
import com.blockit.domain.model.BlockedApp
import com.blockit.domain.model.BlockedDomain
import com.blockit.ui.components.FocusAppIcon
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusEmptyState
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

@Composable
fun ProfileDetailScreen(
    viewModel: ProfileDetailViewModel,
    onBackClick: () -> Unit,
    onAddAppsClick: (Long) -> Unit,
    onConfigureSchedulesClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    val profile by viewModel.profile.collectAsState()
    val blockedApps by viewModel.blockedApps.collectAsState()
    val blockedDomains by viewModel.blockedDomains.collectAsState()
    val currentMode = profile?.blockMode ?: BlockMode.BLACKLIST
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddDomainDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            FocusTopBar(
                title = profile?.name ?: "Profile details",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { profile?.let { onConfigureSchedulesClick(it.id) } }) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = "Schedules",
                            tint = colors.textPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode Selector Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.sm),
                shape = FocusShapes.medium,
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(FocusSpacing.md)) {
                    Text(
                        text = "Blocking mode",
                        style = FocusTypography.label,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(FocusSpacing.xs))
                    Text(
                        text = if (currentMode == BlockMode.BLACKLIST) "Blocklist" else "Allowlist",
                        style = FocusTypography.title,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (currentMode == BlockMode.BLACKLIST)
                            "Phone operates normally, except selected apps and websites are blocked."
                        else
                            "Phone is locked down into a distraction-free kiosk. Only permitted apps can run.",
                        style = FocusTypography.caption,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(FocusSpacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                    ) {
                        Surface(
                            onClick = { viewModel.setBlockMode(BlockMode.BLACKLIST) },
                            shape = FocusShapes.small,
                            color = if (currentMode == BlockMode.BLACKLIST) colors.surfaceVariant else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (currentMode == BlockMode.BLACKLIST) colors.accent else colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = FocusSpacing.sm), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Blocklist",
                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (currentMode == BlockMode.BLACKLIST) colors.accent else colors.textPrimary
                                )
                            }
                        }
                        Surface(
                            onClick = { viewModel.setBlockMode(BlockMode.WHITELIST) },
                            shape = FocusShapes.small,
                            color = if (currentMode == BlockMode.WHITELIST) colors.surfaceVariant else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (currentMode == BlockMode.WHITELIST) colors.accent else colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = FocusSpacing.sm), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Allowlist",
                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (currentMode == BlockMode.WHITELIST) colors.accent else colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.background,
                contentColor = colors.accent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            if (currentMode == BlockMode.WHITELIST) "Allowed apps (${blockedApps.size})" else "Blocked apps (${blockedApps.size})",
                            style = FocusTypography.bodyMedium,
                            color = if (selectedTab == 0) colors.accent else colors.textSecondary
                        )
                    },
                    icon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = if (selectedTab == 0) colors.accent else colors.textSecondary) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Websites (${blockedDomains.size})",
                            style = FocusTypography.bodyMedium,
                            color = if (selectedTab == 1) colors.accent else colors.textSecondary
                        )
                    },
                    icon = { Icon(Icons.Rounded.Language, contentDescription = null, tint = if (selectedTab == 1) colors.accent else colors.textSecondary) }
                )
            }

            when (selectedTab) {
                0 -> AppsTabContent(
                    apps = blockedApps,
                    isWhitelist = currentMode == BlockMode.WHITELIST,
                    onAddApps = { profile?.let { onAddAppsClick(it.id) } },
                    onRemoveApp = { viewModel.removeApp(it.packageName) }
                )
                1 -> WebsitesTabContent(
                    domains = blockedDomains,
                    onAddDomain = { showAddDomainDialog = true },
                    onRemoveDomain = { viewModel.removeDomain(it.id) }
                )
            }
        }

        if (showAddDomainDialog) {
            AddDomainDialog(
                onDismiss = { showAddDomainDialog = false },
                onConfirm = { pattern, isWildcard ->
                    viewModel.addDomain(pattern, isWildcard)
                    showAddDomainDialog = false
                }
            )
        }
    }
}

@Composable
private fun AppsTabContent(
    apps: List<BlockedApp>,
    isWhitelist: Boolean,
    onAddApps: () -> Unit,
    onRemoveApp: (BlockedApp) -> Unit
) {
    val colors = FocusTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FocusSpacing.md)
    ) {
        FocusButton(
            text = if (isWhitelist) "Add permitted apps" else "Add apps to block",
            leadingIcon = Icons.Rounded.Add,
            onClick = onAddApps,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(FocusSpacing.md))

        if (apps.isEmpty()) {
            FocusEmptyState(
                title = if (isWhitelist) "No permitted apps" else "No apps blocked",
                message = if (isWhitelist)
                    "In allowlist mode, only default phone and permitted apps can run."
                else
                    "Add distracting apps you want blocked during focus sessions.",
                icon = Icons.Rounded.PhoneAndroid,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = FocusShapes.medium,
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FocusSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FocusAppIcon(packageName = app.packageName, size = 36.dp)
                            Spacer(modifier = Modifier.width(FocusSpacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = app.packageName,
                                    style = FocusTypography.caption,
                                    color = colors.textTertiary
                                )
                            }
                            IconButton(onClick = { onRemoveApp(app) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Remove",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebsitesTabContent(
    domains: List<BlockedDomain>,
    onAddDomain: () -> Unit,
    onRemoveDomain: (BlockedDomain) -> Unit
) {
    val colors = FocusTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FocusSpacing.md)
    ) {
        FocusButton(
            text = "Add website rule",
            leadingIcon = Icons.Rounded.Add,
            onClick = onAddDomain,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(FocusSpacing.md))

        if (domains.isEmpty()) {
            FocusEmptyState(
                title = "No website rules",
                message = "Block distracting websites by adding domain names or patterns.",
                icon = Icons.Rounded.Language,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
            ) {
                items(domains, key = { it.id }) { domain ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = FocusShapes.medium,
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FocusSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = domain.pattern,
                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (domain.isWildcard) "Wildcard pattern" else "Domain and subdomains",
                                    style = FocusTypography.caption,
                                    color = colors.accent
                                )
                            }
                            IconButton(onClick = { onRemoveDomain(domain) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Remove",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDomainDialog(
    onDismiss: () -> Unit,
    onConfirm: (pattern: String, isWildcard: Boolean) -> Unit
) {
    val colors = FocusTheme.colors
    var rawInput by remember { mutableStateOf("") }
    val cleanHost = remember(rawInput) { DomainMatcher.extractCleanHost(rawInput) }
    val isWildcard = remember(rawInput) { rawInput.contains('*') }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FocusShapes.large,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Block website",
                style = FocusTypography.title,
                color = colors.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a domain (e.g. reddit.com) or pattern (*gambling*, *.twitter.com):",
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(FocusSpacing.sm))
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    label = { Text("Domain or pattern") },
                    placeholder = { Text("e.g. youtube.com") },
                    singleLine = true,
                    shape = FocusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )

                if (rawInput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(FocusSpacing.sm))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = FocusShapes.small,
                        color = colors.surfaceVariant
                    ) {
                        Text(
                            text = if (isWildcard) {
                                "Rule: Matches pattern '$rawInput'"
                            } else {
                                "Rule: Blocks '$cleanHost' and all its subdomains"
                            },
                            style = FocusTypography.caption,
                            color = colors.accent,
                            modifier = Modifier.padding(FocusSpacing.sm)
                        )
                    }
                }
            }
        },
        confirmButton = {
            FocusButton(
                text = "Add rule",
                onClick = {
                    val finalPattern = if (isWildcard) rawInput.trim().lowercase() else cleanHost
                    if (finalPattern.isNotBlank()) {
                        onConfirm(finalPattern, isWildcard)
                    }
                },
                enabled = rawInput.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = FocusShapes.small
            ) {
                Text(
                    text = "Cancel",
                    style = FocusTypography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    )
}
