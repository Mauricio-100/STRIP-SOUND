package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadSoundScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Autres") }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var audioFileName by remember { mutableStateOf<String?>(null) }
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            audioUri = uri
            audioFileName = getFileName(context, uri)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coverUri = uri
        }
    }

    val categories = listOf("Afro", "Rap", "Gospel", "Amapiano", "Rumba", "Électro", "Podcast", "Autres")

    fun upload() {
        if (title.isBlank() || audioUri == null) {
            uploadStatus = "Veuillez renseigner le titre et sélectionner un fichier audio."
            return
        }
        isUploading = true
        uploadStatus = "Téléchargement en cours..."

        coroutineScope.launch {
            try {
                // Write audio to temp file
                val audioFile = File(context.cacheDir, audioFileName ?: "audio.mp3")
                context.contentResolver.openInputStream(audioUri!!)?.use { input ->
                    FileOutputStream(audioFile).use { output -> input.copyTo(output) }
                }

                val audioBody = audioFile.asRequestBody("audio/*".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData("audio_file", audioFile.name, audioBody)

                var coverPart: MultipartBody.Part? = null
                if (coverUri != null) {
                    val coverFileName = getFileName(context, coverUri!!) ?: "cover.jpg"
                    val coverFile = File(context.cacheDir, coverFileName)
                    context.contentResolver.openInputStream(coverUri!!)?.use { input ->
                        FileOutputStream(coverFile).use { output -> input.copyTo(output) }
                    }
                    val coverBody = coverFile.asRequestBody("image/*".toMediaTypeOrNull())
                    coverPart = MultipartBody.Part.createFormData("cover_file", coverFile.name, coverBody)
                }

                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val catBody = category.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = NetworkModule.api.uploadSound(
                    audio_file = audioPart,
                    cover_file = coverPart,
                    title = titleBody,
                    description = descBody,
                    category = catBody
                )

                uploadStatus = "Upload réussi ! (ID: ${response.sound_id})"
                isUploading = false
                
                audioFile.delete() // cleanup
                
                // Allow a moment before navigating back or just clear fields
                title = ""
                description = ""
                audioUri = null
                coverUri = null
            } catch (e: Exception) {
                e.printStackTrace()
                uploadStatus = "Erreur de l'upload: ${e.message}"
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publier un Son") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Cover Image Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (coverUri != null) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = "Cover Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ajouter une vignette", color = Color.Gray)
                    }
                }
            }

            // Audio File Picker
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { audioPicker.launch("audio/*") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Fichier Audio", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(audioFileName ?: "Sélectionner un fichier...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Metadata fields
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = isCategoryDropdownExpanded,
                onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                category = selectionOption
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description & Tags") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 4
            )

            if (uploadStatus != null) {
                Text(text = uploadStatus!!, color = if (uploadStatus!!.contains("réussi")) Color(0xFF06B6D4) else MaterialTheme.colorScheme.error)
            }

            // Upload Button
            Button(
                onClick = { upload() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/8b45fdb1-8a9d-47c3-8820-22e6cd84f67b/N5sXlWIfj3.json"))
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Analyse & Envoi en cours...")
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publier ce son")
                }
            }
        }
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path?.let { File(it).name }
    }
    return result
}
