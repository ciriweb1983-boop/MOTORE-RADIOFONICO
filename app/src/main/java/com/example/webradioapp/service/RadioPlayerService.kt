package com.example.webradioapp.service

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.os.Build
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.webradioapp.SettingsManager
import com.example.webradioapp.playlist.PlaylistManager
import kotlinx.coroutines.*

class RadioPlayerService : MediaSessionService() {
    companion object {
        var instance: RadioPlayerService? = null
    }

    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isDuckedByFocus = false
    
    lateinit var playerA: ExoPlayer
    lateinit var playerB: ExoPlayer
    lateinit var playerSFX: ExoPlayer
    
    val playlistManager = PlaylistManager()
    lateinit var settings: SettingsManager

    var activeDeck = "A" 
    var isFadingOut = false
    var currentMixState = "PLAYING"
    var manualSkipRequested = false
    var tracksSinceLastSpot = 0
    var isEngineRunning = false 
    var playingPlaylistId = -1

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var mixerJob: Job? = null
    private var newsTimerJob: Job? = null
    private var lastNewsInjectionTime = System.currentTimeMillis()

    fun getActiveUserPlayer() = if (activeDeck == "A") playerA else playerB
    fun getIdleUserPlayer() = if (activeDeck == "A") playerB else playerA

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsManager(this)
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        playerA = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            
        playerB = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            
        playerSFX = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build() 
        
        mediaSession = MediaSession.Builder(this, playerA).build()
        
        playlistManager.loadHistory(settings)
        playlistManager.scanLocalFolders(this, settings)
        playlistManager.rollNextTrack(settings)
        
        mixerJob = serviceScope.launch {
            while (true) {
                delay(120)
                if (isEngineRunning) tickMixer()
            }
        }
        
        startNewsTimer()
        setupAudioFocus()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    private fun startNewsTimer() {
        newsTimerJob?.cancel()
        newsTimerJob = serviceScope.launch {
            while (true) {
                delay(60_000L) 
                val interval = settings.newsIntervalMinutes
                if (interval > 0 && isEngineRunning) {
                    if (System.currentTimeMillis() - lastNewsInjectionTime >= interval * 60_000L) {
                        lastNewsInjectionTime = System.currentTimeMillis()
                        withContext(Dispatchers.IO) {
                            val url = NewsManager.fetchLatestNewsUrl()
                            if (url != null) {
                                withContext(Dispatchers.Main) {
                                    playlistManager.addNextTrack(com.example.webradioapp.playlist.Track(url, "Sky TG24", url, -1))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun playInitialMusic() {
        val track = playlistManager.consumeNextMusicTrack(settings)
        if (track != null) {
            val p = getActiveUserPlayer()
            mediaSession?.player = p
            val mediaItem = MediaItem.Builder()
                .setMediaId(track.url)
                .setUri(track.url)
                .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(track.title).build())
                .build()
            p.setMediaItem(mediaItem)
            p.volume = 1.0f
            p.prepare()
            p.play()
            playingPlaylistId = track.playlistId
            isEngineRunning = true
            requestAppAudioFocus()
        }
    }

    private fun setupAudioFocus() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange -> handleAudioFocusChange(focusChange) }
                .build()
        }
    }

    private fun requestAppAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            audioManager?.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                getActiveUserPlayer().pause()
                getIdleUserPlayer().pause()
                playerSFX.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                isDuckedByFocus = true
                getActiveUserPlayer().volume = 0.2f
                playerSFX.volume = 0.2f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isDuckedByFocus) {
                    isDuckedByFocus = false
                    getActiveUserPlayer().volume = 1.0f
                    playerSFX.volume = 1.0f
                } else if (isEngineRunning) {
                    // Try to resume safely
                    if (currentMixState != "JINGLE_SOLO") {
                        getActiveUserPlayer().play()
                    }
                    if (currentMixState == "JINGLE_SOLO" || playerSFX.currentPosition > 0) {
                        playerSFX.play()
                    }
                }
            }
        }
    }

    fun skipTrack() {
        if (!isFadingOut && !manualSkipRequested) {
            manualSkipRequested = true 
            tracksSinceLastSpot = 99999 
        }
    }

