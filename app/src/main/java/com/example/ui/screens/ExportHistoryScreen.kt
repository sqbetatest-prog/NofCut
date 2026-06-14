package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExportEntity
import com.example.ui.VideoEditorViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportHistoryScreen(
    viewModel: VideoEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exports by viewModel.allExports.collectAsStateWithLifecycle()
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            delay(2000)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Галерея экспорта",
                        fontWeight = FontWeight.Bold,
                        color = TextActive
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("gallery_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextActive
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextActive
                ),
                actions = {
                    IconButton(onClick = {
                        snackbarMessage = "Галерея обновлена"
                    }) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Обновить", tint = TextActive)
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Готовые видео (${exports.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextActive
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (exports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, CustomBorder, RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.MovieFilter,
                                contentDescription = "Пусто",
                                tint = TextSubtle,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Нет готовых видео",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextActive,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Откройте проект и экспортируйте в меню рендеринга",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSubtle,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(exports, key = { it.id }) { export ->
                            ExportItemCard(
                                export = export,
                                onShare = {
                                    snackbarMessage = "Ссылка скопирована! Готово к отправке."
                                },
                                onDelete = {
                                    viewModel.deleteExport(export.id)
                                    snackbarMessage = "Экспортированный файл удален"
                                }
                            )
                        }
                    }
                }
            }

            // SnackBar HUD
            AnimatedVisibility(
                visible = snackbarMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = snackbarMessage ?: "",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 18.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ExportItemCard(
    export: ExportEntity,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(export.dateExported) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(export.dateExported))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, CustomBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High fidelity procedurally simulated icon based on selected style preset
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (export.templateName) {
                            "cyberpunk" -> Color(0xFF1A122E)
                            "nature" -> Color(0xFF122E1A)
                            else -> Color(0xFF2E2412)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (export.templateName) {
                        "cyberpunk" -> Icons.Rounded.VideogameAsset
                        "nature" -> Icons.Rounded.Terrain
                        else -> Icons.Rounded.LocalCafe
                    },
                    contentDescription = null,
                    tint = when (export.templateName) {
                        "cyberpunk" -> Color(0xFF00FFF0)
                        "nature" -> Color(0xFF2E8B57)
                        else -> Color(0xFFCD853F)
                    },
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = export.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextActive
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${export.resolution} • ${export.fps}fps",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "|",
                        style = MaterialTheme.typography.bodySmall,
                        color = CustomBorder
                    )
                    Text(
                        text = "${export.fileSizeMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtle
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Экспорт: $dateText",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSubtle
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Поделиться",
                        tint = AccentBlue
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Удалить видео",
                        tint = Color(0xFFFF4D4D)
                    )
                }
            }
        }
    }
}
