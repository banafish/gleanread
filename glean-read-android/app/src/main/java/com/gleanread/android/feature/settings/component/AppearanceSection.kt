package com.gleanread.android.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.data.appearance.ThemeColor
import com.gleanread.android.data.appearance.ThemeMode
import kotlin.math.cos
import kotlin.math.sin

/**
 * 外观设置部分，包含深色模式切换和主题色选择
 */
@Composable
fun AppearanceSection(
    themeMode: ThemeMode,
    themeColor: ThemeColor,
    onThemeModeChange: (ThemeMode) -> Unit,
    onThemeColorChange: (ThemeColor) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 外观标题
        Text(
            text = stringResource(R.string.settings_appearance_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // 深色模式选择卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeModeItem(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    icon = Icons.Default.LightMode,
                    label = stringResource(R.string.settings_theme_mode_light),
                    modifier = Modifier.weight(1f)
                )
                ThemeModeItem(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    icon = Icons.Default.DarkMode,
                    label = stringResource(R.string.settings_theme_mode_dark),
                    modifier = Modifier.weight(1f)
                )
                ThemeModeItem(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    icon = Icons.Default.Colorize,
                    label = stringResource(R.string.settings_theme_mode_system),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 主题色选择卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_theme_color_title),
                    style = MaterialTheme.typography.titleMedium
                )

                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(ThemeColor.entries.size) { index ->
                        val color = ThemeColor.entries[index]
                        ThemeColorItem(
                            themeColor = color,
                            selected = themeColor == color,
                            onClick = { onThemeColorChange(color) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ThemeColorItem(
    themeColor: ThemeColor,
    selected: Boolean,
    onClick: () -> Unit
) {
    val displayColor = when (themeColor) {
        ThemeColor.DYNAMIC -> MaterialTheme.colorScheme.primary
        ThemeColor.OCEAN -> Color(0xFF1E5D7B)
        ThemeColor.PURPLE -> Color(0xFF6750A4)
        ThemeColor.FOREST -> Color(0xFF386A20)
        ThemeColor.SAKURA -> Color(0xFF984061)
        ThemeColor.AMBER -> Color(0xFF785900)
        ThemeColor.GRAPHITE -> Color(0xFF476810)
    }

    val label = when (themeColor) {
        ThemeColor.DYNAMIC -> stringResource(R.string.settings_theme_color_dynamic)
        ThemeColor.OCEAN -> stringResource(R.string.settings_theme_color_ocean)
        ThemeColor.PURPLE -> stringResource(R.string.settings_theme_color_purple)
        ThemeColor.FOREST -> stringResource(R.string.settings_theme_color_forest)
        ThemeColor.SAKURA -> stringResource(R.string.settings_theme_color_sakura)
        ThemeColor.AMBER -> stringResource(R.string.settings_theme_color_amber)
        ThemeColor.GRAPHITE -> stringResource(R.string.settings_theme_color_graphite)
    }

    val shape = if (selected) ScallopedShape() else CircleShape

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(shape)
                .background(displayColor),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 扇形/波浪形形状，用于主题色选中效果
 */
class ScallopedShape(private val segments: Int = 10) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val radius = size.minDimension / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        
        // 使用正弦波模拟波浪边缘
        val numPoints = 120
        for (i in 0 until numPoints) {
            val angle = (i.toFloat() / numPoints) * 2f * Math.PI.toFloat()
            // 深度随角度变化
            val variation = 0.08f * radius * sin(angle * segments)
            val r = radius + variation
            
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
