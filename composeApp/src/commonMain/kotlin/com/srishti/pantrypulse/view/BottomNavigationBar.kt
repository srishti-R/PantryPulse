package com.srishti.pantrypulse.view

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srishti.pantrypulse.model.NavigationItem
import com.srishti.pantrypulse.model.Routes

@Composable
fun BottomNavigationBar(
    items: List<NavigationItem>,
    currentRoute: String?,
    onItemClick: (NavigationItem) -> Unit
) {
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }
    val targetProgress = if (selectedIndex == 0) 0f else 1f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .drawBehind {
                val width = size.width
                val height = size.height

                val baseLineY = 40.dp.toPx()

                val c1X = width * 0.25f
                val c2X = width * 0.75f

                val activeY = 10.dp.toPx()
                val inactiveY = 65.dp.toPx()

                val c1Y = activeY + (inactiveY - activeY) * animatedProgress
                val c2Y = inactiveY - (inactiveY - activeY) * animatedProgress

                val bgPath = Path().apply {
                    moveTo(0f, baseLineY)

                    // First curve (0f to c1X)
                    cubicTo(
                        x1 = width * 0.10f, y1 = baseLineY,
                        x2 = width * 0.15f, y2 = c1Y,
                        x3 = c1X, y3 = c1Y
                    )

                    // Second curve (c1X to c2X) - direct transition between peak and dip
                    cubicTo(
                        x1 = width * 0.40f, y1 = c1Y,
                        x2 = width * 0.60f, y2 = c2Y,
                        x3 = c2X, y3 = c2Y
                    )

                    // Third curve (c2X to width)
                    cubicTo(
                        x1 = width * 0.85f, y1 = c2Y,
                        x2 = width * 0.90f, y2 = baseLineY,
                        x3 = width, y3 = baseLineY
                    )

                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(path = bgPath, color = Color(0xFF0F172A)) // Slate 900

                // Draw top border following both curves
                val borderPath = Path().apply {
                    moveTo(0f, baseLineY)

                    // First curve (0f to c1X)
                    cubicTo(
                        x1 = width * 0.10f, y1 = baseLineY,
                        x2 = width * 0.15f, y2 = c1Y,
                        x3 = c1X, y3 = c1Y
                    )

                    // Second curve (c1X to c2X)
                    cubicTo(
                        x1 = width * 0.40f, y1 = c1Y,
                        x2 = width * 0.60f, y2 = c2Y,
                        x3 = c2X, y3 = c2Y
                    )

                    // Third curve (c2X to width)
                    cubicTo(
                        x1 = width * 0.85f, y1 = c2Y,
                        x2 = width * 0.90f, y2 = baseLineY,
                        x3 = width, y3 = baseLineY
                    )
                }
                drawPath(
                    path = borderPath,
                    color = Color(0xFF1E293B), // Slate 850 border
                    style = Stroke(width = 1.dp.toPx())
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { _, item ->
                val isSelected = currentRoute == item.route
                val isAdd = item.route == Routes.Add.route


                val itemScale = if (isAdd) {
                    1.12f - animatedProgress * 0.27f
                } else {
                    0.85f + animatedProgress * 0.27f
                }

                val itemOffsetY = if (isAdd) {
                    0.dp + (animatedProgress * 40).dp
                } else {
                    40.dp - (animatedProgress * 40).dp
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemClick(item) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .offset(y = itemOffsetY)
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                            }
                    ) {
                        val bgBrush = if (isSelected) {
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.Transparent)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 44.dp else 36.dp)
                                .background(
                                    brush = bgBrush,
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val iconVector = when (item.route) {
                                Routes.Add.route -> {
                                    Icons.Default.AddCircle
                                }
                                Routes.List.route -> {
                                    Icons.Default.CalendarToday
                                }
                                else -> {
                                    Icons.Default.AddCircle
                                }
                            }

                            Icon(
                                imageVector = iconVector,
                                contentDescription = item.title,
                                tint = if (isSelected) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF818CF8) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}