package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VideoEditorViewModel
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ExportHistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: VideoEditorViewModel = viewModel()
                var currentScreen by remember { mutableStateOf("home") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            "home" -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onOpenEditor = { currentScreen = "editor" },
                                    onOpenExports = { currentScreen = "exports" }
                                )
                            }
                            "editor" -> {
                                EditorScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        viewModel.deselectProject()
                                        currentScreen = "home"
                                    }
                                )
                            }
                            "exports" -> {
                                ExportHistoryScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = "home" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
