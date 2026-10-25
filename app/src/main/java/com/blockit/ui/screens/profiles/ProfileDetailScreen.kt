package com.blockit.ui.screens.profiles

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockit.core.overlay.OverlayImageManager
import com.blockit.domain.matcher.DomainMatcher
import com.blockit.domain.model.BlockMode
import com.blockit.domain.model.BlockedApp
import com.blockit.domain.model.BlockedDomain
import com.blockit.domain.model.OverlayBackgroundType
import com.blockit.domain.model.Profile
import com.blockit.ui.components.FocusAppIcon
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusEmptyState
import com.blockit.ui.components.FocusOutlinedButton
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.components.OverlayBackgroundView
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import com.blockit.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                            "Apps (${blockedApps.size})",
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
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Overlay",
                            style = FocusTypography.bodyMedium,
                            color = if (selectedTab == 2) colors.accent else colors.textSecondary
                        )
                    },
                    icon = { Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = if (selectedTab == 2) colors.accent else colors.textSecondary) }
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
                    onAddDomain = { pattern, isWildcard -> viewModel.addDomain(pattern, isWildcard) },
                    onRemoveDomain = { viewModel.removeDomain(it.id) }
                )
                2 -> OverlayTabContent(
                    profile = profile,
                    onSetBackgroundType = { viewModel.setOverlayBackgroundType(it) },
                    onSetCustomText = { viewModel.setOverlayCustomText(it) },
                    onUpdateSlotImage = { slot, path -> viewModel.updateOverlayImage(slot, path) },
                    onRemoveSlotImage = { slot -> viewModel.removeOverlayImage(slot) },
                    onSetMultipleImages = { paths -> viewModel.setMultipleOverlayImages(paths) }
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
    onAddDomain: (pattern: String, isWildcard: Boolean) -> Unit,
    onRemoveDomain: (BlockedDomain) -> Unit
) {
    val colors = FocusTheme.colors
    var domainInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FocusSpacing.md)
    ) {
        // Direct Domain Input Row: automatically cleans scheme, 'www.', path, query
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = domainInput,
                onValueChange = { domainInput = it },
                placeholder = { Text("e.g. reddit.com, youtube.com", color = colors.textMuted) },
                singleLine = true,
                shape = FocusShapes.small,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            FocusButton(
                text = "Add",
                onClick = {
                    val raw = domainInput.trim()
                    if (raw.isNotBlank()) {
                        val isWildcard = raw.contains('*')
                        val finalPattern = if (isWildcard) raw.lowercase() else DomainMatcher.extractCleanHost(raw)
                        if (finalPattern.isNotBlank()) {
                            onAddDomain(finalPattern, isWildcard)
                            domainInput = ""
                        }
                    }
                },
                enabled = domainInput.isNotBlank()
            )
        }

        Spacer(modifier = Modifier.height(FocusSpacing.md))

        if (domains.isEmpty()) {
            FocusEmptyState(
                title = "No website rules",
                message = "Block distracting websites by adding domain names above. Subdomains are automatically included.",
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
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier
                                    .padding(start = FocusSpacing.xs, end = FocusSpacing.sm)
                                    .size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = domain.pattern,
                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (domain.isWildcard) "Wildcard pattern" else "Domain and subdomains included",
                                    style = FocusTypography.caption,
                                    color = colors.textSecondary
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

@Composable
private fun OverlayTabContent(
    profile: Profile?,
    onSetBackgroundType: (OverlayBackgroundType) -> Unit,
    onSetCustomText: (String?) -> Unit,
    onUpdateSlotImage: (Int, String) -> Unit,
    onRemoveSlotImage: (Int) -> Unit,
    onSetMultipleImages: (List<String>) -> Unit
) {
    if (profile == null) return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = FocusTheme.colors

    var activeSlotForPicker by remember { mutableIntStateOf(0) }

    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val path = OverlayImageManager.saveImage(context, profile.id, activeSlotForPicker, uri)
                if (path.isNotBlank()) {
                    onUpdateSlotImage(activeSlotForPicker, path)
                }
            }
        }
    }

    val multiplePhotosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val paths = mutableListOf<String>()
                uris.take(4).forEachIndexed { index, uri ->
                    val path = OverlayImageManager.saveImage(context, profile.id, index, uri)
                    if (path.isNotBlank()) paths.add(path)
                }
                onSetMultipleImages(paths)
            }
        }
    }

    val overlayBitmaps by produceState<List<ImageBitmap?>>(
        initialValue = emptyList(),
        key1 = profile.overlayImageUris
    ) {
        value = withContext(Dispatchers.IO) {
            profile.overlayImageUris.map { path ->
                if (path.isNotBlank()) OverlayImageManager.loadBitmap(path) else null
            }
        }
    }

    var customMantraText by remember(profile.overlayCustomText) {
        mutableStateOf(profile.overlayCustomText ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FocusSpacing.md),
        verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
    ) {
        // 1. Background Style Selector Card
        Surface(
            shape = FocusShapes.medium,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(FocusSpacing.md)) {
                Text(
                    text = "Overlay Background",
                    style = FocusTypography.title,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Atmosphere displayed when opening a blocked app",
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(FocusSpacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                ) {
                    val currentType = profile.overlayBackgroundType
                    BackgroundTypeCard(
                        title = "Default",
                        icon = Icons.Rounded.PhoneAndroid,
                        isSelected = currentType == OverlayBackgroundType.DEFAULT,
                        onClick = { onSetBackgroundType(OverlayBackgroundType.DEFAULT) },
                        modifier = Modifier.weight(1f)
                    )
                    BackgroundTypeCard(
                        title = "Single Photo",
                        icon = Icons.Rounded.Image,
                        isSelected = currentType == OverlayBackgroundType.SINGLE_IMAGE,
                        onClick = { onSetBackgroundType(OverlayBackgroundType.SINGLE_IMAGE) },
                        modifier = Modifier.weight(1f)
                    )
                    BackgroundTypeCard(
                        title = "4-Photo Collage",
                        icon = Icons.Rounded.GridView,
                        isSelected = currentType == OverlayBackgroundType.COLLAGE_4,
                        onClick = { onSetBackgroundType(OverlayBackgroundType.COLLAGE_4) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Photo Upload & Management (Conditional on Type)
        when (profile.overlayBackgroundType) {
            OverlayBackgroundType.SINGLE_IMAGE -> {
                Surface(
                    shape = FocusShapes.medium,
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FocusSpacing.md)) {
                        Text(
                            text = "Background Photo",
                            style = FocusTypography.label,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(FocusSpacing.sm))

                        val singleBitmap = overlayBitmaps.firstOrNull { it != null }
                        if (singleBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(FocusShapes.medium)
                                    .border(1.dp, colors.border, FocusShapes.medium)
                            ) {
                                Image(
                                    bitmap = singleBitmap,
                                    contentDescription = "Background Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(FocusSpacing.sm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                            ) {
                                FocusOutlinedButton(
                                    text = "Change photo",
                                    onClick = {
                                        activeSlotForPicker = 0
                                        singlePhotoPicker.launch("image/*")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FocusOutlinedButton(
                                    text = "Remove",
                                    onClick = { onRemoveSlotImage(0) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Surface(
                                onClick = {
                                    activeSlotForPicker = 0
                                    singlePhotoPicker.launch("image/*")
                                },
                                shape = FocusShapes.medium,
                                color = colors.surfaceVariant,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(FocusSpacing.xs))
                                    Text(
                                        text = "Upload background photo",
                                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Tap to choose an image from gallery",
                                        style = FocusTypography.caption,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OverlayBackgroundType.COLLAGE_4 -> {
                Surface(
                    shape = FocusShapes.medium,
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FocusSpacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "4-Photo Collage Quad",
                                    style = FocusTypography.title,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Tap any slot to replace photo",
                                    style = FocusTypography.caption,
                                    color = colors.textSecondary
                                )
                            }
                            FocusOutlinedButton(
                                text = "Pick 4 at once",
                                onClick = { multiplePhotosPicker.launch("image/*") }
                            )
                        }

                        Spacer(modifier = Modifier.height(FocusSpacing.md))

                        // 2x2 Grid of slots
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                            ) {
                                CollageSlotCard(
                                    slotNumber = 1,
                                    bitmap = overlayBitmaps.getOrNull(0),
                                    onClick = {
                                        activeSlotForPicker = 0
                                        singlePhotoPicker.launch("image/*")
                                    },
                                    onRemove = { onRemoveSlotImage(0) },
                                    modifier = Modifier.weight(1f).height(110.dp)
                                )
                                CollageSlotCard(
                                    slotNumber = 2,
                                    bitmap = overlayBitmaps.getOrNull(1),
                                    onClick = {
                                        activeSlotForPicker = 1
                                        singlePhotoPicker.launch("image/*")
                                    },
                                    onRemove = { onRemoveSlotImage(1) },
                                    modifier = Modifier.weight(1f).height(110.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                            ) {
                                CollageSlotCard(
                                    slotNumber = 3,
                                    bitmap = overlayBitmaps.getOrNull(2),
                                    onClick = {
                                        activeSlotForPicker = 2
                                        singlePhotoPicker.launch("image/*")
                                    },
                                    onRemove = { onRemoveSlotImage(2) },
                                    modifier = Modifier.weight(1f).height(110.dp)
                                )
                                CollageSlotCard(
                                    slotNumber = 4,
                                    bitmap = overlayBitmaps.getOrNull(3),
                                    onClick = {
                                        activeSlotForPicker = 3
                                        singlePhotoPicker.launch("image/*")
                                    },
                                    onRemove = { onRemoveSlotImage(3) },
                                    modifier = Modifier.weight(1f).height(110.dp)
                                )
                            }
                        }
                    }
                }
            }

            OverlayBackgroundType.DEFAULT -> {
                // No upload needed for default solid theme
            }
        }

        // 3. Motivational Mantra / Quote ("Text over the text")
        Surface(
            shape = FocusShapes.medium,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(FocusSpacing.md)) {
                Text(
                    text = "Motivational Mantra",
                    style = FocusTypography.title,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Custom text overlaid prominently to break distraction autopilot",
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(FocusSpacing.md))

                OutlinedTextField(
                    value = customMantraText,
                    onValueChange = {
                        customMantraText = it
                        onSetCustomText(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Personal focus mantra or goal reminder") },
                    placeholder = { Text("e.g. Stay disciplined. Your future self is watching.") },
                    maxLines = 3,
                    shape = FocusShapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(FocusSpacing.sm))

                // Inspiration preset chips
                Text(
                    text = "Quick Inspiration:",
                    style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val presets = listOf(
                    "Stay disciplined",
                    "Focus on what matters",
                    "Discipline equals freedom",
                    "Your goals need you",
                    "One task at a time"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                ) {
                    items(presets) { preset ->
                        Surface(
                            onClick = {
                                customMantraText = preset
                                onSetCustomText(preset)
                            },
                            shape = FocusShapes.small,
                            color = colors.surfaceVariant,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = preset,
                                style = FocusTypography.caption,
                                color = colors.accent,
                                modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Live Realistic Preview Card
        Surface(
            shape = FocusShapes.medium,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(FocusSpacing.md)) {
                Text(
                    text = "Live Overlay Preview",
                    style = FocusTypography.title,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "How this profile's block screen appears on device",
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(FocusSpacing.md))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(FocusShapes.medium)
                        .border(1.dp, colors.border, FocusShapes.medium)
                ) {
                    OverlayBackgroundView(
                        backgroundType = profile.overlayBackgroundType,
                        bitmaps = overlayBitmaps,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(FocusSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = FocusShapes.small,
                            color = colors.surfaceVariant.copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = "${profile.name} is active",
                                style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = 2.dp)
                            )
                        }

                        if (customMantraText.isNotBlank()) {
                            Surface(
                                shape = FocusShapes.small,
                                color = Color.Black.copy(alpha = 0.60f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = FocusSpacing.sm)
                            ) {
                                Text(
                                    text = "“$customMantraText”",
                                    style = FocusTypography.caption.copy(
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = 6.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(FocusShapes.small)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, FocusShapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.HourglassTop,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Blocked App",
                                style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }

                        Surface(
                            shape = FocusShapes.small,
                            color = colors.accent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Resume Focus",
                                    style = FocusTypography.caption.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
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
private fun BackgroundTypeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    Surface(
        onClick = onClick,
        shape = FocusShapes.small,
        color = if (isSelected) colors.surfaceVariant else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = FocusSpacing.sm, horizontal = FocusSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) colors.accent else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = FocusTypography.caption.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) colors.accent else colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CollageSlotCard(
    slotNumber: Int,
    bitmap: ImageBitmap?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    Box(
        modifier = modifier
            .clip(FocusShapes.small)
            .border(1.dp, colors.border, FocusShapes.small)
            .background(colors.surfaceVariant)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Slot $slotNumber",
                modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Slot $slotNumber",
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
            }
        }
    }
}
