package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RichTextToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Bold",
                onClick = { wrapSelection(textFieldValue, onValueChange, "**", "**") }
            )
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                onClick = { wrapSelection(textFieldValue, onValueChange, "*", "*") }
            )
            ToolbarButton(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                onClick = { wrapSelection(textFieldValue, onValueChange, "<u>", "</u>") }
            )
            ToolbarTextButton(
                label = "H1",
                contentDescription = "Heading 1",
                onClick = { insertLinePrefix(textFieldValue, onValueChange, "# ") }
            )
            ToolbarTextButton(
                label = "H2",
                contentDescription = "Heading 2",
                onClick = { insertLinePrefix(textFieldValue, onValueChange, "## ") }
            )
            ToolbarButton(
                icon = Icons.Default.FormatListBulleted,
                contentDescription = "Bullet List",
                onClick = { insertLinePrefix(textFieldValue, onValueChange, "• ") }
            )
            ToolbarButton(
                icon = Icons.Default.FormatListNumbered,
                contentDescription = "Numbered List",
                onClick = { insertLinePrefix(textFieldValue, onValueChange, "1. ") }
            )
            ToolbarButton(
                icon = Icons.Default.CheckBox,
                contentDescription = "Check Item",
                onClick = { insertLinePrefix(textFieldValue, onValueChange, "[ ] ") }
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolbarTextButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun wrapSelection(
    current: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    prefix: String,
    suffix: String
) {
    val text = current.text
    val selection = current.selection
    val newText: String
    val newSelection: TextRange

    if (selection.collapsed) {
        // Just insert empty wrapper and place cursor inside
        val pos = selection.start
        newText = text.substring(0, pos) + prefix + suffix + text.substring(pos)
        newSelection = TextRange(pos + prefix.length)
    } else {
        // Wrap selected text
        val start = selection.min
        val end = selection.max
        val selectedStr = text.substring(start, end)
        newText = text.substring(0, start) + prefix + selectedStr + suffix + text.substring(end)
        newSelection = TextRange(start + prefix.length + selectedStr.length + suffix.length)
    }

    onValueChange(current.copy(text = newText, selection = newSelection))
}

private fun insertLinePrefix(
    current: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    linePrefix: String
) {
    val text = current.text
    val selection = current.selection
    val pos = selection.start

    // Find start of current line
    val lineStart = text.lastIndexOf('\n', pos - 1).let { if (it == -1) 0 else it + 1 }
    val newText = text.substring(0, lineStart) + linePrefix + text.substring(lineStart)
    val newPos = pos + linePrefix.length
    onValueChange(current.copy(text = newText, selection = TextRange(newPos)))
}
