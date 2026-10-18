package com.example

import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.PdfDocumentEntity
import com.example.ui.components.CreatePdfDialog
import com.example.ui.components.FolioHelpDialog
import com.example.ui.components.MergePdfsDialog
import com.example.ui.components.RenameDocumentDialog
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PdfWorkspaceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FolioViewModel
import com.example.ui.viewmodel.MainTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FolioPdfApp()
            }
        }
    }
}

@Composable
fun FolioPdfApp() {
    val context = LocalContext.current
    val viewModel: FolioViewModel = viewModel(
        factory = FolioViewModel.provideFactory(context)
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val documents by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val allDocuments by viewModel.allDocuments.collectAsStateWithLifecycle()
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val coverThumbnails by viewModel.coverThumbnails.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val workspaceState by viewModel.workspaceState.collectAsStateWithLifecycle()
    val smartFilter by viewModel.smartFilter.collectAsStateWithLifecycle()
    val smartSuggestions by viewModel.smartSuggestions.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var showGlobalHelpDialog by remember { mutableStateOf(false) }
    var documentToRename by remember { mutableStateOf<PdfDocumentEntity?>(null) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // SAF OpenDocument Launcher for importing local PDF files
    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var displayName = "Imported Document.pdf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
            viewModel.importExternalPdf(uri, displayName)
        }
    }

    // SAF CreateDocument Launcher for exporting annotated/edited PDFs
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val activeId = workspaceState.activeDocument?.id
        if (targetUri != null && activeId != null) {
            viewModel.exportDocumentToUri(activeId, targetUri)
        }
    }

    if (showCreateDialog) {
        CreatePdfDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, category, body, pageCount, template ->
                viewModel.createNewPdf(
                    title = title,
                    category = category,
                    bodyContent = body,
                    pageCount = pageCount,
                    templateType = template,
                    openImmediately = true
                )
            }
        )
    }

    if (showMergeDialog) {
        MergePdfsDialog(
            documents = allDocuments,
            onDismiss = { showMergeDialog = false },
            onMerge = { selectedDocs, mergedTitle ->
                viewModel.mergeDocuments(selectedDocs, mergedTitle)
            }
        )
    }

    documentToRename?.let { doc ->
        RenameDocumentDialog(
            document = doc,
            onDismiss = { documentToRename = null },
            onRename = { newTitle, newCategory ->
                viewModel.renameDocument(doc, newTitle, newCategory)
            }
        )
    }

    if (showGlobalHelpDialog) {
        FolioHelpDialog(onDismiss = { showGlobalHelpDialog = false })
    }

    Crossfade(
        targetState = workspaceState.activeDocument != null,
        label = "WorkspaceTransition"
    ) { isWorkspaceOpen ->
        if (isWorkspaceOpen && workspaceState.activeDocument != null) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    PdfWorkspaceScreen(
                        state = workspaceState,
                        onClose = { viewModel.closeWorkspace() },
                        onSelectMode = { viewModel.setWorkspaceMode(it) },
                        onGoToPage = { viewModel.goToPage(it) },
                        onNextPage = { viewModel.nextPage() },
                        onPreviousPage = { viewModel.previousPage() },
                        onCycleReadingFilter = { viewModel.cycleReadingFilter() },
                        onToggleBookmark = { viewModel.toggleBookmarkCurrentPage() },
                        onSelectTool = { viewModel.selectAnnotationTool(it) },
                        onSelectColor = { viewModel.selectColor(it) },
                        onSelectStrokeWidth = { viewModel.selectStrokeWidth(it) },
                        onSelectStampPreset = { viewModel.selectStampPreset(it) },
                        onUpdateTextNote = { viewModel.updatePendingTextNote(it) },
                        onSaveSignaturePattern = { viewModel.saveSignaturePattern(it) },
                        onCommitStroke = { viewModel.commitStrokeOnCurrentPage(it) },
                        onPlaceTapAnnotation = { nx, ny -> viewModel.placeTapAnnotationAt(nx, ny) },
                        onUndo = { viewModel.undoAnnotationOnCurrentPage() },
                        onRedo = { viewModel.redoAnnotationOnCurrentPage() },
                        onClearPage = { viewModel.clearCurrentPageAnnotations() },
                        onFlattenAndSave = { viewModel.flattenAndSaveCurrentPdf() },
                        onExportPdfClick = {
                            val docTitle = workspaceState.activeDocument?.title ?: "Folio_Export"
                            exportPdfLauncher.launch("${docTitle.replace(" ", "_")}.pdf")
                        },
                        onRotatePage = { viewModel.rotatePage(it) },
                        onMovePage = { from, to -> viewModel.movePage(from, to) },
                        onDuplicatePage = { viewModel.duplicatePageAt(it) },
                        onExtractPage = { viewModel.extractPageAsStandalonePdf(it) },
                        onDeletePage = { viewModel.deletePageAt(it) },
                        onAppendTemplatePage = { viewModel.appendNewTemplatePage(it) }
                    )
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 680.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = { showCreateDialog = true },
                            icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                            text = { Text(stringResource(R.string.action_create_pdf)) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("fab_create_pdf")
                        )
                    },
                    bottomBar = {
                        if (!isWideScreen) {
                            FolioBottomNavigationBar(
                                selectedTab = selectedTab,
                                onSelectTab = { viewModel.selectMainTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isWideScreen) {
                            FolioNavigationRail(
                                selectedTab = selectedTab,
                                onSelectTab = { viewModel.selectMainTab(it) }
                            )
                        }

                        LibraryScreen(
                            selectedTab = selectedTab,
                            documents = documents,
                            allDocuments = allDocuments,
                            bookmarks = bookmarks,
                            coverThumbnails = coverThumbnails,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            smartFilter = smartFilter,
                            smartSuggestions = smartSuggestions,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onSelectCategory = { viewModel.selectCategory(it) },
                            onSelectSmartFilter = { viewModel.selectSmartFilter(it) },
                            onExecuteSuggestion = { suggestion ->
                                if (suggestion.documentId != null) {
                                    val targetDoc = allDocuments.find { it.id == suggestion.documentId }
                                    if (targetDoc != null) {
                                        viewModel.openDocument(targetDoc, suggestion.targetMode, suggestion.targetPage)
                                    }
                                } else if (suggestion.id == "merge_suggestion") {
                                    showMergeDialog = true
                                }
                            },
                            onShowHelpGuide = { showGlobalHelpDialog = true },
                            onOpenDocument = { doc, mode, page -> viewModel.openDocument(doc, mode, page) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onRenameRequest = { documentToRename = it },
                            onDuplicateDocument = { viewModel.duplicateDocument(it) },
                            onDeleteDocument = { viewModel.deleteDocument(it) },
                            onOpenBookmark = { viewModel.openBookmark(it) },
                            onDeleteBookmark = { viewModel.deleteBookmark(it) },
                            onImportPdfClick = {
                                importPdfLauncher.launch(arrayOf("application/pdf"))
                            },
                            onCreatePdfClick = { showCreateDialog = true },
                            onMergePdfsClick = { showMergeDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolioBottomNavigationBar(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == MainTab.LIBRARY,
            onClick = { onSelectTab(MainTab.LIBRARY) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.LIBRARY) Icons.Filled.FolderCopy else Icons.Outlined.FolderCopy,
                    contentDescription = stringResource(R.string.tab_library)
                )
            },
            label = { Text(stringResource(R.string.tab_library)) },
            modifier = Modifier.testTag("nav_tab_library")
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.FAVORITES,
            onClick = { onSelectTab(MainTab.FAVORITES) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.FAVORITES) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.tab_favorites)
                )
            },
            label = { Text(stringResource(R.string.tab_favorites)) },
            modifier = Modifier.testTag("nav_tab_favorites")
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.BOOKMARKS,
            onClick = { onSelectTab(MainTab.BOOKMARKS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.BOOKMARKS) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.tab_bookmarks)
                )
            },
            label = { Text(stringResource(R.string.tab_bookmarks)) },
            modifier = Modifier.testTag("nav_tab_bookmarks")
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.STUDIO_TOOLS,
            onClick = { onSelectTab(MainTab.STUDIO_TOOLS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.STUDIO_TOOLS) Icons.Filled.Build else Icons.Outlined.Build,
                    contentDescription = stringResource(R.string.tab_tools)
                )
            },
            label = { Text(stringResource(R.string.tab_tools)) },
            modifier = Modifier.testTag("nav_tab_tools")
        )
    }
}

