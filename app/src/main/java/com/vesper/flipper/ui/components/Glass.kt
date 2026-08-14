package com.vesper.flipper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vesper.flipper.ui.theme.GlassFill1
import com.vesper.flipper.ui.theme.GlassFill2
import com.vesper.flipper.ui.theme.GlassFill3
import com.vesper.flipper.ui.theme.GlassSheenBrush
import com.vesper.flipper.ui.theme.GlassStroke
import com.vesper.flipper.ui.theme.GlassStrokeStrong
import com.vesper.flipper.ui.theme.TextSecondary
import com.vesper.flipper.ui.theme.TextTertiary
import com.vesper.flipper.ui.theme.VesperAccent

// ═══════════════════════════════════════════════════════════════════════════
// Glass surfaces
//
// A note on what "glass" is here, because it constrains the design: Compose has
// no backdrop blur. Modifier.blur() blurs a composable's OWN content, not what
// sits behind it, and real backdrop blur needs a RenderEffect on the window —
// expensive, Android 12+ only, and it does not compose with scrolling content.
//
// So the effect is built the way it actually works on this platform: a
// translucent white fill over the app's gradient backdrop, a hairline border,
// and a one-pixel sheen along the top edge. The card picks up whatever part of
// the gradient it sits over, which is what sells the material. It also costs
// nothing to draw, which matters on a screen that scrolls.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The workhorse surface. Everything that would have been a Card is this.
 *
 * @param emphasised brightens the border — for the one card on a screen that
 *        carries live state, e.g. an active connection.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    emphasised: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val fill = when {
        pressed -> GlassFill3
        emphasised -> GlassFill2
        else -> GlassFill1
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        // Hairline edge. Drawn as a bordered Surface rather than a Modifier.border
        // so the stroke sits inside the clip and does not alias on the corners.
        Surface(
            color = Color.Transparent,
            shape = shape,
            border = BorderStroke(1.dp, if (emphasised) GlassStrokeStrong else GlassStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
        // Lit top edge.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlassSheenBrush)
        )
    }
}

/**
 * Eyebrow label sitting above a card title — small, wide-tracked, upper case.
 * Sans, deliberately: the previous theme set these in monospace, which is why
 * headings read as typewriter text beside sans body copy.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = TextTertiary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}

/**
 * Circular translucent button. These replace title bars: the reference app
 * carries no header at all, only floating controls in the corners.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextSecondary,
    size: Dp = 40.dp,
    active: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // No resting fill. A row of filled circles across the top of every screen reads
    // as five competing buttons; the glyphs alone read as tools, and the touch
    // target is unchanged because the Box keeps its size. The disc appears only
    // while a finger is down, where it confirms the press.
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (pressed) GlassFill2 else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) VesperAccent else tint,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

/**
 * Row of "label left, value right". Extracted because it is the single most
 * repeated pattern in the device and settings screens, and because getting the
 * value to truncate rather than push the label off-screen has to be done once.
 */
@Composable
fun GlassStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            maxLines = 1
        )
    }
}