    private fun tickMixer() {
        val activePlayer = getActiveUserPlayer()
        val jinglePlayer = playerSFX

        val d = activePlayer.duration
        val p = activePlayer.currentPosition
        val remainingMusic = if (d > 0 && p > 0) d - p else 999999L

        val jd = jinglePlayer.duration
        val jp = jinglePlayer.currentPosition
        val remainingJingle = if (jd > 0 && jp > 0) jd - jp else 0L

        val isPlaybackEnded = activePlayer.playbackState == androidx.media3.common.Player.STATE_ENDED
        val forceMix = manualSkipRequested || isPlaybackEnded

        val isNews = activePlayer.currentMediaItem?.mediaId?.contains("spreaker", ignoreCase = true) == true || 
                     activePlayer.currentMediaItem?.mediaId?.contains("tg24", ignoreCase = true) == true
        
        val crossfadeThreshold = if (isNews) 100L else 4400L

        if (!isFadingOut && (forceMix || (activePlayer.isPlaying && remainingMusic in 1..crossfadeThreshold))) {
            isFadingOut = true
            manualSkipRequested = false
            currentMixState = "FADING_OUT"
            
            val freq = settings.spotFrequencyTracks
            val isSpotTurn = freq > 0 && tracksSinceLastSpot >= freq
            
            val overlayMedia = if (isSpotTurn) {
                playlistManager.getRandomSpot() ?: playlistManager.getRandomJingle()
            } else {
                playlistManager.getRandomJingle()
            }
            
            if (overlayMedia != null) {
                mediaSession?.player = jinglePlayer
                val mediaItem = MediaItem.Builder()
                    .setMediaId(overlayMedia.url)
                    .setUri(overlayMedia.url)
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(overlayMedia.title).build())
                    .build()
                jinglePlayer.setMediaItem(mediaItem)
                jinglePlayer.volume = 1.0f
                jinglePlayer.prepare()
                jinglePlayer.play()
                if (isSpotTurn && playlistManager.getRandomSpot() != null) tracksSinceLastSpot = 0 else tracksSinceLastSpot++
            } else {
                tracksSinceLastSpot++
            }
            
            serviceScope.launch {
                val step = 0.05f
                var vol = activePlayer.volume
                while (vol > 0f) {
                    vol -= step; if (vol < 0f) vol = 0f
                    activePlayer.volume = vol; delay(120) 
                }
                activePlayer.stop(); activePlayer.clearMediaItems()
                currentMixState = "JINGLE_SOLO" 
            }
        }

        val waitingForNext = currentMixState == "JINGLE_SOLO"
        val jingleAlmostDone = jinglePlayer.isPlaying && remainingJingle in 1..2500L
        val jingleDead = !jinglePlayer.isPlaying 
        
        if (waitingForNext && (jingleAlmostDone || jingleDead)) {
            currentMixState = "FADING_IN"
            val idlePlayer = getIdleUserPlayer()
            val nextTrack = playlistManager.consumeNextMusicTrack(settings)
            
            if (nextTrack != null) {
                activeDeck = if (activeDeck == "A") "B" else "A" 
                val nextActive = getActiveUserPlayer()
                mediaSession?.player = nextActive
                
                val mediaItem = MediaItem.Builder()
                    .setMediaId(nextTrack.url)
                    .setUri(nextTrack.url)
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(nextTrack.title).build())
                    .build()
                nextActive.setMediaItem(mediaItem)
                nextActive.volume = 0f
                nextActive.prepare()
                nextActive.play()
                
                playingPlaylistId = nextTrack.playlistId
                
                serviceScope.launch {
                    val step = 0.05f   
                    var vol = 0f
                    while (vol < 1.0f) {
                        vol += step; if (vol > 1.0f) vol = 1.0f
                        nextActive.volume = vol; delay(100)
                    }
                    isFadingOut = false
                    currentMixState = "PLAYING"
                }
            } else { isFadingOut = false }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mixerJob?.cancel(); newsTimerJob?.cancel(); serviceScope.cancel()
        playerA.release(); playerB.release(); playerSFX.release()
        mediaSession?.release(); mediaSession = null
        instance = null; super.onDestroy()
    }
}
