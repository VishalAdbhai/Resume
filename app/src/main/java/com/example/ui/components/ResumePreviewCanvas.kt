package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResumeProfile
import com.example.data.model.ResumeTemplate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumePreviewSheet(
    resume: ResumeProfile,
    avatarBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val accent = resume.accent.primaryColor
    val softTint = resume.accent.softTintColor
    val template = resume.template
    val paperInk = Color(0xFF1B1F24)
    val paperMuted = Color(0xFF5A6370)
    val paperHairline = Color(0xFFDAD6CE)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minSheetHeight = maxWidth / resume.pdfPageSize.aspectRatio

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color(0xFFE2DDD3)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minSheetHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Section
                val showPhoto = resume.showPhotoInPdf && avatarBitmap != null
                val headerContent: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = resume.fullName.ifBlank { resume.title.ifBlank { "Your Name" } },
                                fontFamily = if (template == ResumeTemplate.CLASSIC) PlayfairDisplayFontFamily else PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (template == ResumeTemplate.CLASSIC) 20.sp else 18.sp,
                                lineHeight = 23.sp,
                                color = if (template == ResumeTemplate.MODERN) accent else paperInk
                            )
                            if (resume.headline.isNotBlank()) {
                                Text(
                                    text = resume.headline,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = if (template == ResumeTemplate.CLASSIC) accent else paperMuted
                                )
                            }
                            val contactList = resume.contactSegments()
                            if (contactList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contactList.joinToString("  •  "),
                                    fontFamily = if (template == ResumeTemplate.TECHNICAL) JetBrainsMonoFontFamily else PlusJakartaSansFontFamily,
                                    fontSize = 8.5.sp,
                                    lineHeight = 12.sp,
                                    color = paperMuted
                                )
                            }
                        }

                        if (resume.showPhotoInPdf && avatarBitmap != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = "Resume profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, accent, CircleShape)
                            )
                        }
                    }
                }

                if (template == ResumeTemplate.MODERN) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(softTint)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .heightIn(min = 68.dp)
                                .background(accent)
                        )
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            headerContent()
                        }
                    }
                } else {
                    headerContent()
                    HorizontalDivider(
                        thickness = if (template == ResumeTemplate.CLASSIC) 1.5.dp else 1.dp,
                        color = accent
                    )
                }

                // Summary
                if (resume.summary.isNotBlank()) {
                    PreviewSectionTitle(
                        title = "SUMMARY",
                        code = "01",
                        template = template,
                        accent = accent,
                        hairline = paperHairline
                    )
                    Text(
                        text = resume.summary.trim(),
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 9.5.sp,
                        lineHeight = 13.5.sp,
                        color = paperInk
                    )
                }

                // Experience
                if (resume.experiences.isNotEmpty()) {
                    PreviewSectionTitle(
                        title = "EXPERIENCE",
                        code = "02",
                        template = template,
                        accent = accent,
                        hairline = paperHairline
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        resume.experiences.forEach { exp ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exp.role.ifBlank { "Role" },
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = paperInk
                                        )
                                        val orgLine = listOf(exp.company.trim(), exp.location.trim())
                                            .filter { it.isNotEmpty() }
                                            .joinToString(" • ")
                                        if (orgLine.isNotEmpty()) {
                                            Text(
                                                text = orgLine,
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 9.sp,
                                                color = paperMuted
                                            )
                                        }
                                    }
                                    val period = exp.formattedPeriod()
                                    if (period.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = period,
                                            fontFamily = if (template == ResumeTemplate.TECHNICAL) JetBrainsMonoFontFamily else PlusJakartaSansFontFamily,
                                            fontSize = 8.sp,
                                            color = paperMuted,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                                exp.bulletLines().forEach { bullet ->
                                    Row(
                                        modifier = Modifier.padding(top = 1.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "•",
                                            fontSize = 9.sp,
                                            color = accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = bullet,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 9.sp,
                                            lineHeight = 12.5.sp,
                                            color = paperInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Skills
                if (resume.skills.isNotEmpty()) {
                    PreviewSectionTitle(
                        title = "SKILLS",
                        code = "03",
                        template = template,
                        accent = accent,
                        hairline = paperHairline
                    )
                    if (template == ResumeTemplate.CLASSIC) {
                        Text(
                            text = resume.skills.joinToString("  •  "),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            color = paperInk
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            resume.skills.forEach { skill ->
                                val bg = if (template == ResumeTemplate.MODERN) softTint else Color(0xFFF3F1EC)
                                val border = if (template == ResumeTemplate.TECHNICAL) BorderStroke(0.6.dp, accent) else null
                                Surface(
                                    color = bg,
                                    shape = RoundedCornerShape(if (template == ResumeTemplate.MODERN) 9.dp else 3.dp),
                                    border = border
                                ) {
                                    Text(
                                        text = skill,
                                        fontFamily = if (template == ResumeTemplate.TECHNICAL) JetBrainsMonoFontFamily else PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 8.sp,
                                        color = if (template == ResumeTemplate.MODERN) accent else paperInk,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Education
                if (resume.educations.isNotEmpty()) {
                    PreviewSectionTitle(
                        title = "EDUCATION",
                        code = "04",
                        template = template,
                        accent = accent,
                        hairline = paperHairline
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        resume.educations.forEach { edu ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = edu.degree.ifBlank { "Degree" },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.8.sp,
                                        color = paperInk
                                    )
                                    val inst = listOf(edu.institution.trim(), edu.location.trim())
                                        .filter { it.isNotEmpty() }
                                        .joinToString(" • ")
                                    if (inst.isNotEmpty()) {
                                        Text(
                                            text = inst,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 8.8.sp,
                                            color = paperMuted
                                        )
                                    }
                                    if (edu.details.isNotBlank()) {
                                        Text(
                                            text = edu.details.trim(),
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 8.6.sp,
                                            color = paperInk,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                }
                                if (edu.dateRange.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = edu.dateRange.trim(),
                                        fontFamily = if (template == ResumeTemplate.TECHNICAL) JetBrainsMonoFontFamily else PlusJakartaSansFontFamily,
                                        fontSize = 8.sp,
                                        color = paperMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Projects & Certifications
                if (resume.projects.isNotEmpty()) {
                    PreviewSectionTitle(
                        title = "PROJECTS & CERTIFICATIONS",
                        code = "05",
                        template = template,
                        accent = accent,
                        hairline = paperHairline
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        resume.projects.forEach { proj ->
                            Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val titleLine = listOf(proj.name.trim(), proj.roleOrIssuer.trim())
                                        .filter { it.isNotEmpty() }
                                        .joinToString(" — ")
                                    Text(
                                        text = titleLine.ifBlank { "Project" },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.6.sp,
                                        color = paperInk,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (proj.yearOrLink.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = proj.yearOrLink.trim(),
                                            fontFamily = if (template == ResumeTemplate.TECHNICAL) JetBrainsMonoFontFamily else PlusJakartaSansFontFamily,
                                            fontSize = 8.sp,
                                            color = accent
                                        )
                                    }
                                }
                                if (proj.description.isNotBlank()) {
                                    Text(
                                        text = proj.description.trim(),
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 8.8.sp,
                                        lineHeight = 12.sp,
                                        color = paperMuted
                                    )
                                }
                            }
                        }
                    }
                }

                if (
                    resume.summary.isBlank() &&
                    resume.experiences.isEmpty() &&
                    resume.skills.isEmpty() &&
                    resume.educations.isEmpty() &&
                    resume.projects.isEmpty()
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Add your summary, work experience, skills, or education in the editor to populate your resume layout.",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.sp,
                        color = paperMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSectionTitle(
    title: String,
    code: String,
    template: ResumeTemplate,
    accent: Color,
    hairline: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = if (template == ResumeTemplate.TECHNICAL) "// $code  $title" else title,
            fontFamily = if (template == ResumeTemplate.TECHNICAL) JetBrainsMonoFontFamily else PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp,
            color = accent
        )
        HorizontalDivider(
            thickness = if (template == ResumeTemplate.MODERN) 1.dp else 0.7.dp,
            color = if (template == ResumeTemplate.MODERN) accent else hairline
        )
    }
}
