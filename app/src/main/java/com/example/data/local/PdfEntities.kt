package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val category: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val lastOpenedPage: Int = 0,
    val isFavorite: Boolean = false,
    val annotationCount: Int = 0,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "pdf_bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val documentTitle: String,
    val pageIndex: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "page_annotations")
data class PageAnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val toolType: String, // PEN, HIGHLIGHTER, TEXT, STAMP, SIGNATURE
    val colorArgb: Long,
    val strokeWidth: Float,
    val pointsData: String = "", // "x1,y1;x2,y2;..." in normalized [0..1] page coordinates
    val textLabel: String = "",
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AnnotationTool(val label: String) {
    PAN("Pan & Zoom"),
    PEN("Ink Pen"),
    HIGHLIGHTER("Highlighter"),
    TEXT("Text Note"),
    STAMP("Official Stamp"),
    SIGNATURE("Signature"),
    ERASER("Eraser")
}

enum class ReadingFilterMode(val label: String) {
    DAY("Original Paper"),
    SEPIA("Warm Sepia"),
    NIGHT("Dark Inverted"),
    CONTRAST("High Contrast")
}

enum class StampPreset(val title: String, val badgeColorArgb: Long) {
    APPROVED("APPROVED", 0xFF168A59L),
    CONFIDENTIAL("CONFIDENTIAL", 0xFFD93829L),
    DRAFT("DRAFT COPY", 0xFFE59800L),
    REVIEWED("REVIEWED", 0xFF1D63DCL),
    SIGN_HERE("SIGN HERE", 0xFF8E33D6L)
}

enum class PageTemplateType(val title: String, val description: String) {
    BLANK("Blank Folio Sheet", "Clean archival cream-white sheet for freeform notes or sketches"),
    RULED("Editorial Ruled Lines", "Horizontal college-ruled lines with a left vermilion margin"),
    DOT_GRID("Architectural Dot Grid", "Precision 5mm isometric dot matrix for technical diagrams"),
    CONTRACT("Executive Memo Header", "Structured document header with date, subject, and signature block")
}

data class NormalizedPoint(val x: Float, val y: Float)

fun encodeNormalizedPoints(points: List<NormalizedPoint>): String {
    return points.joinToString(";") { "${(it.x * 10000).toInt() / 10000f},${(it.y * 10000).toInt() / 10000f}" }
}

fun decodeNormalizedPoints(data: String): List<NormalizedPoint> {
    if (data.isBlank()) return emptyList()
    return data.split(";").mapNotNull { pair ->
        val parts = pair.split(",")
        if (parts.size == 2) {
            val x = parts[0].toFloatOrNull()
            val y = parts[1].toFloatOrNull()
            if (x != null && y != null) NormalizedPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)) else null
        } else null
    }
}
