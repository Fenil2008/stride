package com.example.ui.navigation

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoolGrey
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostedGlassShadow
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.ObsidianBlack
import kotlin.math.abs
import kotlin.math.max

data class NavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

val NAV_ITEMS = listOf(
    NavItem(Screen.Feed, "Home", Icons.Default.Home),
    NavItem(Screen.Explore, "Maps", Icons.Default.Explore),
    NavItem(Screen.Record, "Record", Icons.Default.DirectionsRun),
    NavItem(Screen.History, "Past", Icons.Default.TrendingUp),
    NavItem(Screen.Profile, "You", Icons.Default.Person)
)

@Composable
fun LiquidGlassBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragX by remember { mutableFloatStateOf(-1f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("liquid_glass_bottom_bar")
    ) {
        // Frosted Glass Floating Pill Container (Matching Design HTML: bg-white/70 backdrop-blur-xl border border-white/40 shadow-[0_8px_32px_rgba(0,0,0,0.1)])
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    spotColor = FrostedGlassShadow,
                    ambientColor = Color(0x0D000000)
                )
                .clip(CircleShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = RenderEffect
                                .createBlurEffect(30f, 30f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE61A1D24), // Translucent Graphite Charcoal #1A1D24
                            Color(0xCC1A1D24)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0x3300E5C7), // Subtle Electric Cyan glass border
                    shape = CircleShape
                )
        )

        // Interactive Content Layer with Dock Magnification Gesture Detector
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragX = offset.x
                            isDragging = true
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragX = change.position.x
                        },
                        onDragEnd = {
                            isDragging = false
                            dragX = -1f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragX = -1f
                        }
                    )
                }
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val itemWidthPx = totalWidthPx / NAV_ITEMS.size

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NAV_ITEMS.forEachIndexed { index, item ->
                    val isSelected = currentRoute == item.screen.route
                    val itemCenterXPx = (index + 0.5f) * itemWidthPx

                    // Compute swipe magnification (Dock effect)
                    val targetMagnification = if (isDragging && dragX >= 0) {
                        val dist = abs(dragX - itemCenterXPx)
                        val maxInfluence = itemWidthPx * 1.5f
                        if (dist < maxInfluence) {
                            val factor = 1f - (dist / maxInfluence)
                            1.0f + (factor * factor * 0.38f)
                        } else {
                            1.0f
                        }
                    } else {
                        if (isSelected) 1.06f else 1.0f
                    }

                    val animatedScale by animateFloatAsState(
                        targetValue = targetMagnification,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "dock_scale"
                    )

                    val tintColor by animateColorAsState(
                        targetValue = when {
                            item.screen == Screen.Record -> ObsidianBlack
                            isSelected -> ElectricCyan
                            else -> CoolGrey
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "nav_tint"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = false, radius = 28.dp, color = ElectricCyan.copy(alpha = 0.2f)),
                                onClick = { onNavigate(item.screen) }
                            )
                            .testTag("nav_tab_${item.screen.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.screen == Screen.Record) {
                            // Central prominent action button in Electric Cyan with dark obsidian border and subtle cyan glow
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                    }
                                    .shadow(10.dp, CircleShape, spotColor = Color(0x8000E5C7))
                                    .border(3.dp, GraphiteCharcoal, CircleShape)
                                    .background(ElectricCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = ObsidianBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = tintColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = item.title.uppercase(),
                                    color = tintColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
