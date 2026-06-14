package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Videocam
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ProjectEntity
import com.example.ui.VideoEditorViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VideoEditorViewModel,
    onOpenEditor: () -> Unit,
    onOpenExports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var selectedRatio by remember { mutableStateOf("9:16") }

    // Seed sample projects if none are available on launch to guide the first-time user
    LaunchedEffect(projects) {
        if (projects.isEmpty()) {
            viewModel.createSeededProjectIfNeeded()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on Home */ },
                    icon = {
                        Icon(imageVector = Icons.Rounded.Dashboard, contentDescription = "Проекты")
                    },
                    label = { 
                        Text(
                            "Студия", 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF381E72),
                        selectedTextColor = PrimaryNeon,
                        indicatorColor = PrimaryNeon,
                        unselectedIconColor = TextSubtle,
                        unselectedTextColor = TextSubtle
                    )
                )
                
                NavigationBarItem(
                    selected = false,
                    onClick = { onOpenExports() },
                    icon = {
                        Icon(imageVector = Icons.Rounded.Movie, contentDescription = "Экспорты")
                    },
                    label = { 
                        Text(
                            "Экспорты", 
                            style = MaterialTheme.typography.labelMedium
                        ) 
                    },
                    modifier = Modifier.testTag("exports_tab_button"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF381E72),
                        selectedTextColor = PrimaryNeon,
                        indicatorColor = PrimaryNeon,
                        unselectedIconColor = TextSubtle,
                        unselectedTextColor = TextSubtle
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "V-EDIT",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = PrimaryNeon
                    )
                    Text(
                        text = "Минималистичный монтаж видео",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSubtle
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .testTag("create_project_fab")
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Новый")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Создать", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Templates section
            Text(
                text = "Быстрый старт из шаблонов",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextActive
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    TemplateCard(
                        title = "Киберпанк Reels",
                        subtitle = "Неон & Синтвейв",
                        themeColor = Color(0xFF00FFF0),
                        icon = Icons.Rounded.FlashOn
                    ) {
                        viewModel.createProjectFromTemplate("Киберпанк Мозаика", "cyberpunk")
                    }
                }
                item {
                    TemplateCard(
                        title = "Горная Тишина",
                        subtitle = "Природа & Акустика",
                        themeColor = Color(0xFF2E8B57),
                        icon = Icons.Rounded.NaturePeople
                    ) {
                        viewModel.createProjectFromTemplate("Горная Природа", "nature")
                    }
                }
                item {
                    TemplateCard(
                        title = "Мягкий Loft",
                        subtitle = "Гармония & Кофе",
                        themeColor = Color(0xFFCD853F),
                        icon = Icons.Rounded.Coffee
                    ) {
                        viewModel.createProjectFromTemplate("Cozy Комната", "cozy")
                    }
                }
                item {
                    TemplateCard(
                        title = "Глубокий Космос",
                        subtitle = "Звезды & Амбиент",
                        themeColor = Color(0xFF7F00FF),
                        icon = Icons.Rounded.AutoAwesome
                    ) {
                        viewModel.createProjectFromTemplate("Звездный Путь", "cosmic")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Projects List Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ваши проекты (${projects.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextActive
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (projects.isEmpty()) {
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
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = "Пусто",
                            tint = TextSubtle,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Пока нет проектов",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextActive,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Нажмите Создать или выберите шаблон выше",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSubtle,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(projects, key = { it.id }) { project ->
                        ProjectItemRow(
                            project = project,
                            onClick = {
                                viewModel.selectProject(project)
                                onOpenEditor()
                            },
                            onDelete = {
                                viewModel.deleteProject(project)
                            }
                        )
                    }
                }
            }
        }
    }

    // Creative Creation Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "Новый проект монтажа",
                    color = TextActive,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Название видео") },
                        placeholder = { Text("Напр., Мой влог") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeon,
                            unfocusedBorderColor = CustomBorder,
                            focusedTextColor = TextActive,
                            unfocusedTextColor = TextActive
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_project_title_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Формат кадра",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSubtle
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("9:16", "16:9", "1:1").forEach { ratio ->
                            val isSelected = selectedRatio == ratio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) PrimaryNeon else CustomBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        if (isSelected) PrimaryNeon.copy(alpha = 0.1f) else Color.Transparent
                                    )
                                    .clickable { selectedRatio = ratio }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = when (ratio) {
                                            "9:16" -> Icons.Rounded.StayCurrentPortrait
                                            "16:9" -> Icons.Rounded.StayCurrentLandscape
                                            else -> Icons.Rounded.CropSquare
                                        },
                                        contentDescription = ratio,
                                        tint = if (isSelected) PrimaryNeon else TextSubtle
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when (ratio) {
                                            "9:16" -> "Reels (9:16)"
                                            "16:9" -> "YouTube (16:9)"
                                            else -> "Insta (1:1)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) PrimaryNeon else TextSubtle,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewProject(newProjectName, selectedRatio)
                        showCreateDialog = false
                        newProjectName = ""
                        onOpenEditor()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = CircleShape
                ) {
                    Text("Создать", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSubtle)
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun TemplateCard(
    title: String,
    subtitle: String,
    themeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CustomBorder),
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextActive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSubtle
            )
        }
    }
}

@Composable
fun ProjectItemRow(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(project.dateModified) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(project.dateModified))
    }

    val durationText = remember(project.durationMs) {
        val seconds = (project.durationMs / 1000) % 60
        val minutes = (project.durationMs / 60000) % 60
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, CustomBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini ratio thumbnail proxy
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (project.ratio) {
                            "9:16" -> Icons.Rounded.StayCurrentPortrait
                            "16:9" -> Icons.Rounded.StayCurrentLandscape
                            else -> Icons.Rounded.CropSquare
                        },
                        contentDescription = "Формат",
                        tint = PrimaryNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = project.ratio,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = TextSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextActive,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = TextSubtle,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSubtle
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Rounded.Timelapse,
                        contentDescription = null,
                        tint = TextSubtle,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_project_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Удалить проект",
                    tint = Color(0xFFFF5252)
                )
            }
        }
    }
}
