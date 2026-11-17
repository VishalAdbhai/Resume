package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.EducationItem
import com.example.data.model.ExperienceItem
import com.example.data.model.ProjectItem
import com.example.data.model.ResumeProfile
import com.example.data.model.UiState
import com.example.pdf.AvatarStorage
import com.example.ui.components.EducationEntryDialog
import com.example.ui.components.ExperienceEntryDialog
import com.example.ui.components.ProjectEntryDialog
import com.example.ui.components.ResumePreviewSheet
import com.example.ui.viewmodel.ResumeEditorViewModel
import kotlinx.coroutines.launch

private enum class EditorTab(val label: String) {
    BASICS("Basics"),
    SUMMARY("Summary"),
    EXPERIENCE("Experience"),
    EDUCATION("Education"),
    SKILLS("Skills"),
    PROJECTS("Projects")
}

private val SUGGESTED_SKILLS = listOf(
    "Kotlin", "Jetpack Compose", "Product Strategy", "System Design",
    "SQL", "TypeScript", "Project Management", "Python", "UI/UX Design",
    "Agile", "Cloud Architecture", "Data Analysis"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeEditorScreen(
    viewModel: ResumeEditorViewModel,
    onNavigateBack: () -> Unit,
    onOpenPreview: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIdx by rememberSaveable { mutableIntStateOf(0) }
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var editingExperience by remember { mutableStateOf<Pair<Boolean, ExperienceItem?>?>(null) }
    var editingEducation by remember { mutableStateOf<Pair<Boolean, EducationItem?>?>(null) }
    var editingProject by remember { mutableStateOf<Pair<Boolean, ProjectItem?>?>(null) }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val resumeIdForAvatar = (uiState as? UiState.Success)?.data?.id
    LaunchedEffect(resumeIdForAvatar) {
        if (!resumeIdForAvatar.isNullOrBlank()) {
            avatarBitmap = AvatarStorage.loadAvatarBitmap(context, resumeIdForAvatar)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val resumeId = (uiState as? UiState.Success)?.data?.id ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                val file = AvatarStorage.savePickedPhoto(context, resumeId, uri)
                if (file != null) {
                    avatarBitmap = AvatarStorage.loadAvatarBitmap(context, resumeId)
                    viewModel.updateShowPhotoInPdf(true)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val currentResume = (uiState as? UiState.Success)?.data
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                viewModel.saveChanges(showSavedToast = false) { onNavigateBack() }
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to portfolio"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = currentResume?.title?.ifBlank { "Untitled Resume" } ?: "Resume Editor",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isSaving -> "Saving..."
                                hasUnsavedChanges -> "Unsaved edits"
                                else -> "Synced to cloud"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasUnsavedChanges) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.tertiary
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { viewModel.saveChanges(showSavedToast = true) },
                        enabled = !isSaving && currentResume != null,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("editor_save_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.editor_save))
                    }
                }
            )
        },
        bottomBar = {
            if (uiState is UiState.Success) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val draft = (uiState as UiState.Success).data
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${draft.completenessPercent()}% complete",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${draft.template.displayName} • ${draft.pdfPageSize.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                if (hasUnsavedChanges) {
                                    viewModel.saveChanges(showSavedToast = false) { onOpenPreview() }
                                } else {
                                    onOpenPreview()
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("editor_preview_pdf_button")
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.editor_preview_pdf))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is UiState.Success -> {
                val resume = state.data
                val tabs = EditorTab.entries

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val isWideLayout = maxWidth >= 840.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(if (isWideLayout) 1.1f else 1f)
                                .fillMaxHeight()
                        ) {
                            ScrollableTabRow(
                                selectedTabIndex = selectedTabIdx,
                                containerColor = MaterialTheme.colorScheme.background,
                                edgePadding = 16.dp
                            ) {
                                tabs.forEachIndexed { index, tab ->
                                    val countBadge = when (tab) {
                                        EditorTab.EXPERIENCE -> resume.experiences.size
                                        EditorTab.EDUCATION -> resume.educations.size
                                        EditorTab.SKILLS -> resume.skills.size
                                        EditorTab.PROJECTS -> resume.projects.size
                                        else -> null
                                    }
                                    val tabText = if (countBadge != null && countBadge > 0) {
                                        "${tab.label} ($countBadge)"
                                    } else tab.label
                                    Tab(
                                        selected = selectedTabIdx == index,
                                        onClick = { selectedTabIdx = index },
                                        text = { Text(tabText) },
                                        modifier = Modifier.testTag("editor_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                when (tabs[selectedTabIdx]) {
                                    EditorTab.BASICS -> BasicsSection(
                                        resume = resume,
                                        avatarBitmap = avatarBitmap,
                                        onTitleChange = viewModel::updateTitle,
                                        onBasicsChange = viewModel::updateBasics,
                                        onPickPhoto = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        onRemovePhoto = {
                                            AvatarStorage.deleteAvatar(context, resume.id)
                                            avatarBitmap = null
                                            viewModel.updateShowPhotoInPdf(false)
                                        },
                                        onTogglePhotoInPdf = viewModel::updateShowPhotoInPdf
                                    )

                                    EditorTab.SUMMARY -> SummarySection(
                                        summary = resume.summary,
                                        onSummaryChange = viewModel::updateSummary
                                    )

                                    EditorTab.EXPERIENCE -> ExperienceSection(
                                        experiences = resume.experiences,
                                        onAdd = { editingExperience = true to null },
                                        onEdit = { editingExperience = true to it },
                                        onDelete = { viewModel.deleteExperience(it.entryId) },
                                        onMoveUp = { viewModel.moveExperience(it.entryId, -1) },
                                        onMoveDown = { viewModel.moveExperience(it.entryId, +1) }
                                    )

                                    EditorTab.EDUCATION -> EducationSection(
                                        educations = resume.educations,
                                        onAdd = { editingEducation = true to null },
                                        onEdit = { editingEducation = true to it },
                                        onDelete = { viewModel.deleteEducation(it.entryId) },
                                        onMoveUp = { viewModel.moveEducation(it.entryId, -1) },
                                        onMoveDown = { viewModel.moveEducation(it.entryId, +1) }
                                    )

                                    EditorTab.SKILLS -> SkillsSection(
                                        skills = resume.skills,
                                        onAddSkill = viewModel::addSkill,
                                        onRemoveSkill = viewModel::removeSkill
                                    )

                                    EditorTab.PROJECTS -> ProjectsSection(
                                        projects = resume.projects,
                                        onAdd = { editingProject = true to null },
                                        onEdit = { editingProject = true to it },
                                        onDelete = { viewModel.deleteProject(it.entryId) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        if (isWideLayout) {
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "LIVE PAPER PREVIEW",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    ResumePreviewSheet(
                                        resume = resume,
                                        avatarBitmap = avatarBitmap
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingExperience?.let { (_, item) ->
        ExperienceEntryDialog(
            initial = item,
            onDismiss = { editingExperience = null },
            onSave = { saved ->
                viewModel.upsertExperience(saved)
                editingExperience = null
            }
        )
    }

    editingEducation?.let { (_, item) ->
        EducationEntryDialog(
            initial = item,
            onDismiss = { editingEducation = null },
            onSave = { saved ->
                viewModel.upsertEducation(saved)
                editingEducation = null
            }
        )
    }

    editingProject?.let { (_, item) ->
        ProjectEntryDialog(
            initial = item,
            onDismiss = { editingProject = null },
            onSave = { saved ->
                viewModel.upsertProject(saved)
                editingProject = null
            }
        )
    }
}

@Composable
private fun BasicsSection(
    resume: ResumeProfile,
    avatarBitmap: Bitmap?,
    onTitleChange: (String) -> Unit,
    onBasicsChange: (String, String, String, String, String, String, String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onTogglePhotoInPdf: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = resume.title,
            onValueChange = onTitleChange,
            label = { Text("Resume File Title *") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("basics_title_input")
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = "Selected resume headshot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Profile Headshot (Optional)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onPickPhoto,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("pick_photo_button")
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (avatarBitmap == null) "Add Photo" else "Change")
                        }
                        if (avatarBitmap != null) {
                            IconButton(
                                onClick = onRemovePhoto,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove photo")
                            }
                        }
                    }
                    if (avatarBitmap != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Show in PDF header", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = resume.showPhotoInPdf,
                                onCheckedChange = onTogglePhotoInPdf
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = resume.fullName,
            onValueChange = {
                onBasicsChange(it, resume.headline, resume.email, resume.phone, resume.location, resume.website, resume.linkedIn)
            },
            label = { Text("Full Name") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("basics_fullname_input")
        )
        OutlinedTextField(
            value = resume.headline,
            onValueChange = {
                onBasicsChange(resume.fullName, it, resume.email, resume.phone, resume.location, resume.website, resume.linkedIn)
            },
            label = { Text("Professional Headline / Target Role") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("basics_headline_input")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = resume.email,
                onValueChange = {
                    onBasicsChange(resume.fullName, resume.headline, it, resume.phone, resume.location, resume.website, resume.linkedIn)
                },
                label = { Text("Email") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("basics_email_input")
            )
            OutlinedTextField(
                value = resume.phone,
                onValueChange = {
                    onBasicsChange(resume.fullName, resume.headline, resume.email, it, resume.location, resume.website, resume.linkedIn)
                },
                label = { Text("Phone") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("basics_phone_input")
            )
        }
        OutlinedTextField(
            value = resume.location,
            onValueChange = {
                onBasicsChange(resume.fullName, resume.headline, resume.email, resume.phone, it, resume.website, resume.linkedIn)
            },
            label = { Text("City, State / Country") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = resume.linkedIn,
            onValueChange = {
                onBasicsChange(resume.fullName, resume.headline, resume.email, resume.phone, resume.location, resume.website, it)
            },
            label = { Text("LinkedIn or GitHub URL") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = resume.website,
            onValueChange = {
                onBasicsChange(resume.fullName, resume.headline, resume.email, resume.phone, resume.location, it, resume.linkedIn)
            },
            label = { Text("Portfolio / Personal Website") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SummarySection(
    summary: String,
    onSummaryChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Executive Summary",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Write 2–4 crisp sentences highlighting your years of experience, core domain strengths, and measurable outcomes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = summary,
            onValueChange = onSummaryChange,
            label = { Text("Professional Summary") },
            minLines = 6,
            maxLines = 12,
            supportingText = { Text("${summary.length} / 2000 characters") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("summary_input")
        )
    }
}

@Composable
private fun ExperienceSection(
    experiences: List<ExperienceItem>,
    onAdd: () -> Unit,
    onEdit: (ExperienceItem) -> Unit,
    onDelete: (ExperienceItem) -> Unit,
    onMoveUp: (ExperienceItem) -> Unit,
    onMoveDown: (ExperienceItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Work Experience", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_experience_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Role")
            }
        }

        if (experiences.isEmpty()) {
            SectionEmptyCard("No work experience added yet. Tap \"Add Role\" to document your career milestones.")
        } else {
            experiences.forEachIndexed { idx, item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.role.ifBlank { "Role" }, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = listOf(item.company, item.formattedPeriod()).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { onMoveUp(item) },
                                    enabled = idx > 0,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onMoveDown(item) },
                                    enabled = idx < experiences.lastIndex,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onEdit(item) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit role", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onDelete(item) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete role",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        item.bulletLines().take(3).forEach { bullet ->
                            Text(
                                text = "• $bullet",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EducationSection(
    educations: List<EducationItem>,
    onAdd: () -> Unit,
    onEdit: (EducationItem) -> Unit,
    onDelete: (EducationItem) -> Unit,
    onMoveUp: (EducationItem) -> Unit,
    onMoveDown: (EducationItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Education", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_education_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Degree")
            }
        }

        if (educations.isEmpty()) {
            SectionEmptyCard("Add degrees, bootcamps, or academic credentials.")
        } else {
            educations.forEachIndexed { idx, item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.degree.ifBlank { "Degree" }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = listOf(item.institution, item.dateRange).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { onMoveUp(item) },
                                enabled = idx > 0,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onMoveDown(item) },
                                enabled = idx < educations.lastIndex,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onEdit(item) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit education", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onDelete(item) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete education",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsSection(
    skills: List<String>,
    onAddSkill: (String) -> Unit,
    onRemoveSkill: (String) -> Unit
) {
    var draftSkill by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Skills & Tools", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draftSkill,
                onValueChange = { if (it.length <= 45) draftSkill = it },
                label = { Text("Add a skill (e.g. Kotlin, Figma)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (draftSkill.trim().isNotEmpty()) {
                            onAddSkill(draftSkill)
                            draftSkill = ""
                        }
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("skill_input")
            )
            Button(
                onClick = {
                    onAddSkill(draftSkill)
                    draftSkill = ""
                },
                enabled = draftSkill.trim().isNotEmpty() && skills.size < 50,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("add_skill_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = "Add skill")
            }
        }

        if (skills.isEmpty()) {
            SectionEmptyCard("Add key technical, design, or leadership skills (up to 50).")
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.forEach { skill ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveSkill(skill) },
                        label = { Text(skill) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $skill",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        val remainingSuggestions = remember(skills) {
            SUGGESTED_SKILLS.filter { sug -> skills.none { it.equals(sug, ignoreCase = true) } }
        }
        if (remainingSuggestions.isNotEmpty() && skills.size < 50) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Add Suggestions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    remainingSuggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { onAddSkill(suggestion) },
                            label = { Text("+ $suggestion") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsSection(
    projects: List<ProjectItem>,
    onAdd: () -> Unit,
    onEdit: (ProjectItem) -> Unit,
    onDelete: (ProjectItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Projects & Certifications", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_project_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Entry")
            }
        }

        if (projects.isEmpty()) {
            SectionEmptyCard("Highlight notable projects, open-source work, awards, or certifications.")
        } else {
            projects.forEach { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name.ifBlank { "Project" }, style = MaterialTheme.typography.titleSmall)
                                val sub = listOf(item.roleOrIssuer, item.yearOrLink).filter { it.isNotBlank() }.joinToString(" • ")
                                if (sub.isNotBlank()) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = { onEdit(item) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit project", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onDelete(item) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete project",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (item.description.isNotBlank()) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionEmptyCard(message: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
        )
    }
}
