package com.appclonex.presentation.screens.settings.accessibility

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appclonex.domain.model.FontSizeScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AccessibilityViewModel = hiltViewModel()
) {
    val settings by viewModel.state.collectAsState()

    val containerColor by animateColorAsState(
        targetValue = if (settings.highContrast) {
            Color(0xFF121212)
        } else {
            MaterialTheme.colorScheme.background
        },
        label = "containerColor"
    )

    val onSurfaceColor by animateColorAsState(
        targetValue = if (settings.highContrast) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "onSurfaceColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "无障碍设置",
                        color = onSurfaceColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = onSurfaceColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor
                )
            )
        },
        containerColor = containerColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Font Size Section ──────────────────────────────────
            SettingsSectionCard(
                title = "字体大小",
                icon = Icons.Default.FormatSize,
                highContrast = settings.highContrast,
                onSurfaceColor = onSurfaceColor
            ) {
                FontSizeSlider(
                    currentScale = settings.fontSizeScale,
                    onScaleChange = { viewModel.setFontSizeScale(it) },
                    boldText = settings.boldText,
                    highContrast = settings.highContrast,
                    onSurfaceColor = onSurfaceColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                FontSizeChips(
                    currentScale = settings.fontSizeScale,
                    onScaleSelected = { viewModel.setFontSizeByEnum(it) },
                    highContrast = settings.highContrast,
                    onSurfaceColor = onSurfaceColor
                )
            }

            // ── Display Section ────────────────────────────────────
            SettingsSectionCard(
                title = "显示",
                icon = Icons.Default.HighQuality,
                highContrast = settings.highContrast,
                onSurfaceColor = onSurfaceColor
            ) {
                SettingsToggleRow(
                    title = "高对比度",
                    subtitle = "增强文字与背景的对比度",
                    checked = settings.highContrast,
                    onCheckedChange = { viewModel.toggleHighContrast() },
                    highContrast = settings.highContrast,
                    onSurfaceColor = onSurfaceColor
                )

                SettingsToggleRow(
                    title = "粗体文字",
                    subtitle = "使用粗体显示所有文字",
                    checked = settings.boldText,
                    onCheckedChange = { viewModel.toggleBoldText() },
                    highContrast = settings.highContrast,
                    onSurfaceColor = onSurfaceColor
                )

                SettingsToggleRow(
                    title = "色盲友好模式",
                    subtitle = "使用对色觉障碍用户友好的配色方案",
                    checked = settings.colorBlindFriendly,
                    onCheckedChange = { viewModel.toggleColorBlindFriendly() },
                    highContrast = settings.highContrast,
                    onSurfaceColor = onSurfaceColor
                )
            }

            // ── Interaction Section ────────────────────────────────
            SettingsSectionCard(
                title = "交互",
                icon = Icons.Default.TouchApp,
                highContrast = settings.highContrast,
                onSurfaceColor = onSurfaceColor
            ) {
                SettingsToggleRow(
                    title = "减少动画",
                    subtitle = "减少或关闭界面动画效果",
                    checked = settings.reduceMotion,
                    onCheckedChange = { viewModel.toggleReduceMotion() },
                    highContrast = settings.highContrast,
                    onSurfaceColor = onSurfaceColor
                )

                SettingsToggleRow(
                    title = "屏幕朗读描述",
                    subtitle = "为界面元素提供屏幕朗读器描述",
                    checked = settings.screenReaderDescriptions,
                    onCheckedChange = { viewModel.toggleScreenReaderDescriptions() },
                    highContrast = settings.highContrast,
                    onSurfaceColor = onSurfaceColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                TouchTargetSizeSelector(
                    currentSize = settings.touchTargetSize,
                    onSizeSelected = { viewModel.setTouchTargetSize(it) },
                    highContrast = settings.highContrast,
                    onSurfaceColor = onSurfaceColor
                )
            }

            // ── Preview Section ────────────────────────────────────
            SettingsSectionCard(
                title = "预览效果",
                icon = Icons.Default.Palette,
                highContrast = settings.highContrast,
                onSurfaceColor = onSurfaceColor
            ) {
                AccessibilityPreviewCard(settings = settings)
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Section Card ──────────────────────────────────────────────────

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highContrast: Boolean,
    onSurfaceColor: Color,
    content: @Composable () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (highContrast) {
            Color(0xFF1E1E1E)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "cardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (highContrast) 0.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (highContrast) Color(0xFF64B5F6) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = onSurfaceColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

// ── Toggle Row ────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    highContrast: Boolean,
    onSurfaceColor: Color
) {
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = if (highContrast) Color(0xFF64B5F6) else MaterialTheme.colorScheme.primary,
        checkedTrackColor = if (highContrast) Color(0xFF64B5F6).copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.primaryContainer,
        uncheckedThumbColor = if (highContrast) Color.White else MaterialTheme.colorScheme.outline,
        uncheckedTrackColor = if (highContrast) Color(0xFF424242) else MaterialTheme.colorScheme.surfaceVariant
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics {
                contentDescription = "$title, $subtitle, ${if (checked) "已开启" else "已关闭"}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = onSurfaceColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceColor.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = switchColors
        )
    }
}

// ── Font Size Slider ──────────────────────────────────────────────

@Composable
private fun FontSizeSlider(
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    boldText: Boolean,
    highContrast: Boolean,
    onSurfaceColor: Color
) {
    val sliderPosition = when (currentScale) {
        FontSizeScale.SMALL.scale -> 0f
        FontSizeScale.DEFAULT.scale -> 1f / 3f
        FontSizeScale.LARGE.scale -> 2f / 3f
        FontSizeScale.EXTRA_LARGE.scale -> 1f
        else -> 1f / 3f
    }

    val label = FontSizeScale.entries.firstOrNull { it.scale == currentScale }?.label ?: "自定义"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "缩放比例",
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceColor
            )
            Text(
                text = "${label} (${String.format("%.0f%%", currentScale * 100)})",
                style = MaterialTheme.typography.bodyMedium,
                color = if (highContrast) Color(0xFF64B5F6) else MaterialTheme.colorScheme.primary,
                fontWeight = if (boldText) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = sliderPosition,
            onValueChange = { position ->
                val scale = when {
                    position < 1f / 6f -> FontSizeScale.SMALL.scale
                    position < 0.5f -> FontSizeScale.DEFAULT.scale
                    position < 5f / 6f -> FontSizeScale.LARGE.scale
                    else -> FontSizeScale.EXTRA_LARGE.scale
                }
                onScaleChange(scale)
            },
            steps = 2,
            colors = SliderDefaults.colors(
                thumbColor = if (highContrast) Color(0xFF64B5F6) else MaterialTheme.colorScheme.primary,
                activeTrackColor = if (highContrast) Color(0xFF64B5F6) else MaterialTheme.colorScheme.primary,
                inactiveTrackColor = if (highContrast) Color(0xFF424242) else MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// ── Font Size Chips ───────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontSizeChips(
    currentScale: Float,
    onScaleSelected: (FontSizeScale) -> Unit,
    highContrast: Boolean,
    onSurfaceColor: Color
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FontSizeScale.entries.forEach { size ->
            val selected = size.scale == currentScale
            FilterChip(
                selected = selected,
                onClick = { onScaleSelected(size) },
                label = {
                    Text(
                        text = size.label,
                        color = if (selected) {
                            if (highContrast) Color.Black else MaterialTheme.colorScheme.onPrimary
                        } else {
                            onSurfaceColor
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (highContrast) Color(0xFF64B5F6)
                    else MaterialTheme.colorScheme.primary,
                    selectedLabelColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.onPrimary
                ),
                border = if (!selected && highContrast) {
                    FilterChipDefaults.filterChipBorder(borderColor = Color(0xFF757575), borderWidth = 1.dp)
                } else {
                    FilterChipDefaults.filterChipBorder(enabled = true, selected = selected)
                }
            )
        }
    }
}

// ── Touch Target Size Selector ────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TouchTargetSizeSelector(
    currentSize: Int,
    onSizeSelected: (Int) -> Unit,
    highContrast: Boolean,
    onSurfaceColor: Color
) {
    val sizes = listOf(48, 56, 64)

    Column {
        Text(
            text = "触摸目标大小",
            style = MaterialTheme.typography.bodyMedium,
            color = onSurfaceColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sizes.forEach { size ->
                val selected = size == currentSize
                FilterChip(
                    selected = selected,
                    onClick = { onSizeSelected(size) },
                    label = {
                        Text(
                            text = "${size}dp",
                            color = if (selected) {
                                if (highContrast) Color.Black else MaterialTheme.colorScheme.onPrimary
                            } else {
                                onSurfaceColor
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (highContrast) Color(0xFF64B5F6)
                        else MaterialTheme.colorScheme.primary,
                        selectedLabelColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.onPrimary
                    ),
                    border = if (!selected && highContrast) {
                        FilterChipDefaults.filterChipBorder(borderColor = Color(0xFF757575), borderWidth = 1.dp)
                    } else {
                        FilterChipDefaults.filterChipBorder(enabled = true, selected = selected)
                    }
                )
            }
        }

        // Visual preview of touch target size
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val targetColor = if (highContrast) Color(0xFF64B5F6) else MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(currentSize.dp)
                    .clip(CircleShape)
                    .background(targetColor.copy(alpha = 0.2f))
                    .border(
                        width = 2.dp,
                        color = targetColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${currentSize}",
                    style = MaterialTheme.typography.labelSmall,
                    color = targetColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "触摸目标预览",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceColor.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Preview Card ──────────────────────────────────────────────────

@Composable
private fun AccessibilityPreviewCard(
    settings: com.appclonex.domain.model.AccessibilitySettings
) {
    val previewFontSize by animateFloatAsState(
        targetValue = 16.sp.value * settings.fontSizeScale,
        label = "previewFontSize"
    )

    val previewTitleColor by animateColorAsState(
        targetValue = if (settings.highContrast) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "previewTitleColor"
    )

    val previewBodyColor by animateColorAsState(
        targetValue = if (settings.highContrast) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        label = "previewBodyColor"
    )

    val previewBackgroundColor by animateColorAsState(
        targetValue = if (settings.highContrast) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.surfaceVariant,
        label = "previewBackgroundColor"
    )

    val accentColor = if (settings.colorBlindFriendly) {
        Color(0xFF0D47A1) // Blue-orange safe palette
    } else {
        if (settings.highContrast) Color(0xFF64B5F6) else MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = previewBackgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title preview
            Text(
                text = "示例标题文本",
                fontSize = (previewFontSize * 1.25f).sp,
                fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                color = previewTitleColor
            )

            // Body preview with annotated string
            val annotatedBody = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontSize = previewFontSize.sp,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                        color = previewBodyColor
                    )
                ) {
                    append("这是一段示例正文内容，用于展示当前无障碍设置的效果。")
                }
                withStyle(
                    SpanStyle(
                        fontSize = previewFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                ) {
                    append(" 强调文字")
                }
                withStyle(
                    SpanStyle(
                        fontSize = previewFontSize.sp,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                        color = previewBodyColor
                    )
                ) {
                    append(" 使用了对比色显示。")
                }
            }
            Text(text = annotatedBody)

            // Button-like preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(settings.touchTargetSize.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "示例按钮 (${settings.touchTargetSize}dp)",
                    fontSize = (previewFontSize * 0.9f).sp,
                    fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White
                )
            }

            // Link preview
            Text(
                text = "示例链接文本",
                fontSize = previewFontSize.sp,
                fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                color = accentColor
            )

            // Caption
            Text(
                text = "字体缩放: ${String.format("%.0f%%", settings.fontSizeScale * 100)} | " +
                        "对比度: ${if (settings.highContrast) "高" else "标准"} | " +
                        "粗体: ${if (settings.boldText) "是" else "否"} | " +
                        "动画: ${if (settings.reduceMotion) "减少" else "标准"}",
                fontSize = (previewFontSize * 0.75f).sp,
                color = previewBodyColor.copy(alpha = 0.5f)
            )
        }
    }
}
