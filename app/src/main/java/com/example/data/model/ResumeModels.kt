package com.example.data.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ResumeTemplate(
    val displayName: String,
    val tagline: String
) {
    CLASSIC("Classic Editorial", "Serif headings & timeless rule dividers"),
    MODERN("Modern Executive", "Bold accent band & structured columns"),
    TECHNICAL("Technical Mono", "Crisp typographic hierarchy & tag grid");

    companion object {
        fun fromRaw(raw: String?): ResumeTemplate =
            entries.firstOrNull { it.name == raw } ?: CLASSIC
    }
}

enum class ResumeAccent(
    val displayName: String,
    val primaryArgb: Int,
    val softTintArgb: Int
) {
    INK_NAVY("Ink Navy", 0xFF18293A.toInt(), 0xFFEAF0F6.toInt()),
    TERRACOTTA("Terracotta", 0xFFC85A32.toInt(), 0xFFFBF0EC.toInt()),
    FOREST_SAGE("Forest Sage", 0xFF2A6B53.toInt(), 0xFFEBF5F0.toInt()),
    CHARCOAL("Charcoal", 0xFF24292F.toInt(), 0xFFF1F3F5.toInt()),
    BURGUNDY("Burgundy", 0xFF7A283B.toInt(), 0xFFF9ECEF.toInt());

    val primaryColor: Color get() = Color(primaryArgb)
    val softTintColor: Color get() = Color(softTintArgb)

    companion object {
        fun fromRaw(raw: String?): ResumeAccent =
            entries.firstOrNull { it.name == raw } ?: INK_NAVY
    }
}

enum class PdfPageSize(
    val displayName: String,
    val widthPoints: Int,
    val heightPoints: Int
) {
    A4("A4 (210 x 297 mm)", 595, 842),
    LETTER("US Letter (8.5 x 11 in)", 612, 792);

    val aspectRatio: Float get() = widthPoints.toFloat() / heightPoints.toFloat()

    companion object {
        fun fromRaw(raw: String?): PdfPageSize =
            entries.firstOrNull { it.name == raw } ?: A4
    }
}

@Keep
@IgnoreExtraProperties
data class ExperienceItem(
    val entryId: String = UUID.randomUUID().toString(),
    val role: String = "",
    val company: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val currentlyWorking: Boolean = false,
    val highlights: String = ""
) {
    fun formattedPeriod(): String {
        val start = startDate.trim()
        val end = if (currentlyWorking) "Present" else endDate.trim()
        return when {
            start.isNotEmpty() && end.isNotEmpty() -> "$start – $end"
            start.isNotEmpty() -> start
            end.isNotEmpty() -> end
            else -> ""
        }
    }

    fun bulletLines(): List<String> =
        highlights.lines()
            .map { it.trim().removePrefix("•").removePrefix("-").trim() }
            .filter { it.isNotEmpty() }

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "entryId" to entryId,
        "role" to role,
        "company" to company,
        "location" to location,
        "startDate" to startDate,
        "endDate" to endDate,
        "currentlyWorking" to currentlyWorking,
        "highlights" to highlights
    )
}

@Keep
@IgnoreExtraProperties
data class EducationItem(
    val entryId: String = UUID.randomUUID().toString(),
    val degree: String = "",
    val institution: String = "",
    val location: String = "",
    val dateRange: String = "",
    val details: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "entryId" to entryId,
        "degree" to degree,
        "institution" to institution,
        "location" to location,
        "dateRange" to dateRange,
        "details" to details
    )
}

@Keep
@IgnoreExtraProperties
data class ProjectItem(
    val entryId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val roleOrIssuer: String = "",
    val yearOrLink: String = "",
    val description: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "entryId" to entryId,
        "name" to name,
        "roleOrIssuer" to roleOrIssuer,
        "yearOrLink" to yearOrLink,
        "description" to description
    )
}

@Keep
@IgnoreExtraProperties
data class ResumeProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Untitled Resume",
    val templateId: String = ResumeTemplate.CLASSIC.name,
    val accentId: String = ResumeAccent.INK_NAVY.name,
    val pageSize: String = PdfPageSize.A4.name,
    val showPhotoInPdf: Boolean = false,
    val fullName: String = "",
    val headline: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val linkedIn: String = "",
    val summary: String = "",
    val experiences: List<ExperienceItem> = emptyList(),
    val educations: List<EducationItem> = emptyList(),
    val skills: List<String> = emptyList(),
    val projects: List<ProjectItem> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val template: ResumeTemplate get() = ResumeTemplate.fromRaw(templateId)
    val accent: ResumeAccent get() = ResumeAccent.fromRaw(accentId)
    val pdfPageSize: PdfPageSize get() = PdfPageSize.fromRaw(pageSize)

    fun completenessPercent(): Int {
        var score = 0
        if (fullName.isNotBlank()) score += 15
        if (headline.isNotBlank()) score += 10
        if (email.isNotBlank() || phone.isNotBlank()) score += 15
        if (summary.isNotBlank()) score += 15
        if (experiences.isNotEmpty()) score += 20
        if (educations.isNotEmpty()) score += 10
        if (skills.isNotEmpty()) score += 10
        if (projects.isNotEmpty()) score += 5
        return score.coerceIn(0, 100)
    }

    fun contactSegments(): List<String> =
        listOf(email, phone, location, website, linkedIn)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun sanitizedFileName(): String {
        val base = fullName.ifBlank { title }.ifBlank { "Resume" }
            .replace(Regex("[^A-Za-z0-9_\\- ]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(40)
            .ifBlank { "Resume" }
        return "${base}_Resume.pdf"
    }
}
