package com.calisthenics.ai.ui.navigation

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.LocalAppColors
import com.calisthenics.ai.ui.theme.SecondaryText

data class FloatingBottomBarItem(
    val route: String,
    val icon: ImageVector,
    val label: String? = null,
    val badgeCount: Int = 0,
    val animatedRainbow: Boolean = false
)

object FloatingBottomBarDefaults {
    val Height = 64.dp
    val HorizontalMargin = 16.dp
    val BottomMargin = 14.dp
    val CornerRadius = 28.dp
    const val GlassOpacity = 0.80f
    val ContentBottomPadding = 108.dp
}

/**
 * Apple-inspired floating translucent glass bottom navigation bar.
 *
 * It is rendered as an overlay above the NavHost content without consuming layout space,
 * allowing background content and scrollable lists to extend edge-to-edge behind the glass.
 */
@Composable
fun FloatingBottomBar(
    items: List<FloatingBottomBarItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = FloatingBottomBarDefaults.HorizontalMargin,
    bottomMargin: Dp = FloatingBottomBarDefaults.BottomMargin,
    glassOpacity: Float = FloatingBottomBarDefaults.GlassOpacity,
    showLabels: Boolean = true
) {
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    AnimatedVisibility(
        visible = !isKeyboardVisible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = horizontalMargin)
                .padding(bottom = bottomMargin, top = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val appColors = LocalAppColors.current
            val isDark = appColors.isDark
            val pillShape = RoundedCornerShape(FloatingBottomBarDefaults.CornerRadius)

            // Apple Glass Bevel Border: Light catches the top edge, softens on sides and bottom
            val glassBorderBrush = if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        appColors.strongBorder.copy(alpha = 0.70f),
                        appColors.subtleBorder.copy(alpha = 0.55f),
                        appColors.subtleBorder.copy(alpha = 0.35f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        appColors.border.copy(alpha = 0.90f),
                        appColors.border.copy(alpha = 0.60f),
                        appColors.border.copy(alpha = 0.40f)
                    )
                )
            }

            // Frosted Translucent Glass Fill: Adapts to theme brightness
            val glassBackgroundBrush = if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        appColors.surfaceVariant.copy(alpha = glassOpacity),
                        appColors.surface.copy(alpha = (glassOpacity - 0.02f).coerceAtLeast(0.6f))
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        appColors.surface.copy(alpha = 0.95f),
                        appColors.surfaceVariant.copy(alpha = 0.97f)
                    )
                )
            }

            val highlightBrush = if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        appColors.primaryText.copy(alpha = 0.07f),
                        appColors.primaryText.copy(alpha = 0.01f),
                        Color.Transparent
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 8.dp else 10.dp,
                        shape = pillShape,
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f),
                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.06f)
                    )
                    .clip(pillShape)
                    .background(glassBackgroundBrush)
                    .border(BorderStroke(1.dp, glassBorderBrush), pillShape)
            ) {
                // Subtle Specular Top Highlight Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(highlightBrush)
                )

                // Navigation Items Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FloatingBottomBarDefaults.Height)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        FloatingBottomBarItemView(
                            item = item,
                            selected = isSelected,
                            showLabel = showLabels,
                            onClick = { onItemClick(item.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingBottomBarItemView(
    item: FloatingBottomBarItem,
    selected: Boolean,
    showLabel: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val accentColor = appColors.accent
    val unselectedColor = if (appColors.isDark) {
        appColors.secondaryText.copy(alpha = 0.70f)
    } else {
        appColors.secondaryText.copy(alpha = 0.95f)
    }

    val rainbowColors = remember(appColors) {
        if (appColors.isDark) {
            listOf(
                appColors.error,
                appColors.warning,
                appColors.success,
                appColors.info,
                appColors.accent,
                appColors.info,
                appColors.success,
                appColors.warning,
                appColors.error
            )
        } else {
            listOf(
                Color(0xFFFF5F6D),
                Color(0xFFFFB347),
                Color(0xFFFFE66D),
                Color(0xFF72D572),
                Color(0xFF4DD0E1),
                Color(0xFF5C9DFF),
                Color(0xFF9B7BFF),
                Color(0xFFFF6EC7),
                Color(0xFFFF5F6D)
            )
        }
    }
    val rainbowProgressState = if (item.animatedRainbow && ValueAnimator.areAnimatorsEnabled()) {
        val rainbowTransition = rememberInfiniteTransition(label = "adminRainbow")
        rainbowTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8_000, easing = LinearEasing)
            ),
            label = "adminRainbowProgress"
        )
    } else {
        null
    }

    val iconColor by animateColorAsState(
        targetValue = if (selected) accentColor else unselectedColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "iconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) accentColor else unselectedColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "labelColor"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = if (appColors.isDark) 0.16f else 0.12f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "indicatorColor"
    )
    val indicatorBorderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = if (appColors.isDark) 0.32f else 0.25f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "indicatorBorderColor"
    )
    val indicatorScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.88f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "indicatorScale"
    )

    val itemContentDescription = buildString {
        append(item.label ?: item.route)
        if (item.badgeCount > 0) {
            append(", ${item.badgeCount} pending notifications")
        }
        if (selected) {
            append(", selected")
        }
    }

    val capsuleShape = RoundedCornerShape(14.dp)
    val itemClickModifier = if (item.animatedRainbow) {
        Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .then(itemClickModifier)
            .semantics {
                this.role = Role.Tab
                this.selected = selected
                this.contentDescription = itemContentDescription
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (item.animatedRainbow) {
            val phase = rainbowProgressState?.value ?: 0f
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = appColors.primaryText,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithCache {
                        val rainbowBrush = Brush.linearGradient(
                            colors = rainbowColors,
                            start = androidx.compose.ui.geometry.Offset(phase * size.width, 0f),
                            end = androidx.compose.ui.geometry.Offset((phase + 1f) * size.width, size.height)
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(brush = rainbowBrush, blendMode = BlendMode.SrcIn)
                        }
                    }
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(28.dp)
                    .width(44.dp)
                    .graphicsLayer {
                        scaleX = indicatorScale
                        scaleY = indicatorScale
                    }
                    .clip(capsuleShape)
                    .background(indicatorColor)
                    .border(BorderStroke(1.dp, indicatorBorderColor), capsuleShape)
            ) {
                if (item.badgeCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = Error,
                                contentColor = appColors.primaryText
                            ) {
                                Text(
                                    text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (showLabel && !item.label.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            if (item.animatedRainbow) {
                val phase = rainbowProgressState?.value ?: 0f
                Text(
                    text = item.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = rainbowColors,
                            start = androidx.compose.ui.geometry.Offset(phase * 36f, 0f),
                            end = androidx.compose.ui.geometry.Offset((phase + 1f) * 36f, 12f)
                        ),
                        fontSize = 10.sp,
                        letterSpacing = (-0.2).sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            } else {
                Text(
                    text = item.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    letterSpacing = (-0.2).sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = labelColor
                )
            }
        }
    }
}
