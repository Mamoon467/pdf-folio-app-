package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.NormalizedPoint
import com.example.data.local.PageTemplateType
import com.example.data.local.PdfDocumentEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePdfDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, category: String, bodyContent: String, pageCount: Int, template: PageTemplateType) -> Unit
) {
    var title by remember { mutableStateOf("Executive Project Brief") }
    var selectedCategory by remember { mutableStateOf("Design") }
    var pageCount by remember { mutableIntStateOf(2) }
    var selectedTemplate by remember { mutableStateOf(PageTemplateType.RULED) }
    var bodyContent by remember {
        mutableStateOf(
            "1. Objectives & Overview\nThis document outlines key milestones, architectural decisions, and review items.\n\n2. Action Items\n- Review layout specifications\n- Add ink annotations and approval stamp\n- Sign and export final PDF"
        )
    }

    val categories = listOf("Design", "Contracts", "Reports", "Notes", "Created")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New PDF Document",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_pdf_title_input")
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Page Count",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (pageCount > 1) pageCount-- },
                            modifier = Modifier.testTag("decrease_page_count_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease pages")
                        }
                        Text(
                            text = "$pageCount pages",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { if (pageCount < 12) pageCount++ },
                            modifier = Modifier.testTag("increase_page_count_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase pages")
                        }
                    }
                }

                Text(
                    text = "Paper Template Style",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PageTemplateType.entries.forEach { template ->
                    val isSelected = selectedTemplate == template
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTemplate = template }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = bodyContent,
                    onValueChange = { bodyContent = it },
                    label = { Text("Initial Page 1 Text Content") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_pdf_body_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(title, selectedCategory, bodyContent, pageCount, selectedTemplate)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_create_pdf_button")
            ) {
                Text("Generate PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MergePdfsDialog(
    documents: List<PdfDocumentEntity>,
    onDismiss: () -> Unit,
    onMerge: (selectedDocs: List<PdfDocumentEntity>, mergedTitle: String) -> Unit
) {
    var mergedTitle by remember { mutableStateOf("Combined Folio Packet") }
    val selectedIds = remember {
        mutableStateListOf<Long>().apply {
            documents.take(2).forEach { add(it.id) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Merge Multiple PDFs",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = mergedTitle,
                    onValueChange = { mergedTitle = it },
                    label = { Text("Merged Output Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("merge_pdf_title_input")
                )

                Text(
                    text = "Select 2 or more PDFs in order (${selectedIds.size} selected):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        val isChecked = selectedIds.contains(doc.id)
                        val orderNum = selectedIds.indexOf(doc.id) + 1
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChecked) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isChecked) selectedIds.remove(doc.id) else selectedIds.add(doc.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) {
                                            if (!selectedIds.contains(doc.id)) selectedIds.add(doc.id)
                                        } else {
                                            selectedIds.remove(doc.id)
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${doc.pageCount} pages • ${doc.category}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isChecked) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$orderNum",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val orderedDocs = selectedIds.mapNotNull { id -> documents.find { it.id == id } }
                    onMerge(orderedDocs, mergedTitle)
                    onDismiss()
                },
                enabled = selectedIds.size >= 2,
                modifier = Modifier.testTag("confirm_merge_pdf_button")
            ) {
                Text("Merge (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SignaturePadDialog(
    inkColor: Color,
    onDismiss: () -> Unit,
    onSaveSignature: (List<NormalizedPoint>) -> Unit
) {
    val points = remember { mutableStateListOf<NormalizedPoint>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Draw Digital Signature",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Sign inside the archival box below, then tap 'Use Signature' and tap anywhere on the PDF page to stamp it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFDFBF7))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .testTag("signature_pad_canvas")
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val nx = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val ny = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    points.add(NormalizedPoint(nx, ny))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pos = change.position
                                    val nx = (pos.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val ny = (pos.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    points.add(NormalizedPoint(nx, ny))
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Baseline rule
                        drawLine(
                            color = Color(0xFFD5CEC0),
                            start = Offset(24.dp.toPx(), size.height * 0.75f),
                            end = Offset(size.width - 24.dp.toPx(), size.height * 0.75f),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        if (points.size >= 2) {
                            val path = Path()
                            path.moveTo(points.first().x * size.width, points.first().y * size.height)
                            for (i in 1 until points.size) {
                                path.lineTo(points[i].x * size.width, points[i].y * size.height)
                            }
                            drawPath(
                                path = path,
                                color = inkColor,
                                style = Stroke(
                                    width = 3.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (points.isEmpty()) {
                        Text(
                            text = "Sign Here with Finger or Stylus",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF968E80),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { points.clear() },
                        modifier = Modifier.testTag("clear_signature_button")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Pad")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSignature(points.toList())
                    onDismiss()
                },
                modifier = Modifier.testTag("save_signature_button")
            ) {
                Text("Use Signature")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameDocumentDialog(
    document: PdfDocumentEntity,
    onDismiss: () -> Unit,
    onRename: (newTitle: String, newCategory: String) -> Unit
) {
    var title by remember(document) { mutableStateOf(document.title) }
    var category by remember(document) { mutableStateOf(document.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Document Details", style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_title_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_category_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRename(title, category)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_rename_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FolioHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("💡", style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text("Folio User Guide & Tips", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Quick guide to reading, annotating & editing PDFs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GuideTipItem(
                    icon = "📖",
                    title = "Reading & Zooming",
                    description = "Double-tap anywhere on a page to quickly zoom in 200%. Pinch to zoom up to 320%. Use the top contrast button to switch between Original, Warm Sepia, Dark Inverted, and High Contrast paper filters."
                )
                GuideTipItem(
                    icon = "✍️",
                    title = "Stylus & Freehand Ink",
                    description = "Switch to 'Annotate' mode. Choose the Ink Pen or Highlighter, pick your pigment swatch and stroke thickness, and draw directly onto the page canvas."
                )
                GuideTipItem(
                    icon = "🖋️",
                    title = "Digital Signatures",
                    description = "Select 'Signature' from the toolbar to draw your personal signature once. Then tap anywhere inside contract signature boxes to stamp your signature."
                )
                GuideTipItem(
                    icon = "🏷️",
                    title = "Text Notes & Review Stamps",
                    description = "Use Suggested Quick Notes (like 'Approved ✅', 'Needs Revision ⚠️') or official stamps ('APPROVED', 'CONFIDENTIAL', 'SIGN HERE') and tap on the PDF page to place them."
                )
                GuideTipItem(
                    icon = "📑",
                    title = "Page Organizer & Baking",
                    description = "In 'Pages' mode, rotate pages 90°, reorder, duplicate, delete, or append lined/grid pages. Tap 'Bake PDF' anytime to permanently flatten annotations into the PDF file."
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It!")
            }
        }
    )
}

@Composable
private fun GuideTipItem(
    icon: String,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