@Composable
private fun FolioNavigationRail(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        NavigationRailItem(
            selected = selectedTab == MainTab.LIBRARY,
            onClick = { onSelectTab(MainTab.LIBRARY) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.LIBRARY) Icons.Filled.FolderCopy else Icons.Outlined.FolderCopy,
                    contentDescription = stringResource(R.string.tab_library)
                )
            },
            label = { Text(stringResource(R.string.tab_library)) }
        )
        NavigationRailItem(
            selected = selectedTab == MainTab.FAVORITES,
            onClick = { onSelectTab(MainTab.FAVORITES) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.FAVORITES) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.tab_favorites)
                )
            },
            label = { Text(stringResource(R.string.tab_favorites)) }
        )
        NavigationRailItem(
            selected = selectedTab == MainTab.BOOKMARKS,
            onClick = { onSelectTab(MainTab.BOOKMARKS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.BOOKMARKS) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.tab_bookmarks)
                )
            },
            label = { Text(stringResource(R.string.tab_bookmarks)) }
        )
        NavigationRailItem(
            selected = selectedTab == MainTab.STUDIO_TOOLS,
            onClick = { onSelectTab(MainTab.STUDIO_TOOLS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.STUDIO_TOOLS) Icons.Filled.Build else Icons.Outlined.Build,
                    contentDescription = stringResource(R.string.tab_tools)
                )
            },
            label = { Text(stringResource(R.string.tab_tools)) }
        )
    }
}
