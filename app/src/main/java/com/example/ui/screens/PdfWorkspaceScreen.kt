package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Save
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AnnotationTool
import com.example.data.local.NormalizedPoint
import com.example.data.local.PageAnnotationEntity
import com.example.data.local.PageTemplateType
import com.example.data.local.StampPreset
import com.example.data.local.decodeNormalizedPoints
import com.example.ui.components.FolioHelpDialog
import com.example.ui.components.SignaturePadDialog
import com.example.ui.theme.AnnotationColors
import com.example.ui.viewmodel.WorkspaceMode
import com.example.ui.viewmodel.WorkspaceUiState
import kotlin.math.roundToInt

@Composable
fun PdfWorkspaceScreen(
    state: WorkspaceUiState,
    onClose: () -> Unit,
    onSelectMode: (WorkspaceMode) -> Unit,
    onGoToPage: (Int) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onCycleReadingFilter: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSelectTool: (AnnotationTool) -> Unit,
    onSelectColor: (Long) -> Unit,
    onSelectStrokeWidth: (Float) -> Unit,
    onSelectStampPreset: (StampPreset) -> Unit,
    onUpdateTextNote: (String) -> Unit,
    onSaveSignaturePattern: (List<NormalizedPoint>) -> Unit,
    onCommitStroke: (List<NormalizedPoint>) -> Unit,
    onPlaceTapAnnotation: (Float, Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearPage: () -> Unit,
    onFlattenAndSave: () -> Unit,
    onExportPdfClick: () -> Unit,
    onRotatePage: (Int) -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onDuplicatePage: (Int) -> Unit,
    onExtractPage: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onAppendTemplatePage: (PageTemplateType) -> Unit
) {
    val doc = state.activeDocument ?: return
    BackHandler { onClose() }

    var zoomScale by remember(state.currentPageIndex) { mutableFloatStateOf(1f) }
    var panOffset by remember(state.currentPageIndex) { mutableStateOf(Offset.Zero) }
    val activeDragPoints = remember { mutableStateListOf<NormalizedPoint>() }
    var showSignaturePad by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showTipBanner by remember { mutableStateOf(true) }

    val isPageBookmarked = state.documentBookmarks.any { it.pageIndex == state.currentPageIndex }
    val currentPageAnnotations = remember(state.documentAnnotations, state.currentPageIndex) {
        state.documentAnnotations.filter { it.pageIndex == state.currentPageIndex }
    }

    if (showHelpDialog) {
        FolioHelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            inkColor = Color(state.selectedColorArgb),
            onDismiss = { showSignaturePad = false },
            onSaveSignature = { pts -> onSaveSignaturePattern(pts) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("pdf_workspace_screen")
    ) {
        // Top Studio Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("workspace_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Library"
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Page ${state.currentPageIndex + 1} of ${doc.pageCount} • ${state.readingFilter.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onCycleReadingFilter,
                            modifier = Modifier.testTag("cycle_reading_filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contrast,
                                contentDescription = "Cycle Reading Filter (${state.readingFilter.label})",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier.testTag("toggle_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark current page",
                                tint = if (isPageBookmarked) Color(0xFFE59800) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onFlattenAndSave,
                            modifier = Modifier.testTag("flatten_save_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Flatten annotations into PDF"
                            )
                        }
                        IconButton(
                            onClick = onExportPdfClick,
                            modifier = Modifier.testTag("export_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export PDF to device"
                            )
                        }
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier.testTag("workspace_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "User Guide & Tips",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Segmented Mode Pill Switcher: Reader | Editor | Pages
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WorkspaceMode.entries.forEach { mode ->
                        val selected = state.workspaceMode == mode
                        val label = when (mode) {
                            WorkspaceMode.READER -> "Reader & Zoom"
                            WorkspaceMode.EDITOR -> "Annotate & Sign"
                            WorkspaceMode.PAGES -> "Pages (${doc.pageCount})"
                        }
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { onSelectMode(mode) }
                                .testTag("mode_tab_${mode.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                // Contextual User Friendly Tip & Suggestion Banner
                AnimatedVisibility(visible = showTipBanner) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💡", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = when (state.workspaceMode) {
                                    WorkspaceMode.READER -> "Double-tap page to zoom 200%. Pinch to zoom up to 320%. Use contrast icon to switch paper tones."
                                    WorkspaceMode.EDITOR -> "Pick Ink Pen to write, or tap a Suggested Note below and tap the page to stamp it."
                                    WorkspaceMode.PAGES -> "Rotate pages 90°, tap arrows to reorder, or duplicate and append new template sheets."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showTipBanner = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss tip",
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Workspace Content
        if (state.workspaceMode == WorkspaceMode.PAGES) {
            PageOrganizerView(
                document = doc,
                currentPageIndex = state.currentPageIndex,
                pageThumbnails = state.pageThumbnails,
                annotations = state.documentAnnotations,
                onSelectPage = { idx ->
                    onGoToPage(idx)
                    onSelectMode(WorkspaceMode.READER)
                },
                onRotatePage = onRotatePage,
                onMovePage = onMovePage,
                onDuplicatePage = onDuplicatePage,
                onExtractPage = onExtractPage,
                onDeletePage = onDeletePage,
                onAppendTemplatePage = onAppendTemplatePage,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Interactive PDF Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background( Color(0xFF23252C) )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = state.currentPageBitmap
                val aspectRatio = if (bmp != null && bmp.height > 0) {
                    bmp.width.toFloat() / bmp.height.toFloat()
                } else {
                    595f / 842f
                }

                val isPanOrReader = state.workspaceMode == WorkspaceMode.READER ||
                    state.activeTool == AnnotationTool.PAN

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isPanOrReader) {
                            if (isPanOrReader) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 3.2f)
                                    zoomScale = newScale
                                    if (newScale > 1.01f) {
                                        panOffset = Offset(
                                            x = (panOffset.x + pan.x).coerceIn(-600f, 600f),
                                            y = (panOffset.y + pan.y).coerceIn(-800f, 800f)
                                        )
                                    } else {
                                        panOffset = Offset.Zero
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(aspectRatio)
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                            .shadow(10.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .testTag("pdf_page_canvas")
                            .pointerInput(state.workspaceMode, state.activeTool) {
                                if (state.workspaceMode == WorkspaceMode.READER || state.activeTool == AnnotationTool.PAN) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (zoomScale > 1.1f) {
                                                zoomScale = 1f
                                                panOffset = Offset.Zero
                                            } else {
                                                zoomScale = 2.0f
                                            }
                                        }
                                    )
                                } else {
                                    detectTapGestures(
                                        onTap = { tapOffset ->
                                            val nx = (tapOffset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                            val ny = (tapOffset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                            onPlaceTapAnnotation(nx, ny)
                                        }
                                    )
                                }
                            }
                            .pointerInput(state.workspaceMode, state.activeTool) {
                                val isStrokeTool = state.workspaceMode == WorkspaceMode.EDITOR &&
                                    (state.activeTool == AnnotationTool.PEN ||
                                        state.activeTool == AnnotationTool.HIGHLIGHTER ||
                                        state.activeTool == AnnotationTool.ERASER)
                                if (isStrokeTool) {
                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            activeDragPoints.clear()
                                            val nx = (startOffset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                            val ny = (startOffset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                            activeDragPoints.add(NormalizedPoint(nx, ny))
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val pos = change.position
                                            val nx = (pos.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                            val ny = (pos.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                            activeDragPoints.add(NormalizedPoint(nx, ny))
                                        },
                                        onDragEnd = {
                                            if (activeDragPoints.size >= 2) {
                                                onCommitStroke(activeDragPoints.toList())
                                            }
                                            activeDragPoints.clear()
                                        },
                                        onDragCancel = {
                                            activeDragPoints.clear()
                                        }
                                    )
                                }
                            }
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF Page ${state.currentPageIndex + 1}",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Vector Annotation Overlay Canvas (renders saved annotations + active stroke)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cw = size.width
                            val ch = size.height

                            for (ann in currentPageAnnotations) {
                                val baseColor = Color(ann.colorArgb)
                                when (ann.toolType) {
                                    AnnotationTool.PEN.name, AnnotationTool.SIGNATURE.name -> {
                                        val pts = decodeNormalizedPoints(ann.pointsData)
                                        if (pts.size >= 2) {
                                            val path = Path()
                                            path.moveTo(pts.first().x * cw, pts.first().y * ch)
                                            for (i in 1 until pts.size) {
                                                path.lineTo(pts[i].x * cw, pts[i].y * ch)
                                            }
                                            drawPath(
                                                path = path,
                                                color = baseColor,
                                                style = Stroke(
                                                    width = ann.strokeWidth.dp.toPx() * 0.65f,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                    AnnotationTool.HIGHLIGHTER.name -> {
                                        val pts = decodeNormalizedPoints(ann.pointsData)
                                        if (pts.size >= 2) {
                                            val path = Path()
                                            path.moveTo(pts.first().x * cw, pts.first().y * ch)
                                            for (i in 1 until pts.size) {
                                                path.lineTo(pts[i].x * cw, pts[i].y * ch)
                                            }
                                            drawPath(
                                                path = path,
                                                color = baseColor.copy(alpha = 0.36f),
                                                style = Stroke(
                                                    width = (ann.strokeWidth * 2.2f).dp.toPx(),
                                                    cap = StrokeCap.Square,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                    AnnotationTool.TEXT.name -> {
                                        val px = (ann.normalizedX * cw).coerceIn(16f, cw - 120f)
                                        val py = (ann.normalizedY * ch).coerceIn(24f, ch - 24f)
                                        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 12.dp.toPx()
                                            typeface = android.graphics.Typeface.create(
                                                android.graphics.Typeface.SANS_SERIF,
                                                android.graphics.Typeface.BOLD
                                            )
                                        }
                                        val tw = textPaint.measureText(ann.textLabel)
                                        drawRoundRect(
                                            color = baseColor,
                                            topLeft = Offset(px - 8.dp.toPx(), py - 16.dp.toPx()),
                                            size = Size(tw + 16.dp.toPx(), 24.dp.toPx()),
                                            cornerRadius = CornerRadius(6.dp.toPx())
                                        )
                                        drawContext.canvas.nativeCanvas.drawText(
                                            ann.textLabel,
                                            px,
                                            py,
                                            textPaint
                                        )
                                    }
                                    AnnotationTool.STAMP.name -> {
                                        val px = (ann.normalizedX * cw).coerceIn(20f, cw - 130f)
                                        val py = (ann.normalizedY * ch).coerceIn(28f, ch - 28f)
                                        val stampPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                            color = ann.colorArgb.toInt()
                                            textSize = 13.dp.toPx()
                                            typeface = android.graphics.Typeface.create(
                                                android.graphics.Typeface.MONOSPACE,
                                                android.graphics.Typeface.BOLD
                                            )
                                        }
                                        val tw = stampPaint.measureText(ann.textLabel)
                                        val rectTopLeft = Offset(px - 10.dp.toPx(), py - 18.dp.toPx())
                                        val rectSize = Size(tw + 20.dp.toPx(), 26.dp.toPx())
                                        drawRoundRect(
                                            color = baseColor.copy(alpha = 0.14f),
                                            topLeft = rectTopLeft,
                                            size = rectSize,
                                            cornerRadius = CornerRadius(5.dp.toPx())
                                        )
                                        drawRoundRect(
                                            color = baseColor,
                                            topLeft = rectTopLeft,
                                            size = rectSize,
                                            cornerRadius = CornerRadius(5.dp.toPx()),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                        drawContext.canvas.nativeCanvas.drawText(
                                            ann.textLabel,
                                            px,
                                            py - 1.dp.toPx(),
                                            stampPaint
                                        )
                                    }
                                }
                            }

                            // Active drag stroke preview
                            if (activeDragPoints.size >= 2) {
                                val path = Path()
                                path.moveTo(activeDragPoints.first().x * cw, activeDragPoints.first().y * ch)
                                for (i in 1 until activeDragPoints.size) {
                                    path.lineTo(activeDragPoints[i].x * cw, activeDragPoints[i].y * ch)
                                }
                                val previewColor = when (state.activeTool) {
                                    AnnotationTool.ERASER -> Color(0x99FF5252)
                                    AnnotationTool.HIGHLIGHTER -> Color(state.selectedColorArgb).copy(alpha = 0.38f)
                                    else -> Color(state.selectedColorArgb)
                                }
                                val previewWidth = when (state.activeTool) {
                                    AnnotationTool.HIGHLIGHTER -> (state.selectedStrokeWidth * 2.2f).dp.toPx()
                                    AnnotationTool.ERASER -> 16.dp.toPx()
                                    else -> state.selectedStrokeWidth.dp.toPx() * 0.65f
                                }
                                drawPath(
                                    path = path,
                                    color = previewColor,
                                    style = Stroke(
                                        width = previewWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }

                    // Zoom indicator pill in top-right of canvas
                    if (zoomScale > 1.02f) {
                        Surface(
                            color = Color(0xDD191A1E),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clickable {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${(zoomScale * 100).roundToInt()}% • Tap to Reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (state.isProcessing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // Bottom Control Dock: Either Reader Scrubber OR Editor Annotation Dock
            if (state.workspaceMode == WorkspaceMode.EDITOR) {
                EditorAnnotationDock(
                    state = state,
                    pageAnnotationCount = currentPageAnnotations.size,
                    onSelectTool = onSelectTool,
                    onSelectColor = onSelectColor,
                    onSelectStrokeWidth = onSelectStrokeWidth,
                    onSelectStampPreset = onSelectStampPreset,
                    onUpdateTextNote = onUpdateTextNote,
                    onOpenSignaturePad = { showSignaturePad = true },
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onClearPage = onClearPage,
                    onFlattenAndSave = onFlattenAndSave,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage
                )
            } else {
                ReaderBottomScrubber(
                    document = doc,
                    currentPageIndex = state.currentPageIndex,
                    pageThumbnails = state.pageThumbnails,
                    onGoToPage = onGoToPage,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage
                )
            }
        }
    }
}

@Composable
private fun ReaderBottomScrubber(
    document: com.example.data.local.PdfDocumentEntity,
    currentPageIndex: Int,
    pageThumbnails: Map<Int, android.graphics.Bitmap>,
    onGoToPage: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Thumbnail filmstrip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(document.pageCount, key = { it }) { idx ->
                    val selected = idx == currentPageIndex
                    val thumb = pageThumbnails[idx]
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFDFBF7))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onGoToPage(idx) }
                            .testTag("scrubber_thumb_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = "Jump to Page ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primary else Color(0xBB191A1E),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(2.dp)
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = onPreviousPage,
                    enabled = currentPageIndex > 0,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("reader_prev_page_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous page")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prev")
                }

                Text(
                    text = "PAGE ${currentPageIndex + 1} OF ${document.pageCount}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = onNextPage,
                    enabled = currentPageIndex < document.pageCount - 1,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("reader_next_page_button")
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next page")
                }
            }
        }
    }
}

@Composable
private fun EditorAnnotationDock(
    state: WorkspaceUiState,
    pageAnnotationCount: Int,
    onSelectTool: (AnnotationTool) -> Unit,
    onSelectColor: (Long) -> Unit,
    onSelectStrokeWidth: (Float) -> Unit,
    onSelectStampPreset: (StampPreset) -> Unit,
    onUpdateTextNote: (String) -> Unit,
    onOpenSignaturePad: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearPage: () -> Unit,
    onFlattenAndSave: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val doc = state.activeDocument ?: return

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_annotation_dock")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Contextual Tool Configuration Bar (Stamp Presets, Text Note Input, Signature Button, or Color/Stroke)
            when (state.activeTool) {
                AnnotationTool.STAMP -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tap page to stamp:", style = MaterialTheme.typography.labelMedium)
                        StampPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = state.selectedStamp == preset,
                                onClick = { onSelectStampPreset(preset) },
                                label = { Text(preset.title, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                AnnotationTool.TEXT -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Suggestions:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val suggestions = listOf(
                                "Approved ✅",
                                "Needs Revision ⚠️",
                                "Urgent 🚨",
                                "Reviewed ✍️",
                                "Confidential 🔒",
                                "Sign & Return 📄",
                                "Page Checked ✔️"
                            )
                            suggestions.forEach { note ->
                                FilterChip(
                                    selected = state.pendingTextNote == note,
                                    onClick = { onUpdateTextNote(note) },
                                    label = { Text(note, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.pendingTextNote,
                                onValueChange = onUpdateTextNote,
                                label = { Text("Note Text (Tap page to place)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_text_note_input")
                            )
                        }
                    }
                }
                AnnotationTool.SIGNATURE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick Options:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FilterChip(
                                selected = false,
                                onClick = onOpenSignaturePad,
                                label = { Text("🖋️ Draw Signature", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onUpdateTextNote("Signed: Oct 18, 2026")
                                    onSelectTool(AnnotationTool.TEXT)
                                },
                                label = { Text("📅 Stamp Date (Oct 18)", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onUpdateTextNote("Initial: M.A.")
                                    onSelectTool(AnnotationTool.TEXT)
                                },
                                label = { Text("✍️ Initials (M.A.)", style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tap anywhere on the PDF page to stamp your signature.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = onOpenSignaturePad,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_signature_pad_button")
                            ) {
                                Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Draw Signature")
                            }
                        }
                    }
                }
                else -> {
                    // Color Swatches + Stroke Width Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnnotationColors.forEach { argb ->
                                val isSelected = state.selectedColorArgb == argb
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectColor(argb) }
                                )
                            }
                        }
                        Slider(
                            value = state.selectedStrokeWidth,
                            onValueChange = onSelectStrokeWidth,
                            valueRange = 2f..18f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tool Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tools = listOf(
                    AnnotationTool.PEN to Icons.Default.Create,
                    AnnotationTool.HIGHLIGHTER to Icons.Default.BorderColor,
                    AnnotationTool.TEXT to Icons.Default.TextFields,
                    AnnotationTool.STAMP to Icons.Default.Approval,
                    AnnotationTool.SIGNATURE to Icons.Default.Draw,
                    AnnotationTool.ERASER to Icons.Default.CleaningServices,
                    AnnotationTool.PAN to Icons.Default.PanTool
                )
                for ((tool, icon) in tools) {
                    val selected = state.activeTool == tool
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onSelectTool(tool)
                            if (tool == AnnotationTool.SIGNATURE && state.pendingSignaturePoints.isEmpty()) {
                                onOpenSignaturePad()
                            }
                        },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(tool.label) },
                        modifier = Modifier.testTag("tool_chip_${tool.name.lowercase()}")
                    )
                }
            }

            // Bottom Action Row: Page Nav + Undo / Redo / Clear / Flatten
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousPage,
                        enabled = state.currentPageIndex > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous page")
                    }
                    Text(
                        text = "P.${state.currentPageIndex + 1}/${doc.pageCount}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    IconButton(
                        onClick = onNextPage,
                        enabled = state.currentPageIndex < doc.pageCount - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next page")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUndo,
                        enabled = pageAnnotationCount > 0,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("editor_undo_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo annotation")
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = state.redoStack.isNotEmpty(),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("editor_redo_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo annotation")
                    }
                    IconButton(
                        onClick = onClearPage,
                        enabled = pageAnnotationCount > 0,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("editor_clear_page_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear page annotations")
                    }
                }

                FilledTonalButton(
                    onClick = onFlattenAndSave,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("dock_flatten_save_button")
                ) {
                    Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bake PDF", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
