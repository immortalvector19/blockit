package com.blockit.ui.screens.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blockit.core.system.SystemAppResolver
import com.blockit.data.repository.InstalledAppsRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.InstalledAppInfo
import com.blockit.ui.components.FocusAppIcon
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import kotlinx.coroutines.launch

@Composable
fun AlwaysAllowedAppsScreen(
    systemAppResolver: SystemAppResolver,
    installedAppsRepository: InstalledAppsRepository,
    ruleEvaluator: RuleEvaluator,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var alwaysAllowedSet by remember { mutableStateOf(systemAppResolver.getUserAlwaysAllowedPackages()) }

    val defaultDialer = remember { systemAppResolver.getDefaultDialerPackage() }

    LaunchedEffect(Unit) {
        val loaded = installedAppsRepository.getInstalledApps()
        apps = loaded
        isLoading = false
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            FocusTopBar(
                title = "Always allowed apps",
                onBackClick = onBackClick
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = FocusSpacing.lg)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FocusSpacing.sm),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = FocusShapes.medium,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(FocusSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.sm))
                    Column {
                        Text(
                            text = "Essential and emergency apps",
                            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Selected apps remain usable at all times, including during lockdown whitelist sessions.",
                            style = FocusTypography.caption,
                            color = colors.textMuted
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FocusSpacing.xs),
                placeholder = { Text("Search installed apps", style = FocusTypography.bodySmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = FocusShapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isDialer = app.packageName == defaultDialer
                        val isChecked = isDialer || alwaysAllowedSet.contains(app.packageName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isDialer) {
                                        val updated = alwaysAllowedSet.toMutableSet()
                                        if (isChecked) {
                                            updated.remove(app.packageName)
                                        } else {
                                            updated.add(app.packageName)
                                        }
                                        alwaysAllowedSet = updated
                                        systemAppResolver.setUserAlwaysAllowedPackages(updated)
                                        scope.launch {
                                            ruleEvaluator.refreshRules()
                                        }
                                    }
                                }
                                .padding(vertical = FocusSpacing.sm, horizontal = FocusSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FocusAppIcon(packageName = app.packageName, modifier = Modifier.size(36.dp))

                            Spacer(modifier = Modifier.width(FocusSpacing.sm))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = app.appName,
                                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isDialer) {
                                        Spacer(modifier = Modifier.width(FocusSpacing.xs))
                                        Surface(
                                            shape = FocusShapes.small,
                                            color = colors.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "Default dialer",
                                                style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                                                color = colors.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = app.packageName,
                                    style = FocusTypography.caption,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Checkbox(
                                checked = isChecked,
                                enabled = !isDialer, // Dialer is permanently protected
                                onCheckedChange = { checked ->
                                    if (!isDialer) {
                                        val updated = alwaysAllowedSet.toMutableSet()
                                        if (checked) {
                                            updated.add(app.packageName)
                                        } else {
                                            updated.remove(app.packageName)
                                        }
                                        alwaysAllowedSet = updated
                                        systemAppResolver.setUserAlwaysAllowedPackages(updated)
                                        scope.launch {
                                            ruleEvaluator.refreshRules()
                                        }
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.primary,
                                    uncheckedColor = colors.textMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
