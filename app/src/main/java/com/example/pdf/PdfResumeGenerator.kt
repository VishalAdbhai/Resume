package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.EducationItem
import com.example.data.model.ExperienceItem
import com.example.data.model.ProjectItem
import com.example.data.model.ResumeProfile
import com.example.data.model.ResumeTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfResumeGenerator {

    private const val MARGIN_H = 42f
    private const val MARGIN_TOP = 40f
    private const val MARGIN_BOTTOM = 42f

    suspend fun exportToOutputStream(
        context: Context,
        resume: ResumeProfile,
        outputStream: OutputStream
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val document = buildPdfDocument(context, resume)
            try {
                document.writeTo(outputStream)
                outputStream.flush()
            } finally {
                document.close()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToCacheForSharing(
        context: Context,
        resume: ResumeProfile
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val appContext = context.applicationContext
            val exportsDir = File(appContext.cacheDir, "exports")
            if (!exportsDir.exists()) exportsDir.mkdirs()
            exportsDir.listFiles()?.forEach { oldFile ->
                if (System.currentTimeMillis() - oldFile.lastModified() > 3_600_000L) {
                    oldFile.delete()
                }
            }

            val outFile = File(exportsDir, resume.sanitizedFileName())
            val document = buildPdfDocument(appContext, resume)
            try {
                FileOutputStream(outFile).use { stream ->
                    document.writeTo(stream)
                    stream.flush()
                }
            } finally {
                document.close()
            }

            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                outFile
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPdfDocument(context: Context, resume: ResumeProfile): PdfDocument {
        val pageSize = resume.pdfPageSize
        val pageWidth = pageSize.widthPoints
        val pageHeight = pageSize.heightPoints
        val contentWidth = (pageWidth - 2 * MARGIN_H).toInt()

        val serifFont = runCatching { ResourcesCompat.getFont(context, R.font.playfair_display) }.getOrNull() ?: Typeface.SERIF
        val sansFont = runCatching { ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) }.getOrNull() ?: Typeface.SANS_SERIF
        val monoFont = runCatching { ResourcesCompat.getFont(context, R.font.jetbrains_mono) }.getOrNull() ?: Typeface.MONOSPACE

        val avatarBitmap = if (resume.showPhotoInPdf && resume.id.isNotBlank()) {
            AvatarStorage.loadAvatarBitmap(context, resume.id)
        } else null

        val document = PdfDocument()
        try {
            val state = PageState(
                document = document,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                resume = resume,
                sansFont = sansFont
            )
            state.startFirstPage()

        val accentColor = resume.accent.primaryArgb
        val softTint = resume.accent.softTintArgb
        val inkColor = Color.rgb(27, 31, 36)
        val mutedColor = Color.rgb(90, 99, 112)
        val hairlineColor = Color.rgb(218, 214, 206)

        val template = resume.template
        val headerFont = when (template) {
            ResumeTemplate.CLASSIC -> serifFont
            ResumeTemplate.MODERN -> sansFont
            ResumeTemplate.TECHNICAL -> sansFont
        }

        // Draw Header
        drawHeader(
            state = state,
            resume = resume,
            template = template,
            accentColor = accentColor,
            softTint = softTint,
            inkColor = inkColor,
            mutedColor = mutedColor,
            headerFont = headerFont,
            sansFont = sansFont,
            monoFont = monoFont,
            contentWidth = contentWidth,
            avatarBitmap = avatarBitmap
        )

        // Summary
        if (resume.summary.isNotBlank()) {
            drawSectionHeader(state, template, "SUMMARY", "01", accentColor, hairlineColor, sansFont, monoFont, contentWidth)
            val summaryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                textSize = 9.8f
                typeface = sansFont
                letterSpacing = 0.01f
            }
            val layout = buildStaticLayout(resume.summary.trim(), summaryPaint, contentWidth)
            state.ensureSpace(layout.height + 10f)
            state.drawStaticLayout(layout, MARGIN_H)
            state.cursorY += 12f
        }

        // Experience
        if (resume.experiences.isNotEmpty()) {
            drawSectionHeader(state, template, "EXPERIENCE", "02", accentColor, hairlineColor, sansFont, monoFont, contentWidth)
            resume.experiences.forEachIndexed { idx, exp ->
                drawExperienceBlock(
                    state = state,
                    exp = exp,
                    template = template,
                    accentColor = accentColor,
                    inkColor = inkColor,
                    mutedColor = mutedColor,
                    sansFont = sansFont,
                    monoFont = monoFont,
                    contentWidth = contentWidth
                )
                if (idx < resume.experiences.lastIndex) {
                    state.cursorY += 8f
                }
            }
            state.cursorY += 10f
        }

        // Skills
        if (resume.skills.isNotEmpty()) {
            drawSectionHeader(state, template, "SKILLS", "03", accentColor, hairlineColor, sansFont, monoFont, contentWidth)
            drawSkillsBlock(
                state = state,
                skills = resume.skills,
                template = template,
                accentColor = accentColor,
                softTint = softTint,
                inkColor = inkColor,
                sansFont = sansFont,
                monoFont = monoFont,
                contentWidth = contentWidth
            )
            state.cursorY += 12f
        }

        // Education
        if (resume.educations.isNotEmpty()) {
            drawSectionHeader(state, template, "EDUCATION", "04", accentColor, hairlineColor, sansFont, monoFont, contentWidth)
            resume.educations.forEachIndexed { idx, edu ->
                drawEducationBlock(
                    state = state,
                    edu = edu,
                    template = template,
                    inkColor = inkColor,
                    mutedColor = mutedColor,
                    sansFont = sansFont,
                    monoFont = monoFont,
                    contentWidth = contentWidth
                )
                if (idx < resume.educations.lastIndex) {
                    state.cursorY += 7f
                }
            }
            state.cursorY += 10f
        }

        // Projects & Certifications
        if (resume.projects.isNotEmpty()) {
            drawSectionHeader(state, template, "PROJECTS & CERTIFICATIONS", "05", accentColor, hairlineColor, sansFont, monoFont, contentWidth)
            resume.projects.forEachIndexed { idx, proj ->
                drawProjectBlock(
                    state = state,
                    proj = proj,
                    template = template,
                    accentColor = accentColor,
                    inkColor = inkColor,
                    mutedColor = mutedColor,
                    sansFont = sansFont,
                    monoFont = monoFont,
                    contentWidth = contentWidth
                )
                if (idx < resume.projects.lastIndex) {
                    state.cursorY += 7f
                }
            }
        }

        state.finishCurrentPage()
        avatarBitmap?.recycle()
        return document
        } catch (t: Throwable) {
            avatarBitmap?.recycle()
            runCatching { document.close() }
            throw t
        }
    }

    private fun drawHeader(
        state: PageState,
        resume: ResumeProfile,
        template: ResumeTemplate,
        accentColor: Int,
        softTint: Int,
        inkColor: Int,
        mutedColor: Int,
        headerFont: Typeface,
        sansFont: Typeface,
        monoFont: Typeface,
        contentWidth: Int,
        avatarBitmap: Bitmap?
    ) {
        val canvas = state.canvas
        val photoSize = 52f
        val hasPhoto = avatarBitmap != null
        val textWidth = if (hasPhoto) (contentWidth - photoSize - 14f).toInt() else contentWidth

        val nameText = resume.fullName.ifBlank { resume.title.ifBlank { "Your Name" } }
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (template == ResumeTemplate.MODERN) accentColor else inkColor
            textSize = if (template == ResumeTemplate.CLASSIC) 23f else 21.5f
            typeface = Typeface.create(headerFont, Typeface.BOLD)
        }
        val nameLayout = buildStaticLayout(nameText, namePaint, textWidth)

        val headlineLayout = if (resume.headline.isNotBlank()) {
            val headlinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (template == ResumeTemplate.CLASSIC) accentColor else mutedColor
                textSize = 11.5f
                typeface = Typeface.create(sansFont, Typeface.BOLD)
            }
            buildStaticLayout(resume.headline, headlinePaint, textWidth)
        } else null

        val contactStr = resume.contactSegments().joinToString("   •   ")
        val contactLayout = if (contactStr.isNotBlank()) {
            val contactPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedColor
                textSize = 8.6f
                typeface = if (template == ResumeTemplate.TECHNICAL) monoFont else sansFont
            }
            buildStaticLayout(contactStr, contactPaint, textWidth)
        } else null

        val textHeight = nameLayout.height +
            (headlineLayout?.let { it.height + 3f } ?: 0f) +
            (contactLayout?.let { it.height + 6f } ?: 0f)
        val headerBlockHeight = maxOf(textHeight, if (hasPhoto) photoSize else 0f)

        if (template == ResumeTemplate.MODERN) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = softTint }
            val padV = 14f
            val bgRect = RectF(
                MARGIN_H - 10f,
                state.cursorY - 8f,
                MARGIN_H + contentWidth + 10f,
                state.cursorY + headerBlockHeight + padV
            )
            canvas.drawRoundRect(bgRect, 8f, 8f, bgPaint)

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
            canvas.drawRoundRect(
                RectF(MARGIN_H - 10f, state.cursorY - 8f, MARGIN_H - 5f, state.cursorY + headerBlockHeight + padV),
                4f,
                4f,
                barPaint
            )
        }

        val startY = state.cursorY
        state.drawStaticLayout(nameLayout, MARGIN_H)
        headlineLayout?.let {
            state.cursorY += 3f
            state.drawStaticLayout(it, MARGIN_H)
        }
        contactLayout?.let {
            state.cursorY += 6f
            state.drawStaticLayout(it, MARGIN_H)
        }

        if (avatarBitmap != null) {
            val photoX = MARGIN_H + contentWidth - photoSize
            val photoY = startY + (headerBlockHeight - photoSize) / 2f
            drawCircularBitmap(canvas, avatarBitmap, photoX, photoY, photoSize, accentColor)
        }

        state.cursorY = startY + headerBlockHeight + if (template == ResumeTemplate.MODERN) 20f else 10f

        if (template == ResumeTemplate.CLASSIC) {
            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                strokeWidth = 1.8f
            }
            canvas.drawLine(MARGIN_H, state.cursorY, MARGIN_H + contentWidth, state.cursorY, rulePaint)
            state.cursorY += 14f
        } else if (template == ResumeTemplate.TECHNICAL) {
            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                strokeWidth = 1.2f
            }
            canvas.drawLine(MARGIN_H, state.cursorY, MARGIN_H + contentWidth, state.cursorY, rulePaint)
            state.cursorY += 14f
        }
    }

    private fun drawSectionHeader(
        state: PageState,
        template: ResumeTemplate,
        title: String,
        indexCode: String,
        accentColor: Int,
        hairlineColor: Int,
        sansFont: Typeface,
        monoFont: Typeface,
        contentWidth: Int
    ) {
        state.ensureSpace(34f)
        val label = if (template == ResumeTemplate.TECHNICAL) "// $indexCode  $title" else title
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = if (template == ResumeTemplate.TECHNICAL) 9.5f else 9.8f
            typeface = Typeface.create(
                if (template == ResumeTemplate.TECHNICAL) monoFont else sansFont,
                Typeface.BOLD
            )
            letterSpacing = 0.08f
        }
        val layout = buildStaticLayout(label, paint, contentWidth)
        state.drawStaticLayout(layout, MARGIN_H)
        state.cursorY += 3f

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (template == ResumeTemplate.MODERN) accentColor else hairlineColor
            strokeWidth = if (template == ResumeTemplate.MODERN) 1.2f else 0.8f
        }
        state.canvas.drawLine(MARGIN_H, state.cursorY, MARGIN_H + contentWidth, state.cursorY, linePaint)
        state.cursorY += 8f
    }

    private fun drawExperienceBlock(
        state: PageState,
        exp: ExperienceItem,
        template: ResumeTemplate,
        accentColor: Int,
        inkColor: Int,
        mutedColor: Int,
        sansFont: Typeface,
        monoFont: Typeface,
        contentWidth: Int
    ) {
        val period = exp.formattedPeriod()
        val periodWidth = if (period.isNotEmpty()) 135 else 0
        val leftWidth = contentWidth - periodWidth

        val rolePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 10.5f
            typeface = Typeface.create(sansFont, Typeface.BOLD)
        }
        val companyLine = listOf(exp.company.trim(), exp.location.trim())
            .filter { it.isNotEmpty() }
            .joinToString("  •  ")
        val companyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 9.4f
            typeface = Typeface.create(sansFont, Typeface.NORMAL)
        }
        val periodPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 8.6f
            typeface = if (template == ResumeTemplate.TECHNICAL) monoFont else sansFont
        }

        val roleLayout = buildStaticLayout(exp.role.ifBlank { "Role" }, rolePaint, leftWidth)
        val companyLayout = if (companyLine.isNotEmpty()) buildStaticLayout(companyLine, companyPaint, leftWidth) else null
        val periodLayout = if (period.isNotEmpty()) {
            StaticLayout.Builder.obtain(period, 0, period.length, periodPaint, periodWidth)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .build()
        } else null

        val headerHeight = maxOf(
            roleLayout.height + (companyLayout?.let { it.height + 2f } ?: 0f),
            periodLayout?.height?.toFloat() ?: 0f
        )

        val bullets = exp.bulletLines()
        val bulletPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 9.4f
            typeface = sansFont
        }
        val bulletLayouts = bullets.map { line ->
            buildStaticLayout(line, bulletPaint, contentWidth - 14)
        }
        val firstBulletHeight = bulletLayouts.firstOrNull()?.height?.toFloat() ?: 0f
        state.ensureSpace(headerHeight + firstBulletHeight + 10f)

        val rowStartY = state.cursorY
        state.drawStaticLayout(roleLayout, MARGIN_H)
        companyLayout?.let {
            state.cursorY += 2f
            state.drawStaticLayout(it, MARGIN_H)
        }
        if (periodLayout != null) {
            state.canvas.save()
            state.canvas.translate(MARGIN_H + leftWidth, rowStartY + 1f)
            periodLayout.draw(state.canvas)
            state.canvas.restore()
        }
        state.cursorY = rowStartY + headerHeight + 4f

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        bulletLayouts.forEach { bLayout ->
            state.ensureSpace(bLayout.height + 4f)
            state.canvas.drawCircle(MARGIN_H + 4.5f, state.cursorY + 5.5f, 1.8f, dotPaint)
            state.drawStaticLayout(bLayout, MARGIN_H + 12f)
            state.cursorY += 3f
        }
    }

    private fun drawSkillsBlock(
        state: PageState,
        skills: List<String>,
        template: ResumeTemplate,
        accentColor: Int,
        softTint: Int,
        inkColor: Int,
        sansFont: Typeface,
        monoFont: Typeface,
        contentWidth: Int
    ) {
        if (template == ResumeTemplate.CLASSIC) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                textSize = 9.5f
                typeface = sansFont
            }
            val layout = buildStaticLayout(skills.joinToString("   •   "), paint, contentWidth)
            state.ensureSpace(layout.height + 6f)
            state.drawStaticLayout(layout, MARGIN_H)
            return
        }

        val chipTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (template == ResumeTemplate.MODERN) accentColor else inkColor
            textSize = 8.6f
            typeface = Typeface.create(
                if (template == ResumeTemplate.TECHNICAL) monoFont else sansFont,
                Typeface.BOLD
            )
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (template == ResumeTemplate.MODERN) softTint else Color.rgb(243, 241, 236)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }

        var x = MARGIN_H
        val chipHeight = 18f
        val padH = 8f
        val gapX = 6f
        val gapY = 6f

        state.ensureSpace(chipHeight + 4f)
        skills.forEach { skill ->
            val textW = chipTextPaint.measureText(skill)
            val chipW = textW + padH * 2f
            if (x + chipW > MARGIN_H + contentWidth && x > MARGIN_H) {
                x = MARGIN_H
                state.cursorY += chipHeight + gapY
                state.ensureSpace(chipHeight + 4f)
            }
            val rect = RectF(x, state.cursorY, x + chipW, state.cursorY + chipHeight)
            val radius = if (template == ResumeTemplate.MODERN) 9f else 3f
            state.canvas.drawRoundRect(rect, radius, radius, bgPaint)
            if (template == ResumeTemplate.TECHNICAL) {
                state.canvas.drawRoundRect(rect, radius, radius, borderPaint)
            }
            val textY = state.cursorY + (chipHeight / 2f) - ((chipTextPaint.descent() + chipTextPaint.ascent()) / 2f)
            state.canvas.drawText(skill, x + padH, textY, chipTextPaint)
            x += chipW + gapX
        }
        state.cursorY += chipHeight + 2f
    }

    private fun drawEducationBlock(
        state: PageState,
        edu: EducationItem,
        template: ResumeTemplate,
        inkColor: Int,
        mutedColor: Int,
        sansFont: Typeface,
        monoFont: Typeface,
        contentWidth: Int
    ) {
        val dateWidth = if (edu.dateRange.isNotBlank()) 130 else 0
        val leftWidth = contentWidth - dateWidth

        val degreePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 10.2f
            typeface = Typeface.create(sansFont, Typeface.BOLD)
        }
        val instLine = listOf(edu.institution.trim(), edu.location.trim())
            .filter { it.isNotEmpty() }
            .joinToString("  •  ")
        val instPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 9.3f
            typeface = sansFont
        }
        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 8.6f
            typeface = if (template == ResumeTemplate.TECHNICAL) monoFont else sansFont
        }
        val detailsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 9.0f
            typeface = sansFont
        }

        val degreeLayout = buildStaticLayout(edu.degree.ifBlank { "Degree" }, degreePaint, leftWidth)
        val instLayout = if (instLine.isNotEmpty()) buildStaticLayout(instLine, instPaint, leftWidth) else null
        val detailsLayout = if (edu.details.isNotBlank()) buildStaticLayout(edu.details.trim(), detailsPaint, contentWidth) else null
        val dateLayout = if (edu.dateRange.isNotBlank()) {
            StaticLayout.Builder.obtain(edu.dateRange.trim(), 0, edu.dateRange.trim().length, datePaint, dateWidth)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .build()
        } else null

        val blockHeight = maxOf(
            degreeLayout.height + (instLayout?.let { it.height + 2f } ?: 0f),
            dateLayout?.height?.toFloat() ?: 0f
        ) + (detailsLayout?.let { it.height + 3f } ?: 0f)

        state.ensureSpace(blockHeight + 4f)
        val rowStartY = state.cursorY
        state.drawStaticLayout(degreeLayout, MARGIN_H)
        instLayout?.let {
            state.cursorY += 2f
            state.drawStaticLayout(it, MARGIN_H)
        }
        if (dateLayout != null) {
            state.canvas.save()
            state.canvas.translate(MARGIN_H + leftWidth, rowStartY + 1f)
            dateLayout.draw(state.canvas)
            state.canvas.restore()
        }
        state.cursorY = rowStartY + maxOf(
            degreeLayout.height + (instLayout?.let { it.height + 2f } ?: 0f),
            dateLayout?.height?.toFloat() ?: 0f
        )
        detailsLayout?.let {
            state.cursorY += 3f
            state.drawStaticLayout(it, MARGIN_H)
        }
    }

    private fun drawProjectBlock(
        state: PageState,
        proj: ProjectItem,
        template: ResumeTemplate,
        accentColor: Int,
        inkColor: Int,
        mutedColor: Int,
        sansFont: Typeface,
        monoFont: Typeface,
        contentWidth: Int
    ) {
        val rightWidth = if (proj.yearOrLink.isNotBlank()) 150 else 0
        val leftWidth = contentWidth - rightWidth

        val titleLine = listOf(proj.name.trim(), proj.roleOrIssuer.trim())
            .filter { it.isNotEmpty() }
            .joinToString("  —  ")
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 10.0f
            typeface = Typeface.create(sansFont, Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 8.4f
            typeface = if (template == ResumeTemplate.TECHNICAL) monoFont else sansFont
        }
        val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 9.2f
            typeface = sansFont
        }

        val titleLayout = buildStaticLayout(titleLine.ifBlank { "Project" }, titlePaint, leftWidth)
        val metaLayout = if (proj.yearOrLink.isNotBlank()) {
            StaticLayout.Builder.obtain(proj.yearOrLink.trim(), 0, proj.yearOrLink.trim().length, metaPaint, rightWidth)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .build()
        } else null
        val descLayout = if (proj.description.isNotBlank()) {
            buildStaticLayout(proj.description.trim(), descPaint, contentWidth)
        } else null

        val topHeight = maxOf(titleLayout.height.toFloat(), metaLayout?.height?.toFloat() ?: 0f)
        val totalHeight = topHeight + (descLayout?.let { it.height + 3f } ?: 0f)

        state.ensureSpace(totalHeight + 4f)
        val rowStartY = state.cursorY
        state.drawStaticLayout(titleLayout, MARGIN_H)
        if (metaLayout != null) {
            state.canvas.save()
            state.canvas.translate(MARGIN_H + leftWidth, rowStartY + 1f)
            metaLayout.draw(state.canvas)
            state.canvas.restore()
        }
        state.cursorY = rowStartY + topHeight
        descLayout?.let {
            state.cursorY += 3f
            state.drawStaticLayout(it, MARGIN_H)
        }
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(10))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1.05f)
            .build()

    private fun drawCircularBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Float,
        top: Float,
        size: Float,
        borderColor: Int
    ) {
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = maxOf(size / bitmap.width, size / bitmap.height)
        val dx = left + (size - bitmap.width * scale) / 2f
        val dy = top + (size - bitmap.height * scale) / 2f
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        shader.setLocalMatrix(matrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        val radius = size / 2f
        canvas.drawCircle(left + radius, top + radius, radius, paint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawCircle(left + radius, top + radius, radius, borderPaint)
    }

    private class PageState(
        private val document: PdfDocument,
        private val pageWidth: Int,
        private val pageHeight: Int,
        private val resume: ResumeProfile,
        private val sansFont: Typeface
    ) {
        private var currentPage: PdfDocument.Page? = null
        private var pageNumber = 0
        var cursorY = MARGIN_TOP

        val canvas: Canvas
            get() = checkNotNull(currentPage).canvas

        private val bottomLimit: Float
            get() = pageHeight - MARGIN_BOTTOM

        fun startFirstPage() {
            startNewPage(drawContinuationHeader = false)
        }

        fun ensureSpace(neededHeight: Float) {
            if (cursorY + neededHeight > bottomLimit && cursorY > MARGIN_TOP + 36f) {
                finishCurrentPage()
                startNewPage(drawContinuationHeader = true)
            }
        }

        fun drawStaticLayout(layout: StaticLayout, x: Float) {
            canvas.save()
            canvas.translate(x, cursorY)
            layout.draw(canvas)
            canvas.restore()
            cursorY += layout.height
        }

        fun finishCurrentPage() {
            currentPage?.let {
                document.finishPage(it)
                currentPage = null
            }
        }

        private fun startNewPage(drawContinuationHeader: Boolean) {
            pageNumber += 1
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = document.startPage(pageInfo)
            currentPage?.canvas?.drawColor(Color.WHITE)
            cursorY = MARGIN_TOP
            if (drawContinuationHeader) {
                val label = listOf(
                    resume.fullName.ifBlank { resume.title },
                    "Page $pageNumber"
                ).filter { it.isNotBlank() }.joinToString("   •   ")
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(120, 128, 138)
                    textSize = 8.2f
                    typeface = sansFont
                }
                canvas.drawText(label, MARGIN_H, cursorY - 12f, paint)
                cursorY += 4f
            }
        }
    }
}
