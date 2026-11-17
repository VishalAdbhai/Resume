package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.EducationItem
import com.example.data.model.ExperienceItem
import com.example.data.model.ProjectItem
import java.util.UUID

@Composable
fun ExperienceEntryDialog(
    initial: ExperienceItem?,
    onDismiss: () -> Unit,
    onSave: (ExperienceItem) -> Unit
) {
    var role by remember { mutableStateOf(initial?.role ?: "") }
    var company by remember { mutableStateOf(initial?.company ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var startDate by remember { mutableStateOf(initial?.startDate ?: "") }
    var endDate by remember { mutableStateOf(initial?.endDate ?: "") }
    var currentlyWorking by remember { mutableStateOf(initial?.currentlyWorking ?: false) }
    var highlights by remember { mutableStateOf(initial?.highlights ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Experience" else "Edit Experience",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = role,
                    onValueChange = { if (it.length <= 120) role = it },
                    label = { Text("Job Title *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exp_role_input")
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { if (it.length <= 120) company = it },
                    label = { Text("Company / Organization *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exp_company_input")
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 100) location = it },
                    label = { Text("Location (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { if (it.length <= 30) startDate = it },
                        label = { Text("Start (e.g. Jan 2022)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = if (currentlyWorking) "Present" else endDate,
                        onValueChange = { if (!currentlyWorking && it.length <= 30) endDate = it },
                        enabled = !currentlyWorking,
                        label = { Text("End Date") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("I currently work here", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = currentlyWorking,
                        onCheckedChange = { currentlyWorking = it }
                    )
                }
                OutlinedTextField(
                    value = highlights,
                    onValueChange = { if (it.length <= 1500) highlights = it },
                    label = { Text("Key achievements (one bullet per line)") },
                    minLines = 4,
                    maxLines = 7,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exp_highlights_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ExperienceItem(
                            entryId = initial?.entryId ?: UUID.randomUUID().toString(),
                            role = role.trim(),
                            company = company.trim(),
                            location = location.trim(),
                            startDate = startDate.trim(),
                            endDate = if (currentlyWorking) "" else endDate.trim(),
                            currentlyWorking = currentlyWorking,
                            highlights = highlights.trim()
                        )
                    )
                },
                enabled = role.trim().isNotEmpty() || company.trim().isNotEmpty(),
                modifier = Modifier.testTag("save_experience_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EducationEntryDialog(
    initial: EducationItem?,
    onDismiss: () -> Unit,
    onSave: (EducationItem) -> Unit
) {
    var degree by remember { mutableStateOf(initial?.degree ?: "") }
    var institution by remember { mutableStateOf(initial?.institution ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var dateRange by remember { mutableStateOf(initial?.dateRange ?: "") }
    var details by remember { mutableStateOf(initial?.details ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Education" else "Edit Education",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = degree,
                    onValueChange = { if (it.length <= 120) degree = it },
                    label = { Text("Degree or Program *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edu_degree_input")
                )
                OutlinedTextField(
                    value = institution,
                    onValueChange = { if (it.length <= 120) institution = it },
                    label = { Text("University / School *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edu_institution_input")
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 100) location = it },
                    label = { Text("Location (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateRange,
                    onValueChange = { if (it.length <= 40) dateRange = it },
                    label = { Text("Date Range (e.g. 2018 – 2022)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 600) details = it },
                    label = { Text("Honors, GPA, or Relevant Coursework") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        EducationItem(
                            entryId = initial?.entryId ?: UUID.randomUUID().toString(),
                            degree = degree.trim(),
                            institution = institution.trim(),
                            location = location.trim(),
                            dateRange = dateRange.trim(),
                            details = details.trim()
                        )
                    )
                },
                enabled = degree.trim().isNotEmpty() || institution.trim().isNotEmpty(),
                modifier = Modifier.testTag("save_education_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ProjectEntryDialog(
    initial: ProjectItem?,
    onDismiss: () -> Unit,
    onSave: (ProjectItem) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var roleOrIssuer by remember { mutableStateOf(initial?.roleOrIssuer ?: "") }
    var yearOrLink by remember { mutableStateOf(initial?.yearOrLink ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Project or Certification" else "Edit Entry",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 120) name = it },
                    label = { Text("Project or Certificate Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("proj_name_input")
                )
                OutlinedTextField(
                    value = roleOrIssuer,
                    onValueChange = { if (it.length <= 100) roleOrIssuer = it },
                    label = { Text("Role or Issuing Org (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = yearOrLink,
                    onValueChange = { if (it.length <= 120) yearOrLink = it },
                    label = { Text("Year or URL (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 800) description = it },
                    label = { Text("Impact & Highlights") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ProjectItem(
                            entryId = initial?.entryId ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            roleOrIssuer = roleOrIssuer.trim(),
                            yearOrLink = yearOrLink.trim(),
                            description = description.trim()
                        )
                    )
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("save_project_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
