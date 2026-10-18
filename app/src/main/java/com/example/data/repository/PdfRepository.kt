package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.AnnotationTool
import com.example.data.local.BookmarkEntity
import com.example.data.local.NormalizedPoint
import com.example.data.local.PageAnnotationEntity
import com.example.data.local.PageTemplateType
import com.example.data.local.PdfDao
import com.example.data.local.PdfDocumentEntity
import com.example.data.local.ReadingFilterMode
import com.example.data.local.encodeNormalizedPoints
import com.example.data.pdf.PdfEngine
import com.example.data.pdf.SamplePdfFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PdfRepository(
    private val appContext: Context,
    private val pdfDao: PdfDao,
    private val pdfEngine: PdfEngine = PdfEngine()
) {
    val allDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.observeAllDocuments()
    val allBookmarks: Flow<List<BookmarkEntity>> = pdfDao.observeAllBookmarks()

    private val pdfStorageDir: File
        get() = File(appContext.filesDir, "pdfs").apply { if (!exists()) mkdirs() }

    fun observeBookmarksForDocument(documentId: Long): Flow<List<BookmarkEntity>> =
        pdfDao.observeBookmarksForDocument(documentId)

    fun observeAnnotationsForDocument(documentId: Long): Flow<List<PageAnnotationEntity>> =
        pdfDao.observeAnnotationsForDocument(documentId)

    suspend fun ensureSeedDocumentsLoaded() = withContext(Dispatchers.IO) {
        val existing = pdfDao.getAllDocumentsOnce()
        if (existing.isNotEmpty()) return@withContext

        for ((index, spec) in SamplePdfFactory.getSeedSpecs().withIndex()) {
            val outFile = File(pdfStorageDir, spec.fileName)
            val pages = spec.generator(outFile)
            val docId = pdfDao.insertDocument(
                PdfDocumentEntity(
                    title = spec.title,
                    filePath = outFile.absolutePath,
                    category = spec.category,
                    pageCount = pages,
                    fileSizeBytes = outFile.length(),
                    lastOpenedPage = 0,
                    isFavorite = spec.isFavorite,
                    annotationCount = if (index == 0) 2 else if (index == 1) 1 else 0,
                    lastModified = System.currentTimeMillis() - (index * 3600_000L)
                )
            )

            if (index == 0) {
                // Add sample bookmark and initial editable annotations on the Typography Specimen
                pdfDao.insertBookmark(
                    BookmarkEntity(
                        documentId = docId,
                        documentTitle = spec.title,
                        pageIndex = 0,
                        label = "Cover & Marginalia Overview"
                    )
                )
                pdfDao.insertBookmark(
                    BookmarkEntity(
                        documentId = docId,
                        documentTitle = spec.title,
                        pageIndex = 2,
                        label = "Pigment & Highlighter Swatches"
                    )
                )
                pdfDao.insertAnnotation(
                    PageAnnotationEntity(
                        documentId = docId,
                        pageIndex = 0,
                        toolType = AnnotationTool.HIGHLIGHTER.name,
                        colorArgb = 0xFFE59800L,
                        strokeWidth = 10f,
                        pointsData = encodeNormalizedPoints(
                            listOf(NormalizedPoint(0.08f, 0.65f), NormalizedPoint(0.72f, 0.65f))
                        )
                    )
                )
                pdfDao.insertAnnotation(
                    PageAnnotationEntity(
                        documentId = docId,
                        pageIndex = 0,
                        toolType = AnnotationTool.STAMP.name,
                        colorArgb = 0xFF168A59L,
                        strokeWidth = 3f,
                        textLabel = "APPROVED",
                        normalizedX = 0.66f,
                        normalizedY = 0.11f
                    )
                )
            } else if (index == 1) {
                pdfDao.insertBookmark(
                    BookmarkEntity(
                        documentId = docId,
                        documentTitle = spec.title,
                        pageIndex = 2,
                        label = "Counterparty Signature Block"
                    )
                )
                pdfDao.insertAnnotation(
                    PageAnnotationEntity(
                        documentId = docId,
                        pageIndex = 2,
                        toolType = AnnotationTool.STAMP.name,
                        colorArgb = 0xFF8E33D6L,
                        strokeWidth = 3f,
                        textLabel = "SIGN HERE",
                        normalizedX = 0.62f,
                        normalizedY = 0.36f
                    )
                )
            }
        }
    }

    suspend fun renderPdfPage(
        document: PdfDocumentEntity,
        pageIndex: Int,
        targetWidthPx: Int = 1080,
        filterMode: ReadingFilterMode = ReadingFilterMode.DAY
    ): Bitmap? {
        return pdfEngine.renderPage(
            file = File(document.filePath),
            pageIndex = pageIndex,
            targetWidthPx = targetWidthPx,
            filterMode = filterMode
        )
    }

    suspend fun updateLastOpenedPage(documentId: Long, pageIndex: Int) {
        val doc = pdfDao.getDocumentById(documentId) ?: return
        if (doc.lastOpenedPage != pageIndex) {
            pdfDao.updateDocument(
                doc.copy(
                    lastOpenedPage = pageIndex.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0)),
                    lastModified = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun toggleFavorite(document: PdfDocumentEntity) {
        pdfDao.updateDocument(document.copy(isFavorite = !document.isFavorite))
    }

    suspend fun renameDocument(document: PdfDocumentEntity, newTitle: String, newCategory: String) {
        if (newTitle.isBlank()) return
        pdfDao.updateDocument(
            document.copy(
                title = newTitle.trim(),
                category = newCategory.trim().ifBlank { document.category },
                lastModified = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteDocument(document: PdfDocumentEntity) = withContext(Dispatchers.IO) {
        pdfDao.deleteBookmarksForDocument(document.id)
        pdfDao.clearAnnotationsForDocument(document.id)
        pdfDao.deleteDocumentById(document.id)
        File(document.filePath).delete()
    }

    suspend fun duplicateDocument(document: PdfDocumentEntity): PdfDocumentEntity? =
        withContext(Dispatchers.IO) {
            val srcFile = File(document.filePath)
            if (!srcFile.exists()) return@withContext null
            val destFile = File(
                pdfStorageDir,
                "${srcFile.nameWithoutExtension}_copy_${System.currentTimeMillis()}.pdf"
            )
            srcFile.copyTo(destFile, overwrite = true)
            val newDoc = PdfDocumentEntity(
                title = "${document.title} (Copy)",
                filePath = destFile.absolutePath,
                category = document.category,
                pageCount = document.pageCount,
                fileSizeBytes = destFile.length(),
                lastOpenedPage = 0,
                isFavorite = document.isFavorite,
                annotationCount = 0,
                lastModified = System.currentTimeMillis()
            )
            val id = pdfDao.insertDocument(newDoc)
            pdfDao.getDocumentById(id)
        }

    suspend fun toggleBookmark(
        document: PdfDocumentEntity,
        pageIndex: Int,
        customLabel: String? = null
    ): Boolean {
        val existing = pdfDao.getBookmarkForPage(document.id, pageIndex)
        return if (existing != null) {
            pdfDao.deleteBookmarkById(existing.id)
            false
        } else {
            val label = customLabel?.takeIf { it.isNotBlank() }
                ?: "Page ${pageIndex + 1} Bookmark"
            pdfDao.insertBookmark(
                BookmarkEntity(
                    documentId = document.id,
                    documentTitle = document.title,
                    pageIndex = pageIndex,
                    label = label
                )
            )
            true
        }
    }

    suspend fun deleteBookmark(bookmarkId: Long) {
        pdfDao.deleteBookmarkById(bookmarkId)
    }

    suspend fun addAnnotation(annotation: PageAnnotationEntity): PageAnnotationEntity {
        val id = pdfDao.insertAnnotation(annotation)
        refreshAnnotationCount(annotation.documentId)
        return annotation.copy(id = id)
    }

    suspend fun deleteAnnotationById(documentId: Long, annotationId: Long) {
        pdfDao.deleteAnnotationById(annotationId)
        refreshAnnotationCount(documentId)
    }

    suspend fun clearPageAnnotations(documentId: Long, pageIndex: Int) {
        pdfDao.clearAnnotationsForPage(documentId, pageIndex)
        refreshAnnotationCount(documentId)
    }

    private suspend fun refreshAnnotationCount(documentId: Long) {
        val doc = pdfDao.getDocumentById(documentId) ?: return
        val allAnns = pdfDao.getAnnotationsForDocumentOnce(documentId)
        pdfDao.updateDocument(
            doc.copy(
                annotationCount = allAnns.size,
                lastModified = System.currentTimeMillis()
            )
        )
    }

    /**
     * Permanently bakes all active vector annotations into the underlying PDF file on disk
     * and clears the non-destructive overlay layer so they become part of the standard PDF pages.
     */
    suspend fun flattenAnnotationsIntoPdfFile(documentId: Long): Boolean {
        val doc = pdfDao.getDocumentById(documentId) ?: return false
        val allAnns = pdfDao.getAnnotationsForDocumentOnce(documentId)
        if (allAnns.isEmpty()) return true

        val grouped = allAnns.groupBy { it.pageIndex }
        val file = File(doc.filePath)
        val ok = pdfEngine.flattenAnnotationsIntoPdf(file, grouped)
        if (ok) {
            pdfDao.clearAnnotationsForDocument(documentId)
            pdfDao.updateDocument(
                doc.copy(
                    fileSizeBytes = file.length(),
                    annotationCount = 0,
                    lastModified = System.currentTimeMillis()
                )
            )
        }
        return ok
    }

    suspend fun rotatePage(documentId: Long, pageIndex: Int): Boolean {
        val doc = pdfDao.getDocumentById(documentId) ?: return false
        val file = File(doc.filePath)
        val ok = pdfEngine.rotatePageInPdf(file, pageIndex)
        if (ok) {
            pdfDao.updateDocument(
                doc.copy(
                    fileSizeBytes = file.length(),
                    lastModified = System.currentTimeMillis()
                )
            )
        }
        return ok
    }

    suspend fun reorganizePages(documentId: Long, newPageOrder: List<Int>): PdfDocumentEntity? {
        val doc = pdfDao.getDocumentById(documentId) ?: return null
        val file = File(doc.filePath)
        val updatedCount = pdfEngine.reorganizePagesInPdf(file, newPageOrder)
        if (updatedCount > 0) {
            val updated = doc.copy(
                pageCount = updatedCount,
                fileSizeBytes = file.length(),
                lastOpenedPage = doc.lastOpenedPage.coerceIn(0, (updatedCount - 1).coerceAtLeast(0)),
                lastModified = System.currentTimeMillis()
            )
            pdfDao.updateDocument(updated)
            return updated
        }
        return null
    }

    suspend fun appendTemplatePage(
        documentId: Long,
        templateType: PageTemplateType
    ): PdfDocumentEntity? {
        val doc = pdfDao.getDocumentById(documentId) ?: return null
        val file = File(doc.filePath)
        val updatedCount = pdfEngine.appendTemplatePageToPdf(file, templateType)
        if (updatedCount > 0) {
            val updated = doc.copy(
                pageCount = updatedCount,
                fileSizeBytes = file.length(),
                lastModified = System.currentTimeMillis()
            )
            pdfDao.updateDocument(updated)
            return updated
        }
        return null
    }

    suspend fun extractPageAsNewDocument(
        document: PdfDocumentEntity,
        pageIndex: Int
    ): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        val srcFile = File(document.filePath)
        if (!srcFile.exists()) return@withContext null
        val destFile = File(
            pdfStorageDir,
            "extracted_p${pageIndex + 1}_${System.currentTimeMillis()}.pdf"
        )
        srcFile.copyTo(destFile, overwrite = true)
        val count = pdfEngine.reorganizePagesInPdf(destFile, listOf(pageIndex))
        if (count > 0) {
            val newDoc = PdfDocumentEntity(
                title = "${document.title} — Page ${pageIndex + 1}",
                filePath = destFile.absolutePath,
                category = document.category,
                pageCount = count,
                fileSizeBytes = destFile.length(),
                lastOpenedPage = 0,
                isFavorite = false,
                annotationCount = 0
            )
            val id = pdfDao.insertDocument(newDoc)
            pdfDao.getDocumentById(id)
        } else {
            destFile.delete()
            null
        }
    }

    suspend fun createNewCustomPdf(
        title: String,
        category: String,
        bodyContent: String,
        pageCount: Int,
        templateType: PageTemplateType
    ): PdfDocumentEntity? {
        val cleanTitle = title.trim().ifBlank { "Untitled Folio Document" }
        val fileName = "folio_${System.currentTimeMillis()}.pdf"
        val destFile = File(pdfStorageDir, fileName)
        val writtenPages = pdfEngine.createNewPdfDocument(
            destFile = destFile,
            title = cleanTitle,
            bodyContent = bodyContent,
            pageCount = pageCount,
            templateType = templateType
        )
        if (writtenPages <= 0) return null
        val entity = PdfDocumentEntity(
            title = cleanTitle,
            filePath = destFile.absolutePath,
            category = category.ifBlank { "Created" },
            pageCount = writtenPages,
            fileSizeBytes = destFile.length()
        )
        val id = pdfDao.insertDocument(entity)
        return pdfDao.getDocumentById(id)
    }

    suspend fun mergeDocumentsIntoNewPdf(
        documents: List<PdfDocumentEntity>,
        mergedTitle: String
    ): PdfDocumentEntity? {
        if (documents.size < 2) return null
        val cleanTitle = mergedTitle.trim().ifBlank { "Merged Folio Compilation" }
        val destFile = File(pdfStorageDir, "merged_${System.currentTimeMillis()}.pdf")
        val sourceFiles = documents.map { File(it.filePath) }
        val totalPages = pdfEngine.mergePdfFiles(sourceFiles, destFile)
        if (totalPages <= 0) return null
        val entity = PdfDocumentEntity(
            title = cleanTitle,
            filePath = destFile.absolutePath,
            category = "Merged",
            pageCount = totalPages,
            fileSizeBytes = destFile.length()
        )
        val id = pdfDao.insertDocument(entity)
        return pdfDao.getDocumentById(id)
    }

    suspend fun importPdfFromUri(uri: Uri, fallbackName: String): PdfDocumentEntity? =
        withContext(Dispatchers.IO) {
            try {
                val destFile = File(pdfStorageDir, "imported_${System.currentTimeMillis()}.pdf")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val pageCount = pdfEngine.getPageCount(destFile)
                if (pageCount <= 0) {
                    destFile.delete()
                    return@withContext null
                }
                val cleanTitle = fallbackName.removeSuffix(".pdf").removeSuffix(".PDF")
                    .ifBlank { "Imported Document" }
                val entity = PdfDocumentEntity(
                    title = cleanTitle,
                    filePath = destFile.absolutePath,
                    category = "Imported",
                    pageCount = pageCount,
                    fileSizeBytes = destFile.length()
                )
                val id = pdfDao.insertDocument(entity)
                pdfDao.getDocumentById(id)
            } catch (e: Exception) {
                null
            }
        }

    suspend fun exportPdfToUri(documentId: Long, targetUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // First flatten any unbaked annotations into a copy or the file itself
                flattenAnnotationsIntoPdfFile(documentId)
                val doc = pdfDao.getDocumentById(documentId) ?: return@withContext false
                val srcFile = File(doc.filePath)
                if (!srcFile.exists()) return@withContext false
                appContext.contentResolver.openOutputStream(targetUri)?.use { output ->
                    FileInputStream(srcFile).use { input ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
}
