package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMetadataScreen(
    onBack: () -> Unit
) {
    var sampleRate by remember { mutableStateOf("44100") }
    var bitDepth by remember { mutableStateOf("16") }
    var duration by remember { mutableStateOf("03:45") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Metadata") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = sampleRate,
                onValueChange = { sampleRate = it },
                label = { Text("Sample Rate (Hz)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bitDepth,
                onValueChange = { bitDepth = it },
                label = { Text("Bit Depth") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("File Duration (MM:SS)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    // Save metadata action
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
            ) {
                Text("Save Metadata", color = Color.White)
            }
        }
    }
}
