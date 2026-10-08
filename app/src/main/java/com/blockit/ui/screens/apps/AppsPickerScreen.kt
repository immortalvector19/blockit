package com.blockit.ui.screens.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blockit.ui.components.FocusAppIcon
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

@Composable
fun AppsPickerScreen(
    viewModel: AppsPickerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val colors = FocusTheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        topBar = {
            FocusTopBar(
                title = "Select apps",
                onBackClick = onBackClick,
                actions = {
                    if (state.selectedPackages.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text(
                                text = "Clear",
                                style = FocusTypography.bodyBold,
                                color = colors.accent
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.selectedPackages.isNotEmpty()) {
                val selectedCount = state.selectedPackages.size
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FocusSpacing.lg)
                ) {
                    FocusButton(
                        text = if (selectedCount == 1) "Add 1 selected app" else "Add $selectedCount selected apps",
                        onClick = {
                            viewModel.saveSelectedApps(onComplete = onBackClick)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search installed apps",
                        style = FocusTypography.body,
                        color = colors.textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = colors.textSecondary
                    )
                },
                singleLine = true,
                shape = FocusShapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.divider,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    cursorColor = colors.accent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.sm)
            )

            // Filter Chips & Select All
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.showSystemApps,
                    onClick = { viewModel.setShowSystemApps(!state.showSystemApps) },
                    label = {
                        Text(
                            text = "Include system apps",
                            style = FocusTypography.caption
                        )
                    },
                    shape = FocusShapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent.copy(alpha = 0.15f),
                        selectedLabelColor = colors.accent
                    )
                )
                Spacer(modifier = Modifier.width(FocusSpacing.sm))
                if (!state.isLoading && state.apps.isNotEmpty()) {
                    TextButton(onClick = { viewModel.selectAllVisible() }) {
                        Text(
                            text = "Select all (${state.apps.size})",
                            style = FocusTypography.captionBold,
                            color = colors.accent
                        )
                    }
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (state.apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No apps found",
                        style = FocusTypography.body,
                        color = colors.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(state.apps, key = { it.packageName }) { app ->
                        val isSelected = state.selectedPackages.contains(app.packageName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleAppSelection(app.packageName) }
                                .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FocusAppIcon(
                                packageName = app.packageName,
                                appName = app.appName,
                                size = 40.dp
                            )

                            Spacer(modifier = Modifier.width(FocusSpacing.md))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = FocusTypography.bodyBold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (state.searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = app.packageName,
                                        style = FocusTypography.caption,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleAppSelection(app.packageName) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.accent,
                                    uncheckedColor = colors.textSecondary,
                                    checkmarkColor = colors.onAccent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
