package com.example.webradioapp.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.webradioapp.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.webradioapp.SettingsManager
import com.example.webradioapp.viewmodel.RadioViewModel
import com.example.webradioapp.playlist.PlayMode
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RadioScreen(viewModel: RadioViewModel, settingsManager: SettingsManager) {
    var isLocked by remember { mutableStateOf(true) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var currentAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(5000)
        showSplash = false
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (showSplash) return@BackHandler
        if (showSettings) showSettings = false
        else if (showUnlockDialog) showUnlockDialog = false
        else showExitDialog = true
    }

    if (showSplash) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF151515)), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "Splash Screen",
                modifier = Modifier.fillMaxSize().padding(32.dp)
            )
        }
        return
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Arresto Motori Regia", color = Color.White) },
            text = { Text("Vuoi spegnere definitivamente l'automazione radiofonica totale e uscire dall'app?", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = { 
                    showExitDialog = false
                    viewModel.shutdownEngine(context)
                }) { Text("SPEGNI RADIO", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("ANNULLA", color = Color.Gray) }
            },
            containerColor = Color(0xFF222222)
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Elimina Brano", color = Color.White) },
            text = { Text("Sei sicuro di voler eliminare DEFINITIVAMENTE dal dispositivo il brano in riproduzione?\n\nQuesta azione non può essere annullata.", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteConfirmDialog = false
                    val url = viewModel.currentTrackUrl.value
                    if (url != null) viewModel.deleteTrack(context, url)
                }) { Text("ELIMINA", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("ANNULLA", color = Color.Gray) }
            },
            containerColor = Color(0xFF222222)
        )
    }

    val pickAudioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let { viewModel.addUriToQueue(context, it) }
    }

    fun requireUnlock(action: () -> Unit) {
        if (isLocked) {
            currentAction = action
            showUnlockDialog = true
        } else {
            action()
        }
    }

    if (showSettings) {
        SettingsScreen(settingsManager, viewModel) { showSettings = false }
        return
    }

    if (showUnlockDialog) {
        var pin by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text("Console Bloccata", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pin, 
                        onValueChange = { 
                            pin = it
                            isError = false
                            if (pin == settingsManager.password) {
                                isLocked = false
                                showUnlockDialog = false
                                currentAction?.invoke()
                                currentAction = null
                            }
                        }, 
                        label = { Text("PIN", color = Color.LightGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), 
                        visualTransformation = PasswordVisualTransformation(),
                        isError = isError, 
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    if (isError) Text("PIN errato!", color = Color.Red, fontSize = 12.sp)
                }
            },
            confirmButton = { }, 
            dismissButton = { TextButton(onClick = { showUnlockDialog = false }) { Text("ANNULLA", color = Color.Gray) } }, 
            containerColor = Color(0xFF222222)
        )
    }

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    val currentTrackUrl by viewModel.currentTrackUrl.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val nextTrackTitle by viewModel.nextTrackTitle.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val queueSize by viewModel.queueSize.collectAsState()
    
    val playingPId by viewModel.playingPlaylistId.collectAsState()
    val upcomingPId by viewModel.upcomingPlaylistId.collectAsState()

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var batteryPct by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(Unit) {
        var tick = 0
        while (true) { 
            currentTimeMs = System.currentTimeMillis()
            if (tick % 60 == 0) {
                try {
                    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                        context.registerReceiver(null, ifilter)
                    }
                    val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    if (level != -1 && scale != -1) {
                        batteryPct = (level * 100 / scale.toFloat())
                    }
                } catch(e: Exception){}
            }
            tick++
            delay(1000) 
        }
    }

    val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.ITALY)
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ITALY)
    val dateStr = dateFormat.format(Date(currentTimeMs)).uppercase()
    val timeStr = timeFormat.format(Date(currentTimeMs))

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFF151515)).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("radioautomation by Ciry", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock", tint = if (isLocked) Color.Red else Color.Green,
                    modifier = Modifier.size(32.dp).clickable { if (isLocked) { requireUnlock { } } else { isLocked = true } }
                )
                Icon(imageVector = Icons.Default.Build, contentDescription = "Settings", tint = Color.Red, modifier = Modifier.size(32.dp).clickable { requireUnlock { showSettings = true } })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Box(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B1C2A)) 
            .border(2.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentTitle.uppercase(), 
                            color = Color(0xFF00FF00), 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        if (currentTrackUrl != null && !currentTrackUrl!!.startsWith("http")) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina Brano",
                                tint = Color.Red,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(28.dp)
                                    .clickable { requireUnlock { showDeleteConfirmDialog = true } }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress }, 
                        modifier = Modifier.fillMaxWidth().height(4.dp), 
                        color = Color(0xFF00FF00), 
                        trackColor = Color(0xFF003300)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val minutes = (currentPosition / 1000) / 60
                        val seconds = (currentPosition / 1000) % 60
                        val totalMinutes = (duration / 1000) / 60
                        val totalSeconds = (duration / 1000) % 60
                        Text(String.format("%02d:%02d", minutes, seconds), color = Color(0xFF00FF00), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(String.format("%02d:%02d", totalMinutes, totalSeconds), color = Color(0xFF00FF00), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr, 
                        color = Color(0xFF00FF00), 
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = timeStr, 
                            color = Color(0xFF00FF00), 
                            fontSize = 32.sp, 
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${batteryPct.toInt()}% \uD83D\uDD0B", 
                            color = Color(0xFF00FF00), 
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(95.dp).padding(start = 12.dp)) {
                    VuMeterBar(isPlaying, 200)
                    VuMeterBar(isPlaying, 350)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF222222)).padding(8.dp)) {
            Text(text = "PROSSIMA: $nextTrackTitle", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))
        SectionLabel("SELEZIONE PLAYLIST")
        
        fun getButtonColor(mode: PlayMode, modeId: Int): Color {
            if (currentMode == mode && currentMode != PlayMode.RANDOM_1_5) return Color.Red
            if (currentMode == PlayMode.RANDOM_1_5) {
                if (playingPId == modeId) return Color.Green
                if (upcomingPId == modeId) return Color.Yellow
            }
            return Color(0xFF333333)
        }
        
        val playlistsRow1 = listOf(Triple(PlayMode.PLAYLIST_1, "PLAY 1", 1), Triple(PlayMode.PLAYLIST_2, "PLAY 2", 2), Triple(PlayMode.PLAYLIST_3, "PLAY 3", 3))
        val playlistsRow2 = listOf(Triple(PlayMode.PLAYLIST_4, "PLAY 4", 4), Triple(PlayMode.PLAYLIST_5, "PLAY 5", 5), Triple(PlayMode.PLAYLIST_6, "PLAY 6", 6))
        
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                playlistsRow1.forEach { (m, l, id) -> PlaylistButton(l, getButtonColor(m, id), { requireUnlock { viewModel.changePlayMode(m) } }, Modifier.weight(1f)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                playlistsRow2.forEach { (m, l, id) -> PlaylistButton(l, getButtonColor(m, id), { requireUnlock { viewModel.changePlayMode(m) } }, Modifier.weight(1f)) }
            }
            PlaylistButton("RANDOM PLAYLIST (1-5)", if(currentMode == PlayMode.RANDOM_1_5) Color.Red else Color(0xFF333333), { requireUnlock { viewModel.changePlayMode(PlayMode.RANDOM_1_5) } }, Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(8.dp))
        SectionLabel("CONTROLLI DIRETTA")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { requireUnlock { viewModel.playPause() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Text("PLAY/PAUSA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center) }
            Button(onClick = { requireUnlock { viewModel.skipTrack() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Text("SKIP (MIX)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center) }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SectionLabel("GESTIONE CODA E PUBBLICITÀ")
        Button(
            onClick = { requireUnlock { pickAudioLauncher.launch("audio/*") } }, 
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), 
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("+ AGGIUNGI BRANO (FILE BROWSER)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        Text(text = if (queueSize == 0) "Coda vuota" else "$queueSize brani in coda", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))

        Spacer(modifier = Modifier.height(16.dp))
        
        SectionLabel("PODCAST ON-DEMAND")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Button(onClick = { requireUnlock { viewModel.playPodcast(context, "ZANZARA") } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Text("LA ZANZARA", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center) }
            Button(onClick = { requireUnlock { viewModel.playPodcast(context, "ZOO_105") } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Text("LO ZOO DI 105", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center) }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(8.dp)).padding(8.dp)) {
            var spotsEnabled by remember { mutableStateOf(settingsManager.spotFrequencyTracks > 0) }
            var freqCounter by remember { mutableStateOf(if (settingsManager.spotFrequencyTracks > 0) settingsManager.spotFrequencyTracks else 5) }
            
            Switch(
                checked = spotsEnabled, 
                onCheckedChange = { isChecked ->
                    requireUnlock {
                        spotsEnabled = isChecked
                        settingsManager.spotFrequencyTracks = if (isChecked) freqCounter else 0
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (spotsEnabled) "SPOT ON" else "SPOT OFF", color = if (spotsEnabled) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (spotsEnabled) {
                Text("Ogni", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                BasicTextField(
                    value = freqCounter.toString(),
                    onValueChange = { 
                        requireUnlock {
                            val newFreq = it.toIntOrNull() ?: 0
                            if (newFreq >= 0) {
                                freqCounter = newFreq
                                settingsManager.spotFrequencyTracks = newFreq
                            }
                        }
                    },
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(30.dp).background(Color(0xFF333333), RoundedCornerShape(4.dp)).padding(4.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("brani", color = Color.Gray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(8.dp)).padding(8.dp)) {
            var newsEnabled by remember { mutableStateOf(settingsManager.newsIntervalMinutes > 0) }
            var newsFreqCounter by remember { mutableStateOf(if (settingsManager.newsIntervalMinutes > 0) settingsManager.newsIntervalMinutes else 60) }
            
            Switch(
                checked = newsEnabled, 
                onCheckedChange = { isChecked ->
                    requireUnlock {
                        newsEnabled = isChecked
                        settingsManager.newsIntervalMinutes = if (isChecked) newsFreqCounter else 0
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2196F3), checkedTrackColor = Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (newsEnabled) "NEWS ON" else "NEWS OFF", color = if (newsEnabled) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (newsEnabled) {
                Text("Ogni", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                BasicTextField(
                    value = newsFreqCounter.toString(),
                    onValueChange = { 
                        requireUnlock {
                            val newFreq = it.toIntOrNull() ?: 0
                            if (newFreq >= 0) {
                                newsFreqCounter = newFreq
                                settingsManager.newsIntervalMinutes = newFreq
                            }
                        }
                    },
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(35.dp).background(Color(0xFF333333), RoundedCornerShape(4.dp)).padding(4.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("minuti", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SettingsScreen(settingsManager: SettingsManager, viewModel: RadioViewModel, onClose: () -> Unit) {
    var password by remember { mutableStateOf(settingsManager.password) }
    var folder1 by remember { mutableStateOf(settingsManager.getFolderName("music1", "Music1")) }
    var folder2 by remember { mutableStateOf(settingsManager.getFolderName("music2", "Music2")) }
    var folder3 by remember { mutableStateOf(settingsManager.getFolderName("music3", "Music3")) }
    var folder4 by remember { mutableStateOf(settingsManager.getFolderName("music4", "Music4")) }
    var folder5 by remember { mutableStateOf(settingsManager.getFolderName("music5", "Music5")) }
    var folder6 by remember { mutableStateOf(settingsManager.getFolderName("music6", "Music6")) }
    var jingles by remember { mutableStateOf(settingsManager.getFolderName("jingles", "Jingles")) }
    var folderSpot by remember { mutableStateOf(settingsManager.getFolderName("spot", "Spot")) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF151515)).padding(16.dp)) {
        Button(onClick = { 
            settingsManager.password = password
            settingsManager.setFolderName("music1", folder1)
            settingsManager.setFolderName("music2", folder2)
            settingsManager.setFolderName("music3", folder3)
            settingsManager.setFolderName("music4", folder4)
            settingsManager.setFolderName("music5", folder5)
            settingsManager.setFolderName("music6", folder6)
            settingsManager.setFolderName("jingles", jingles)
            settingsManager.setFolderName("spot", folderSpot)
            viewModel.refreshLibraries()
            onClose()
        }) { Text("SALVA E TORNA") }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Impostazioni Regia", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("PIN di Sblocco", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }
            item { OutlinedTextField(value = folder1, onValueChange = { folder1 = it }, label = { Text("Cartella Music1", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }
            item { OutlinedTextField(value = folder2, onValueChange = { folder2 = it }, label = { Text("Cartella Music2", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }
            item { OutlinedTextField(value = folder3, onValueChange = { folder3 = it }, label = { Text("Cartella Music3", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }
            item { OutlinedTextField(value = folder4, onValueChange = { folder4 = it }, label = { Text("Cartella Music4", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }
            item { OutlinedTextField(value = folder5, onValueChange = { folder5 = it }, label = { Text("Cartella Music5", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }
            item { OutlinedTextField(value = folder6, onValueChange = { folder6 = it }, label = { Text("Cartella Music6", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }
            item { OutlinedTextField(value = jingles, onValueChange = { jingles = it }, label = { Text("Cartella Jingles", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }
            item { OutlinedTextField(value = folderSpot, onValueChange = { folderSpot = it }, label = { Text("Cartella Spot Commerciali", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }
            item { Spacer(modifier = Modifier.height(300.dp)) }
        }
    }
}

@Composable
fun SectionLabel(text: String) { Text(text = text, color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp)) }

@Composable
fun PlaylistButton(label: String, bgColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bgColor).clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) { 
        Text(text = label, color = if (bgColor == Color.Yellow) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) 
    }
}

@Composable
fun VuMeterBar(isPlaying: Boolean, delayMillis: Int) {
    var level by remember { mutableFloatStateOf(0.1f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                level = kotlin.random.Random.nextFloat() * 0.8f + 0.2f
                delay(delayMillis.toLong())
            }
        } else { level = 0.0f }
    }
    val animatedLevel by animateFloatAsState(targetValue = level, animationSpec = tween(delayMillis))
    
    val numLeds = 14
    Column(modifier = Modifier.width(22.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
        for (i in 0 until numLeds) {
            val threshold = 1f - (i.toFloat() / numLeds)
            val isOn = animatedLevel >= threshold

            val ledColor = when {
                i < 2 -> Color.Red
                i < 5 -> Color.Yellow
                else -> Color.Green
            }
            
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 1.dp)
                .background(if (isOn) ledColor else ledColor.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
            )
        }
    }
}
