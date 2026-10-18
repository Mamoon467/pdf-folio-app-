package com.example.data.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object SamplePdfFactory {

    private const val PAGE_WIDTH = 595 // Standard A4 points width
    private const val PAGE_HEIGHT = 842 // Standard A4 points height

    data class SeedSpec(
        val fileName: String,
        val title: String,
        val category: String,
        val isFavorite: Boolean,
        val generator: (File) -> Int
    )

    fun getSeedSpecs(): List<SeedSpec> = listOf(
        SeedSpec(
            fileName = "editorial_typography_specimen.pdf",
            title = "Editorial Design & Typography Specimen",
            category = "Design",
            isFavorite = true,
            generator = ::generateEditorialSpecimenPdf
        ),
        SeedSpec(
            fileName = "service_agreement_contract.pdf",
            title = "Master Creative Services Agreement",
            category = "Contracts",
            isFavorite = true,
            generator = ::generateContractPdf
        ),
        SeedSpec(
            fileName = "q4_system_architecture_blueprint.pdf",
            title = "Q4 Cloud & Mobile Architecture Blueprint",
            category = "Reports",
            isFavorite = false,
            generator = ::generateArchitectureBlueprintPdf
        ),
        SeedSpec(
            fileName = "field_research_journal.pdf",
            title = "Field Research & Reading Notes",
            category = "Notes",
            isFavorite = false,
            generator = ::generateResearchNotesPdf
        )
    )

    private fun generateEditorialSpecimenPdf(targetFile: File): Int {
        val doc = PdfDocument()
        val pageCount = 4
        for (pageIdx in 0 until pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            drawPaperBackground(canvas, pageIdx + 1, pageCount, "FOLIO DESIGN SYSTEM • ISSUE 04")

            when (pageIdx) {
                0 -> drawSpecimenCoverPage(canvas)
                1 -> drawSpecimenGridAndScalePage(canvas)
                2 -> drawSpecimenColorPalettePage(canvas)
                3 -> drawSpecimenMarginaliaWorksheet(canvas)
            }
            doc.finishPage(page)
        }
        FileOutputStream(targetFile).use { doc.writeTo(it) }
        doc.close()
        return pageCount
    }

    private fun generateContractPdf(targetFile: File): Int {
        val doc = PdfDocument()
        val pageCount = 3
        for (pageIdx in 0 until pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            drawPaperBackground(canvas, pageIdx + 1, pageCount, "LEGAL INSTRUMENT • REF #MSA-2026-89")

            when (pageIdx) {
                0 -> drawContractPageOne(canvas)
                1 -> drawContractPageTwo(canvas)
                2 -> drawContractSignaturePage(canvas)
            }
            doc.finishPage(page)
        }
        FileOutputStream(targetFile).use { doc.writeTo(it) }
        doc.close()
        return pageCount
    }

    private fun generateArchitectureBlueprintPdf(targetFile: File): Int {
        val doc = PdfDocument()
        val pageCount = 3
        for (pageIdx in 0 until pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            drawPaperBackground(canvas, pageIdx + 1, pageCount, "ENGINEERING RFC • ARCHITECTURE BLUEPRINT")

            when (pageIdx) {
                0 -> drawBlueprintOverviewPage(canvas)
                1 -> drawBlueprintMetricsPage(canvas)
                2 -> drawBlueprintRoadmapPage(canvas)
            }
            doc.finishPage(page)
        }
        FileOutputStream(targetFile).use { doc.writeTo(it) }
        doc.close()
        return pageCount
    }

    private fun generateResearchNotesPdf(targetFile: File): Int {
        val doc = PdfDocument()
        val pageCount = 2
        for (pageIdx in 0 until pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            drawPaperBackground(canvas, pageIdx + 1, pageCount, "FIELD NOTEBOOK • ARCHIVAL SERIES")

            if (pageIdx == 0) {
                drawResearchSummaryPage(canvas)
            } else {
                drawRuledObservationsPage(canvas)
            }
            doc.finishPage(page)
        }
        FileOutputStream(targetFile).use { doc.writeTo(it) }
        doc.close()
        return pageCount
    }

    private fun drawPaperBackground(
        canvas: Canvas,
        pageNumber: Int,
        totalPages: Int,
        runningHeader: String
    ) {
        val bgPaint = Paint().apply { color = Color.parseColor("#FDFBF7") }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2DBD0")
            strokeWidth = 1f
        }
        canvas.drawLine(48f, 52f, PAGE_WIDTH - 48f, 52f, rulePaint)
        canvas.drawLine(48f, PAGE_HEIGHT - 52f, PAGE_WIDTH - 48f, PAGE_HEIGHT - 52f, rulePaint)

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757067")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        canvas.drawText(runningHeader, 48f, 40f, metaPaint)
        val pageLabel = "PAGE 0$pageNumber / 0$totalPages"
        val labelWidth = metaPaint.measureText(pageLabel)
        canvas.drawText(pageLabel, PAGE_WIDTH - 48f - labelWidth, 40f, metaPaint)
        canvas.drawText("FOLIO STUDIO ARCHIVE", 48f, PAGE_HEIGHT - 34f, metaPaint)
    }

    private fun drawSpecimenCoverPage(canvas: Canvas) {
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D93829") }
        canvas.drawRoundRect(RectF(48f, 76f, 168f, 100f), 6f, 6f, accentPaint)

        val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("SPECIMEN NO. 01", 60f, 92f, badgeText)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("The Art of Print &", 48f, 152f, titlePaint)
        canvas.drawText("Digital Marginalia", 48f, 194f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5C5A54")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(
            "A practical study in Swiss grids, editorial typography, and interactive PDF annotation.",
            48f,
            228f,
            subtitlePaint
        )

        // Hero geometric composition block
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#191A1E") }
        canvas.drawRoundRect(RectF(48f, 256f, PAGE_WIDTH - 48f, 470f), 14f, 14f, cardPaint)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D93829") }
        canvas.drawCircle(160f, 363f, 68f, circlePaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E59800") }
        canvas.drawRoundRect(RectF(260f, 300f, 505f, 342f), 8f, 8f, goldPaint)

        val whiteSerif = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAF6F0")
            textSize = 44f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Aa", 132f, 378f, whiteSerif)

        val cardSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2DBD0")
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(" Display Serif + Grotesque Sans + Technical Mono", 260f, 375f, cardSubPaint)
        canvas.drawText(" Designed for high-density reading & stylus markup.", 260f, 398f, cardSubPaint)
        canvas.drawText(" Try the Highlighter or Ink Pen on the paragraphs below!", 260f, 421f, cardSubPaint)

        drawParagraphBlock(
            canvas = canvas,
            startX = 48f,
            startY = 515f,
            heading = "1. Reading & Annotating with Precision",
            lines = listOf(
                "Modern technical documents demand more than passive viewing. Engineers, researchers,",
                "and designers annotate directly on the page—highlighting key clauses, sketching margin",
                "diagrams, and stamping review approvals directly onto the portable document canvas.",
                "",
                "Use the Editor toolbar below to test freehand Ink strokes, translucent Highlighter",
                "markers, custom Text Callouts, official Stamps, or your personal Signature."
            )
        )
    }

    private fun drawSpecimenGridAndScalePage(canvas: Canvas) {
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Modular Type Scale & Swiss Proportions", 48f, 105f, h1)

        val sizes = listOf(
            Triple("Display 36pt", 30f, "Editorial Headline Specimen"),
            Triple("Headline 24pt", 22f, "Section Header & Folio Title"),
            Triple("Title 18pt", 17f, "Sub-section Lead & Callout Header"),
            Triple("Body 14pt", 13.5f, "Primary long-form reading copy set with generous line height.")
        )

        var y = 160f
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D93829")
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5DEC9")
            strokeWidth = 1f
        }

        for ((label, size, sample) in sizes) {
            canvas.drawText(label, 48f, y, monoPaint)
            val samplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#191A1E")
                textSize = size
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
            canvas.drawText(sample, 48f, y + size + 8f, samplePaint)
            y += size + 36f
            canvas.drawLine(48f, y - 14f, PAGE_WIDTH - 48f, y - 14f, linePaint)
        }

        // Draw 2-column grid demonstration
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3EDE2")
        }
        canvas.drawRoundRect(RectF(48f, 470f, 285f, 740f), 10f, 10f, boxPaint)
        canvas.drawRoundRect(RectF(310f, 470f, PAGE_WIDTH - 48f, 740f), 10f, 10f, boxPaint)

        drawParagraphBlock(
            canvas,
            64f,
            505f,
            "Column A: Asymmetric Rhythm",
            listOf(
                "Pairing a high-contrast display",
                "serif with a clean geometric sans",
                "creates immediate hierarchy",
                "across multi-page reports.",
                "",
                "Readers scan headings in 200ms",
                "before committing to body text."
            )
        )
        drawParagraphBlock(
            canvas,
            326f,
            505f,
            "Column B: Monospace Data",
            listOf(
                "Tabular figures and page",
                "coordinates belong in a crisp",
                "monospace typeface so numbers",
                "align vertically across rows.",
                "",
                "Ratio: 1 : 1.414 (ISO A4)"
            )
        )
    }

    private fun drawSpecimenColorPalettePage(canvas: Canvas) {
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Pigment & Highlighter Calibration", 48f, 105f, h1)

        val swatches = listOf(
            Triple("Vermilion Ink", "#D93829", "Primary editorial markup & correction lines"),
            Triple("Amber Gold", "#E59800", "High-visibility passage highlighter"),
            Triple("Cobalt Blueprint", "#1D63DC", "Architectural notes & technical cross-references"),
            Triple("Emerald Verified", "#168A59", "Sign-off stamps & audit verification"),
            Triple("Obsidian Charcoal", "#191A1E", "Primary archival body text & signatures")
        )

        var top = 140f
        for ((name, hex, desc) in swatches) {
            val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(hex) }
            canvas.drawRoundRect(RectF(48f, top, 148f, top + 85f), 12f, 12f, swatchPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2DBD0")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            canvas.drawRoundRect(RectF(168f, top, PAGE_WIDTH - 48f, top + 85f), 12f, 12f, borderPaint)

            val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#191A1E")
                textSize = 15f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val hexP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(hex)
                textSize = 11f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val descP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5C5A54")
                textSize = 12f
            }
            canvas.drawText(name, 186f, top + 30f, titleP)
            canvas.drawText(hex, 186f, top + 48f, hexP)
            canvas.drawText(desc, 186f, top + 68f, descP)

            top += 108f
        }
    }

    private fun drawSpecimenMarginaliaWorksheet(canvas: Canvas) {
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Interactive Annotation Practice Sheet", 48f, 105f, h1)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBC3B4")
        }
        for (x in 60..(PAGE_WIDTH - 60) step 20) {
            for (y in 140..750 step 20) {
                canvas.drawCircle(x.toFloat(), y.toFloat(), 1.3f, dotPaint)
            }
        }

        val calloutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF6E0")
        }
        canvas.drawRoundRect(RectF(72f, 170f, PAGE_WIDTH - 72f, 290f), 12f, 12f, calloutPaint)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E59800")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(72f, 170f, PAGE_WIDTH - 72f, 290f), 12f, 12f, strokePaint)

        drawParagraphBlock(
            canvas,
            92f,
            204f,
            "Try Folio Studio Tools Here:",
            listOf(
                "1. Switch to Editor mode in the top bar.",
                "2. Select Ink Pen or Highlighter and draw on the dot-grid canvas below.",
                "3. Tap Signature or Stamp to place an official mark, then tap Flatten & Save!"
            )
        )
    }

    private fun drawContractPageOne(canvas: Canvas) {
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("MASTER CREATIVE SERVICES AGREEMENT", 48f, 108f, h1)

        drawParagraphBlock(
            canvas,
            48f,
            155f,
            "1. PARTIES & EFFECTIVE DATE",
            listOf(
                "This Master Services Agreement (\"Agreement\") is entered into as of October 18, 2026,",
                "by and between Folio Editorial Labs LLC (\"Studio\") and Client Counterparty (\"Client\").",
                "Both parties agree to the deliverables, milestones, and intellectual property terms",
                "set forth across Pages 1 through 3 of this instrument."
            )
        )

        drawParagraphBlock(
            canvas,
            48f,
            290f,
            "2. SCOPE OF DELIVERABLES",
            listOf(
                "2.1 Design System Architecture: Full cross-platform component tokens & PDF templates.",
                "2.2 Interactive Document Engine: High-resolution rendering and vector ink layer.",
                "2.3 Quality Assurance: Verification across mobile handhelds and large-screen tablets."
            )
        )

        // Schedule table
        val tableTop = 430f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#191A1E") }
        canvas.drawRect(48f, tableTop, PAGE_WIDTH - 48f, tableTop + 34f, headerPaint)

        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("PHASE / MILESTONE", 62f, tableTop + 22f, thPaint)
        canvas.drawText("TIMELINE", 310f, tableTop + 22f, thPaint)
        canvas.drawText("FEE ALLOCATION", 430f, tableTop + 22f, thPaint)

        val rows = listOf(
            Triple("Phase I: Discovery & Specs", "Weeks 1 - 2", "$12,500.00"),
            Triple("Phase II: Core PDF Engine", "Weeks 3 - 5", "$24,000.00"),
            Triple("Phase III: Studio Polish & QA", "Weeks 6 - 7", "$11,500.00"),
            Triple("TOTAL CONTRACT VALUE", "7 Weeks", "$48,000.00")
        )
        var rowY = tableTop + 34f
        val borderP = Paint().apply {
            color = Color.parseColor("#D8D0C2")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cellP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 12f
        }
        for ((idx, row) in rows.withIndex()) {
            if (idx % 2 == 1) {
                val stripeP = Paint().apply { color = Color.parseColor("#F3EDE2") }
                canvas.drawRect(48f, rowY, PAGE_WIDTH - 48f, rowY + 36f, stripeP)
            }
            canvas.drawRect(48f, rowY, PAGE_WIDTH - 48f, rowY + 36f, borderP)
            canvas.drawText(row.first, 62f, rowY + 23f, cellP)
            canvas.drawText(row.second, 310f, rowY + 23f, cellP)
            canvas.drawText(row.third, 430f, rowY + 23f, cellP)
            rowY += 36f
        }
    }

    private fun drawContractPageTwo(canvas: Canvas) {
        drawParagraphBlock(
            canvas,
            48f,
            105f,
            "3. INTELLECTUAL PROPERTY & LICENSING",
            listOf(
                "Upon receipt of full payment for each Milestone, Studio assigns to Client all right,",
                "title, and interest in the custom deliverables produced specifically for Client.",
                "Studio retains ownership of pre-existing foundational tools and typography libraries."
            )
        )
        drawParagraphBlock(
            canvas,
            48f,
            245f,
            "4. CONFIDENTIALITY & DATA PROTECTION",
            listOf(
                "Each party agrees to hold proprietary business, technical, and financial disclosures",
                "in strict confidence for a period of three (3) years from the Effective Date.",
                "All local PDF records remain encrypted and stored within the user's device sandbox."
            )
        )
        drawParagraphBlock(
            canvas,
            48f,
            385f,
            "5. TERM, TERMINATION & REVISIONS",
            listOf(
                "Either party may terminate this Agreement upon fourteen (14) days written notice.",
                "Client is entitled to three (3) rounds of structured design revisions per milestone."
            )
        )
    }

    private fun drawContractSignaturePage(canvas: Canvas) {
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("6. AUTHORIZATION & SIGNATURES", 48f, 108f, h1)

        drawParagraphBlock(
            canvas,
            48f,
            150f,
            "Execution Instructions",
            listOf(
                "Please review all clauses carefully. Use the Folio PDF 'Signature' or 'Stamp' tool",
                "in Editor Mode to sign inside the designated Counterparty Authorization box below."
            )
        )

        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D93829")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        canvas.drawRoundRect(RectF(48f, 280f, PAGE_WIDTH - 48f, 450f), 12f, 12f, dashPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D93829")
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("SIGN HERE • AUTHORIZED CLIENT SIGNATURE", 68f, 310f, labelPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757067")
            strokeWidth = 1.2f
        }
        canvas.drawLine(68f, 415f, 340f, 415f, linePaint)
        canvas.drawLine(370f, 415f, PAGE_WIDTH - 68f, 415f, linePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5C5A54")
            textSize = 10f
        }
        canvas.drawText("Signature of Authorized Representative", 68f, 434f, subPaint)
        canvas.drawText("Date Signed", 370f, 434f, subPaint)
    }

    private fun drawBlueprintOverviewPage(canvas: Canvas) {
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Q4 PDF Rendering Pipeline Architecture", 48f, 105f, h1)

        // Architecture diagram boxes
        val nodes = listOf(
            Triple("Storage Access / File IO", 60f, Color.parseColor("#1D63DC")),
            Triple("Android PdfRenderer Engine", 225f, Color.parseColor("#D93829")),
            Triple("Vector Ink & Stamp Layer", 390f, Color.parseColor("#168A59"))
        )
        for ((title, x, color) in nodes) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            canvas.drawRoundRect(RectF(x, 160f, x + 145f, 250f), 10f, 10f, p)
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val words = title.split(" ")
            var ty = 195f
            for (w in words) {
                canvas.drawText(w, x + 14f, ty, tp)
                ty += 16f
            }
        }

        drawParagraphBlock(
            canvas,
            48f,
            310f,
            "Executive Technical Summary",
            listOf(
                "The Folio PDF Pipeline decouples bitmap page rasterization from non-destructive vector",
                "annotations. Users can draw freehand strokes, place stamps, and reorganize pages",
                "at 60fps in Jetpack Compose, then flatten changes directly into standard ISO PDF",
                "streams when ready to share or archive."
            )
        )
    }

    private fun drawBlueprintMetricsPage(canvas: Canvas) {
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Benchmark Telemetry & Frame Budgets", 48f, 105f, h1)

        val bars = listOf(
            Triple("Cold Page Raster (1080p)", 0.82f, "#1D63DC"),
            Triple("Cached Tile Render", 0.96f, "#168A59"),
            Triple("Vector Ink Bake Speed", 0.91f, "#E59800"),
            Triple("Multi-PDF Merge Throughput", 0.88f, "#D93829")
        )
        var y = 165f
        for ((label, ratio, hex) in bars) {
            val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#191A1E")
                textSize = 13f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(label, 48f, y, lp)
            val trackP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E6DFD3") }
            canvas.drawRoundRect(RectF(48f, y + 12f, PAGE_WIDTH - 48f, y + 36f), 12f, 12f, trackP)
            val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(hex) }
            val fillW = (PAGE_WIDTH - 96f) * ratio
            canvas.drawRoundRect(RectF(48f, y + 12f, 48f + fillW, y + 36f), 12f, 12f, fillP)
            y += 78f
        }
    }

    private fun drawBlueprintRoadmapPage(canvas: Canvas) {
        drawParagraphBlock(
            canvas,
            48f,
            105f,
            "Release Checklist & Verification Sign-Off",
            listOf(
                "[x] Multi-page PDF Viewer with pinch-to-zoom & double-tap zoom",
                "[x] Reading color filters: Day Paper, Warm Sepia, Night Inverted, High Contrast",
                "[x] Freehand Pen & translucent Highlighter with adjustable stroke width",
                "[x] Custom Text Callouts, Official Review Stamps & Drawn Signatures",
                "[x] Page Organizer: Rotate 90°, Reorder, Insert Page, Extract & Merge PDFs"
            )
        )
    }

    private fun drawResearchSummaryPage(canvas: Canvas) {
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Field Study: Paper vs. Stylus Reading", 48f, 105f, h1)

        drawParagraphBlock(
            canvas,
            48f,
            155f,
            "Abstract & Key Findings",
            listOf(
                "Active reading—characterized by underlining, margin synthesis, and page bookmarking—",
                "improves long-term comprehension by 38% compared to passive scrolling.",
                "",
                "When digital readers provide tactile paper tones (Sepia/Warm Ivory) alongside instant",
                "bookmarking and ink annotation, reader retention matches physical paper folios."
            )
        )
    }

    private fun drawRuledObservationsPage(canvas: Canvas) {
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Ruled Margin Notes & Sketch Sheet", 48f, 100f, h1)

        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D6CFC2")
            strokeWidth = 1f
        }
        for (y in 140..760 step 28) {
            canvas.drawLine(48f, y.toFloat(), PAGE_WIDTH - 48f, y.toFloat(), rulePaint)
        }
        val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D93829")
            strokeWidth = 1.5f
        }
        canvas.drawLine(104f, 120f, 104f, 770f, marginPaint)
    }

    private fun drawParagraphBlock(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        heading: String,
        lines: List<String>
    ) {
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191A1E")
            textSize = 15.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(heading, startX, startY, headPaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#353430")
            textSize = 12.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        var y = startY + 26f
        for (line in lines) {
            if (line.isNotEmpty()) {
                canvas.drawText(line, startX, y, bodyPaint)
            }
            y += 20f
        }
    }
}
