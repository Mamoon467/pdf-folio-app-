package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AnnotationTool
import com.example.data.local.BookmarkEntity
import com.example.data.local.FolioDatabase
import com.example.data.local.NormalizedPoint
import com.example.data.local.PageAnnotationEntity
import com.example.data.local.PageTemplateType
import com.example.data.local.PdfDocumentEntity
import com.example.data.local.ReadingFilterMode
import com.example.data.local.StampPreset
import com.example.data.local. decodeNormalizedPoints
import com.example.data.local.encodeNormalizedPoints
import com.example.data.repository.PdfRepository
import com.example.ui.theme.AnnotationColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.hypot

enum class MainTab {
    LIBRARY,
    FAVORITES,
    BOOKMARKS,
    STUDIO_TOOLS
}

enum class WorkspaceMode {
    READER,
    EDITOR,
    PAGES
}

data class WorkspaceUiState(
    val activeDocument: PdfDocumentEntity? = null,
    val workspaceMode: WorkspaceMode = WorkspaceMode.READER,
    val currentPageIndex: Int = 0,
    val readingFilter: ReadingFilterMode = ReadingFilterMode.DAY,
    val activeTool: AnnotationTool = AnnotationTool.PEN,
    val selectedColorArgb: Long = AnnotationColors.first(),
    val selectedStrokeWidth: Float = 5f,
    val selectedStamp: StampPreset = StampPreset.APPROVED,
    val pendingTextNote: String = "Key Review Note",
    val pendingSignaturePoints: List<NormalizedPoint> = emptyList(),
    val currentPageBitmap: Bitmap? = null,
    val pageThumbnails: Map<Int, Bitmap> = emptyMap(),
    val documentAnnotations: List<PageAnnotationEntity> = emptyList(),
    val redoStack: List<PageAnnotationEntity> = emptyList(),
    val documentBookmarks: List<BookmarkEntity> = emptyList(),
    val isProcessing: Boolean = false
)

enum class SmartFilter(val label: String, val emoji: String) {
    ALL("All", "📁"),
    STARRED("Starred", "⭐"),
    WITH_MARKUPS("Marked Up", "✍️"),
    CONTRACTS("Contracts", "📜"),
    MULTI_PAGE("Multi-Page", "📑")
}

data class FolioSuggestion(
    val id: String,
    val iconEmoji: String,
    val title: String,
    val description: String,
    val actionText: String,
    val documentId: Long? = null,
    val targetMode: WorkspaceMode = WorkspaceMode.READER,
    val targetPage: Int = 0
)

