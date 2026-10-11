package com.example.webradioapp.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.webradioapp.SettingsManager
import com.example.webradioapp.playlist.PlayMode
import com.example.webradioapp.service.RadioPlayerService
import com.example.webradioapp.playlist.Track
import com.google.common.util.concurrent.ListenableFuture
import com.example.webradioapp.service.NewsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RadioViewModel : ViewModel() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentTitle = MutableStateFlow("Regia Pronta - Premi PLAY per cominciare")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()
    private val _currentMode = MutableStateFlow(PlayMode.PLAYLIST_1)
    val currentMode: StateFlow<PlayMode> = _currentMode.asStateFlow()
    private val _nextTrackTitle = MutableStateFlow("Scansione file in corso...")
    val nextTrackTitle: StateFlow<String> = _nextTrackTitle.asStateFlow()
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    private val _playingPlaylistId = MutableStateFlow(-1)
    val playingPlaylistId: StateFlow<Int> = _playingPlaylistId.asStateFlow()
    private val _upcomingPlaylistId = MutableStateFlow(-1)
    val upcomingPlaylistId: StateFlow<Int> = _upcomingPlaylistId.asStateFlow()
    private val _currentTrackUrl = MutableStateFlow<String?>(null)
    val currentTrackUrl: StateFlow<String?> = _currentTrackUrl.asStateFlow()

    private var uiSyncJob: kotlinx.coroutines.Job? = null
    
    // Stub obsoleto di compatibilità con eventuale chiamata in MainActivity 
    fun scanLibraries(context: Context, settingsManager: SettingsManager) { }
    fun startNewsTimer(settingsManager: SettingsManager) { }

    fun initializeController(context: Context, settings: SettingsManager) {
        val sessionToken = SessionToken(context, ComponentName(context, RadioPlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        uiSyncJob?.cancel()
        uiSyncJob = viewModelScope.launch {
            while (RadioPlayerService.instance == null) { delay(200) }
            
            while (true) {
                delay(100)
                val srv = RadioPlayerService.instance ?: continue
                val activePlayer = srv.getActiveUserPlayer()
                val jinglePlayer = srv.playerSFX
                
                _isPlaying.value = activePlayer.isPlaying || jinglePlayer.isPlaying
                _currentMode.value = srv.playlistManager.currentMode
                _playingPlaylistId.value = srv.playingPlaylistId
                
                val next = srv.playlistManager.upcomingTrack
                if (next != null) {
                    _nextTrackTitle.value = next.title
                    _upcomingPlaylistId.value = next.playlistId
                } else {
                    _nextTrackTitle.value = "CARTELLE VUOTE"
                }
                _queueSize.value = srv.playlistManager.getQueue().size

                if (!srv.isEngineRunning) {
                    _currentTitle.value = "Regia Pronta - Premi PLAY per cominciare"
                    _duration.value = 0L
                    _currentPosition.value = 0L
                    _currentTrackUrl.value = null
                } else if (jinglePlayer.isPlaying || srv.currentMixState == "JINGLE_SOLO") {
                    _currentPosition.value = jinglePlayer.currentPosition
                    _duration.value = jinglePlayer.duration.coerceAtLeast(0)
                    val md = jinglePlayer.mediaMetadata.title?.toString()
                    _currentTitle.value = if (!md.isNullOrEmpty()) md else "INTERMEZZO SPOT/JINGLE..."
                    _currentTrackUrl.value = jinglePlayer.currentMediaItem?.mediaId
                } else {
                    _currentPosition.value = activePlayer.currentPosition
                    _duration.value = activePlayer.duration.coerceAtLeast(0)
                    val md = activePlayer.mediaMetadata.title?.toString()
                    if (!md.isNullOrEmpty() && activePlayer.mediaItemCount > 0) _currentTitle.value = md
                    _currentTrackUrl.value = activePlayer.currentMediaItem?.mediaId
                }
            }
        }
    }

    fun releaseController() {
        controllerFuture?.let { 
            MediaController.releaseFuture(it)
            controllerFuture = null
        }
        uiSyncJob?.cancel()
    }

    fun playPause() {
        val srv = RadioPlayerService.instance ?: return
        val activePlayer = srv.getActiveUserPlayer()
        val jingle = srv.playerSFX
        
        if (activePlayer.isPlaying || jingle.isPlaying) {
            activePlayer.pause(); jingle.pause()
        } else {
            if (!srv.isEngineRunning) srv.playInitialMusic() else { activePlayer.play(); jingle.play() } 
        }
    }

    fun skipTrack() { RadioPlayerService.instance?.skipTrack() }


    fun deleteTrack(context: Context, url: String) {
        val srv = RadioPlayerService.instance
        if (srv != null && currentTrackUrl.value == url) {
            skipTrack() // Avvia la sfumatura e il passaggio
            Toast.makeText(context, "Eliminazione programmata...", Toast.LENGTH_SHORT).show()
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Attendiamo che il player finisca la sfumatura e rilasci il file (circa 2.5s)
            delay(3500)
            
            var deleted = false
            try {
                val file = java.io.File(url)
                if (file.exists() && file.delete()) {
                    deleted = true
                } else {
                    // Fallback con MediaStore se File.delete fallisce su Android 10+
                    val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    val selection = "_data = ?"
                    val selectionArgs = arrayOf(url)
                    val deletedRows = context.contentResolver.delete(uri, selection, selectionArgs)
                    if (deletedRows > 0) deleted = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            withContext(Dispatchers.Main) {
                if (deleted) {
                    Toast.makeText(context, "Brano eliminato definitivamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Impossibile eliminare il file", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun changePlayMode(mode: PlayMode) {
        val srv = RadioPlayerService.instance ?: return
        srv.playlistManager.currentMode = mode
        srv.playlistManager.rollNextTrack(srv.settings)
    }

    fun refreshLibraries() {
        val srv = RadioPlayerService.instance ?: return
        srv.playlistManager.scanLocalFolders(srv, srv.settings)
        srv.playlistManager.rollNextTrack(srv.settings)
    }

    fun addUriToQueue(context: Context, uri: Uri) {
        val srv = RadioPlayerService.instance ?: return
        var title = "Brano Manuale"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { 
                if (it.moveToFirst()) title = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)).substringBeforeLast(".") 
            }
        } catch(e:Exception){}
        srv.playlistManager.addToQueue(Track(uri.toString(), title, uri.toString(), -1))
        srv.playlistManager.rollNextTrack(srv.settings)
    }

    fun playPodcast(context: Context, type: String) {
        val srv = RadioPlayerService.instance ?: return
        
        Toast.makeText(context, "Ricerca ultima puntata in corso...", Toast.LENGTH_SHORT).show()
        
        viewModelScope.launch(Dispatchers.IO) {
            val url = when (type) {
                "ZANZARA" -> NewsManager.fetchLatestZanzaraUrl()
                "ZOO_105" -> NewsManager.fetchLatestZoo105Url()
                else -> null
            }
            
            withContext(Dispatchers.Main) {
                if (url != null) {
                    val title = if (type == "ZANZARA") "La Zanzara - Ultima Puntata" else "Lo Zoo di 105 - Ultima Puntata"
                    srv.playlistManager.addToQueue(Track(url, title, url, -1))
                    srv.playlistManager.rollNextTrack(srv.settings)
                    Toast.makeText(context, "Podcast aggiunto in coda", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Errore nel caricamento del podcast", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun shutdownEngine(context: Context) {
        val srv = RadioPlayerService.instance
        srv?.stopSelf()
        releaseController()
        (context as? android.app.Activity)?.finishAndRemoveTask()
        kotlin.system.exitProcess(0)
    }
    
    override fun onCleared() {
        releaseController()
        super.onCleared()
    }
}
