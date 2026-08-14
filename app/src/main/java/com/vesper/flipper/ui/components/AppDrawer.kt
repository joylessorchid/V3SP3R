package com.vesper.flipper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vesper.flipper.ui.theme.GlassFill2
import com.vesper.flipper.ui.theme.TextPrimary
import com.vesper.flipper.ui.theme.TextSecondary
import com.vesper.flipper.ui.theme.TextTertiary
import com.vesper.flipper.ui.theme.VesperAccent

/**
 * Lets any screen open the drawer without every screen taking a callback
 * parameter it would then have to thread through its own header composables.
 * Defaults to a no-op so a screen rendered outside the drawer (a preview, a
 * test) still composes.
 */
val LocalOpenDrawer = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * One destination in the drawer.
 *
 * Deliberately flat — an icon, a label, and a rounded fill when selected. No
 * container, no divider, no card. The reference app's sidebar is a plain list
 * and it is legible at a glance precisely because nothing is drawn around it.
 */
@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    // Last, so call sites can pass it as a trailing lambda — this list is long
    // and reads far better without ten `onClick =` labels down the left.
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GlassFill2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) VesperAccent else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}

/** Section caption between groups of destinations. */
@Composable
fun DrawerSection(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TextTertiary,
        modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 6.dp)
    )
}

/**
 * Drawer shell: the app name, then whatever destinations the caller supplies.
 *
 * Navigation lives here rather than in a bottom bar for two reasons. A bar can
 * hold four items before the labels start truncating, and this app has ten
 * destinations — five of which had no way to be reached at all because they did
 * not fit. And on the chat screen the bar sat directly beneath the composer, so
 * the bottom of the screen carried two stacked chrome elements competing for the
 * same thumb.
 */
@Composable
fun AppDrawerContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Flipper AI",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp)
        )
        content()
    }
}

/** Spacer that pushes trailing drawer content to the bottom of the panel. */
@Composable
fun DrawerFlexibleSpace(minHeight: androidx.compose.ui.unit.Dp = 24.dp) {
    Box(modifier = Modifier.height(minHeight))
}
