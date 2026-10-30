package com.engrshuvo.financemanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engrshuvo.financemanager.ui.state.FinanceTab
import com.engrshuvo.financemanager.ui.theme.IncomeGreen

/**
 * Modern Finance Bottom Navigation Bar
 * Follows the reference design:
 * - Wide, rounded dark charcoal container in dark theme (or crisp surface in light theme)
 * - Subtle border and clean Material 3 spacing
 * - Consistent modern line icons with active fill/tint
 * - Green accent for selected destination
 * - Slightly prominent, highlighted center Dashboard icon
 * - Accessible 48dp touch targets and navigation-bar insets padding
 */
@Composable
fun ModernBottomNavigationBar(
    activeTab: FinanceTab,
    onTabSelected: (FinanceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) Color(0xFF18181B) else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) Color(0xFF27272A) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = containerBg,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FinanceTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    val isCenter = tab == FinanceTab.DASHBOARD

                    ModernNavItem(
                        tab = tab,
                        isSelected = isSelected,
                        isCenter = isCenter,
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernNavItem(
    tab: FinanceTab,
    isSelected: Boolean,
    isCenter: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAccent = IncomeGreen
    val unselectedColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val icon: ImageVector = when (tab) {
        FinanceTab.REPORTS -> if (isSelected) Icons.Default.Analytics else Icons.Outlined.Analytics
        FinanceTab.BUDGET -> if (isSelected) Icons.Default.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
        FinanceTab.DASHBOARD -> if (isSelected) Icons.Default.Dashboard else Icons.Outlined.Dashboard
        FinanceTab.LOANS -> if (isSelected) Icons.Default.Handshake else Icons.Outlined.Handshake
        FinanceTab.MORE -> if (isSelected) Icons.Default.MoreHoriz else Icons.Outlined.MoreHoriz
    }

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navIconScale"
    )

    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) activeAccent else unselectedColor,
        label = "navIconColor"
    )

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .testTag("nav_tab_${tab.name.lowercase()}")
            .semantics {
                role = Role.Tab
                selected = isSelected
                contentDescription = "${tab.label} tab${if (isSelected) ", selected" else ""}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isCenter) {
            // Prominent center Dashboard container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            activeAccent.copy(alpha = 0.18f)
                        } else if (isDark) {
                            Color(0xFF27272A)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = animatedIconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // Standard item icon with subtle active pill highlight
            Box(
                modifier = Modifier
                    .scale(iconScale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) activeAccent.copy(alpha = 0.12f) else Color.Transparent
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = animatedIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (isSelected) 10.5.sp else 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) activeAccent else unselectedColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
