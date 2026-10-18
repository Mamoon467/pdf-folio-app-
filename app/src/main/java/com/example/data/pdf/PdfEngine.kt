package com.example.data.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.local.AnnotationTool
import com.example.data.local.PageAnnotationEntity
import com.example.data.local.PageTemplateType
import com.example.data.local.ReadingFilterMode
import com.example.data.local.decodeNormalizedPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfEngine {

    private val renderMutex = Mutex()

    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext 0
        renderMutex.withLock {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        renderer.pageCount
                    }
                }
            } catch (e: Exception) {
                0
            }
        }
    }

    suspend fun renderPage(
        file: File,
        pageIndex: Int,
        targetWidthPx: Int = 1080,
        filterMode: ReadingFilterMode = ReadingFilterMode.DAY
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        renderMutex.withLock {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null
                        renderer.openPage(pageIndex).use { page ->
                            val scale = targetWidthPx.toFloat() / page.width.coerceAtLeast(1)
                            val bmpW = targetWidthPx.coerceAtLeast(1)
                            val bmpH = (page.height * scale).toInt().coerceAtLeast(1)

                            val rawBitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(rawBitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(rawBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            if (filterMode == ReadingFilterMode.DAY) {
                                rawBitmap
                            } else {
                                applyReadingFilter(rawBitmap, filterMode)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun applyReadingFilter(source: Bitmap, mode: ReadingFilterMode): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val matrix = when (mode) {
            ReadingFilterMode.DAY -> ColorMatrix()
            ReadingFilterMode.SEPIA -> ColorMatrix(
                floatArrayOf(
                    0.94f, 0.04f, 0.0f, 0f, 8f,
                    0.02f, 0.89f, 0.02f, 0f, 4f,
                    0.0f, 0.04f, 0.78f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ReadingFilterMode.NIGHT -> ColorMatrix(
                floatArrayOf(
                    -0.88f, 0f, 0f, 0f, 240f,
                    0f, -0.88f, 0f, 0f, 242f,
                    0f, 0f, -0.85f, 0f, 248f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ReadingFilterMode.CONTRAST -> ColorMatrix(
                floatArrayOf(
                    1.35f, 0f, 0f, 0f, -35f,
                    0f, 1.35f, 0f, 0f, -35f,
                    0f, 0f, 1.35f, 0f, -35f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        source.recycle()
        return output
    }

    /**
     * Flattens ("bakes") all vector annotations for a document directly into the PDF pages
     * so exported or saved files permanently include the user's ink, highlights, stamps, text, and signatures.
     */
    suspend fun flattenAnnotationsIntoPdf(
        pdfFile: File,
        annotationsByPage: Map<Int, List<PageAnnotationEntity>>
    ): Boolean = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext false
        val tempFile = File(pdfFile.parentFile, "${pdfFile.nameWithoutExtension}_flatten_tmp.pdf")
        val success = renderMutex.withLock {
            try {
                val outDoc = PdfDocument()
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { srcPage ->
                                val w = srcPage.width
                                val h = srcPage.height
                                // Render at 2x resolution for crisp output
                                val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                                val bmpCanvas = Canvas(bmp)
                                bmpCanvas.drawColor(Color.WHITE)
                                srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                                val destPage = outDoc.startPage(pageInfo)
                                val pdfCanvas = destPage.canvas
                                pdfCanvas.drawBitmap(
                                    bmp,
                                    null,
                                    RectF(0f, 0f, w.toFloat(), h.toFloat()),
                                    Paint(Paint.FILTER_BITMAP_FLAG)
                                )
                                bmp.recycle()

                                val pageAnns = annotationsByPage[i].orEmpty()
                                drawAnnotationsOnPdfCanvas(pdfCanvas, w.toFloat(), h.toFloat(), pageAnns)
                                outDoc.finishPage(destPage)
                            }
                        }
                    }
                }
                FileOutputStream(tempFile).use { outDoc.writeTo(it) }
                outDoc.close()
                true
            } catch (e: Exception) {
                false
            }
        }
        if (success && tempFile.exists()) {
            tempFile.copyTo(pdfFile, overwrite = true)
            tempFile.delete()
            return@withContext true
        }
        return@withContext false
    }

    private fun drawAnnotationsOnPdfCanvas(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        annotations: List<PageAnnotationEntity>
    ) {
        for (ann in annotations) {
            val baseColor = ann.colorArgb.toInt()
            when (ann.toolType) {
                AnnotationTool.PEN.name, AnnotationTool.SIGNATURE.name -> {
                    val pts = decodeNormalizedPoints(ann.pointsData)
                    if (pts.size >= 2) {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = baseColor
                            style = Paint.Style.STROKE
                            strokeWidth = ann.strokeWidth.coerceAtLeast(2f)
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }
                        val path = Path()
                        path.moveTo(pts.first().x * pageWidth, pts.first().y * pageHeight)
                        for (k in 1 until pts.size) {
                            path.lineTo(pts[k].x * pageWidth, pts[k].y * pageHeight)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                AnnotationTool.HIGHLIGHTER.name -> {
                    val pts = decodeNormalizedPoints(ann.pointsData)
                    if (pts.size >= 2) {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.argb(
                                95,
                                Color.red(baseColor),
                                Color.green(baseColor),
                                Color.blue(baseColor)
                            )
                            style = Paint.Style.STROKE
                            strokeWidth = (ann.strokeWidth * 2.4f).coerceAtLeast(14f)
                            strokeCap = Paint.Cap.SQUARE
                            strokeJoin = Paint.Join.ROUND
                        }
                        val path = Path()
                        path.moveTo(pts.first().x * pageWidth, pts.first().y * pageHeight)
                        for (k in 1 until pts.size) {
                            path.lineTo(pts[k].x * pageWidth, pts[k].y * pageHeight)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                AnnotationTool.TEXT.name -> {
                    if (ann.textLabel.isNotBlank()) {
                        val x = (ann.normalizedX * pageWidth).coerceIn(24f, pageWidth - 160f)
                        val y = (ann.normalizedY * pageHeight).coerceIn(32f, pageHeight - 32f)
                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.WHITE
                            textSize = 12f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        }
                        val textW = textPaint.measureText(ann.textLabel)
                        val pillRect = RectF(x - 10f, y - 18f, x + textW + 10f, y + 8f)
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = baseColor }
                        canvas.drawRoundRect(pillRect, 6f, 6f, bgPaint)
                        canvas.drawText(ann.textLabel, x, y - 1f, textPaint)
                    }
                }
                AnnotationTool.STAMP.name -> {
                    if (ann.textLabel.isNotBlank()) {
                        val x = (ann.normalizedX * pageWidth).coerceIn(36f, pageWidth - 150f)
                        val y = (ann.normalizedY * pageHeight).coerceIn(36f, pageHeight - 36f)
                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = baseColor
                            textSize = 14f
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        }
                        val textW = textPaint.measureText(ann.textLabel)
                        val stampRect = RectF(x - 12f, y - 22f, x + textW + 12f, y + 10f)
                        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = baseColor
                            style = Paint.Style.STROKE
                            strokeWidth = 2.5f
                        }
                        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.argb(
                                32,
                                Color.red(baseColor),
                                Color.green(baseColor),
                                Color.blue(baseColor)
                            )
                        }
                        canvas.drawRoundRect(stampRect, 5f, 5f, fillPaint)
                        canvas.drawRoundRect(stampRect, 5f, 5f, borderPaint)
                        canvas.drawText(ann.textLabel, x, y - 1f, textPaint)
                    }
                }
            }
        }
    }

    /**
     * Rotates a specific page in the PDF by 90 degrees clockwise.
     */
    suspend fun rotatePageInPdf(pdfFile: File, targetPageIndex: Int): Boolean = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext false
        val tempFile = File(pdfFile.parentFile, "${pdfFile.nameWithoutExtension}_rot_tmp.pdf")
        val ok = renderMutex.withLock {
            try {
                val outDoc = PdfDocument()
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { srcPage ->
                                val w = srcPage.width
                                val h = srcPage.height
                                val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                if (i == targetPageIndex) {
                                    val pageInfo = PdfDocument.PageInfo.Builder(h, w, i + 1).create()
                                    val destPage = outDoc.startPage(pageInfo)
                                    val matrix = Matrix().apply {
                                        postScale(0.5f, 0.5f)
                                        postRotate(90f)
                                        postTranslate(h.toFloat(), 0f)
                                    }
                                    destPage.canvas.drawBitmap(bmp, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
                                    outDoc.finishPage(destPage)
                                } else {
                                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                                    val destPage = outDoc.startPage(pageInfo)
                                    destPage.canvas.drawBitmap(
                                        bmp,
                                        null,
                                        RectF(0f, 0f, w.toFloat(), h.toFloat()),
                                        Paint(Paint.FILTER_BITMAP_FLAG)
                                    )
                                    outDoc.finishPage(destPage)
                                }
                                bmp.recycle()
                            }
                        }
                    }
                }
                FileOutputStream(tempFile).use { outDoc.writeTo(it) }
                outDoc.close()
                true
            } catch (e: Exception) {
                false
            }
        }
        if (ok && tempFile.exists()) {
            tempFile.copyTo(pdfFile, overwrite = true)
            tempFile.delete()
            return@withContext true
        }
        false
    }

    /**
     * Reorders, deletes, or duplicates pages according to [newPageOrder] (list of source page indices).
     */
    suspend fun reorganizePagesInPdf(pdfFile: File, newPageOrder: List<Int>): Int = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || newPageOrder.isEmpty()) return@withContext 0
        val tempFile = File(pdfFile.parentFile, "${pdfFile.nameWithoutExtension}_reorg_tmp.pdf")
        val finalCount = renderMutex.withLock {
            try {
                val outDoc = PdfDocument()
                var written = 0
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for ((outIdx, srcIdx) in newPageOrder.withIndex()) {
                            if (srcIdx in 0 until renderer.pageCount) {
                                renderer.openPage(srcIdx).use { srcPage ->
                                    val w = srcPage.width
                                    val h = srcPage.height
                                    val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                                    bmp.eraseColor(Color.WHITE)
                                    srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, outIdx + 1).create()
                                    val destPage = outDoc.startPage(pageInfo)
                                    destPage.canvas.drawBitmap(
                                        bmp,
                                        null,
                                        RectF(0f, 0f, w.toFloat(), h.toFloat()),
                                        Paint(Paint.FILTER_BITMAP_FLAG)
                                    )
                                    outDoc.finishPage(destPage)
                                    bmp.recycle()
                                    written++
                                }
                            }
                        }
                    }
                }
                FileOutputStream(tempFile).use { outDoc.writeTo(it) }
                outDoc.close()
                written
            } catch (e: Exception) {
                0
            }
        }
        if (finalCount > 0 && tempFile.exists()) {
            tempFile.copyTo(pdfFile, overwrite = true)
            tempFile.delete()
            return@withContext finalCount
        }
        0
    }

    /**
     * Appends a new formatted page (Blank, Ruled, Dot Grid, or Contract Header) to an existing PDF.
     */
    suspend fun appendTemplatePageToPdf(pdfFile: File, templateType: PageTemplateType): Int =
        withContext(Dispatchers.IO) {
            if (!pdfFile.exists()) return@withContext 0
            val tempFile = File(pdfFile.parentFile, "${pdfFile.nameWithoutExtension}_append_tmp.pdf")
            val newCount = renderMutex.withLock {
                try {
                    val outDoc = PdfDocument()
                    var count = 0
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            for (i in 0 until renderer.pageCount) {
                                renderer.openPage(i).use { srcPage ->
                                    val w = srcPage.width
                                    val h = srcPage.height
                                    val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                                    bmp.eraseColor(Color.WHITE)
                                    srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                                    val destPage = outDoc.startPage(pageInfo)
                                    destPage.canvas.drawBitmap(
                                        bmp,
                                        null,
                                        RectF(0f, 0f, w.toFloat(), h.toFloat()),
                                        Paint(Paint.FILTER_BITMAP_FLAG)
                                    )
                                    outDoc.finishPage(destPage)
                                    bmp.recycle()
                                    count++
                                }
                            }
                        }
                    }
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, count + 1).create()
                    val newPage = outDoc.startPage(pageInfo)
                    drawTemplateSheet(newPage.canvas, templateType, "Inserted Folio Page #${count + 1}", "")
                    outDoc.finishPage(newPage)
                    count++

                    FileOutputStream(tempFile).use { outDoc.writeTo(it) }
                    outDoc.close()
                    count
                } catch (e: Exception) {
                    0
                }
            }
            if (newCount > 0 && tempFile.exists()) {
                tempFile.copyTo(pdfFile, overwrite = true)
                tempFile.delete()
                return@withContext newCount
            }
            0
        }

    /**
     * Creates a brand-new multi-page PDF document from user parameters and template style.
     */
    suspend fun createNewPdfDocument(
        destFile: File,
        title: String,
        bodyContent: String,
        pageCount: Int,
        templateType: PageTemplateType
    ): Int = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            try {
                val doc = PdfDocument()
                val total = pageCount.coerceIn(1, 20)
                for (i in 0 until total) {
                    val info = PdfDocument.PageInfo.Builder(595, 842, i + 1).create()
                    val page = doc.startPage(info)
                    drawTemplateSheet(
                        canvas = page.canvas,
                        templateType = templateType,
                        heading = if (i == 0) title else "$title — Page ${i + 1}",
                        bodyText = if (i == 0) bodyContent else ""
                    )
                    doc.finishPage(page)
                }
                FileOutputStream(destFile).use { doc.writeTo(it) }
                doc.close()
                total
            } catch (e: Exception) {
                0
            }
        }
    }

    /**
     * Merges multiple PDF files into a single output PDF file.
     */
    suspend fun mergePdfFiles(sourceFiles: List<File>, destFile: File): Int = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            try {
                val outDoc = PdfDocument()
                var totalPages = 0
                for (file in sourceFiles) {
                    if (!file.exists()) continue
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            for (i in 0 until renderer.pageCount) {
                                renderer.openPage(i).use { srcPage ->
                                    val w = srcPage.width
                                    val h = srcPage.height
                                    val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                                    bmp.eraseColor(Color.WHITE)
                                    srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                    totalPages++
                                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, totalPages).create()
                                    val destPage = outDoc.startPage(pageInfo)
                                    destPage.canvas.drawBitmap(
                                        bmp,
                                        null,
                                        RectF(0f, 0f, w.toFloat(), h.toFloat()),
                                        Paint(Paint.FILTER_BITMAP_FLAG)
                                    )
                                    outDoc.finishPage(destPage)
                                    bmp.recycle()
                                }
                            }
                        }
                    }
                }
                if (totalPages > 0) {
                    FileOutputStream(destFile).use { outDoc.writeTo(it) }
                }
                outDoc.close()
                totalPages
            } catch (e: Exception) {
                0
            }
        }
    }

    private fun drawTemplateSheet(
        canvas: Canvas,
        templateType: PageTemplateType,
        heading: String,
        bodyText: String
    ) {
        val bgPaint = Paint().apply { color = Color.parseColor("#FDFBF7") }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        when (templateType) {
            PageTemplateType.BLANK -> Unit
            PageTemplateType.RULED -> {
                val lineP = Paint().apply {
                    color = Color.parseColor("#DED7C8")
                    strokeWidth = 1f
                }
                for (y in 130..780 step 26) {
                    canvas.drawLine(48f, y.toFloat(), 547f, y.toFloat(), lineP)
                }
                val marginP = Paint().apply {
                    color = Color.parseColor("#D93829")
                    strokeWidth = 1.4f
                }
                canvas.drawLine(96f, 90f, 96f, 790f, marginP)
            }
            PageTemplateType.DOT_GRID -> {
                val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C7BFB0") }
                for (x in 52..545 step 22) {
                    for (y in 125..785 step 22) {
                        canvas.drawCircle(x.toFloat(), y.toFloat(), 1.3f, dotP)
                    }
                }
            }
            PageTemplateType.CONTRACT -> {
                val barP = Paint().apply { color = Color.parseColor("#191A1E") }
                canvas.drawRect(48f, 48f, 547f, 64f, barP)
                val sigP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#D93829")
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                canvas.drawRoundRect(RectF(48f, 660f, 547f, 775f), 10f, 10f, sigP)
                val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#D93829")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                canvas.drawText("EXECUTIVE SIGN-OFF & STAMP AREA", 64f, 684f, labelP)
            }
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText(heading, 48f, 96f, titlePaint)

        if (bodyText.isNotBlank()) {
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#353430")
                textSize = 13f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val lines = bodyText.lines()
            var y = 148f
            for (rawLine in lines) {
                // Wrap long lines roughly at 68 chars
                val chunks = rawLine.chunked(68).ifEmpty { listOf("") }
                for (chunk in chunks) {
                    if (y < 760f) {
                        canvas.drawText(chunk, 48f, y, bodyPaint)
                        y += 24f
                    }
                }
            }
        }
    }
}
