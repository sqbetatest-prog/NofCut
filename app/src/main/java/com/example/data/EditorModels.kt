package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class VideoClip(
    val id: String,
    val title: String,
    val durationMs: Long,
    val speed: Float = 1.0f,
    val styleTheme: String = "Neon", // "Retro", "Nature", "Cinema", "Cozy"
    val filterName: String = "None", // "None", "Noir", "Vaporwave", "Cyberpunk", "Vintage"
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val colorHex: String = "#FF2D55", // visual indicator in the timeline
    val transition: String = "Нет" // Transitions: "Нет", "Вспышка", "Затухание", "Сдвиг", "Глитч"
)

data class TextOverlay(
    val id: String,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val colorHex: String = "#FFFFFF",
    val fontSize: Int = 18,
    val align: String = "Center" // "Bottom", "Center", "Top"
)

data class MusicTrack(
    val id: String,
    val name: String,
    val durationMs: Long,
    val volume: Float = 0.8f,
    val isPlaying: Boolean = true
)

object EditorSerializer {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val clipListType = Types.newParameterizedType(List::class.java, VideoClip::class.java)
    private val textListType = Types.newParameterizedType(List::class.java, TextOverlay::class.java)
    
    private val clipAdapter = moshi.adapter<List<VideoClip>>(clipListType)
    private val textAdapter = moshi.adapter<List<TextOverlay>>(textListType)
    private val musicAdapter = moshi.adapter(MusicTrack::class.java)

    fun serializeClips(clips: List<VideoClip>): String {
        return try { clipAdapter.toJson(clips) } catch (e: Exception) { "[]" }
    }

    fun deserializeClips(json: String?): List<VideoClip> {
        if (json.isNullOrEmpty()) return emptyList()
        return try { clipAdapter.fromJson(json) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun serializeTextOverlays(texts: List<TextOverlay>): String {
        return try { textAdapter.toJson(texts) } catch (e: Exception) { "[]" }
    }

    fun deserializeTextOverlays(json: String?): List<TextOverlay> {
        if (json.isNullOrEmpty()) return emptyList()
        return try { textAdapter.fromJson(json) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun serializeMusicTrack(track: MusicTrack?): String {
        if (track == null) return ""
        return try { musicAdapter.toJson(track) } catch (e: Exception) { "" }
    }

    fun deserializeMusicTrack(json: String?): MusicTrack? {
        if (json.isNullOrEmpty()) return null
        return try { musicAdapter.fromJson(json) } catch (e: Exception) { null }
    }
}
