package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.local.PdfDocumentEntity
import com.example.ui.viewmodel.MainTab
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.TextButton
import com.example.ui.viewmodel.FolioSuggestion
import com.example.ui.viewmodel.SmartFilter
import com.example.ui.viewmodel.WorkspaceMode
import java.util.Locale

@Composable
fun LibraryScreen(
    selectedTab: MainTab,
    documents: List<PdfDocumentEntity>,
    allDocuments: List<PdfDocumentEntity>,
    bookmarks: List<BookmarkEntity>,
    coverThumbnails: Map<Long, Bitmap>,
    searchQuery: String,
    selectedCategory: String,
    smartFilter: SmartFilter,
    smartSuggestions: List<FolioSuggestion>,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectSmartFilter: (SmartFilter) -> Unit,
    onExecuteSuggestion: (FolioSuggestion) -> Unit,
    onShowHelpGuide: () -> Unit,
    onOpenDocument: (PdfDocumentEntity, WorkspaceMode, Int) -> Unit,
    onToggleFavorite: (PdfDocumentEntity) -> Unit,
    onRenameRequest: (PdfDocumentEntity) -> Unit,
    onDuplicateDocument: (PdfDocumentEntity) -> Unit,
    onDeleteDocument: (PdfDocumentEntity) -> Unit,
    onOpenBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onImportPdfClick: () -> Unit,
    onCreatePdfClick: () -> Unit,
    onMergePdfsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Design", "Contracts", "Reports", "Notes", "Created", "Merged", "Imported")

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (selectedTab) {
            MainTab.LIBRARY, MainTab.FAVORITES -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .testTag("library_document_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        EditorialHeroBanner(
                            totalDocs = allDocuments.size,
                            totalPages = allDocuments.sumOf { it.pageCount },
                            totalAnnotations = allDocuments.sumOf { it.annotationCount },
                            onImportClick = onImportPdfClick,
                            onCreateClick = onCreatePdfClick,
                            onMergeClick = onMergePdfsClick,
                            onHelpClick = onShowHelpGuide
                        )
                    }

                    // Smart Suggestions Section
                    if (smartSuggestions.isNotEmpty() && searchQuery.isBlank() && selectedCategory == "All") {
                        item {
                            SmartSuggestionsSection(
                                suggestions = smartSuggestions,
                                onExecuteSuggestion = onExecuteSuggestion,
                                onShowHelpGuide = onShowHelpGuide
                            )
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { Text(stringResource(R.string.search_documents_hint)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search documents")
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_documents_input")
                            )

                            // Quick Smart Filter Tags (Suggestions)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SmartFilter.entries.forEach { sf ->
                                    val isSelected = smartFilter == sf
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelectSmartFilter(sf) },
                                        label = {
                                            Text(
                                                text = "${sf.emoji} ${sf.label}",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.testTag("smart_filter_${sf.name.lowercase()}")
                                    )
                                }
                            }

                            // Category Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    val selected = selectedCategory.equals(cat, ignoreCase = true)
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectCategory(cat) },
                                        label = {
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("category_chip_${cat.lowercase(Locale.ROOT)}")
                                    )
                                }
                            }
                        }
                    }

                    if (documents.isEmpty()) {
                        item {
                            EmptyLibraryCard(
                                isFavoritesTab = selectedTab == MainTab.FAVORITES,
                                onResetFilters = {
                                    onSearchQueryChange("")
                                    onSelectCategory("All")
                                },
                                onCreatePdfClick = onCreatePdfClick
                            )
                        }
                    } else {
                        items(documents, key = { it.id }) { doc ->
                            PdfFolioCard(
                                document = doc,
                                thumbnail = coverThumbnails[doc.id],
                                onReadClick = { onOpenDocument(doc, WorkspaceMode.READER, doc.lastOpenedPage) },
                                onEditClick = { onOpenDocument(doc, WorkspaceMode.EDITOR, doc.lastOpenedPage) },
                                onPagesClick = { onOpenDocument(doc, WorkspaceMode.PAGES, doc.lastOpenedPage) },
                                onToggleFavorite = { onToggleFavorite(doc) },
                                onRename = { onRenameRequest(doc) },
                                onDuplicate = { onDuplicateDocument(doc) },
                                onDelete = { onDeleteDocument(doc) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(76.dp)) }
                }
            }

            MainTab.BOOKMARKS -> {
                BookmarksListView(
                    bookmarks = bookmarks,
                    onOpenBookmark = onOpenBookmark,
                    onDeleteBookmark = onDeleteBookmark
                )
            }

            MainTab.STUDIO_TOOLS -> {
                PdfStudioToolsWorkbench(
                    documents = allDocuments,
                    onImportPdfClick = onImportPdfClick,
                    onCreatePdfClick = onCreatePdfClick,
                    onMergePdfsClick = onMergePdfsClick,
                    onOpenDocumentInMode = { doc, mode -> onOpenDocument(doc, mode, 0) }
                )
            }
        }
    }
}

