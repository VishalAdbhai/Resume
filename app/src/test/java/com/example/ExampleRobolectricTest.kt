package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.EducationItem
import com.example.data.model.ExperienceItem
import com.example.data.model.PdfPageSize
import com.example.data.model.ProjectItem
import com.example.data.model.ResumeAccent
import com.example.data.model.ResumeProfile
import com.example.data.model.ResumeTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ResumeCraft", appName)
  }

  @Test
  fun `resume profile completeness, filename sanitization, and bullet parsing`() {
    val profile = ResumeProfile(
      id = "res_test",
      userId = "uid_1",
      title = "Android Engineer CV",
      templateId = ResumeTemplate.MODERN.name,
      accentId = ResumeAccent.TERRACOTTA.name,
      pageSize = PdfPageSize.LETTER.name,
      fullName = "Jordan Taylor!",
      headline = "Senior Android Engineer",
      email = "jordan@example.com",
      summary = "Engineer building high-craft mobile apps.",
      experiences = listOf(
        ExperienceItem(
          role = "Senior Engineer",
          company = "Acme Studio",
          startDate = "2022",
          currentlyWorking = true,
          highlights = "• Led Jetpack Compose architecture.\n- Reduced startup latency by 30%."
        )
      ),
      educations = listOf(
        EducationItem(
          degree = "B.S. Computer Science",
          institution = "UT Austin",
          dateRange = "2018 – 2022"
        )
      ),
      skills = listOf("Kotlin", "Jetpack Compose", "PDF Generation"),
      projects = listOf(
        ProjectItem(
          name = "ResumeCraft",
          roleOrIssuer = "Creator",
          yearOrLink = "2026",
          description = "Vector PDF resume exporter."
        )
      )
    )

    assertEquals(100, profile.completenessPercent())
    assertEquals("Jordan_Taylor_Resume.pdf", profile.sanitizedFileName())
    assertEquals("2022 – Present", profile.experiences.first().formattedPeriod())
    assertEquals(
      listOf("Led Jetpack Compose architecture.", "Reduced startup latency by 30%."),
      profile.experiences.first().bulletLines()
    )
    assertEquals(ResumeTemplate.MODERN, profile.template)
    assertEquals(ResumeAccent.TERRACOTTA, profile.accent)
    assertEquals(PdfPageSize.LETTER, profile.pdfPageSize)
    assertTrue(profile.contactSegments().contains("jordan@example.com"))
  }
}
