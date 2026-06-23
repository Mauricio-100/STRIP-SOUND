package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.AudioPlayerManager
import com.example.data.remote.CollaborationWebSocketManager
import com.example.data.remote.NetworkModule
import com.example.domain.model.CollaboratorAdd
import com.example.domain.model.ProjectCreate
import com.example.domain.model.ProjectEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.util.Base64
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody

// High-fidelity Dashboard Data Models
data class CollabProject(
    val id: String,
    val title: String,
    val description: String,
    val status: String, // "LIVE", "REMIX", "SYNCED", "DRAFT", "IN_REVIEW"
    val bpm: Int,
    val genre: String,
    val coverUrl: String? = null,
    val collaborators: List<CollabUser>,
    val createdAt: String,
    val tracks: List<CollabTrack>,
    val listenersCount: Int = 0,
    val visitorsCount: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val followersCount: Int = 0
)

data class CollabUser(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val role: String // "owner", "editor", "viewer"
)

data class VoiceMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val audioUrl: String,
    val timestamp: String,
    val isMe: Boolean
)

data class CollabTrack(
    val id: String,
    val name: String,
    val duration: Float = 120f,
    val volume: Float = 0.8f,
    val pan: Float = 0f, // -1f (L) to +1f (R)
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollabLabScreen(
    currentUserId: String,
    onBack: () -> Unit,
    audioPlayerManager: AudioPlayerManager? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val webSocketManager = remember { CollaborationWebSocketManager { NetworkModule.tokenProvider?.invoke() } }
    
    val globalIsPlaying by audioPlayerManager?.isPlaying?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val globalPosition by audioPlayerManager?.currentPosition?.collectAsState(initial = 0L) ?: remember { mutableStateOf(0L) }
    val globalDuration by audioPlayerManager?.duration?.collectAsState(initial = 0L) ?: remember { mutableStateOf(0L) }
    var playingPreviewProjectId by remember { mutableStateOf<String?>(null) }
    var joiningProject by remember { mutableStateOf<CollabProject?>(null) }

    // Active project editor selection state
    var currentProjectId by remember { mutableStateOf<String?>(null) }
    
    // Centralized repository of active collaborative projects
    var projectsList by remember { mutableStateOf<List<CollabProject>>(emptyList()) }
    var isLoadingProjects by remember { mutableStateOf(true) }

    // Connect WebSocket on startup and fetch real projects + sounds as collaboration projects
    LaunchedEffect(Unit) {
        webSocketManager.connect(currentUserId)
        
        // 1. Fetch real projects created by users on the backend server
        isLoadingProjects = true
        try {
            android.util.Log.i("CollabLabScreen", "Chargement des projets collaboratifs depuis le serveur...")
            val serverProjects = NetworkModule.api.getProjects()
            val mappedServerProjects = serverProjects.map { proj ->
                CollabProject(
                    id = proj.id,
                    title = proj.title,
                    description = proj.description.ifEmpty { "Session de création collective StripSound." },
                    status = "LIVE",
                    bpm = 124,
                    genre = "StripSound Mix",
                    coverUrl = proj.cover_url,
                    createdAt = proj.created_at,
                    collaborators = listOf(
                        CollabUser(currentUserId, "Moi", null, "owner")
                    ),
                    tracks = listOf(
                        CollabTrack("t_init", "Audio Principal", color = Color(0xFF00FFCC))
                    ),
                    listenersCount = proj.listeners_count,
                    visitorsCount = proj.visitors_count,
                    likesCount = proj.likes_count,
                    commentsCount = proj.comments_count,
                    followersCount = proj.followers_count
                )
            }
            projectsList = mappedServerProjects
            android.util.Log.i("CollabLabScreen", "${serverProjects.size} projets serveur chargés.")
        } catch (e: Exception) {
            android.util.Log.w("CollabLabScreen", "Erreur lors du chargement des projets : ${e.message}")
        } finally {
            isLoadingProjects = false
        }
    }

    // Currently edited project instance holder
    val currentProject = remember(currentProjectId, projectsList) {
        projectsList.find { it.id == currentProjectId }
    }

    LaunchedEffect(currentProjectId) {
        if (currentProjectId != null) {
            val exists = projectsList.any { it.id == currentProjectId }
            if (!exists) {
                try {
                    android.util.Log.i("CollabLabScreen", "Fetching details for selected project: $currentProjectId")
                    val projectDetails = NetworkModule.api.getProjectDetails(currentProjectId!!)
                    val newCollab = CollabProject(
                        id = projectDetails.id,
                        title = projectDetails.title,
                        description = "Projet participatif hébergé sur le serveur.",
                        status = "SYNCED",
                        bpm = 120,
                        genre = "Electronic",
                        createdAt = "Créé le ${projectDetails.created_at}",
                        collaborators = listOf(
                            CollabUser("current_user", "Moi", null, "owner")
                        ),
                        tracks = listOf(
                            CollabTrack("t_init", "Piste Audio Initiale", color = Color(0xFF00FFCC))
                        )
                    )
                    projectsList = projectsList + newCollab
                    android.util.Log.i("CollabLabScreen", "Loaded and added new project: ${projectDetails.title}")
                    try {
                        webSocketManager.joinProject(currentProjectId!!)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CollabLabScreen", "Failed to fetch project details: ${e.message}")
                    val newCollab = CollabProject(
                        id = currentProjectId!!,
                        title = "Projet ${currentProjectId!!.take(5)}",
                        description = "Chargé depuis le serveur.",
                        status = "SYNCED",
                        bpm = 120,
                        genre = "Collab",
                        createdAt = "À l'instant",
                        collaborators = listOf(
                            CollabUser("current_user", "Moi", null, "owner")
                        ),
                        tracks = listOf(
                            CollabTrack("t_init", "Piste Audio Initiale", color = Color(0xFF00FFCC))
                        )
                    )
                    projectsList = projectsList + newCollab
                    try {
                        webSocketManager.joinProject(currentProjectId!!)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            } else {
                try {
                    webSocketManager.joinProject(currentProjectId!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Tab state and server collaborators state
    var activeTab by remember { mutableStateOf(0) } // 0 for projects feed, 1 for server collaborators
    var serverCollaborators by remember { mutableStateOf<List<com.example.domain.model.UserResponse>>(emptyList()) }
    var isFetchingCollaborators by remember { mutableStateOf(false) }

    LaunchedEffect(activeTab) {
        if (activeTab == 1 && serverCollaborators.isEmpty()) {
            isFetchingCollaborators = true
            try {
                android.util.Log.i("CollabLabScreen", "Fetching server collaborators with query query = 'a'...")
                val userSearchResp = NetworkModule.api.searchUsers(query = "a", limit = 35)
                val users = userSearchResp.results.filter { it.id != currentUserId }
                serverCollaborators = users
                android.util.Log.i("CollabLabScreen", "Loaded ${users.size} server collaborators successfully.")
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val userSearchResp = NetworkModule.api.searchUsers(query = "", limit = 20)
                    serverCollaborators = userSearchResp.results.filter { it.id != currentUserId }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            } finally {
                isFetchingCollaborators = false
            }
        }
    }

    // State of list search
    var searchQuery by remember { mutableStateOf("") }
    
    // Dialogue controllers
    var showCreateDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedProjectForInvite by remember { mutableStateOf<CollabProject?>(null) }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedProjectForEdit by remember { mutableStateOf<CollabProject?>(null) }

    var showCreatorCollabDialog by remember { mutableStateOf(false) }
    var selectedCreatorForCollab by remember { mutableStateOf<com.example.domain.model.UserResponse?>(null) }

    var activeProjectVoiceMessages by remember(currentProjectId) {
        mutableStateOf<List<VoiceMessage>>(emptyList())
    }

    // WebSocket event collector
    val websocketEvents by webSocketManager.events.collectAsState(initial = null)
    
    LaunchedEffect(websocketEvents) {
        websocketEvents?.let { event ->
            when (event) {
                is ProjectEvent.ProjectUpdated -> {
                    try {
                        val changes = JSONObject(event.changesJson)
                        if (changes.has("track_added")) {
                            val trackName = changes.getString("track_added")
                            val pid = changes.optString("project_id", currentProjectId ?: "")
                            if (pid.isNotEmpty()) {
                                projectsList = projectsList.map { proj ->
                                    if (proj.id == pid) {
                                        val nextColor = when (proj.tracks.size % 4) {
                                            0 -> Color(0xFF00FFCC)
                                            1 -> Color(0xFF8B5CF6)
                                            2 -> Color(0xFFF59E0B)
                                            else -> Color(0xFFE11D48)
                                        }
                                        proj.copy(
                                            tracks = proj.tracks + CollabTrack("custom_${Random.nextLong()}", trackName, color = nextColor)
                                        )
                                    } else proj
                                }
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Projet mis à jour par un collaborateur : $trackName")
                                }
                            }
                        }
                        if (changes.has("track_volume")) {
                            val trackId = changes.getString("track_id")
                            val volume = changes.getDouble("track_volume").toFloat()
                            val pid = changes.optString("project_id", currentProjectId ?: "")
                            if (pid.isNotEmpty()) {
                                projectsList = projectsList.map { proj ->
                                    if (proj.id == pid) {
                                        proj.copy(
                                            tracks = proj.tracks.map { track ->
                                                if (track.id == trackId) track.copy(volume = volume) else track
                                            }
                                        )
                                    } else proj
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                is ProjectEvent.PlaylistUpdated -> {}
                is ProjectEvent.VoiceMessageReceived -> {
                    val isMe = event.senderId == currentUserId
                    val newMsg = VoiceMessage(
                        id = "vm_${System.currentTimeMillis()}_${Random.nextInt()}",
                        senderId = event.senderId,
                        senderName = event.senderName,
                        audioUrl = event.audioUrl,
                        timestamp = "À l'instant",
                        isMe = isMe
                    )
                    activeProjectVoiceMessages = activeProjectVoiceMessages + newMsg
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Message vocal reçu de @${event.senderName} !")
                    }
                }
            }
        }
    }

    // Clean up connections
    DisposableEffect(Unit) {
        onDispose {
            currentProjectId?.let { webSocketManager.leaveProject(it) }
            webSocketManager.disconnect()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            CollaborationDrawerSheet(
                project = currentProject,
                allProjects = projectsList,
                currentUserId = currentUserId,
                webSocketManager = webSocketManager,
                voiceMessages = activeProjectVoiceMessages,
                onInviteClick = {
                    currentProject?.let {
                        selectedProjectForInvite = it
                        showInviteDialog = true
                    }
                },
                onAddTrack = { trackName ->
                    currentProject?.let { activeProj ->
                        projectsList = projectsList.map { proj ->
                            if (proj.id == activeProj.id) {
                                val nextColor = when (proj.tracks.size % 4) {
                                    0 -> Color(0xFF00FFCC)
                                    1 -> Color(0xFF8B5CF6)
                                    2 -> Color(0xFFF59E0B)
                                    else -> Color(0xFFE11D48)
                                }
                                proj.copy(
                                    tracks = proj.tracks + CollabTrack("custom_${Random.nextLong()}", trackName, color = nextColor)
                                )
                            } else proj
                        }
                    }
                },
                onProjectSelect = { otherProj ->
                    currentProjectId = otherProj.id
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = if (currentProjectId == null) "CollabLab Central" else "Studio Collab : ${currentProject?.title}",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        if (currentProjectId != null) {
                            Text(
                                text = "${currentProject?.genre} • ${currentProject?.bpm} BPM",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentProjectId != null) {
                            currentProjectId?.let { webSocketManager.leaveProject(it) }
                            currentProjectId = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (currentProjectId != null && currentProject != null) {
                        // Open Collaboration Panel button
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.People, contentDescription = "Collaborateurs", tint = Color.White)
                        }
                        
                        // Workspace Sync indicator icon
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (currentProject.status == "LIVE") Color.Red else Color(0xFF10B981))
                        )
                    } else {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Parcourir les projets", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // Create project shortcut button in dashboard header
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Créer", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0B0C))
            )
        },
        containerColor = Color(0xFF0B0B0C)
    ) { padding ->
        if (currentProjectId == null) {
            // DASHBOARD SCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Interactive Mini Metrics Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricWidget(
                        title = "Projets actifs",
                        value = projectsList.size.toString(),
                        accentColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    )
                    MetricWidget(
                        title = "Sessions LIVE",
                        value = projectsList.count { it.status == "LIVE" }.toString(),
                        accentColor = Color(0xFFE11D48),
                        modifier = Modifier.weight(1f)
                    )
                    MetricWidget(
                        title = "Remix Actifs",
                        value = projectsList.count { it.status == "REMIX" }.toString(),
                        accentColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Modern Search Field with custom stylings
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Chercher un projet, genre, artiste...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.LightGray)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF141416),
                        unfocusedContainerColor = Color(0xFF141416),
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0xFF262629),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Central Custom Segmented Control (Pills Layout)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFF141416), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeTab == 0) Color(0xFF1E1E22) else Color.Transparent)
                            .clickable { activeTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = if (activeTab == 0) Color(0xFF00FFCC) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Flux Collab",
                                color = if (activeTab == 0) Color.White else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeTab == 1) Color(0xFF1E1E22) else Color.Transparent)
                            .clickable { activeTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = if (activeTab == 1) Color(0xFF8B5CF6) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Co-Créateurs",
                                color = if (activeTab == 1) Color.White else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (activeTab == 0) {
                    if (isLoadingProjects) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF00FFCC))
                        }
                    } else {
                        val filteredProjects = remember(searchQuery, projectsList) {
                            if (searchQuery.isEmpty()) {
                                projectsList
                            } else {
                                projectsList.filter {
                                    it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.description.contains(searchQuery, ignoreCase = true) ||
                                    it.genre.contains(searchQuery, ignoreCase = true) ||
                                    it.collaborators.any { collab -> collab.username.contains(searchQuery, ignoreCase = true) }
                                }
                            }
                        }

                        if (filteredProjects.isEmpty()) {
                            // Empty list design state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00FFCC).copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp),
                                            tint = Color(0xFF00FFCC).copy(alpha = 0.3f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Text(
                                        "Bienvenue au CollabLab",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Aucun projet actif sur le serveur pour le moment. Soyez le premier à lancer une session !",
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("CRÉER UN LAB", color = Color.Black, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        } else {
                        // Dashboard Central Project Grid / Feed List
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredProjects, key = { it.id }) { project ->
                                ProjectDashboardCard(
                                    project = project,
                                    onJoin = {
                                        joiningProject = project
                                    },
                                    onInvite = {
                                        selectedProjectForInvite = project
                                        showInviteDialog = true
                                    },
                                    onOptionsClick = {
                                        selectedProjectForEdit = project
                                        showEditDialog = true
                                    },
                                    playingPreviewProjectId = playingPreviewProjectId,
                                    globalIsPlaying = globalIsPlaying,
                                    globalPosition = globalPosition,
                                    globalDuration = globalDuration,
                                    onPlayPreviewClick = {
                                        if (playingPreviewProjectId == project.id) {
                                            audioPlayerManager?.togglePlayPause()
                                        } else {
                                            audioPlayerManager?.player?.stop()
                                            playingPreviewProjectId = project.id
                                            val streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-${(project.id.hashCode() % 15).coerceAtLeast(1)}.mp3"
                                            audioPlayerManager?.playTrack(streamUrl)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                } else {
                    val filteredServerCollaborators = remember(searchQuery, serverCollaborators) {
                        if (searchQuery.isEmpty()) {
                            serverCollaborators
                        } else {
                            serverCollaborators.filter {
                                it.username.contains(searchQuery, ignoreCase = true) ||
                                (it.bio ?: "").contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (isFetchingCollaborators) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF8B5CF6))
                        }
                    } else if (filteredServerCollaborators.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Aucun collaborateur trouvé sur le serveur", color = Color.Gray, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("D'autres créateurs apparaîtront lorsque le serveur sera actif", color = Color.DarkGray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredServerCollaborators, key = { it.id }) { creator ->
                                CreatorDashboardCard(
                                    creator = creator,
                                    onInvite = {
                                        selectedCreatorForCollab = creator
                                        showCreatorCollabDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else if (currentProject != null) {
            // INTEGRATED DAW-LIKE WORKSPACE COMPONENT (PROJECT EDITOR)
            StudioDAWWorkspace(
                project = currentProject,
                currentUserId = currentUserId,
                webSocketManager = webSocketManager,
                voiceMessages = activeProjectVoiceMessages,
                onAddTrack = { trackName ->
                    projectsList = projectsList.map { proj ->
                        if (proj.id == currentProject.id) {
                            val nextColor = when (proj.tracks.size % 4) {
                                0 -> Color(0xFF00FFCC)
                                1 -> Color(0xFF8B5CF6)
                                2 -> Color(0xFFF59E0B)
                                else -> Color(0xFFE11D48)
                            }
                            proj.copy(
                                tracks = proj.tracks + CollabTrack("custom_${Random.nextLong()}", trackName, color = nextColor)
                            )
                        } else proj
                    }
                },
                onUpdateTrackControls = { trackId, vol, pan, muted ->
                    projectsList = projectsList.map { proj ->
                        if (proj.id == currentProject.id) {
                            proj.copy(
                                tracks = proj.tracks.map { trk ->
                                    if (trk.id == trackId) trk.copy(volume = vol, pan = pan, isMuted = muted) else trk
                                }
                            )
                        } else proj
                    }
                    // Send change to WebSocket room
                    try {
                        val changes = JSONObject().apply {
                            put("project_id", currentProject.id)
                            put("track_id", trackId)
                            put("track_volume", vol.toDouble())
                        }.toString()
                        webSocketManager.sendProjectUpdate(currentProject.id, changes)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onCompilePublish = {
                    coroutineScope.launch {
                        try {
                            if (currentProject.id.startsWith("project_")) {
                                val resp = NetworkModule.api.createProject(ProjectCreate(currentProject.title))
                                projectsList = projectsList.map { proj ->
                                    if (proj.id == currentProject.id) {
                                        proj.copy(id = resp.id, status = "SYNCED")
                                    } else proj
                                }
                                snackbarHostState.showSnackbar("Chef-d'œuvre créé et publié en ligne avec succès sur le serveur !")
                            } else {
                                snackbarHostState.showSnackbar("Chef-d'œuvre synchronisé et publié sous gopu.inc !")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            snackbarHostState.showSnackbar("Chef-d'œuvre publié localement (Offline)")
                        }
                    }
                    projectsList = projectsList.map { proj ->
                        if (proj.id == currentProject.id) proj.copy(status = "SYNCED") else proj
                    }
                    currentProjectId = null
                },
                audioPlayerManager = audioPlayerManager
            )
        }
    }

    // Modern High-Fidelity Project JOINING progress dialog with live visual sync & real player integration
    if (joiningProject != null) {
        val proj = joiningProject!!
        var joinStep by remember { mutableStateOf(0) }
        var joinProgress by remember { mutableStateOf(0f) }
        
        LaunchedEffect(proj) {
            joinStep = 0
            joinProgress = 0f
            while (joinProgress < 1f) {
                delay(35)
                joinProgress += 0.012f
                if (joinProgress >= 0.25f && joinStep == 0) joinStep = 1
                if (joinProgress >= 0.50f && joinStep == 1) joinStep = 2
                if (joinProgress >= 0.75f && joinStep == 2) joinStep = 3
            }
            joinStep = 4
            delay(300)
            val pid = proj.id
            currentProjectId = pid
            webSocketManager.joinProject(pid)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Bienvenue dans le studio ${proj.title}")
            }
            joiningProject = null
        }

        // Animated Equalizer Visualizer heights
        val eqBarsCount = 12
        var eqHeights by remember { mutableStateOf(List(eqBarsCount) { 0.15f }) }
        LaunchedEffect(globalIsPlaying) {
            while (true) {
                if (globalIsPlaying) {
                    eqHeights = List(eqBarsCount) { 0.15f + Random.nextFloat() * 0.8f }
                } else {
                    eqHeights = List(eqBarsCount) { 0.15f }
                }
                delay(100)
            }
        }

        AlertDialog(
            onDismissRequest = { 
                joiningProject = null 
                audioPlayerManager?.player?.stop()
            },
            modifier = Modifier.border(1.5.dp, Color(0xFF00FFCC).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            containerColor = Color(0xFF0B0B0C),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Headline Brand Pulsing Icon
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(colors = listOf(Color(0xFF00FFCC).copy(alpha = 0.3f), Color.Transparent)))
                            .border(1.dp, Color(0xFF00FFCC), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier
                                .size(34.dp)
                                .graphicsLayer {
                                    val scale = 1f + (joinProgress * 0.12f)
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Connexion au Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = proj.title,
                            color = Color(0xFF00FFCC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Neon progress indicator
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { joinProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00FFCC),
                            trackColor = Color(0xFF141416)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Synchro en cours...",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${(joinProgress * 100).toInt()}%",
                                fontSize = 10.sp,
                                color = Color(0xFF00FFCC),
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    Divider(color = Color(0xFF262629), thickness = 1.dp)

                    // Step Status List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val steps = listOf(
                            "Initialisation du pont de collaboration..." to 0,
                            "Connexion sécurisée via protocole WebSocket..." to 1,
                            "Indexation & téléchargement des pistes audio..." to 2,
                            "Alignement temporel & configuration du mixeur..." to 3
                        )

                        steps.forEach { (label, idx) ->
                            val isActive = joinStep == idx
                            val isCompleted = joinStep > idx
                            
                            val stepColor = when {
                                isCompleted -> Color(0xFF00FFCC)
                                isActive -> Color.White
                                else -> Color.DarkGray
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF00FFCC),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (isActive) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF00FFCC),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .border(1.dp, Color.DarkGray, CircleShape)
                                    )
                                }
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = stepColor,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFF262629), thickness = 1.dp)

                    // Play music while waiting feature (requested by user)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                        border = BorderStroke(1.dp, Color(0xFF262629)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (globalIsPlaying) Color(0xFFFF5500) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (globalIsPlaying) "Aperçu de fond actif 🎼" else "Lancer la musique d'ambiance 🎶",
                                        color = if (globalIsPlaying) Color.White else Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (globalIsPlaying) {
                                            audioPlayerManager?.togglePlayPause()
                                        } else {
                                            val url = when (proj.id) {
                                                "p_retro_synth" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                                                "p_lofi_sunset" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
                                                "p_guitar_cover" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
                                                else -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
                                            }
                                            audioPlayerManager?.playTrack(url)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(if (globalIsPlaying) Color(0xFFFF5500) else Color(0xFF262629), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (globalIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause ambient background music",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (globalIsPlaying) {
                                // Live graphic equalizer pulsing inside waiting card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(18.dp)
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    eqHeights.forEach { heightWeight ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(heightWeight)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color(0xFFFF5500), Color(0xFF00FFCC))
                                                    ),
                                                    shape = RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Modal Dialog for Server-Side Collaborator Actions (Invite or Co-Create)
    if (showCreatorCollabDialog && selectedCreatorForCollab != null) {
        val creator = selectedCreatorForCollab!!
        var expandedSelectProject by remember { mutableStateOf(false) }
        var chosenProjectForCollab by remember { mutableStateOf<CollabProject?>(null) }
        val selectableProjects = projectsList

        AlertDialog(
            onDismissRequest = { showCreatorCollabDialog = false },
            title = { Text("Collaborer avec @${creator.username}", fontWeight = FontWeight.Black, color = Color.White) },
            containerColor = Color(0xFF141416),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Inviter @${creator.username} à travailler sur l'une de vos productions ou démarrer une nouvelle session.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sélectionner un projet existant :", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1E22))
                            .border(BorderStroke(1.dp, Color(0xFF2A2A2E)), RoundedCornerShape(10.dp))
                            .clickable { expandedSelectProject = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = chosenProjectForCollab?.title ?: "Choisir une production...",
                                color = if (chosenProjectForCollab != null) Color.White else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expandedSelectProject,
                        onDismissRequest = { expandedSelectProject = false },
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .background(Color(0xFF1E1E22))
                    ) {
                        selectableProjects.forEach { proj ->
                            DropdownMenuItem(
                                text = { Text(proj.title, color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    chosenProjectForCollab = proj
                                    expandedSelectProject = false
                                }
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatorCollabDialog = false }) {
                    Text("Annuler", color = Color.Gray)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Option to co-create a new project together
                    Button(
                        onClick = {
                            val newProjId = "proj_${System.currentTimeMillis()}"
                            val newProject = CollabProject(
                                id = newProjId,
                                title = "Co-Création avec ${creator.username}",
                                description = "Une production collaborative excitante initiée depuis le tableau de bord.",
                                status = "LIVE",
                                bpm = 120,
                                genre = "Electronic",
                                createdAt = "À l'instant",
                                collaborators = listOf(
                                    CollabUser("current_user", "Moi (cmo1)", null, "owner"),
                                    CollabUser(creator.id, creator.username, creator.avatar_url, "editor")
                                ),
                                tracks = listOf(
                                    CollabTrack("t_init", "Piste Rythmique Initiale", color = Color(0xFF00FFCC))
                                )
                            )
                            projectsList = listOf(newProject) + projectsList
                            
                            coroutineScope.launch {
                                try {
                                    val r = NetworkModule.api.createProject(ProjectCreate(newProject.title))
                                    projectsList = projectsList.map { p ->
                                        if (p.id == newProjId) p.copy(id = r.id) else p
                                    }
                                    NetworkModule.api.addProjectCollaborator(r.id, CollaboratorAdd(creator.id, "editor"))
                                    snackbarHostState.showSnackbar("Projet créé et invité sur le serveur !")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Production démarrée localement")
                                }
                             }
                             showCreatorCollabDialog = false
                         },
                         colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141416)),
                         border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                         shape = RoundedCornerShape(10.dp)
                     ) {
                         Text("Co-créer", color = Color(0xFF8B5CF6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                     }

                     Button(
                         enabled = chosenProjectForCollab != null,
                         onClick = {
                             val selectedProj = chosenProjectForCollab!!
                             coroutineScope.launch {
                                 try {
                                     NetworkModule.api.addProjectCollaborator(
                                         selectedProj.id,
                                         CollaboratorAdd(creator.id, "editor")
                                     )
                                     projectsList = projectsList.map { proj ->
                                         if (proj.id == selectedProj.id) {
                                             val alreadyExists = proj.collaborators.any { it.userId == creator.id }
                                             if (alreadyExists) proj else {
                                                 proj.copy(
                                                     collaborators = proj.collaborators + CollabUser(
                                                         userId = creator.id,
                                                         username = creator.username,
                                                         avatarUrl = creator.avatar_url,
                                                         role = "editor"
                                                     )
                                                 )
                                             }
                                         } else proj
                                     }
                                     snackbarHostState.showSnackbar("Invitation serveur envoyée à ${creator.username} !")
                                 } catch (e: Exception) {
                                     // Fallback local addition
                                     projectsList = projectsList.map { proj ->
                                         if (proj.id == selectedProj.id) {
                                             proj.copy(
                                                 collaborators = proj.collaborators + CollabUser(
                                                     userId = creator.id,
                                                     username = creator.username,
                                                     avatarUrl = creator.avatar_url,
                                                     role = "editor"
                                                 )
                                             )
                                         } else proj
                                     }
                                     snackbarHostState.showSnackbar("Ajouté localement : ${creator.username}")
                                 }
                             }
                             showCreatorCollabDialog = false
                         },
                         colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                         shape = RoundedCornerShape(10.dp)
                     ) {
                         Text("Inviter", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                     }
                 }
             }
         )
     }

    // Modal Create Project Dialog
    if (showCreateDialog) {
        var createTitle by remember { mutableStateOf("") }
        var createDesc by remember { mutableStateOf("") }
        var createBpm by remember { mutableStateOf(120f) }
        var createGenre by remember { mutableStateOf("Techno") }
        var createBaseSound by remember { mutableStateOf<String?>(null) }
        val genres = listOf("Techno", "Hip-hop", "Lo-Fi", "Synthwave", "House", "Ambient")

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nouveau Projet Collaboratif", fontWeight = FontWeight.Black, color = Color.White) },
            containerColor = Color(0xFF141416),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = createTitle,
                        onValueChange = { createTitle = it },
                        label = { Text("Titre du projet") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = createDesc,
                        onValueChange = { createDesc = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC)
                        ),
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // BPM Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tempo : ${createBpm.toInt()} BPM", color = Color.LightGray, fontSize = 12.sp)
                        }
                        Slider(
                            value = createBpm,
                            onValueChange = { createBpm = it },
                            valueRange = 60f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00FFCC),
                                activeTrackColor = Color(0xFF00FFCC)
                            )
                        )
                    }

                    // Genre Selector Row
                    Column {
                        Text("Style / Genre", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(genres) { gName ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (createGenre == gName) Color(0xFF00FFCC) else Color(0xFF262629))
                                        .clickable { createGenre = gName }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = gName,
                                        color = if (createGenre == gName) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Optional Base remix check
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF262629))
                            .clickable {
                                createBaseSound = if (createBaseSound == null) "sound_ambient_wave" else null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = createBaseSound != null,
                            onCheckedChange = { checked ->
                                createBaseSound = if (checked) "sound_ambient_wave" else null
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF06B6D4))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Créer comme Remix", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Utiliser un son existant de la communauté comme base", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (createTitle.trim().isNotEmpty()) {
                            val newProject = CollabProject(
                                id = "project_${System.currentTimeMillis()}",
                                title = createTitle,
                                description = createDesc.ifEmpty { "Nouveau projet audio participatif." },
                                status = if (createBaseSound != null) "REMIX" else "LIVE",
                                bpm = createBpm.toInt(),
                                genre = createGenre,
                                createdAt = "À l'instant",
                                collaborators = listOf(
                                    CollabUser("current_user", "Moi (cmo1)", null, "owner")
                                ),
                                tracks = listOf(
                                    CollabTrack("t1_init", "Piste Audio Initiale", color = Color(0xFF00FFCC))
                                )
                            )
                            projectsList = listOf(newProject) + projectsList
                            
                            // Hit background API endpoint securely via non-blocking call
                            coroutineScope.launch {
                                try {
                                    val resp = NetworkModule.api.createProject(ProjectCreate(createTitle, createBaseSound))
                                    projectsList = projectsList.map { p ->
                                        if (p.id == newProject.id) {
                                            p.copy(id = resp.id)
                                        } else p
                                    }
                                    snackbarHostState.showSnackbar("Projet créé avec succès sur le serveur !")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    snackbarHostState.showSnackbar("Créé localement (Mode hors ligne)")
                                }
                            }
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                ) {
                    Text("Créer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Annuler", color = Color.Gray)
                }
            }
        )
    }

    // Modal Invite Dialog
    if (showInviteDialog && selectedProjectForInvite != null) {
        var inviteName by remember { mutableStateOf("") }
        var inviteRole by remember { mutableStateOf("editor") }

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Inviter un collaborateur", fontWeight = FontWeight.Black, color = Color.White) },
            containerColor = Color(0xFF141416),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Projet : ${selectedProjectForInvite?.title}", color = Color.Gray, fontSize = 13.sp)
                    OutlinedTextField(
                        value = inviteName,
                        onValueChange = { inviteName = it },
                        label = { Text("Nom d'utilisateur ou ID") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Role selectors
                    Column {
                        Text("Permission de collaboration", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("editor", "viewer").forEach { role ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (inviteRole == role) Color(0xFF00FFCC) else Color(0xFF262629))
                                        .clickable { inviteRole = role }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (role == "editor") "Éditeur (Modifie)" else "Lecteur (Écoute)",
                                        color = if (inviteRole == role) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Display list of existing project collaborators with removal support
                    selectedProjectForInvite?.let { proj ->
                        if (proj.collaborators.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.HorizontalDivider(color = Color(0xFF262629), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Membres actuels :", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                proj.collaborators.forEach { collab ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E22))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (collab.role == "owner") Color(0xFF06B6D4) else Color(0xFF8B5CF6)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = collab.username.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                            Column {
                                                Text(collab.username, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = if (collab.role == "owner") "Créateur" else if (collab.role == "editor") "Éditeur" else "Lecteur",
                                                    color = Color.Gray,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }

                                        // Only show remove option if they are not the project creator or own creator
                                        if (collab.userId != "current_user" && collab.role != "owner") {
                                            IconButton(
                                                onClick = {
                                                    projectsList = projectsList.map { p ->
                                                        if (p.id == proj.id) {
                                                            p.copy(collaborators = p.collaborators.filterNot { it.userId == collab.userId })
                                                        } else p
                                                    }
                                                    // Immediately update the dialog reference
                                                    selectedProjectForInvite = projectsList.find { p -> p.id == proj.id }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Retirer",
                                                    tint = Color(0xFFE11D48),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteName.trim().isNotEmpty()) {
                            val targetProject = selectedProjectForInvite!!
                            coroutineScope.launch {
                                try {
                                    // 1. Try to search user by username on remote server
                                    val searchResp = NetworkModule.api.searchUsers(query = inviteName, type = "users")
                                    val matchedUser = searchResp.results.find {
                                        it.username.equals(inviteName, ignoreCase = true) || it.id.equals(inviteName, ignoreCase = true)
                                    } ?: searchResp.results.firstOrNull()

                                    val resolvedUserId = matchedUser?.id ?: "user_${System.currentTimeMillis()}"
                                    val resolvedUsername = matchedUser?.username ?: inviteName
                                    val resolvedAvatar = matchedUser?.avatar_url

                                    // 2. Submit collaborator registration to the server
                                    NetworkModule.api.addProjectCollaborator(
                                        targetProject.id,
                                        CollaboratorAdd(userId = resolvedUserId, role = inviteRole)
                                    )

                                    // 3. Update UI state on successful response
                                    projectsList = projectsList.map { proj ->
                                        if (proj.id == targetProject.id) {
                                            val alreadyExists = proj.collaborators.any { it.userId == resolvedUserId }
                                            if (alreadyExists) proj else {
                                                proj.copy(
                                                    collaborators = proj.collaborators + CollabUser(
                                                        userId = resolvedUserId,
                                                        username = resolvedUsername,
                                                        avatarUrl = resolvedAvatar,
                                                        role = inviteRole
                                                    )
                                                )
                                            }
                                        } else proj
                                    }
                                    snackbarHostState.showSnackbar("Collaborateur $resolvedUsername ajouté via le serveur !")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // Fallback to local inclusion
                                    val localId = "user_${System.currentTimeMillis()}"
                                    projectsList = projectsList.map { proj ->
                                        if (proj.id == targetProject.id) {
                                            proj.copy(
                                                collaborators = proj.collaborators + CollabUser(
                                                    userId = localId,
                                                    username = inviteName,
                                                    avatarUrl = null,
                                                    role = inviteRole
                                                )
                                            )
                                        } else proj
                                    }
                                    snackbarHostState.showSnackbar("Ajouté localement : $inviteName")
                                }
                            }
                            showInviteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                ) {
                    Text("Inviter", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Fermer", color = Color.Gray)
                }
            }
        )
    }

    // Modal Edit / Manage Project Dialog
    if (showEditDialog && selectedProjectForEdit != null) {
        var editTitle by remember { mutableStateOf(selectedProjectForEdit?.title ?: "") }
        var editBpm by remember { mutableStateOf(selectedProjectForEdit?.bpm?.toFloat() ?: 120f) }
        var editGenre by remember { mutableStateOf(selectedProjectForEdit?.genre ?: "Techno") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Options du Projet", fontWeight = FontWeight.Black, color = Color.White) },
            containerColor = Color(0xFF141416),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Titre du projet") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // BPM Slider
                    Column {
                        Text("Tempo : ${editBpm.toInt()} BPM", color = Color.LightGray, fontSize = 12.sp)
                        Slider(
                            value = editBpm,
                            onValueChange = { editBpm = it },
                            valueRange = 60f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00FFCC),
                                activeTrackColor = Color(0xFF00FFCC)
                            )
                        )
                    }

                    // Quick deletion action
                    Button(
                        onClick = {
                            projectsList = projectsList.filterNot { it.id == selectedProjectForEdit?.id }
                            showEditDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Projet supprimé définitivement")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Supprimer le projet", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        projectsList = projectsList.map { proj ->
                            if (proj.id == selectedProjectForEdit?.id) {
                                proj.copy(
                                    title = editTitle,
                                    bpm = editBpm.toInt(),
                                    genre = editGenre
                                )
                            } else proj
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                ) {
                    Text("Sauvegarder", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Fermer", color = Color.Gray)
                }
            }
        )
    }
    }
}

// Stats / Metrics Widgets
@Composable
fun MetricWidget(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun CreatorDashboardCard(
    creator: com.example.domain.model.UserResponse,
    onInvite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF00FFCC))
                                )
                            )
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF0B0B0C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = creator.username.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = creator.username,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (creator.is_verified) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Vérifié",
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = if (creator.is_verified) "Producteur Certifié" else "Artiste Indépendant",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick = onInvite,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("INVITER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }

            if (!creator.bio.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = creator.bio,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CreatorMetric(
                        icon = Icons.Default.Favorite,
                        value = "${creator.followers_count}",
                        color = Color(0xFFFF5C8A)
                    )
                    CreatorMetric(
                        icon = Icons.Default.Audiotrack,
                        value = "${creator.total_sounds}",
                        color = Color(0xFF00FFCC)
                    )
                }
                
                if (creator.zodiac_sign != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = creator.zodiac_sign,
                            color = Color(0xFF8B5CF6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnalyticItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
    }
}

// Gorgeous Dashboard Project Item Card UI
@Composable
fun ProjectDashboardCard(
    project: CollabProject,
    onJoin: () -> Unit,
    onInvite: () -> Unit,
    onOptionsClick: () -> Unit,
    playingPreviewProjectId: String? = null,
    globalIsPlaying: Boolean = false,
    globalPosition: Long = 0L,
    globalDuration: Long = 0L,
    onPlayPreviewClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.1f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJoin() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Cover & Title & Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Project Cover with overlay preview button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .clickable { onPlayPreviewClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (project.coverUrl != null) {
                        AsyncImage(
                            model = project.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = Color(0xFF00FFCC).copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    // Play overlay if not playing
                    if (playingPreviewProjectId != project.id || !globalIsPlaying) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = project.genre,
                        fontSize = 11.sp,
                        color = Color(0xFF00FFCC),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = project.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                ProjectStatusBadge(status = project.status)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Analytics Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalyticItem(icon = Icons.Default.Headset, value = project.listenersCount.toString(), label = "AUDITEURS")
                AnalyticItem(icon = Icons.Default.Visibility, value = project.visitorsCount.toString(), label = "VISITEURS")
                AnalyticItem(icon = Icons.Default.Favorite, value = project.likesCount.toString(), label = "LIKES")
                AnalyticItem(icon = Icons.Default.Comment, value = project.commentsCount.toString(), label = "COMMS")
                AnalyticItem(icon = Icons.Default.PersonAdd, value = project.followersCount.toString(), label = "FOLLOWS")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Metadata Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProjectInfoChip(
                    icon = Icons.Default.GraphicEq,
                    text = "${project.tracks.size} Pistes",
                    color = Color.White.copy(alpha = 0.7f)
                )
                ProjectInfoChip(
                    icon = Icons.Default.Speed,
                    text = "${project.bpm} BPM",
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Collaborators Avatars
                Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                    project.collaborators.take(3).forEach { collab ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.Black, CircleShape)
                                .background(Color.Gray)
                        )
                    }
                    if (project.collaborators.size > 3) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${project.collaborators.size - 3}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (playingPreviewProjectId == project.id) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (globalIsPlaying) Color(0xFF00FFCC) else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (globalIsPlaying) "PRÉCOUTE..." else "EN PAUSE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            val progressFraction = if (globalDuration > 0) globalPosition.toFloat() / globalDuration else 0f
                            Text(
                                text = String.format("%02d:%02d", (globalPosition / 1000) / 60, (globalPosition / 1000) % 60),
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { if (globalDuration > 0) globalPosition.toFloat() / globalDuration else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = Color(0xFF00FFCC),
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            // Bottom actions & collaborators avatars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CollaboratorsStack(collaborators = project.collaborators)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPlayPreviewClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (playingPreviewProjectId == project.id && globalIsPlaying) Color(0xFFFF5500) else Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = if (playingPreviewProjectId == project.id && globalIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (playingPreviewProjectId == project.id && globalIsPlaying) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = onJoin,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "OUVRIR",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// Overlapping visual stack of collaborators avatars
@Composable
fun CollaboratorsStack(collaborators: List<CollabUser>) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        collaborators.take(3).forEachIndexed { index, user ->
            Box(
                modifier = Modifier
                    .offset(x = (-8 * index).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF141416), CircleShape)
                    .background(
                        when (user.role) {
                            "owner" -> Color(0xFF06B6D4)
                            "editor" -> Color(0xFF8B5CF6)
                            else -> Color(0xFF10B981)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }
        if (collaborators.size > 3) {
            Box(
                modifier = Modifier
                    .offset(x = (-8 * 3).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF141416), CircleShape)
                    .background(Color(0xFF262629)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${collaborators.size - 3}",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// Status badge helper component
@Composable
fun ProjectStatusBadge(status: String) {
    val (bColor, tColor, labelText) = when (status) {
        "LIVE" -> Triple(Color(0xFFFFEAEB), Color(0xFFE11D48), "LIVE")
        "REMIX" -> Triple(Color(0xFFEDE9FE), Color(0xFF8B5CF6), "REMIX")
        "SYNCED" -> Triple(Color(0xFFECFDF5), Color(0xFF10B981), "SYNCED")
        "IN_REVIEW" -> Triple(Color(0xFFECFEFF), Color(0xFF06B6D4), "REVUE")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF4B5563), "DRAFT")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bColor.copy(alpha = 0.15f))
            .border(1.dp, bColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (status == "LIVE") {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(850, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer(alpha = alpha)
                    .clip(CircleShape)
                    .background(tColor)
            )
        }
        Text(
            text = labelText,
            color = tColor,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp
        )
    }
}

// MULTI-TRACK AUDIO WORKSPACE (DAW/STUDIO COMPONENT)
@Composable
fun StudioDAWWorkspace(
    project: CollabProject,
    currentUserId: String,
    webSocketManager: CollaborationWebSocketManager,
    voiceMessages: List<VoiceMessage>,
    onAddTrack: (String) -> Unit,
    onUpdateTrackControls: (String, Float, Float, Boolean) -> Unit,
    onCompilePublish: () -> Unit,
    audioPlayerManager: AudioPlayerManager? = null
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Connect to actual physical audio player states
    val isPlaying by audioPlayerManager?.isPlaying?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val currentPosMs by audioPlayerManager?.currentPosition?.collectAsState(initial = 0L) ?: remember { mutableStateOf(0L) }
    val durationMs by audioPlayerManager?.duration?.collectAsState(initial = 0L) ?: remember { mutableStateOf(0L) }

    val playheadPosition = remember(currentPosMs, durationMs, isPlaying) {
        if (durationMs > 0) {
            (currentPosMs.toFloat() / durationMs.toFloat()) * 100f
        } else {
            0f
        }
    }

    // Add Sound / Track State managers
    var showAddTrackDialog by remember { mutableStateOf(false) }
    var customTrackName by remember { mutableStateOf("") }
    var communitySounds by remember { mutableStateOf<List<com.example.domain.model.Sound>>(emptyList()) }
    var isLoadingCommunitySounds by remember { mutableStateOf(false) }

    LaunchedEffect(showAddTrackDialog) {
        if (showAddTrackDialog && communitySounds.isEmpty()) {
            isLoadingCommunitySounds = true
            try {
                communitySounds = NetworkModule.api.getRecommendedSounds(limit = 8)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingCommunitySounds = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeline and Audio tracks
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(project.tracks, key = { it.id }) { track ->
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StudioTrackItem(
                        track = track,
                        playheadPercent = playheadPosition,
                        isPlaying = isPlaying,
                        onVolumeChange = { vol ->
                            onUpdateTrackControls(track.id, vol, track.pan, track.isMuted)
                        },
                        onMuteToggle = {
                            onUpdateTrackControls(track.id, track.volume, track.pan, !track.isMuted)
                        }
                    )
                }
            }
        }

        // Action controllers bar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
            border = BorderStroke(1.dp, Color(0xFF262629)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Seekable Timeline Progress Bar with timestamps
                if (durationMs > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentSec = (currentPosMs / 1000) % 60
                            val currentMin = (currentPosMs / 1000) / 60
                            val durationSec = (durationMs / 1000) % 60
                            val durationMin = (durationMs / 1000) / 60
                            
                            Text(
                                text = String.format("%02d:%02d", currentMin.toInt(), currentSec.toInt()),
                                color = Color(0xFF00FFCC),
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "Mixeur Live en Temps Réel 🎧",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Text(
                                text = String.format("%02d:%02d", durationMin.toInt(), durationSec.toInt()),
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = playheadPosition,
                            onValueChange = { percent ->
                                val targetPositionMs = (percent / 100f * durationMs).toLong()
                                audioPlayerManager?.seekTo(targetPositionMs)
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00FFCC),
                                activeTrackColor = Color(0xFF00FFCC),
                                inactiveTrackColor = Color(0xFF262629)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                // Play and DAW buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Play / Pause Toggle Button
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    audioPlayerManager?.togglePlayPause()
                                } else {
                                    val streamUrl = when (project.id) {
                                        "p_retro_synth" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                                        "p_lofi_sunset" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
                                        "p_guitar_cover" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
                                        else -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
                                    }
                                    val currentTrackUri = audioPlayerManager?.currentTrack?.value?.requestMetadata?.mediaUri?.toString()
                                        ?: audioPlayerManager?.currentTrack?.value?.localConfiguration?.uri?.toString()
                                    if (currentTrackUri == streamUrl) {
                                        audioPlayerManager?.togglePlayPause()
                                    } else {
                                        audioPlayerManager?.playTrack(streamUrl)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) Color(0xFFE11D48) else Color(0xFF00FFCC))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black
                            )
                        }

                        // Stop/Rewind Button
                        IconButton(
                            onClick = {
                                audioPlayerManager?.player?.stop()
                                audioPlayerManager?.seekTo(0L)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF262629))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = Color.LightGray
                            )
                        }
                    }

                    // Add Track Button
                    Button(
                        onClick = { showAddTrackDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5500)),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Divider(color = Color(0xFF262629), thickness = 1.dp)

                // Publish work
                Button(
                    onClick = onCompilePublish,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compiler / Publier le Remix", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // Add Track / Sound Dialog
    if (showAddTrackDialog) {
        AlertDialog(
            onDismissRequest = { showAddTrackDialog = false },
            containerColor = Color(0xFF141416),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFFFF5500), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bibliothèque de sons & Pistes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Custom track name option
                    Text("1. CRÉER UNE PISTE PERSONNALISÉE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = customTrackName,
                        onValueChange = { customTrackName = it },
                        placeholder = { Text("Ex: Synth Wave, Voix Off...", color = Color.Gray) },
                        maxLines = 1,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF5500),
                            unfocusedBorderColor = Color(0xFF262629),
                            cursorColor = Color(0xFFFF5500)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (customTrackName.isNotBlank()) {
                                onAddTrack(customTrackName)
                                customTrackName = ""
                                showAddTrackDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262629)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = customTrackName.isNotBlank()
                    ) {
                        Text("Ajouter cette piste vide", fontWeight = FontWeight.Bold)
                    }

                    Divider(color = Color(0xFF262629))

                    // Community sounds option
                    Text("2. IMPORTER UN SON DE LA COMMUNAUTÉ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (isLoadingCommunitySounds) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF5500),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (communitySounds.isEmpty()) {
                        Text("Aucun son communautaire disponible hors ligne", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(communitySounds) { sound ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                                    border = BorderStroke(1.dp, Color(0xFF262629)),
                                    onClick = {
                                        onAddTrack(sound.title)
                                        // Robust: Send a websocket message when adding a track
                                        try {
                                            val payload = JSONObject().apply {
                                                put("type", "track_added")
                                                put("track_name", sound.title)
                                                put("sound_id", sound.id)
                                            }.toString()
                                            webSocketManager.sendProjectUpdate(project.id, payload)
                                        } catch (e: Exception) { e.printStackTrace() }
                                        showAddTrackDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color(0xFF00FFCC),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(sound.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Text("@${sound.username ?: sound.author_username ?: "Auteur"}", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color(0xFF262629))

                    // Presets
                    Text("3. PRESETS RAPIDES", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Beat Drums", "Vocal Echo", "Melodic Synth").forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1E22))
                                    .border(1.dp, Color(0xFF262629), RoundedCornerShape(8.dp))
                                    .clickable {
                                        onAddTrack(preset)
                                        try {
                                            val payload = JSONObject().apply {
                                                put("type", "track_added")
                                                put("track_name", preset)
                                                put("is_preset", true)
                                            }.toString()
                                            webSocketManager.sendProjectUpdate(project.id, payload)
                                        } catch (e: Exception) { e.printStackTrace() }
                                        showAddTrackDialog = false
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(preset, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddTrackDialog = false }) {
                    Text("Annuler", color = Color.Gray)
                }
            }
        )
    }
}

// Detailed multitrack visualizer with dynamic level meter and custom drawn waveforms
@Composable
fun StudioTrackItem(
    track: CollabTrack,
    playheadPercent: Float,
    isPlaying: Boolean,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
        border = BorderStroke(1.dp, Color(0xFF1A1A1C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Title and Action Buttons (Mute/Volume)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(track.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = track.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute (M) button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (track.isMuted) Color(0xFFE11D48) else Color(0xFF262629))
                            .clickable { onMuteToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            color = if (track.isMuted) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // dB Volume level reader
                    Text(
                        text = if (track.isMuted) "-∞ dB" else "${(track.volume * 10).toInt()} dB",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Timeline Waveform area and Gain Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Waveform drawn on custom Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F0F10))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val barsCount = 34
                        val barWidth = width / (barsCount * 1.5f)
                        val barGap = barWidth * 0.5f
                        
                        // Seed points
                        val waveSeed = track.id.hashCode()
                        val rand = Random(waveSeed)
                        
                        for (i in 0 until barsCount) {
                            val barHeight = height * (0.2f + rand.nextFloat() * 0.7f)
                            val x = i * (barWidth + barGap)
                            val y = (height - barHeight) / 2f
                            
                            val barColor = if (playheadPercent > (i.toFloat() / barsCount * 100f)) {
                                track.color
                            } else {
                                track.color.copy(alpha = 0.25f)
                            }
                            
                            drawRect(
                                color = if (track.isMuted) Color.DarkGray.copy(alpha = 0.4f) else barColor,
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                        }

                        // Drawing playhead vector line
                        val playheadX = width * (playheadPercent / 100f)
                        drawLine(
                            color = Color.White,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, height),
                            strokeWidth = 2f
                        )
                    }
                }

                // Inline Slider to adjust Gain Volume
                Slider(
                    value = track.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.width(70.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = track.color,
                        activeTrackColor = track.color,
                        inactiveTrackColor = Color(0xFF262629)
                    )
                )

                // Compact Animated level meter (EQ lights)
                Column(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (i in 0 until 5) {
                        val lit = isPlaying && !track.isMuted && Random.nextFloat() > (i / 5.5f)
                        val lightColor = if (i == 4) Color.Red else if (i == 3) Color.Yellow else Color.Green
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(if (lit) lightColor else Color(0xFF1E1E22))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceChatSection(
    project: CollabProject,
    currentUserId: String,
    webSocketManager: CollaborationWebSocketManager,
    voiceMessages: List<VoiceMessage>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // States
    var audioFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    
    var playingMessageId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    val otherCollaborators = remember(project.collaborators) {
        project.collaborators.filter { it.userId != currentUserId }
    }
    var selectedRecipient by remember { mutableStateOf<CollabUser?>(null) }
    
    // Default recipient logic
    LaunchedEffect(otherCollaborators) {
        if (selectedRecipient == null && otherCollaborators.isNotEmpty()) {
            selectedRecipient = otherCollaborators.first()
        }
    }
    
    // Timer Effect
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration += 1
            }
        }
    }
    
    // Release resources on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.release()
                mediaPlayer?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // Native microphone request permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val file = File(context.cacheDir, "voice_msg_${System.currentTimeMillis()}.m4a")
                audioFile = file
                
                val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder
                isRecording = true
                recordingDuration = 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F0F11))
            .border(1.dp, Color(0xFF232326), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chat Header with receiver dropdown & indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Discussion Vocal live",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                     text = "Studio Lab Live - Échangez vos idées",
                     color = Color.Gray,
                     fontSize = 11.sp
                )
            }
            
            // Receiver selector
            if (otherCollaborators.isNotEmpty()) {
                var expandedRecipient by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1C1C1E))
                            .clickable { expandedRecipient = !expandedRecipient }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "A: ${selectedRecipient?.username ?: "Tous"}",
                                color = Color(0xFF00FFCC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expandedRecipient,
                        onDismissRequest = { expandedRecipient = false },
                        modifier = Modifier.background(Color(0xFF1E1E22))
                    ) {
                        otherCollaborators.forEach { colleague ->
                            DropdownMenuItem(
                                text = { Text(colleague.username, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    selectedRecipient = colleague
                                    expandedRecipient = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text(
                     text = "Seul dans le studio",
                     color = Color.Gray,
                     fontSize = 11.sp,
                     fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        HorizontalDivider(color = Color(0xFF1B1B1F), thickness = 1.dp)
        
        // Chat messages bubble log list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            reverseLayout = true
        ) {
            val sortedMessages = voiceMessages.reversed()
            if (sortedMessages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(36.dp))
                        Text("Aucun message vocal pour le moment", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            } else {
                items(sortedMessages, key = { it.id }) { msg ->
                    VoiceMessageBubble(
                        msg = msg,
                        isPlaying = playingMessageId == msg.id,
                        onPlayToggle = {
                            if (playingMessageId == msg.id) {
                                try {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                mediaPlayer = null
                                playingMessageId = null
                            } else {
                                try {
                                    mediaPlayer?.release()
                                } catch (e: Exception) {}
                                
                                playingMessageId = msg.id
                                mediaPlayer = MediaPlayer().apply {
                                    try {
                                        setDataSource(msg.audioUrl)
                                        prepareAsync()
                                        setOnPreparedListener { start() }
                                        setOnCompletionListener {
                                            playingMessageId = null
                                            release()
                                            mediaPlayer = null
                                        }
                                        setOnErrorListener { _, _, _ ->
                                            playingMessageId = null
                                            release()
                                            mediaPlayer = null
                                            true
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        playingMessageId = null
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        
        // Recording / Control bar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
            border = BorderStroke(1.dp, Color(0xFF262629)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                        )
                        Text(
                            text = "Enregistrement... ${recordingDuration}s",
                            color = Color(0xFFE11D48),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Stop & Send button
                    Button(
                        onClick = {
                            val recipientId = selectedRecipient?.userId ?: "all_collaborators"
                            try {
                                mediaRecorder?.apply {
                                    stop()
                                    release()
                                }
                                mediaRecorder = null
                                isRecording = false
                                
                                val file = audioFile
                                if (file != null && file.exists()) {
                                    coroutineScope.launch {
                                        try {
                                            val bytes = file.readBytes()
                                            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                            webSocketManager.sendVoiceMessage(recipientId, base64Data)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Envoyer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Choisissez dest. puis enregistrez",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    
                    // Start Recording button
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                try {
                                    val file = File(context.cacheDir, "voice_msg_${System.currentTimeMillis()}.m4a")
                                    audioFile = file
                                    
                                    val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        MediaRecorder(context)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        MediaRecorder()
                                    }
                                    
                                    recorder.apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                        setOutputFile(file.absolutePath)
                                        prepare()
                                        start()
                                    }
                                    mediaRecorder = recorder
                                    isRecording = true
                                    recordingDuration = 0
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enregistrer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceMessageBubble(
    msg: VoiceMessage,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit
) {
    val bubbleColor = if (msg.isMe) Color(0xFF2E1065) else Color(0xFF1F1F22)
    val alignment = if (msg.isMe) Alignment.End else Alignment.Start
    val headingColor = if (msg.isMe) Color(0xFF00FFCC) else Color(0xFF8B5CF6)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (msg.isMe) "Moi" else "@${msg.senderName}",
                color = headingColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = msg.timestamp,
                color = Color.DarkGray,
                fontSize = 9.sp
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleColor)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) Color(0xFFE11D48) else Color(0xFF00FFCC))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Decorative audio waves representing the voice message
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val waveHeights = listOf(12, 18, 8, 24, 14, 20, 10, 16)
                waveHeights.forEach { height ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isPlaying) Color(0xFFE11D48) else Color.LightGray)
                    )
                }
            }
            
            Text(
                text = "Audio .m4a",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CollaborationDrawerSheet(
    project: CollabProject?,
    allProjects: List<CollabProject>,
    currentUserId: String,
    webSocketManager: CollaborationWebSocketManager,
    voiceMessages: List<VoiceMessage>,
    onInviteClick: () -> Unit,
    onAddTrack: (String) -> Unit,
    onProjectSelect: (CollabProject) -> Unit
) {
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickTrackName by remember { mutableStateOf("") }
    
    // Image picker for cover
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val coverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val file = File(context.cacheDir, "project_cover_${project?.id}.jpg")
                    context.contentResolver.openInputStream(it)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("cover_file", file.name, requestFile)
                    project?.let { proj ->
                        NetworkModule.api.updateProjectCover(proj.id, body)
                        // Trigger a local refresh or notify user
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Project Browser states
    var serverProjects by remember { mutableStateOf<List<com.example.domain.model.ProjectResponse>>(emptyList()) }
    var isLoadingProjects by remember { mutableStateOf(false) }
    var projectSearchQuery by remember { mutableStateOf("") }

    var drawerTabState by remember(project) { mutableStateOf(if (project == null) 1 else 0) }

    LaunchedEffect(Unit) {
        isLoadingProjects = true
        try {
            serverProjects = NetworkModule.api.getProjects()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingProjects = false
        }
    }

    if (showQuickAddDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddDialog = false },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(24.dp),
            title = { Text("Ajouter une piste", color = Color.White, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = quickTrackName,
                    onValueChange = { quickTrackName = it },
                    placeholder = { Text("Ex: Lead Synth, Vocal Chop...", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickTrackName.isNotBlank()) {
                            onAddTrack(quickTrackName)
                            quickTrackName = ""
                            showQuickAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ajouter", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddDialog = false }) {
                    Text("Annuler", color = Color.Gray)
                }
            }
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF0F172A),
        modifier = Modifier.width(340.dp),
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
    ) {
        if (project != null) {
            TabRow(
                selectedTabIndex = drawerTabState,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF00FFCC),
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[drawerTabState]),
                        color = Color(0xFF00FFCC)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = drawerTabState == 0,
                    onClick = { drawerTabState = 0 },
                    text = { Text("SESSION", fontSize = 12.sp, fontWeight = FontWeight.Black) }
                )
                Tab(
                    selected = drawerTabState == 1,
                    onClick = { drawerTabState = 1 },
                    text = { Text("NAVIGUER", fontSize = 12.sp, fontWeight = FontWeight.Black) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (drawerTabState == 0 && project != null) {
                // SESSION INFO & ANALYTICS
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { coverLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (project.coverUrl != null) {
                                AsyncImage(
                                    model = project.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Ajouter une cover", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                                }
                            }
                            
                            // Edit overlay button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(project.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(project.genre, color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Analytics Display in Sidebar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnalyticItem(icon = Icons.Default.Headset, value = project.listenersCount.toString(), label = "AUD.")
                        AnalyticItem(icon = Icons.Default.Visibility, value = project.visitorsCount.toString(), label = "VIS.")
                        AnalyticItem(icon = Icons.Default.Favorite, value = project.likesCount.toString(), label = "LIKES")
                        AnalyticItem(icon = Icons.Default.Comment, value = project.commentsCount.toString(), label = "COM.")
                        AnalyticItem(icon = Icons.Default.PersonAdd, value = project.followersCount.toString(), label = "FOL.")
                    }
                }

                // Chat/Voice Messaging Section
                item {
                    Text("CHAT & MESSAGES VOCAUX", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    ) {
                        VoiceChatSection(
                            project = project,
                            currentUserId = currentUserId,
                            webSocketManager = webSocketManager,
                            voiceMessages = voiceMessages
                        )
                    }
                }

                // Tracks List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PISTES (${project.tracks.size})", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        IconButton(onClick = { showQuickAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                items(project.tracks) { track ->
                    StudioTrackItem(track = track)
                }
                
                // Collaborators
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("COLLABORATEURS (${project.collaborators.size})", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        IconButton(onClick = onInviteClick) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                items(project.collaborators) { collab ->
                    CollaboratorItem(collab = collab, isMe = collab.userId == currentUserId)
                }

            } else {
                // ALL COLLABS BROWSER
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("DÉCOUVRIR", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("Sessions ouvertes au public", color = Color.Gray, fontSize = 12.sp)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = projectSearchQuery,
                            onValueChange = { projectSearchQuery = it },
                            placeholder = { Text("Rechercher un lab...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FFCC),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
                        )
                    }
                }

                val filteredServerProj = serverProjects.filter { it.title.contains(projectSearchQuery, ignoreCase = true) }
                
                if (isLoadingProjects) {
                    item { Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00FFCC)) } }
                } else {
                    items(filteredServerProj) { proj ->
                        ProjectListItem(
                            proj = proj,
                            isSelected = project?.id == proj.id,
                            onSelect = {
                                val mapped = CollabProject(
                                    id = proj.id,
                                    title = proj.title,
                                    description = proj.description,
                                    status = "SYNCED",
                                    bpm = 120,
                                    genre = "StripSound Mix",
                                    coverUrl = proj.cover_url,
                                    createdAt = proj.created_at,
                                    collaborators = listOf(CollabUser(currentUserId, "Moi", null, "owner")),
                                    tracks = listOf(CollabTrack("t_init", "Piste Audio Initiale", color = Color(0xFF00FFCC))),
                                    listenersCount = proj.listeners_count,
                                    visitorsCount = proj.visitors_count,
                                    likesCount = proj.likes_count,
                                    commentsCount = proj.comments_count,
                                    followersCount = proj.followers_count
                                )
                                onProjectSelect(mapped)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudioTrackItem(track: CollabTrack) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(track.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = track.color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(if (track.isMuted) "Muet" else "Volume ${(track.volume * 100).toInt()}%", color = Color.Gray, fontSize = 10.sp)
        }
        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun CollaboratorItem(collab: CollabUser, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155)),
            contentAlignment = Alignment.Center
        ) {
            Text(collab.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(collab.username + (if (isMe) " (Vous)" else ""), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(collab.role.uppercase(), color = Color(0xFF00FFCC), fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ProjectListItem(proj: com.example.domain.model.ProjectResponse, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1E293B) else Color.White.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    if (proj.cover_url != null) {
                        AsyncImage(
                            model = proj.cover_url,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(proj.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text("Session Lab • 120 BPM", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallAnalytic(icon = Icons.Default.Headset, value = proj.listeners_count.toString())
                SmallAnalytic(icon = Icons.Default.Favorite, value = proj.likes_count.toString())
                SmallAnalytic(icon = Icons.Default.Comment, value = proj.comments_count.toString())
            }
        }
    }
}

@Composable
fun SmallAnalytic(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(12.dp))
        Text(value, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
