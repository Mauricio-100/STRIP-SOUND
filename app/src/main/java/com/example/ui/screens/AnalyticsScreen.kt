package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.entryModelOf

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment

// ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var mySounds by remember { mutableStateOf<List<com.example.domain.model.Sound>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Audio Preview Player
    val exoPlayer = remember { 
        val attr = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setAudioAttributes(attr, false)
            .build()
    }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var trackDuration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        try {
            val user = com.example.data.remote.NetworkModule.api.getMyProfile()
            mySounds = com.example.data.remote.NetworkModule.api.getUserSounds(user.id)
        } catch(e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    trackDuration = exoPlayer.duration.coerceAtLeast(0L)
                } else if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    isPlaying = false
                    currentPosition = 0L
                    exoPlayer.seekTo(0L)
                    exoPlayer.pause()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while(isPlaying) {
            currentPosition = exoPlayer.currentPosition
            kotlinx.coroutines.delay(200)
        }
    }

    // Mock data for charts
    val readsChartEntryModel = entryModelOf(15f, 22f, 35f, 30f, 45f, 60f, 75f) 
    val retentionChartEntryModel = entryModelOf(100f, 85f, 75f, 60f, 50f, 40f, 35f)
    val geoChartEntryModel = entryModelOf(80f, 40f, 60f, 20f, 30f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard des Projets") },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Dashboard Audio Projects
            Column {
                Text("Projets Audio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (mySounds.isEmpty()) {
                    Text("Aucun projet uploadé", color = Color.Gray)
                } else {
                    mySounds.forEach { sound ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    coil.compose.AsyncImage(
                                        model = sound.cover_url,
                                        contentDescription = "Cover",
                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sound.title, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Durée: ${String.format("%.1f", sound.duration ?: 0f)} s", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (currentlyPlayingId == sound.id && isPlaying) {
                                                exoPlayer.pause()
                                            } else {
                                                if (currentlyPlayingId != sound.id) {
                                                    sound.audio_url?.let {
                                                        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(it))
                                                        exoPlayer.prepare()
                                                    }
                                                    currentlyPlayingId = sound.id
                                                }
                                                exoPlayer.play()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentlyPlayingId == sound.id && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    val itemCoroutineScope = rememberCoroutineScope()
                                    IconButton(
                                        onClick = {
                                            itemCoroutineScope.launch {
                                                try {
                                                    com.example.data.remote.NetworkModule.api.deleteSound(sound.id)
                                                    mySounds = mySounds.filter { it.id != sound.id }
                                                    if (currentlyPlayingId == sound.id) {
                                                        exoPlayer.stop()
                                                        currentlyPlayingId = null
                                                    }
                                                } catch(e: Exception) {}
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                }
                                
                                // Player controls exactly like an HTML5 audio player when active
                                if (currentlyPlayingId == sound.id) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${String.format("%02d:%02d", (currentPosition / 1000) / 60, (currentPosition / 1000) % 60)}",
                                            color = Color.LightGray, 
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Slider(
                                            value = if (trackDuration > 0) currentPosition.toFloat() / trackDuration else 0f,
                                            onValueChange = { frac ->
                                                val target = (frac * trackDuration).toLong()
                                                exoPlayer.seekTo(target)
                                                currentPosition = target
                                            },
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        )
                                        Text(
                                            "${String.format("%02d:%02d", (trackDuration / 1000) / 60, (trackDuration / 1000) % 60)}",
                                            color = Color.LightGray, 
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    // Volume control
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        var vol by remember { mutableFloatStateOf(exoPlayer.volume) }
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Volume",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Slider(
                                            value = vol,
                                            onValueChange = { exoPlayer.volume = it; vol = it },
                                            modifier = Modifier.fillMaxWidth(0.4f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.Gray,
                                                activeTrackColor = Color.LightGray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ProvideChartStyle(m3ChartStyle()) {
                
                // Lectures (Reads) over time
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Lectures / Vues (7 derniers jours)", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Chart(
                            chart = lineChart(),
                            model = readsChartEntryModel,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(),
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }

                // Taux de Rétention (Retention rate)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Taux de Rétention (%)", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Chart(
                            chart = lineChart(),
                            model = retentionChartEntryModel,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(),
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }

                // Portée Géographique
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Portée Géographique (Top Pays)", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Chart(
                            chart = columnChart(),
                            model = geoChartEntryModel,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(),
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }
            }
        }
    }
}
