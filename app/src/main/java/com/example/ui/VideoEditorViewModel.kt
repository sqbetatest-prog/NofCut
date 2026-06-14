package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProjectRepository(database.projectDao)
    }

    // List of all projects stored locally
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of all exported files
    val allExports: StateFlow<List<ExportEntity>> = repository.allExports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently opened project
    private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
    val currentProject: StateFlow<ProjectEntity?> = _currentProject.asStateFlow()

    // Workspace states for the interactive timeline
    private val _clips = MutableStateFlow<List<VideoClip>>(emptyList())
    val clips = _clips.asStateFlow()

    private val _textOverlays = MutableStateFlow<List<TextOverlay>>(emptyList())
    val textOverlays = _textOverlays.asStateFlow()

    private val _musicTrack = MutableStateFlow<MusicTrack?>(null)
    val musicTrack = _musicTrack.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId = _selectedClipId.asStateFlow()

    private val _activeRatio = MutableStateFlow("9:16") // "9:16", "16:9", "1:1"
    val activeRatio = _activeRatio.asStateFlow()

    // Export Process state
    private val _exportProgress = MutableStateFlow<Int?>(null)
    val exportProgress = _exportProgress.asStateFlow()

    private val _exportingFormat = MutableStateFlow<String>("1080p | 60 FPS")
    val exportingFormat = _exportingFormat.asStateFlow()

    private var playbackJob: Job? = null

    // Total sequence duration calculated dynamically from clips
    val totalDurationMs: StateFlow<Long> = _clips.map { clipList ->
        clipList.sumOf { (it.durationMs / it.speed).toLong() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        // Seed some sample data if the project is freshly created and database is empty
        viewModelScope.launch {
            _currentProject.collectLatest {
                // Keep database sync active whenever clips update
                if (it != null) {
                    saveWorkspaceToProject(it.id)
                }
            }
        }
    }

    // Create a database default seeding if empty
    fun createSeededProjectIfNeeded() {
        viewModelScope.launch {
            if (allProjects.value.isEmpty()) {
                createProjectFromTemplate("Киберпанк Reels", "cyberpunk")
                createProjectFromTemplate("Утренний Эмбиент", "cozy")
            }
        }
    }

    // Workspace controllers
    fun selectProject(project: ProjectEntity) {
        pausePlayback()
        _currentProject.value = project
        _activeRatio.value = project.ratio
        _clips.value = EditorSerializer.deserializeClips(project.clipsJson)
        _textOverlays.value = EditorSerializer.deserializeTextOverlays(project.textOverlaysJson)
        _musicTrack.value = EditorSerializer.deserializeMusicTrack(project.musicTrackJson)
        _currentTimeMs.value = 0L
        _selectedClipId.value = _clips.value.firstOrNull()?.id
    }

    fun deselectProject() {
        pausePlayback()
        _currentProject.value = null
        _clips.value = emptyList()
        _textOverlays.value = emptyList()
        _musicTrack.value = null
        _currentTimeMs.value = 0L
        _selectedClipId.value = null
    }

    fun updateRatio(newRatio: String) {
        _activeRatio.value = newRatio
        viewModelScope.launch {
            val proj = _currentProject.value
            if (proj != null) {
                val updated = proj.copy(ratio = newRatio, dateModified = System.currentTimeMillis())
                repository.updateProject(updated)
                _currentProject.value = updated
            }
        }
    }

    // Change current playback head position manually
    fun seekTo(timeMs: Long) {
        val maxDuration = totalDurationMs.value
        _currentTimeMs.value = timeMs.coerceIn(0L, maxDuration)
    }

    // Toggle playback coroutine loop
    fun togglePlayback() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (_isPlaying.value) {
                delay(20) // approx 50 fps updates
                val now = System.currentTimeMillis()
                val delta = now - lastTime
                lastTime = now

                val nextVal = _currentTimeMs.value + delta
                val maxDuration = totalDurationMs.value
                if (nextVal >= maxDuration) {
                    _currentTimeMs.value = 0L
                    _isPlaying.value = false
                } else {
                    _currentTimeMs.value = nextVal
                }
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    // Clips Modification Operations
    fun addClip(clipPreset: VideoClip) {
        val updated = _clips.value.toMutableList()
        updated.add(clipPreset)
        _clips.value = updated
        if (_selectedClipId.value == null) {
            _selectedClipId.value = clipPreset.id
        }
        triggerWorkspaceAutoSave()
    }

    fun deleteSelectedClip() {
        val selId = _selectedClipId.value ?: return
        val currentList = _clips.value
        if (currentList.size <= 1) return // Keep at least one clip
        val updated = currentList.filterNot { it.id == selId }
        _clips.value = updated
        _selectedClipId.value = updated.firstOrNull()?.id
        seekTo(0)
        triggerWorkspaceAutoSave()
    }

    fun updateSelectedClip(updater: (VideoClip) -> VideoClip) {
        val selId = _selectedClipId.value ?: return
        _clips.value = _clips.value.map {
            if (it.id == selId) updater(it) else it
        }
        triggerWorkspaceAutoSave()
    }

    fun applyColorCorrectionToAllClips(filterName: String, brightness: Float, contrast: Float, saturation: Float, colorHex: String) {
        _clips.value = _clips.value.map {
            it.copy(
                filterName = filterName,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                colorHex = colorHex
            )
        }
        triggerWorkspaceAutoSave()
    }

    fun applyAiSuggestions(musicName: String, shouldCutClips: Boolean) {
        if (shouldCutClips) {
            _clips.value = _clips.value.map {
                it.copy(
                    title = if (it.title.contains("(ИИ нарезка)")) it.title else "${it.title} (ИИ нарезка)",
                    durationMs = (it.durationMs * 0.75f).toLong().coerceAtLeast(3000L)
                )
            }
        }
        if (musicName.isNotEmpty() && musicName != "Без музыки") {
            updateMusicTrack(musicName)
        }
        // Apply standard visual transitions by modifying themes interchangeably
        _clips.value = _clips.value.mapIndexed { index, clip ->
            if (index % 2 == 1) {
                clip.copy(filterName = "Cyberpunk", styleTheme = "Cyberpunk", colorHex = "#00F0FF")
            } else {
                clip.copy(filterName = "Vintage", styleTheme = "Cozy", colorHex = "#D2691E")
            }
        }
        seekTo(0)
        triggerWorkspaceAutoSave()
    }

    fun selectClip(clipId: String) {
        _selectedClipId.value = clipId
        // Seek playhead to the starting position of this clip in timeline
        var startOffset = 0L
        for (clr in _clips.value) {
            if (clr.id == clipId) {
                seekTo(startOffset)
                break
            }
            startOffset += (clr.durationMs / clr.speed).toLong()
        }
    }

    // Cut / Split selected clip at relative time
    fun splitSelectedClip() {
        val selId = _selectedClipId.value ?: return
        val currentPlayhead = _currentTimeMs.value
        
        // Find relative time within selection
        var accumulatedTime = 0L
        var targetIndex = -1
        var relativeTimeInClip = 0L

        val list = _clips.value
        for (i in list.indices) {
            val clipDuration = (list[i].durationMs / list[i].speed).toLong()
            if (currentPlayhead >= accumulatedTime && currentPlayhead < accumulatedTime + clipDuration) {
                targetIndex = i
                relativeTimeInClip = ((currentPlayhead - accumulatedTime) * list[i].speed).toLong()
                break
            }
            accumulatedTime += clipDuration
        }

        if (targetIndex != -1) {
            val clipToSplit = list[targetIndex]
            if (relativeTimeInClip > 1000L && (clipToSplit.durationMs - relativeTimeInClip) > 1000L) {
                // Perform the split!
                val firstPart = clipToSplit.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${clipToSplit.title} (Часть 1)",
                    durationMs = relativeTimeInClip
                )
                val secondPart = clipToSplit.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${clipToSplit.title} (Часть 2)",
                    durationMs = clipToSplit.durationMs - relativeTimeInClip
                )

                val updated = list.toMutableList()
                updated.removeAt(targetIndex)
                updated.add(targetIndex, firstPart)
                updated.add(targetIndex + 1, secondPart)

                _clips.value = updated
                _selectedClipId.value = secondPart.id
                triggerWorkspaceAutoSave()
            }
        }
    }

    // Text Overlay Operations
    fun addTextOverlay(
        text: String, 
        startOffsetSec: Float, 
        durationSec: Float = 4f,
        colorHex: String = "#FFFFFF",
        fontSize: Int = 18,
        align: String = "Center"
    ) {
        val totalMs = totalDurationMs.value
        val startMs = (startOffsetSec * 1000).toLong().coerceIn(0L, totalMs)
        val endMs = (startMs + (durationSec * 1000).toLong()).coerceIn(0L, totalMs)

        val overlay = TextOverlay(
            id = UUID.randomUUID().toString(),
            text = text,
            startTimeMs = startMs,
            endTimeMs = endMs,
            colorHex = colorHex,
            fontSize = fontSize,
            align = align
        )
        _textOverlays.value = _textOverlays.value + overlay
        triggerWorkspaceAutoSave()
    }

    fun removeTextOverlay(id: String) {
        _textOverlays.value = _textOverlays.value.filterNot { it.id == id }
        triggerWorkspaceAutoSave()
    }

    fun updateMusicTrack(trackName: String) {
        if (trackName.isEmpty() || trackName == "Без музыки") {
            _musicTrack.value = null
        } else {
            _musicTrack.value = MusicTrack(
                id = UUID.randomUUID().toString(),
                name = trackName,
                durationMs = 60000L,
                volume = 0.8f
            )
        }
        triggerWorkspaceAutoSave()
    }

    fun updateMusicVolume(volume: Float) {
        val curr = _musicTrack.value ?: return
        _musicTrack.value = curr.copy(volume = volume.coerceIn(0f, 1f))
        triggerWorkspaceAutoSave()
    }

    // Save actual workspace sequence state back into the SQLite schema holding it
    private fun triggerWorkspaceAutoSave() {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            saveWorkspaceToProject(proj.id)
        }
    }

    private suspend fun saveWorkspaceToProject(projectId: Int) {
        val clipsJson = EditorSerializer.serializeClips(_clips.value)
        val textJson = EditorSerializer.serializeTextOverlays(_textOverlays.value)
        val musicJson = EditorSerializer.serializeMusicTrack(_musicTrack.value)

        val latestProjState = repository.getProjectById(projectId) ?: return
        val updated = latestProjState.copy(
            dateModified = System.currentTimeMillis(),
            durationMs = totalDurationMs.value,
            clipsJson = clipsJson,
            textOverlaysJson = textJson,
            musicTrackJson = musicJson
        )
        repository.updateProject(updated)
        _currentProject.value = updated
    }

    // Save and close workspace back into DB
    fun createNewProject(title: String, ratio: String = "9:16") {
        viewModelScope.launch {
            // Setup with 1 clean default stock clip (Neon Sunset theme)
            val defaultClip = VideoClip(
                id = UUID.randomUUID().toString(),
                title = "Клип 1 (Закат)",
                durationMs = 6000L,
                styleTheme = "Cosmic",
                colorHex = "#FF4500",
                filterName = "None"
            )
            val clipsList = listOf(defaultClip)

            val entity = ProjectEntity(
                title = title.ifEmpty { "Новый проект" },
                ratio = ratio,
                durationMs = 6000L,
                clipsJson = EditorSerializer.serializeClips(clipsList),
                textOverlaysJson = "[]",
                musicTrackJson = ""
            )
            val newId = repository.insertProject(entity)
            val savedProject = entity.copy(id = newId.toInt())
            selectProject(savedProject)
        }
    }

    fun createProjectFromTemplate(name: String, templateType: String) {
        viewModelScope.launch {
            val clipsList = when (templateType) {
                "cyberpunk" -> listOf(
                    VideoClip(UUID.randomUUID().toString(), "Перекресток Сити", 5000, 1.0f, "Cyberpunk", "Cyberpunk", colorHex = "#00F0FF"),
                    VideoClip(UUID.randomUUID().toString(), "Эстакада Машин", 4000, 2.0f, "Cyberpunk", "Vaporwave", colorHex = "#FF007F"),
                    VideoClip(UUID.randomUUID().toString(), "Цифровой Дождь", 6000, 1.0f, "Cyberpunk", "Noir", colorHex = "#7F00FF")
                )
                "nature" -> listOf(
                    VideoClip(UUID.randomUUID().toString(), "Шепот Озера", 6000, 1.0f, "Nature", "None", colorHex = "#2E8B57"),
                    VideoClip(UUID.randomUUID().toString(), "Сосновый Бор", 5000, 0.8f, "Nature", "Warm", colorHex = "#556B2F"),
                    VideoClip(UUID.randomUUID().toString(), "Вершины в тумане", 7000, 1.0f, "Nature", "Vintage", colorHex = "#4682B4")
                )
                "cosmic" -> listOf(
                    VideoClip(UUID.randomUUID().toString(), "Туманность Андромеды", 6000, 1.0f, "Cosmic", "Vaporwave", colorHex = "#7F00FF"),
                    VideoClip(UUID.randomUUID().toString(), "Гравитационный Маневр", 5000, 1.5f, "Cosmic", "None", colorHex = "#00FFF0"),
                    VideoClip(UUID.randomUUID().toString(), "Вспышка Квазара", 4000, 1.0f, "Cosmic", "Cyberpunk", colorHex = "#FF007F")
                )
                else -> listOf(
                    VideoClip(UUID.randomUUID().toString(), "Мягкий свет лампы", 8000, 1.0f, "Cozy", "None", colorHex = "#D2691E"),
                    VideoClip(UUID.randomUUID().toString(), "Книжная Полка", 7000, 1.0f, "Cozy", "Warm", colorHex = "#CD853F")
                )
            }

            val defaultText = when(templateType) {
                "cyberpunk" -> listOf(
                    TextOverlay(UUID.randomUUID().toString(), "CYBERPUNK NEON", 500, 3500, "#FFE600", 22, "Center"),
                    TextOverlay(UUID.randomUUID().toString(), "SYSTEM LINK ACTIVE", 6000, 11000, "#00FFF0", 18, "Bottom")
                )
                "nature" -> listOf(
                    TextOverlay(UUID.randomUUID().toString(), "ТИШИНА ГОРОДОВ", 1000, 5000, "#FFFFFF", 20, "Center")
                )
                "cosmic" -> listOf(
                    TextOverlay(UUID.randomUUID().toString(), "ИССЛЕДОВАНИЕ КОСМОСА", 500, 4500, "#00FFF0", 24, "Center"),
                    TextOverlay(UUID.randomUUID().toString(), "ДОСТИЖЕНИЕ ОРБИТЫ", 5500, 10500, "#FF2D55", 18, "Bottom")
                )
                else -> listOf(
                    TextOverlay(UUID.randomUUID().toString(), "Тепло и уют", 1500, 6500, "#FFEBCD", 18, "Bottom")
                )
            }

            val musicName = when (templateType) {
                "cyberpunk" -> "Synthwave Speedtrack"
                "nature" -> "Guitar Forest Trail"
                "cosmic" -> "Synthwave Speedtrack"
                else -> "Lofi Coffee Sip"
            }

            val music = MusicTrack(UUID.randomUUID().toString(), musicName, 30000L, 0.7f)

            val totalLen = clipsList.sumOf { (it.durationMs / it.speed).toLong() }

            val entity = ProjectEntity(
                title = name,
                ratio = "9:16",
                durationMs = totalLen,
                clipsJson = EditorSerializer.serializeClips(clipsList),
                textOverlaysJson = EditorSerializer.serializeTextOverlays(defaultText),
                musicTrackJson = EditorSerializer.serializeMusicTrack(music)
            )
            repository.insertProject(entity)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            if (_currentProject.value?.id == project.id) {
                deselectProject()
            }
            repository.deleteProject(project)
        }
    }

    // Simulated high-fidelity export renderer
    fun startExportSimulation(format: String, onFinished: () -> Unit) {
        pausePlayback()
        _exportingFormat.value = format
        _exportProgress.value = 0
        viewModelScope.launch {
            for (progress in 0..100 step 4) {
                delay(120) // approx 3 seconds total
                _exportProgress.value = progress
            }
            
            // Add rendered output to public exported gallery records
            val pName = _currentProject.value?.title ?: "Видео Монтаж"
            val resolution = format.substringBefore(" |")
            val fpsVal = if (format.contains("60 FPS")) 60 else 30
            val mockSize = (totalDurationMs.value / 1000.0) * (if (resolution == "4K") 3.5 else 1.25)
            
            val tName = _clips.value.firstOrNull()?.styleTheme?.lowercase() ?: "cyberpunk"
            
            val exportItem = ExportEntity(
                title = "$pName ($resolution)",
                resolution = resolution,
                fps = fpsVal,
                fileSizeMb = Math.round(mockSize * 10.0) / 10.0,
                templateName = tName
            )
            repository.insertExport(exportItem)
            
            _exportProgress.value = null
            onFinished()
        }
    }

    fun deleteExport(exportId: Int) {
        viewModelScope.launch {
            repository.deleteExportById(exportId)
        }
    }
}
