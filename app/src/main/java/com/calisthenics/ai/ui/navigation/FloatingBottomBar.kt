package com.calisthenics.ai.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.SecondaryText

data class FloatingBottomBarItem(
    val route: String,
    val icon: ImageVector,
    val label: String? = null,
    val badgeCount: Int = 0
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
            val pillShape = RoundedCornerShape(FloatingBottomBarDefaults.CornerRadius)

            // Apple Glass Bevel Border: Light catches the top edge (white 0.26f), softens on sides (0.10f) and bottom (0.04f)
            val glassBorderBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.26f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.04f)
                )
            )

            // Frosted Translucent Glass Fill: Top-to-bottom subtle luminosity
            val glassBackgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF262626).copy(alpha = glassOpacity),
                    Color(0xFF141414).copy(alpha = (glassOpacity - 0.02f).coerceAtLeast(0.6f))
                )
            )

            Box(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = pillShape,
                        spotColor = Color.Black.copy(alpha = 0.50f),
                        ambientColor = Color.Black.copy(alpha = 0.30f)
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
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.02f),
                                    Color.Transparent
                                )
                            )
                        )
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
    val accentColor = MaterialTheme.colorScheme.primary

    val iconColor by animateColorAsState(
        targetValue = if (selected) accentColor else SecondaryText.copy(alpha = 0.70f),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "iconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) accentColor else SecondaryText.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "labelColor"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "indicatorColor"
    )
    val indicatorBorderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.32f) else Color.Transparent,
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

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics {
                this.role = Role.Tab
                this.selected = selected
                this.contentDescription = itemContentDescription
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active indicator capsule behind icon
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
                            contentColor = Color.White
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

        if (showLabel && !item.label.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
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
