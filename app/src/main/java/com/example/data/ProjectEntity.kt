package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateModified: Long = System.currentTimeMillis(),
    val durationMs: Long = 15000L,
    val ratio: String = "9:16", // "9:16", "16:9", "1:1"
    
    // JSON serializations for simple list structures of timeline elements
    val clipsJson: String = "",
    val textOverlaysJson: String = "",
    val musicTrackJson: String = ""
)

@Entity(tableName = "exports")
data class ExportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateExported: Long = System.currentTimeMillis(),
    val resolution: String = "1080p",
    val fps: Int = 60,
    val fileSizeMb: Double = 14.5,
    val templateName: String = "cyberpunk"
)
