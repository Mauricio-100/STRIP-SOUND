package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import com.example.data.remote.NetworkModule

fun getFileFromUri(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("story_temp", ".jpg", context.cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        tempFile
    } catch (e: Exception) {
        null
    }
}

fun getFileFromBitmap(context: Context, bitmap: android.graphics.Bitmap): File? {
    return try {
        val tempFile = File.createTempFile("story_temp", ".jpg", context.cacheDir)
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
        }
        tempFile
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(onBack: () -> Unit) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    var capturedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            capturedImageBitmap = bitmap
            selectedImageUri = null // clear URI if we have a bitmap
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle Story Audio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (capturedImageBitmap != null) {
                    AsyncImage(
                        model = capturedImageBitmap,
                        contentDescription = "Captured Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Prévisualisation de la story", color = Color.Gray)
                    }
                }
            }

            if (isSuccess) {
                Text("Story publiée avec succès !", color = Color.Green, style = MaterialTheme.typography.titleMedium)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Galerie")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Caméra")
                    }
                }
                Button(
                    onClick = {
                        if (selectedImageUri != null || capturedImageBitmap != null) {
                            coroutineScope.launch {
                                isUploading = true
                                try {
                                    val file = if (selectedImageUri != null) {
                                        getFileFromUri(context, selectedImageUri!!)
                                    } else {
                                        getFileFromBitmap(context, capturedImageBitmap!!)
                                    }

                                    if (file != null) {
                                        val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                        val body = MultipartBody.Part.createFormData("file", file.name, reqFile)
                                        NetworkModule.api.uploadStory(body, null)
                                        isSuccess = true
                                        delay(1000)
                                        onBack() // go back automatically after success
                                    } else {
                                        // Handle file error
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isUploading = false
                                }
                            }
                        }
                    },
                    enabled = (selectedImageUri != null || capturedImageBitmap != null) && !isUploading,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publier la story")
                    }
                }
            }
        }
    }
}
