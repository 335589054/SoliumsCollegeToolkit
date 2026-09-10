package com.solium.Unitoolkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val inlineRegex = Regex("""(\*\*.+?\*\*|\*[^*\n]+?\*|`.+?`|\[.+?]\(.+?\))""")

private fun buildInline(text: String, base: Color, linkColor: Color): androidx.compose.ui.text.AnnotatedString {
    val result = buildAnnotatedString {
        var last = 0
        inlineRegex.findAll(text).forEach { m ->
            if (m.range.first > last) append(text.substring(last, m.range.first))
            val token = m.value
            when {
                token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(token.removePrefix("**").removeSuffix("**")) }
                token.startsWith("`") -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = base.copy(alpha = 0.1f))) { append(token.removePrefix("`").removeSuffix("`")) }
                token.startsWith("[") -> {
                    val name = token.substringAfter('[').substringBefore(']')
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(name) }
                }
                else -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(token.removePrefix("*").removeSuffix("*")) }
            }
            last = m.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
    return result
}

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary

    if (markdown.isBlank()) {
        Text("空笔记", modifier = modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val lines = markdown.lines()
        var inCode = false
        val codeBuf = StringBuilder()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.trim().startsWith("```") -> {
                    if (!inCode) { inCode = true; codeBuf.clear() } else {
                        inCode = false
                        Text(
                            codeBuf.toString(),
                            modifier = Modifier.fillMaxWidth().background(base.copy(alpha = 0.06f)).padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = base,
                        )
                    }
                }
                inCode -> codeBuf.appendLine(line)
                line.isBlank() -> Spacer(Modifier.width(1.dp))
                line.trimStart().startsWith("# ") -> Text(buildInline(line.trimStart().removePrefix("# "), base, linkColor), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = base)
                line.trimStart().startsWith("## ") -> Text(buildInline(line.trimStart().removePrefix("## "), base, linkColor), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = base)
                line.trimStart().startsWith("### ") -> Text(buildInline(line.trimStart().removePrefix("### "), base, linkColor), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = base)
                line.trimStart().startsWith("> ") -> Text(
                    buildInline(line.trimStart().removePrefix("> "), base, linkColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp, 12.dp, 12.dp, 0.dp))
                        .padding(8.dp),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                line.trimStart().startsWith("- ") -> Row(Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                    Text("•  ", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Text(buildInline(line.trimStart().removePrefix("- "), base, linkColor), fontSize = 14.sp, color = base)
                }
                line.trim().matches(Regex("""\d+\.\s.*""")) -> Row(Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                    val num = line.trim().substringBefore('.')
                    Text("$num.  ", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Text(buildInline(line.trim().substringAfter(".").trimStart(), base, linkColor), fontSize = 14.sp, color = base)
                }
                else -> Text(buildInline(line, base, linkColor), fontSize = 14.sp, color = base)
            }
            i++
        }
        if (inCode) {
            Text(codeBuf.toString(), modifier = Modifier.fillMaxWidth().background(base.copy(alpha = 0.06f)).padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = base)
        }
    }
}