package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadSoundScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Afro") }
    var status by remember { mutableStateOf("available") }
    
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        audioUri = uri
    }
    
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        coverUri = uri
    }

    val categories = listOf("Afro", "Rap", "Gospel", "Amapiano", "Rumba", "Électro", "Podcast", "Autres")
    val statuses = listOf("available" to "Public", "coming_soon" to "Bientôt Dispo", "draft" to "Brouillon")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "S-3 STUDIO", 
                        color = Color(0xFF00FFCC), 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Section File Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Audio Selection Card
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B))
                            .border(
                                width = 2.dp,
                                brush = if (audioUri != null) Brush.linearGradient(listOf(Color(0xFF00FFCC), Color(0xFF06B6D4))) else Brush.linearGradient(listOf(Color.DarkGray, Color.DarkGray)),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { audioPicker.launch("audio/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (audioUri == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("AUDIO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Click to pick", color = Color.Gray, fontSize = 10.sp)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Ready", color = Color(0xFF00FFCC), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text("Tap to change", color = Color.LightGray, fontSize = 10.sp)
                            }
                        }
                    }

                    // Cover Selection Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { coverPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverUri == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("COVER", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            AsyncImage(
                                model = coverUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp))
                        }
                    }
                }

                // Section Metadata
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("INFORMATIONS GÉNÉRALES", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre du morceau") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Histoire") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC),
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (séparés par des espaces)") },
                        placeholder = { Text("#afro #hit #strip") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = Color.Gray) }
                    )
                }

                // Section Categorization
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("GENRE MUSICAL", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E293B))
                                    .clickable { category = cat }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Section Status
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("VISIBILITÉ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        statuses.forEach { (id, label) ->
                            val isSelected = status == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.1f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) Color(0xFF00FFCC) else Color.DarkGray, RoundedCornerShape(12.dp))
                                    .clickable { status = id }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF00FFCC) else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Action Button
                Button(
                    onClick = {
                        if (audioUri == null || title.isBlank()) {
                            message = "Audio et Titre requis"
                            isError = true
                            return@Button
                        }
                        
                        isUploading = true
                        message = null
                        isError = false
                        
                        coroutineScope.launch {
                            try {
                                val audioBytes = context.contentResolver.openInputStream(audioUri!!)?.readBytes() ?: throw Exception("Fail to read audio")
                                val audioPart = MultipartBody.Part.createFormData(
                                    "audio_file", "track.mp3",
                                    audioBytes.toRequestBody("audio/mpeg".toMediaTypeOrNull())
                                )
                                
                                var coverPart: MultipartBody.Part? = null
                                coverUri?.let { uri ->
                                    val coverBytes = context.contentResolver.openInputStream(uri)?.readBytes()
                                    coverBytes?.let {
                                        coverPart = MultipartBody.Part.createFormData(
                                            "cover_file", "cover.jpg",
                                            it.toRequestBody("image/jpeg".toMediaTypeOrNull())
                                        )
                                    }
                                }

                                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                                val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                                val catBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
                                val tagsBody = tags.toRequestBody("text/plain".toMediaTypeOrNull())
                                val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
                                
                                val res = com.example.data.remote.NetworkModule.api.uploadSound(
                                    audio_file = audioPart,
                                    cover_file = coverPart,
                                    title = titleBody,
                                    description = descBody,
                                    category = catBody,
                                    tags = tagsBody,
                                    status = statusBody
                                )
                                
                                message = "PUBLICATION RÉUSSIE !"
                                isError = false
                                // Reset fields or navigate back
                            } catch (e: Exception) {
                                e.printStackTrace()
                                message = "ERREUR: ${e.localizedMessage}"
                                isError = true
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("PUBLIER DANS LE FEED", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }
                
                message?.let {
                    Text(
                        text = it,
                        color = if (isError) Color.Red else Color(0xFF00FFCC),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

