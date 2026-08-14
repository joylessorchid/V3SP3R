package com.vesper.flipper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════
// FLIPPER AI — dark glass
//
// Two rules hold this together; breaking either is what makes a dark UI look
// cheap:
//
//   1. Depth comes from TRANSLUCENT WHITE lifted off the backdrop, never from
//      a lighter opaque grey. Glass1 < Glass2 < Glass3 are the only fills a
//      surface may use, so every card sits in the same light.
//   2. Colour is reserved for meaning. The accent marks what is live or
//      selected; the risk colours mark risk. Nothing is tinted for decoration,
//      which is what keeps a HIGH-risk confirmation legible at a glance.
//
// Every name previously exported is kept, so all screens restyle at once and
// the compiler proves nothing was missed.
// ═══════════════════════════════════════════════════════════════════════════

// ── Ground ────────────────────────────────────────────────────────────────
// Near-black with a cool cast rather than pure black: #000 kills the sense of
// depth glass depends on, and shows banding on OLED gradients.
val VesperBackground = Color(0xFF07090E)
val VesperBackgroundDeep = Color(0xFF04060A)
val VesperBackgroundGlow = Color(0xFF0A0E18)

val VesperBackdropBrush = Brush.verticalGradient(
    colors = listOf(
        VesperBackgroundDeep,
        VesperBackground,
        VesperBackgroundGlow,
        VesperBackgroundDeep
    )
)

// ── Glass ─────────────────────────────────────────────────────────────────
// The whole surface system. Translucent white over the backdrop, so a card
// over the gradient picks up the gradient instead of flattening it.
val GlassFill1 = Color(0x0DFFFFFF)      // 5%  — resting cards
val GlassFill2 = Color(0x14FFFFFF)      // 8%  — raised rows, inputs
val GlassFill3 = Color(0x1FFFFFFF)      // 12% — pressed / hovered
val GlassStroke = Color(0x1AFFFFFF)     // 10% — hairline edge
val GlassStrokeStrong = Color(0x33FFFFFF) // 20% — edge on emphasised surfaces

/** Top-edge sheen. Applied as a 1dp gradient it reads as a lit bevel. */
val GlassSheenBrush = Brush.verticalGradient(
    colors = listOf(Color(0x40FFFFFF), Color(0x00FFFFFF))
)

val VesperSurface = Color(0xFF0E121A)
val VesperSurfaceVariant = Color(0xFF141A24)

// ── Accent ────────────────────────────────────────────────────────────────
// Cold, not warm. A hardware tool reads as instrumentation, not as jewellery.
val VesperAccent = Color(0xFF5AA9FF)         // ice blue — live, selected, primary
val VesperAccentSoft = Color(0xFF8CC4FF)
val VesperAccentDeep = Color(0xFF2E6FD1)
val VesperAqua = Color(0xFF4FE3C1)           // secondary accent — success, connected

// Names kept from the previous palette so no screen fails to compile. They now
// point at the cold accent; the wine/gold identity is gone.
val VesperWine = VesperAccent
val VesperWineLight = VesperAccentSoft
val VesperWineDark = VesperAccentDeep
val VesperOrange = VesperAccent
val VesperOrangeDark = VesperAccentDeep
val VesperGold = VesperAqua
val VesperAccentGold = VesperAqua
val VesperGoldMuted = Color(0xFF2F8C7C)
val VesperSecondary = Color(0xFF141A24)
val VesperGunmetal = Color(0xFF1C2432)

// ── Text ──────────────────────────────────────────────────────────────────
// Three levels only. More than three and hierarchy stops reading.
val TextPrimary = Color(0xFFF2F5FA)
val TextSecondary = Color(0xFF9AA6BD)
val TextTertiary = Color(0xFF63708A)

// ── Risk ──────────────────────────────────────────────────────────────────
// These gate BadUSB and recursive delete. Kept maximally distinct from the
// accent so a confirmation never blends into ordinary chrome.
val RiskLow = Color(0xFF3ED9A4)
val RiskMedium = Color(0xFFFFC24B)
val RiskHigh = Color(0xFFFF6B6B)
val RiskBlocked = Color(0xFF6B7793)

// ── Diff ──────────────────────────────────────────────────────────────────
val DiffAdded = Color(0xFF3ED9A4)
val DiffRemoved = Color(0xFFFF6B6B)
val DiffChanged = Color(0xFFFFC24B)
val DiffAddedBackground = Color(0x263ED9A4)
val DiffRemovedBackground = Color(0x26FF6B6B)

// ── Chat ──────────────────────────────────────────────────────────────────
// The assistant's reply is NOT a bubble. Long technical answers read far better
// as plain text on the ground, and it is what lets a reply run full width.
// Only the user's own line gets a container.
val ChatUser = GlassFill2
val ChatAssistant = Color(0x00000000)
val ChatTool = GlassFill1
val ChatToolAccent = VesperAqua

private val DarkColorScheme = darkColorScheme(
    primary = VesperAccent,
    onPrimary = Color(0xFF04121F),
    primaryContainer = VesperAccentDeep,
    onPrimaryContainer = Color(0xFFE8F2FF),
    secondary = VesperAqua,
    onSecondary = Color(0xFF03201B),
    secondaryContainer = VesperSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = VesperAqua,
    onTertiary = Color(0xFF03201B),
    background = VesperBackground,
    onBackground = TextPrimary,
    surface = VesperSurface,
    onSurface = TextPrimary,
    surfaceVariant = VesperSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF2A3346),
    outlineVariant = Color(0xFF1A212D),
    error = RiskHigh,
    onError = Color(0xFF2A0A0A)
)

// The app ships dark and every screen is painted against VesperBackdropBrush,
// so this exists only to satisfy the system-theme branch. Keeping it legible
// rather than pretending it is designed.
private val LightColorScheme = lightColorScheme(
    primary = VesperAccentDeep,
    onPrimary = Color.White,
    secondary = VesperGoldMuted,
    onSecondary = Color.White,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF0E121A),
    surface = Color.White,
    onSurface = Color(0xFF0E121A),
    surfaceVariant = Color(0xFFE7EBF2),
    onSurfaceVariant = Color(0xFF48526A),
    outline = Color(0xFF9AA6BD),
    error = Color(0xFFC53434),
    onError = Color.White
)

// Generous radii. Small corners on a translucent fill read as a flat panel;
// the roundness is what makes it read as a pane of glass.
private val VesperShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val BaseTypography = androidx.compose.material3.Typography()

// One sans family throughout. The previous theme set serif headings and
// MONOSPACE labels, which is why "Compatibility Layer" and "Connection
// Autotuner" rendered as typewriter text next to sans body copy. Monospace now
// appears only where it carries meaning — CLI output, payload source, hex —
// and those call bodySmall/labelSmall explicitly at the call site.
val VesperTypography = BaseTypography.copy(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // Chat answers live here. 16sp at 25sp leading is the readable-paragraph
    // setting; the old 15sp with default leading was cramped for long replies.
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    // Eyebrow labels over a card title: small, wide-tracked, upper case at the
    // call site. Sans, not mono.
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
)

/** Monospace, for the places where character alignment carries meaning. */
val VesperMonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 19.sp
)

@Composable
fun VesperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = VesperTypography,
        shapes = VesperShapes,
        content = content
    )
}