@Composable
private fun SmartSuggestionsSection(
    suggestions: List<FolioSuggestion>,
    onExecuteSuggestion: (FolioSuggestion) -> Unit,
    onShowHelpGuide: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_suggestions_section")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Smart Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onShowHelpGuide,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tips & Guide", style = MaterialTheme.typography.labelSmall)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestions, key = { it.id }) { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .width(230.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onExecuteSuggestion(suggestion) }
                            .testTag("suggestion_card_${suggestion.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(suggestion.iconEmoji, style = MaterialTheme.typography.titleMedium)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = suggestion.actionText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = suggestion.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = suggestion.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialHeroBanner(
    totalDocs: Int,
    totalPages: Int,
    totalAnnotations: Int,
    onImportClick: () -> Unit,
    onCreateClick: () -> Unit,
    onMergeClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191A1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editorial_hero_banner")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Custom Editorial Canvas Art Background
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x44D93829), Color.Transparent),
                        center = Offset(w * 0.85f, h * 0.25f),
                        radius = w * 0.45f
                    ),
                    center = Offset(w * 0.85f, h * 0.25f),
                    radius = w * 0.45f
                )
                // Tilted decorative folio sheets on right side
                drawRoundRect(
                    color = Color(0x18FAF6F0),
                    topLeft = Offset(w * 0.68f, h * 0.14f),
                    size = Size(w * 0.26f, h * 0.68f),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0x28E59800),
                    topLeft = Offset(w * 0.72f, h * 0.28f),
                    size = Size(w * 0.18f, 10.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFD93829),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.studio_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "$totalDocs FOLIOS • $totalPages PAGES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC5BFB5)
                        )
                    }

                    FilledTonalButton(
                        onClick = onHelpClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0x33FFFFFF),
                            contentColor = Color(0xFFFAF6F0)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("hero_guide_button")
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tips", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Text(
                    text = "Folio PDF Studio",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFFFAF6F0)
                )

                Text(
                    text = "Read with archival paper filters, annotate with freehand ink & highlighters, stamp approvals, or reorganize & merge PDF pages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD0C9BE)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCreateClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD93829),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_create_pdf_button")
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New PDF", maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = onImportClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF2B2E37),
                            contentColor = Color(0xFFFAF6F0)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_import_pdf_button")
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open File", maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = onMergeClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF2B2E37),
                            contentColor = Color(0xFFFFBE3D)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_merge_pdf_button")
                    ) {
                        Icon(Icons.Default.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Merge", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfFolioCard(
    document: PdfDocumentEntity,
    thumbnail: Bitmap?,
    onReadClick: () -> Unit,
    onEditClick: () -> Unit,
    onPagesClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onReadClick() }
            .testTag("pdf_document_card_${document.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Live PDF Page 1 Thumbnail Preview
                Box(
                    modifier = Modifier
                        .width(86.dp)
                        .height(118.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF4EFE6))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = "Preview of ${document.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Surface(
                        color = Color(0xCC191A1E),
                        shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "${document.pageCount}P",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = document.category.uppercase(Locale.ROOT),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(36.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("favorite_button_${document.id}")
                            ) {
                                Icon(
                                    imageVector = if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (document.isFavorite) Color(0xFFE59800) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("more_menu_button_${document.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Document options"
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename & Category") },
                                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onRename()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duplicate PDF") },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onDuplicate()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Organize / Rotate Pages") },
                                        leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onPagesClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete PDF") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val kbSize = (document.fileSizeBytes / 1024L).coerceAtLeast(1L)
                    val annBadge = if (document.annotationCount > 0) {
                        " • ${document.annotationCount} markups"
                    } else {
                        " • Clean copy"
                    }
                    Text(
                        text = "${document.pageCount} pages • ${kbSize} KB$annBadge",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Reading progress bar
                    val progress = ((document.lastOpenedPage + 1).toFloat() / document.pageCount.coerceAtLeast(1))
                        .coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "P.${document.lastOpenedPage + 1}/${document.pageCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct mode launch actions: Reader, Editor, Page Organizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onReadClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_reader_button_${document.id}")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read", style = MaterialTheme.typography.labelLarge)
                }

                FilledTonalButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_editor_button_${document.id}")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Annotate", style = MaterialTheme.typography.labelLarge)
                }

                FilledTonalButton(
                    onClick = onPagesClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_pages_button_${document.id}")
                ) {
                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pages", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun BookmarksListView(
    bookmarks: List<BookmarkEntity>,
    onOpenBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .testTag("bookmarks_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Saved Page Bookmarks",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Tap any bookmark ribbon to jump directly to that page inside the PDF Reader.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (bookmarks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = stringResource(R.string.bookmarks_empty_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.bookmarks_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(bookmarks, key = { it.id }) { bm ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenBookmark(bm) }
                        .testTag("bookmark_item_${bm.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bm.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${bm.documentTitle} • Page ${bm.pageIndex + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onDeleteBookmark(bm) },
                            modifier = Modifier.testTag("delete_bookmark_${bm.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove bookmark"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfStudioToolsWorkbench(
    documents: List<PdfDocumentEntity>,
    onImportPdfClick: () -> Unit,
    onCreatePdfClick: () -> Unit,
    onMergePdfsClick: () -> Unit,
    onOpenDocumentInMode: (PdfDocumentEntity, WorkspaceMode) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .testTag("studio_tools_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "PDF Studio Workbench",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Create custom PDF templates, merge multi-document packets, or jump directly into annotation & page tools.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudioActionTile(
                    title = "Create New PDF",
                    subtitle = "Blank, Ruled, Dot Grid, or Contract Memo",
                    icon = Icons.Default.NoteAdd,
                    accentColor = Color(0xFFD93829),
                    onClick = onCreatePdfClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("workbench_create_pdf_tile")
                )
                StudioActionTile(
                    title = "Merge PDFs",
                    subtitle = "Combine multiple PDFs into one file",
                    icon = Icons.Default.CallMerge,
                    accentColor = Color(0xFFE59800),
                    onClick = onMergePdfsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("workbench_merge_pdf_tile")
                )
            }
        }

        item {
            StudioActionTile(
                title = "Import PDF from Device Storage",
                subtitle = "Open any local .pdf file via Android Storage Access Framework",
                icon = Icons.Default.FileOpen,
                accentColor = Color(0xFF1D63DC),
                onClick = onImportPdfClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("workbench_import_pdf_tile")
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Quick Jump to Editor or Page Organizer",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(documents, key = { it.id }) { doc ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(
                            "${doc.pageCount} pages • ${doc.annotationCount} annotations",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { onOpenDocumentInMode(doc, WorkspaceMode.EDITOR) }
                    ) {
                        Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Annotate")
                    }
                    FilledTonalButton(
                        onClick = { onOpenDocumentInMode(doc, WorkspaceMode.PAGES) }
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pages")
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor
                    )
                }
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    isFavoritesTab: Boolean,
    onResetFilters: () -> Unit,
    onCreatePdfClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_library_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isFavoritesTab) Icons.Default.StarBorder else Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = if (isFavoritesTab) "No starred PDFs yet" else stringResource(R.string.empty_library_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (isFavoritesTab) {
                    "Tap the star icon on any PDF card in your library to pin it here for instant access."
                } else {
                    stringResource(R.string.empty_library_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onResetFilters) {
                    Text("Reset Filters")
                }
                Button(onClick = onCreatePdfClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create PDF")
                }
            }
        }
    }
}