class FolioViewModel(
    private val repository: PdfRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainTab.LIBRARY)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _smartFilter = MutableStateFlow(SmartFilter.ALL)
    val smartFilter: StateFlow<SmartFilter> = _smartFilter.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _coverThumbnails = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())
    val coverThumbnails: StateFlow<Map<Long, Bitmap>> = _coverThumbnails.asStateFlow()

    private val _workspaceState = MutableStateFlow(WorkspaceUiState())
    val workspaceState: StateFlow<WorkspaceUiState> = _workspaceState.asStateFlow()

    val allDocuments: StateFlow<List<PdfDocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDocuments: StateFlow<List<PdfDocumentEntity>> = combine(
        repository.allDocuments,
        _searchQuery,
        _selectedCategory,
        _selectedTab,
        _smartFilter
    ) { docs, query, category, tab, filter ->
        docs.filter { doc ->
            val matchesTab = if (tab == MainTab.FAVORITES) doc.isFavorite else true
            val matchesCat = category == "All" || doc.category.equals(category, ignoreCase = true)
            val matchesFilter = when (filter) {
                SmartFilter.ALL -> true
                SmartFilter.STARRED -> doc.isFavorite
                SmartFilter.WITH_MARKUPS -> doc.annotationCount > 0
                SmartFilter.CONTRACTS -> doc.category.equals("Contracts", true) ||
                    doc.title.contains("agreement", true) || doc.title.contains("contract", true)
                SmartFilter.MULTI_PAGE -> doc.pageCount > 1
            }
            val matchesQuery = query.isBlank() ||
                doc.title.contains(query, ignoreCase = true) ||
                doc.category.contains(query, ignoreCase = true)
            matchesTab && matchesCat && matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smartSuggestions: StateFlow<List<FolioSuggestion>> = combine(
        repository.allDocuments,
        _workspaceState
    ) { docs, _ ->
        val list = mutableListOf<FolioSuggestion>()
        val inProgress = docs.firstOrNull { it.lastOpenedPage > 0 }
        if (inProgress != null) {
            list.add(
                FolioSuggestion(
                    id = "resume_${inProgress.id}",
                    iconEmoji = "📖",
                    title = "Resume Reading",
                    description = "Continue reading '${inProgress.title}' right on Page ${inProgress.lastOpenedPage + 1}",
                    actionText = "Continue",
                    documentId = inProgress.id,
                    targetMode = WorkspaceMode.READER,
                    targetPage = inProgress.lastOpenedPage
                )
            )
        }

        val contract = docs.firstOrNull {
            it.category.equals("Contracts", true) ||
                it.title.contains("agreement", true) ||
                it.title.contains("contract", true)
        }
        if (contract != null) {
            val sigPage = (contract.pageCount - 1).coerceAtLeast(0)
            list.add(
                FolioSuggestion(
                    id = "sign_${contract.id}",
                    iconEmoji = "✍️",
                    title = "Quick Sign Contract",
                    description = "Place digital signature & stamp on '${contract.title}'",
                    actionText = "Sign P.${sigPage + 1}",
                    documentId = contract.id,
                    targetMode = WorkspaceMode.EDITOR,
                    targetPage = sigPage
                )
            )
        }

        val designDoc = docs.firstOrNull {
            (it.category.equals("Design", true) || it.title.contains("specimen", true)) &&
                it.id != contract?.id
        }
        if (designDoc != null) {
            list.add(
                FolioSuggestion(
                    id = "markup_${designDoc.id}",
                    iconEmoji = "🎨",
                    title = "Review & Markup",
                    description = "Test freehand ink and yellow highlighter on '${designDoc.title}'",
                    actionText = "Markup",
                    documentId = designDoc.id,
                    targetMode = WorkspaceMode.EDITOR,
                    targetPage = 0
                )
            )
        }

        if (docs.size >= 2) {
            list.add(
                FolioSuggestion(
                    id = "merge_suggestion",
                    iconEmoji = "📑",
                    title = "Merge PDF Dossier",
                    description = "Combine ${docs.size} files into a single unified packet",
                    actionText = "Merge PDFs",
                    documentId = null
                )
            )
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeDocObserverJob: Job? = null
    private var pageRenderJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeedDocumentsLoaded()
        }
        viewModelScope.launch {
            repository.allDocuments.collectLatest { docs ->
                // Sync activeDocument metadata if open
                val currentActive = _workspaceState.value.activeDocument
                if (currentActive != null) {
                    val updated = docs.find { it.id == currentActive.id }
                    if (updated != null && updated != currentActive) {
                        _workspaceState.update { it.copy(activeDocument = updated) }
                    }
                }
                // Generate cover thumbnails for library cards
                for (doc in docs) {
                    if (!_coverThumbnails.value.containsKey(doc.id)) {
                        val bmp = repository.renderPdfPage(doc, 0, targetWidthPx = 360)
                        if (bmp != null) {
                            _coverThumbnails.update { it + (doc.id to bmp) }
                        }
                    }
                }
            }
        }
    }

    fun selectMainTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectSmartFilter(filter: SmartFilter) {
        _smartFilter.value = filter
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun postMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun openDocument(
        document: PdfDocumentEntity,
        initialMode: WorkspaceMode = WorkspaceMode.READER,
        initialPage: Int = document.lastOpenedPage
    ) {
        val safePage = initialPage.coerceIn(0, (document.pageCount - 1).coerceAtLeast(0))
        _workspaceState.value = WorkspaceUiState(
            activeDocument = document,
            workspaceMode = initialMode,
            currentPageIndex = safePage,
            readingFilter = ReadingFilterMode.DAY,
            activeTool = if (initialMode == WorkspaceMode.EDITOR) AnnotationTool.PEN else AnnotationTool.PAN,
            isProcessing = true
        )

        activeDocObserverJob?.cancel()
        activeDocObserverJob = viewModelScope.launch {
            combine(
                repository.observeAnnotationsForDocument(document.id),
                repository.observeBookmarksForDocument(document.id)
            ) { annotations, bookmarks ->
                annotations to bookmarks
            }.collectLatest { (annotations, bookmarks) ->
                _workspaceState.update {
                    it.copy(
                        documentAnnotations = annotations,
                        documentBookmarks = bookmarks
                    )
                }
            }
        }

        renderActivePageAndThumbnails(document, safePage, ReadingFilterMode.DAY, reloadThumbs = true)
        viewModelScope.launch {
            repository.updateLastOpenedPage(document.id, safePage)
        }
    }

    fun closeWorkspace() {
        activeDocObserverJob?.cancel()
        pageRenderJob?.cancel()
        _workspaceState.value = WorkspaceUiState()
    }

    fun setWorkspaceMode(mode: WorkspaceMode) {
        _workspaceState.update { state ->
            val nextTool = when (mode) {
                WorkspaceMode.READER -> AnnotationTool.PAN
                WorkspaceMode.EDITOR -> if (state.activeTool == AnnotationTool.PAN) AnnotationTool.PEN else state.activeTool
                WorkspaceMode.PAGES -> state.activeTool
            }
            state.copy(workspaceMode = mode, activeTool = nextTool)
        }
        val doc = _workspaceState.value.activeDocument ?: return
        if (mode == WorkspaceMode.PAGES && _workspaceState.value.pageThumbnails.size < doc.pageCount) {
            loadAllPageThumbnails(doc)
        }
    }

    fun goToPage(pageIndex: Int) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        val target = pageIndex.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0))
        if (target == state.currentPageIndex && state.currentPageBitmap != null) return

        _workspaceState.update { it.copy(currentPageIndex = target, redoStack = emptyList()) }
        renderActivePageAndThumbnails(doc, target, state.readingFilter, reloadThumbs = false)
        viewModelScope.launch {
            repository.updateLastOpenedPage(doc.id, target)
        }
    }

    fun nextPage() {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        if (state.currentPageIndex < doc.pageCount - 1) {
            goToPage(state.currentPageIndex + 1)
        }
    }

    fun previousPage() {
        val state = _workspaceState.value
        if (state.currentPageIndex > 0) {
            goToPage(state.currentPageIndex - 1)
        }
    }

    fun cycleReadingFilter() {
        val modes = ReadingFilterMode.entries
        val current = _workspaceState.value.readingFilter
        val next = modes[(current.ordinal + 1) % modes.size]
        setReadingFilter(next)
    }

    fun setReadingFilter(filterMode: ReadingFilterMode) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        _workspaceState.update { it.copy(readingFilter = filterMode) }
        renderActivePageAndThumbnails(doc, state.currentPageIndex, filterMode, reloadThumbs = false)
    }

    private fun renderActivePageAndThumbnails(
        document: PdfDocumentEntity,
        pageIndex: Int,
        filterMode: ReadingFilterMode,
        reloadThumbs: Boolean
    ) {
        pageRenderJob?.cancel()
        pageRenderJob = viewModelScope.launch {
            _workspaceState.update { it.copy(isProcessing = true) }
            val pageBmp = repository.renderPdfPage(
                document = document,
                pageIndex = pageIndex,
                targetWidthPx = 1200,
                filterMode = filterMode
            )
            _workspaceState.update {
                it.copy(
                    currentPageBitmap = pageBmp,
                    isProcessing = false
                )
            }
            if (reloadThumbs) {
                loadAllPageThumbnails(document)
            }
        }
    }

    private fun loadAllPageThumbnails(document: PdfDocumentEntity) {
        viewModelScope.launch {
            val thumbs = mutableMapOf<Int, Bitmap>()
            for (i in 0 until document.pageCount) {
                val bmp = repository.renderPdfPage(
                    document = document,
                    pageIndex = i,
                    targetWidthPx = 320,
                    filterMode = ReadingFilterMode.DAY
                )
                if (bmp != null) {
                    thumbs[i] = bmp
                    _workspaceState.update { it.copy(pageThumbnails = thumbs.toMap()) }
                }
            }
            thumbs[0]?.let { firstBmp ->
                _coverThumbnails.update { it + (document.id to firstBmp) }
            }
        }
    }

    // Annotation Tools & Actions
    fun selectAnnotationTool(tool: AnnotationTool) {
        _workspaceState.update {
            it.copy(
                activeTool = tool,
                workspaceMode = if (tool != AnnotationTool.PAN) WorkspaceMode.EDITOR else it.workspaceMode
            )
        }
    }

    fun selectColor(colorArgb: Long) {
        _workspaceState.update { it.copy(selectedColorArgb = colorArgb) }
    }

    fun selectStrokeWidth(width: Float) {
        _workspaceState.update { it.copy(selectedStrokeWidth = width.coerceIn(2f, 24f)) }
    }

    fun selectStampPreset(stamp: StampPreset) {
        _workspaceState.update {
            it.copy(
                selectedStamp = stamp,
                selectedColorArgb = stamp.badgeColorArgb,
                activeTool = AnnotationTool.STAMP
            )
        }
    }

    fun updatePendingTextNote(text: String) {
        _workspaceState.update { it.copy(pendingTextNote = text) }
    }

    fun saveSignaturePattern(points: List<NormalizedPoint>) {
        _workspaceState.update {
            it.copy(
                pendingSignaturePoints = points,
                activeTool = AnnotationTool.SIGNATURE
            )
        }
        postMessage("Signature ready — tap anywhere on the PDF page to place it")
    }

    fun commitStrokeOnCurrentPage(points: List<NormalizedPoint>) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        if (points.size < 2) return

        viewModelScope.launch {
            if (state.activeTool == AnnotationTool.ERASER) {
                eraseAnnotationsNearPoints(doc.id, state.currentPageIndex, points)
                return@launch
            }

            val toolType = when (state.activeTool) {
                AnnotationTool.HIGHLIGHTER -> AnnotationTool.HIGHLIGHTER.name
                else -> AnnotationTool.PEN.name
            }

            val entity = PageAnnotationEntity(
                documentId = doc.id,
                pageIndex = state.currentPageIndex,
                toolType = toolType,
                colorArgb = state.selectedColorArgb,
                strokeWidth = state.selectedStrokeWidth,
                pointsData = encodeNormalizedPoints(points)
            )
            repository.addAnnotation(entity)
            _workspaceState.update { it.copy(redoStack = emptyList()) }
        }
    }

    fun placeTapAnnotationAt(normX: Float, normY: Float) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        val x = normX.coerceIn(0.05f, 0.90f)
        val y = normY.coerceIn(0.05f, 0.92f)

        viewModelScope.launch {
            when (state.activeTool) {
                AnnotationTool.TEXT -> {
                    val label = state.pendingTextNote.trim().ifBlank { "Editorial Note" }
                    repository.addAnnotation(
                        PageAnnotationEntity(
                            documentId = doc.id,
                            pageIndex = state.currentPageIndex,
                            toolType = AnnotationTool.TEXT.name,
                            colorArgb = state.selectedColorArgb,
                            strokeWidth = state.selectedStrokeWidth,
                            textLabel = label,
                            normalizedX = x,
                            normalizedY = y
                        )
                    )
                    _workspaceState.update { it.copy(redoStack = emptyList()) }
                }
                AnnotationTool.STAMP -> {
                    val stamp = state.selectedStamp
                    repository.addAnnotation(
                        PageAnnotationEntity(
                            documentId = doc.id,
                            pageIndex = state.currentPageIndex,
                            toolType = AnnotationTool.STAMP.name,
                            colorArgb = stamp.badgeColorArgb,
                            strokeWidth = 3f,
                            textLabel = stamp.title,
                            normalizedX = x,
                            normalizedY = y
                        )
                    )
                    _workspaceState.update { it.copy(redoStack = emptyList()) }
                }
                AnnotationTool.SIGNATURE -> {
                    val basePoints = state.pendingSignaturePoints.ifEmpty {
                        defaultStylizedSignaturePoints()
                    }
                    // Translate signature points around tapped normalized (x, y)
                    val translated = basePoints.map { pt ->
                        NormalizedPoint(
                            x = (x + (pt.x - 0.5f) * 0.28f).coerceIn(0.02f, 0.98f),
                            y = (y + (pt.y - 0.5f) * 0.12f).coerceIn(0.02f, 0.98f)
                        )
                    }
                    repository.addAnnotation(
                        PageAnnotationEntity(
                            documentId = doc.id,
                            pageIndex = state.currentPageIndex,
                            toolType = AnnotationTool.SIGNATURE.name,
                            colorArgb = state.selectedColorArgb,
                            strokeWidth = 3.5f,
                            pointsData = encodeNormalizedPoints(translated),
                            normalizedX = x,
                            normalizedY = y
                        )
                    )
                    _workspaceState.update { it.copy(redoStack = emptyList()) }
                }
                AnnotationTool.ERASER -> {
                    eraseAnnotationsNearPoints(doc.id, state.currentPageIndex, listOf(NormalizedPoint(x, y)))
                }
                else -> Unit
            }
        }
    }

    private suspend fun eraseAnnotationsNearPoints(
        documentId: Long,
        pageIndex: Int,
        eraserPoints: List<NormalizedPoint>
    ) {
        val pageAnns = _workspaceState.value.documentAnnotations.filter { it.pageIndex == pageIndex }
        val threshold = 0.065f
        for (ann in pageAnns) {
            val hit = if (ann.pointsData.isNotBlank()) {
                val strokePts = decodeNormalizedPoints(ann.pointsData)
                strokePts.any { sp ->
                    eraserPoints.any { ep -> hypot(sp.x - ep.x, sp.y - ep.y) < threshold }
                }
            } else {
                eraserPoints.any { ep ->
                    hypot(ann.normalizedX - ep.x, ann.normalizedY - ep.y) < threshold * 1.4f
                }
            }
            if (hit) {
                repository.deleteAnnotationById(documentId, ann.id)
            }
        }
    }

    fun undoAnnotationOnCurrentPage() {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        val pageAnns = state.documentAnnotations.filter { it.pageIndex == state.currentPageIndex }
        val last = pageAnns.lastOrNull() ?: return

        viewModelScope.launch {
            repository.deleteAnnotationById(doc.id, last.id)
            _workspaceState.update { it.copy(redoStack = it.redoStack + last) }
        }
    }

    fun redoAnnotationOnCurrentPage() {
        val state = _workspaceState.value
        val lastRedo = state.redoStack.lastOrNull() ?: return
        viewModelScope.launch {
            repository.addAnnotation(lastRedo.copy(id = 0))
            _workspaceState.update { it.copy(redoStack = it.redoStack.dropLast(1)) }
        }
    }

    fun clearCurrentPageAnnotations() {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        viewModelScope.launch {
            repository.clearPageAnnotations(doc.id, state.currentPageIndex)
            _workspaceState.update { it.copy(redoStack = emptyList()) }
            postMessage("Cleared annotations on Page ${state.currentPageIndex + 1}")
        }
    }

    fun flattenAndSaveCurrentPdf() {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        viewModelScope.launch {
            _workspaceState.update { it.copy(isProcessing = true) }
            val ok = repository.flattenAnnotationsIntoPdfFile(doc.id)
            if (ok) {
                renderActivePageAndThumbnails(doc, state.currentPageIndex, state.readingFilter, reloadThumbs = true)
                postMessage("Annotations permanently flattened into ${doc.title}.pdf")
            } else {
                _workspaceState.update { it.copy(isProcessing = false) }
                postMessage("Unable to flatten PDF annotations")
            }
        }
    }

    // Bookmarks
    fun toggleBookmarkCurrentPage(customLabel: String? = null) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        viewModelScope.launch {
            val added = repository.toggleBookmark(doc, state.currentPageIndex, customLabel)
            postMessage(
                if (added) "Bookmarked Page ${state.currentPageIndex + 1}"
                else "Removed bookmark on Page ${state.currentPageIndex + 1}"
            )
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark.id)
            postMessage("Removed bookmark \"${bookmark.label}\"")
        }
    }

    fun openBookmark(bookmark: BookmarkEntity) {
        val doc = allDocuments.value.find { it.id == bookmark.documentId } ?: return
        openDocument(doc, initialMode = WorkspaceMode.READER, initialPage = bookmark.pageIndex)
    }

    // Page Manipulation (Rotate, Reorder, Duplicate, Delete, Insert, Extract)
    fun rotatePage(pageIndex: Int) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        viewModelScope.launch {
            _workspaceState.update { it.copy(isProcessing = true) }
            val ok = repository.rotatePage(doc.id, pageIndex)
            if (ok) {
                renderActivePageAndThumbnails(doc, state.currentPageIndex, state.readingFilter, reloadThumbs = true)
                postMessage("Rotated Page ${pageIndex + 1} by 90°")
            } else {
                _workspaceState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        if (toIndex !in 0 until doc.pageCount || fromIndex == toIndex) return

        val order = (0 until doc.pageCount).toMutableList()
        val item = order.removeAt(fromIndex)
        order.add(toIndex, item)

        viewModelScope.launch {
            _workspaceState.update { it.copy(isProcessing = true) }
            val updatedDoc = repository.reorganizePages(doc.id, order)
            if (updatedDoc != null) {
                _workspaceState.update {
                    it.copy(activeDocument = updatedDoc, currentPageIndex = toIndex)
                }
                renderActivePageAndThumbnails(updatedDoc, toIndex, state.readingFilter, reloadThumbs = true)
                postMessage("Moved Page ${fromIndex + 1} to position ${toIndex + 1}")
            } else {
                _workspaceState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun duplicatePageAt(pageIndex: Int) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        val order = (0 until doc.pageCount).toMutableList()
        order.add(pageIndex + 1, pageIndex)

        viewModelScope.launch {
            _workspaceState.update { it.copy(isProcessing = true) }
            val updatedDoc = repository.reorganizePages(doc.id, order)
            if (updatedDoc != null) {
                _workspaceState.update { it.copy(activeDocument = updatedDoc) }
                renderActivePageAndThumbnails(updatedDoc, pageIndex + 1, state.readingFilter, reloadThumbs = true)
                postMessage("Duplicated Page ${pageIndex + 1}")
            } else {
                _workspaceState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun deletePageAt(pageIndex: Int) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        if (doc.pageCount <= 1) {
            postMessage("A PDF document must contain at least 1 page")
            return
        }
        val order = (0 until doc.pageCount).filter { it != pageIndex }
        viewModelScope.launch {
            _workspaceState.update { it.copy(isProcessing = true) }
            val updatedDoc = repository.reorganizePages(doc.id, order)
            if (updatedDoc != null) {
                val nextIdx = state.currentPageIndex.coerceIn(0, updatedDoc.pageCount - 1)
                _workspaceState.update {
                    it.copy(activeDocument = updatedDoc, currentPageIndex = nextIdx)
                }
                renderActivePageAndThumbnails(updatedDoc, nextIdx, state.readingFilter, reloadThumbs = true)
                postMessage("Deleted Page ${pageIndex + 1}")
            } else {
                _workspaceState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun appendNewTemplatePage(templateType: PageTemplateType) {
        val state = _workspaceState.value
        val doc = state.activeDocument ?: return
        viewModelScope.launch {
            _workspaceState.update { it.copy(isProcessing = true) }
            val updatedDoc = repository.appendTemplatePage(doc.id, templateType)
            if (updatedDoc != null) {
                val newLastIdx = updatedDoc.pageCount - 1
                _workspaceState.update {
                    it.copy(activeDocument = updatedDoc, currentPageIndex = newLastIdx)
                }
                renderActivePageAndThumbnails(updatedDoc, newLastIdx, state.readingFilter, reloadThumbs = true)
                postMessage("Appended ${templateType.title} as Page ${updatedDoc.pageCount}")
            } else {
                _workspaceState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun extractPageAsStandalonePdf(pageIndex: Int) {
        val doc = _workspaceState.value.activeDocument ?: return
        viewModelScope.launch {
            val created = repository.extractPageAsNewDocument(doc, pageIndex)
            if (created != null) {
                postMessage("Extracted Page ${pageIndex + 1} as \"${created.title}\"")
            }
        }
    }

    // Library Management & Studio Creation
    fun toggleFavorite(document: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(document)
        }
    }

    fun renameDocument(document: PdfDocumentEntity, newTitle: String, newCategory: String) {
        viewModelScope.launch {
            repository.renameDocument(document, newTitle, newCategory)
            postMessage("Updated \"${newTitle.trim()}\"")
        }
    }

    fun duplicateDocument(document: PdfDocumentEntity) {
        viewModelScope.launch {
            val copy = repository.duplicateDocument(document)
            if (copy != null) {
                postMessage("Created copy \"${copy.title}\"")
            }
        }
    }

    fun deleteDocument(document: PdfDocumentEntity) {
        viewModelScope.launch {
            if (_workspaceState.value.activeDocument?.id == document.id) {
                closeWorkspace()
            }
            repository.deleteDocument(document)
            postMessage("Deleted \"${document.title}\"")
        }
    }

    fun createNewPdf(
        title: String,
        category: String,
        bodyContent: String,
        pageCount: Int,
        templateType: PageTemplateType,
        openImmediately: Boolean = true
    ) {
        viewModelScope.launch {
            val created = repository.createNewCustomPdf(
                title = title,
                category = category,
                bodyContent = bodyContent,
                pageCount = pageCount,
                templateType = templateType
            )
            if (created != null) {
                postMessage("Created \"${created.title}\" (${created.pageCount} pages)")
                if (openImmediately) {
                    openDocument(created, initialMode = WorkspaceMode.EDITOR, initialPage = 0)
                }
            } else {
                postMessage("Could not create PDF document")
            }
        }
    }

    fun mergeDocuments(documents: List<PdfDocumentEntity>, mergedTitle: String) {
        viewModelScope.launch {
            val merged = repository.mergeDocumentsIntoNewPdf(documents, mergedTitle)
            if (merged != null) {
                postMessage("Merged ${documents.size} PDFs into \"${merged.title}\" (${merged.pageCount} pages)")
                openDocument(merged, initialMode = WorkspaceMode.READER, initialPage = 0)
            } else {
                postMessage("Select at least 2 valid PDFs to merge")
            }
        }
    }

    fun importExternalPdf(uri: Uri, displayName: String) {
        viewModelScope.launch {
            val imported = repository.importPdfFromUri(uri, displayName)
            if (imported != null) {
                postMessage("Imported \"${imported.title}\" (${imported.pageCount} pages)")
                openDocument(imported, initialMode = WorkspaceMode.READER, initialPage = 0)
            } else {
                postMessage("Could not read valid PDF pages from selected file")
            }
        }
    }

    fun exportDocumentToUri(documentId: Long, targetUri: Uri) {
        viewModelScope.launch {
            _workspaceState.update { it.copy(isProcessing = true) }
            val ok = repository.exportPdfToUri(documentId, targetUri)
            _workspaceState.update { it.copy(isProcessing = false) }
            postMessage(if (ok) "Exported PDF successfully to device storage" else "Export failed")
        }
    }

    private fun defaultStylizedSignaturePoints(): List<NormalizedPoint> = listOf(
        NormalizedPoint(0.20f, 0.65f),
        NormalizedPoint(0.32f, 0.30f),
        NormalizedPoint(0.36f, 0.68f),
        NormalizedPoint(0.48f, 0.42f),
        NormalizedPoint(0.55f, 0.60f),
        NormalizedPoint(0.68f, 0.36f),
        NormalizedPoint(0.75f, 0.58f),
        NormalizedPoint(0.86f, 0.52f)
    )

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = FolioDatabase.getInstance(context.applicationContext)
                    val repo = PdfRepository(context.applicationContext, db.pdfDao())
                    return FolioViewModel(repo) as T
                }
            }
    }
}
