package com.vesper.flipper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vesper.flipper.ui.theme.GlassFill2
import com.vesper.flipper.ui.theme.TextPrimary
import com.vesper.flipper.ui.theme.TextSecondary
import com.vesper.flipper.ui.theme.TextTertiary
import com.vesper.flipper.ui.theme.VesperAccent
import com.vesper.flipper.ui.theme.VesperSurface

// ═══════════════════════════════════════════════════════════════════════════
// Inset grouped list
//
// The structural idea this app was missing. Related settings live in ONE rounded
// container separated by hairlines, with a small label above the group and the
// explanation below it — rather than each item getting its own card.
//
// Why it reads better than a stack of cards: a card says "this is a separate
// thing". A screen of eight cards therefore says "here are eight unrelated
// things", and the reader has to work out the grouping themselves. A single
// container with dividers says "these belong together", and the whitespace
// between groups does the separating — so the eye gets structure for free and
// the screen stops looking like a pile of boxes.
//
// The divider is inset to the text, not full-bleed. Aligning it under the label
// rather than under the icon is what makes a list look drawn rather than
// assembled.
// ═══════════════════════════════════════════════════════════════════════════

private val GroupShape = RoundedCornerShape(18.dp)

/**
 * @param label small caption above the group; omit for an unlabelled group
 * @param footer explanation below the group, outside the container — the place
 *        for the sentence that would otherwise bloat a row into two lines
 */
@Composable
fun ListGroup(
    modifier: Modifier = Modifier,
    label: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextTertiary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GroupShape)
                .background(VesperSurface),
            content = content
        )
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
            )
        }
    }
}

/**
 * Text-field colours for a field that lives INSIDE a [ListGroup].
 *
 * Borderless on purpose. An outlined field inside a rounded container draws a
 * second rounded rectangle a few dp inside the first, and its floating label
 * lands on top of the group's own edge — which is what made the settings screen
 * look like boxes inside boxes. The group is the container; the field is a row
 * in it.
 */
@Composable
fun flatFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextSecondary,
    focusedLabelColor = VesperAccent,
    unfocusedLabelColor = TextTertiary,
    cursorColor = VesperAccent
)

/** Hairline between rows, inset so it starts under the label rather than the icon. */
@Composable
fun ListDivider(insetStart: Dp = 16.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = insetStart)
            .height(1.dp)
            .background(Color(0x14FFFFFF))
    )
}

/**
 * One row. Everything optional except the title, because the same row serves a
 * navigation target, a read-only value and a switch host.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = TextSecondary,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (pressed && onClick != null) GlassFill2 else Color.Transparent)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Box(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (trailing != null) {
            Row(
                modifier = Modifier.padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = trailing
            )
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(20.dp)
            )
        }
    }
}
