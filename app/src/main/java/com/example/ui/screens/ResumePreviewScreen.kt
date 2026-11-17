package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PdfPageSize
import com.example.data.model.ResumeAccent
import com.example.data.model.ResumeTemplate
import com.example.data.model.UiState
import com.example.pdf.AvatarStorage
import com.example.pdf.PdfResumeGenerator
import com.example.ui.components.ResumePreviewSheet
import com.example.ui.viewmodel.ResumeEditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumePreviewScreen(
    viewModel: ResumeEditorViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val resume = (uiState as? UiState.Success)?.data
    LaunchedEffect(resume?.id) {
        val id = resume?.id
        if (!id.isNullOrBlank()) {
            avatarBitmap = AvatarStorage.loadAvatarBitmap(context, id)
        }
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val currentResume = (uiState as? UiState.Success)?.data ?: return@rememberLauncherForActivityResult
        if (targetUri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isExporting = true
            val result = try {
                val stream = context.contentResolver.openOutputStream(targetUri)
                if (stream == null) {
                    Result.failure(IllegalStateException("Could not open selected file destination."))
                } else {
                    stream.use { out ->
                        PdfResumeGenerator.exportToOutputStream(context, currentResume, out)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
            isExporting = false
            result.onSuccess {
                snackbarHostState.showSnackbar("Saved ${currentResume.sanitizedFileName()}")
            }.onFailure { err ->
                snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to save PDF")
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = resume?.title ?: "PDF Export Studio",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = resume?.let { "${it.template.displayName} • ${it.pdfPageSize.name}" } ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("preview_edit_button")
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }
                }
            )
        },
        bottomBar = {
            if (resume != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                createPdfLauncher.launch(resume.sanitizedFileName())
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(50.dp)
                                .testTag("save_pdf_button")
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.preview_save_pdf))
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    PdfResumeGenerator.exportToCacheForSharing(context, resume)
                                        .onSuccess { uri ->
                                            isExporting = false
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, "${resume.fullName.ifBlank { resume.title }} Resume")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Resume PDF"))
                                        }
                                        .onFailure { err ->
                                            isExporting = false
                                            snackbarHostState.showSnackbar(err.localizedMessage ?: "Unable to share PDF")
                                        }
                                }
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("share_pdf_button")
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    PdfResumeGenerator.exportToCacheForSharing(context, resume)
                                        .onSuccess { uri ->
                                            isExporting = false
                                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try {
                                                context.startActivity(viewIntent)
                                            } catch (_: Exception) {
                                                snackbarHostState.showSnackbar("No PDF viewer app installed. Use Save PDF or Share.")
                                            }
                                        }
                                        .onFailure { err ->
                                            isExporting = false
                                            snackbarHostState.showSnackbar(err.localizedMessage ?: "Unable to open PDF")
                                        }
                                }
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("open_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = stringResource(R.string.preview_open_pdf),
                                modifier = Modifier.size(18.dp)
                            )
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
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UiState.Success -> {
                val current = state.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Template Style", style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ResumeTemplate.entries) { tpl ->
                                    FilterChip(
                                        selected = current.template == tpl,
                                        onClick = { viewModel.updateStyleSettings(template = tpl) },
                                        label = { Text(tpl.displayName) },
                                        modifier = Modifier.testTag("preview_tpl_${tpl.name.lowercase()}")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Accent Ink", style = MaterialTheme.typography.titleSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        ResumeAccent.entries.forEach { accent ->
                                            val selected = current.accent == accent
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(accent.primaryColor)
                                                    .border(
                                                        width = if (selected) 2.5.dp else 1.dp,
                                                        color = if (selected) MaterialTheme.colorScheme.onBackground
                                                        else Color.White.copy(alpha = 0.5f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { viewModel.updateStyleSettings(accent = accent) }
                                                    .testTag("accent_${accent.name.lowercase()}")
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = accent.displayName,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Paper Size", style = MaterialTheme.typography.titleSmall)
                                SingleChoiceSegmentedButtonRow {
                                    PdfPageSize.entries.forEachIndexed { index, sizeOption ->
                                        SegmentedButton(
                                            selected = current.pdfPageSize == sizeOption,
                                            onClick = { viewModel.updateStyleSettings(pageSize = sizeOption) },
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = PdfPageSize.entries.size
                                            ),
                                            modifier = Modifier.testTag("page_size_${sizeOption.name.lowercase()}")
                                        ) {
                                            Text(sizeOption.name)
                                        }
                                    }
                                }
                            }

                            if (avatarBitmap != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Show Headshot in PDF", style = MaterialTheme.typography.titleSmall)
                                    Switch(
                                        checked = current.showPhotoInPdf,
                                        onCheckedChange = {
                                            viewModel.updateStyleSettings(showPhotoInPdf = it)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        ResumePreviewSheet(
                            resume = current,
                            avatarBitmap = avatarBitmap
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
