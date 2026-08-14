package com.vesper.flipper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.flipper.ui.theme.GlassFill1
import com.vesper.flipper.ui.theme.GlassFill2
import com.vesper.flipper.ui.theme.TextPrimary
import com.vesper.flipper.ui.theme.TextSecondary
import com.vesper.flipper.ui.theme.VesperAqua

// ═══════════════════════════════════════════════════════════════════════════
// Markdown rendering
//
// The model answers in markdown — it lists directories in backticks, bolds
// headings, and fences code. Until now the reply was drawn with a plain Text, so
// all of that arrived on screen as literal syntax: "**Files:**" with the stars
// showing and every filename wrapped in grave accents.
//
// Written here rather than pulled in as a dependency because the subset that
// actually appears in these answers is small and closed — headings, bullets,
// bold, italic, inline code and fenced blocks — and a renderer that fits on two
// screens is easier to keep correct than a general one is to keep pinned.
//
// Deliberately NOT supported: raw HTML. The text comes from a model that has
// just read file contents off the Flipper, so it is untrusted input; rendering
// markup from it would turn a chat answer into an injection surface.
// ═══════════════════════════════════════════════════════════════════════════

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val text: String, val level: Int) : MdBlock
    data class Bullet(val text: String, val ordinal: String?) : MdBlock
    data class Code(val text: String, val language: String?) : MdBlock
}

private fun parseBlocks(source: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = source.replace("\r\n", "\n").split("\n")
    var i = 0
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks += MdBlock.Paragraph(paragraph.toString().trim())
        paragraph.setLength(0)
    }

    while (i < lines.size) {
        val line = lines[i]
        when {
            line.trimStart().startsWith("```") -> {
                flushParagraph()
                val language = line.trimStart().removePrefix("```").trim().ifBlank { null }
                val body = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    body.appendLine(lines[i])
                    i++
                }
                blocks += MdBlock.Code(body.toString().trimEnd('\n'), language)
            }

            line.trimStart().startsWith("#") -> {
                flushParagraph()
                val trimmed = line.trimStart()
                val level = trimmed.takeWhile { it == '#' }.length
                blocks += MdBlock.Heading(trimmed.dropWhile { it == '#' }.trim(), level)
            }

            Regex("^\\s*[-*+]\\s+").containsMatchIn(line) -> {
                flushParagraph()
                blocks += MdBlock.Bullet(line.replaceFirst(Regex("^\\s*[-*+]\\s+"), ""), null)
            }

            Regex("^\\s*\\d+[.)]\\s+").containsMatchIn(line) -> {
                flushParagraph()
                val ordinal = Regex("^\\s*(\\d+)").find(line)?.groupValues?.get(1)
                blocks += MdBlock.Bullet(line.replaceFirst(Regex("^\\s*\\d+[.)]\\s+"), ""), ordinal)
            }

            line.isBlank() -> flushParagraph()

            else -> {
                if (paragraph.isNotEmpty()) paragraph.append('\n')
                paragraph.append(line)
            }
        }
        i++
    }
    flushParagraph()
    return blocks
}

/**
 * Inline spans. Order matters: code is taken first so that a `**` inside a code
 * span is not mistaken for emphasis.
 */
private fun inline(source: String): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("`([^`]+)`|\\*\\*([^*]+)\\*\\*|__([^_]+)__|\\*([^*]+)\\*|\\[([^\\]]+)]\\(([^)]+)\\)")
    var last = 0
    pattern.findAll(source).forEach { m ->
        append(source.substring(last, m.range.first))
        when {
            m.groupValues[1].isNotEmpty() -> withStyleAppend(
                m.groupValues[1],
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = VesperAqua,
                    background = GlassFill2
                )
            )
            m.groupValues[2].isNotEmpty() ->
                withStyleAppend(m.groupValues[2], SpanStyle(fontWeight = FontWeight.Bold))
            m.groupValues[3].isNotEmpty() ->
                withStyleAppend(m.groupValues[3], SpanStyle(fontWeight = FontWeight.Bold))
            m.groupValues[4].isNotEmpty() ->
                withStyleAppend(m.groupValues[4], SpanStyle(fontStyle = FontStyle.Italic))
            m.groupValues[5].isNotEmpty() ->
                withStyleAppend(m.groupValues[5], SpanStyle(color = VesperAqua))
        }
        last = m.range.last + 1
    }
    append(source.substring(last))
}

private fun AnnotatedString.Builder.withStyleAppend(text: String, style: SpanStyle) {
    pushStyle(style)
    append(text)
    pop()
}

/**
 * Renders one assistant message. Blocks are separate composables rather than one
 * AnnotatedString so a fenced block can scroll horizontally on its own — a Flipper
 * path or a hex dump is wider than the screen and wrapping it destroys alignment.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseBlocks(text) }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> Text(
                    text = inline(block.text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                is MdBlock.Heading -> Text(
                    text = inline(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )

                is MdBlock.Bullet -> Row(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text = block.ordinal?.let { "$it." } ?: "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        modifier = Modifier.width(if (block.ordinal != null) 26.dp else 18.dp)
                    )
                    Text(
                        text = inline(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }

                is MdBlock.Code -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassFill1)
                        .padding(12.dp)
                ) {
                    if (block.language != null) {
                        Text(
                            text = block.language,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextPrimary,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}
