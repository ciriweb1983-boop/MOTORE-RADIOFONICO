package com.example.webradioapp.playlist

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.webradioapp.SettingsManager
import java.io.File

data class Track(
    val id: String,
    val title: String, 
    val url: String, 
    val playlistId: Int 
)

enum class PlayMode {
    PLAYLIST_1, PLAYLIST_2, PLAYLIST_3, PLAYLIST_4, PLAYLIST_5, PLAYLIST_6, RANDOM_1_5
}

class PlaylistManager {
    private val playlists = mutableMapOf<Int, List<Track>>()
    private val playedHistory = mutableMapOf<String, Long>()
    private val manualQueue = mutableListOf<Track>()

    var currentMode: PlayMode = PlayMode.PLAYLIST_1

    var upcomingTrack: Track? = null
        private set

    companion object {
        const val FIVE_DAYS_MS = 5L * 24 * 60 * 60 * 1000 
    }

    fun loadHistory(settingsManager: SettingsManager) {
        playedHistory.clear()
        playedHistory.putAll(settingsManager.playedHistory)
        val now = System.currentTimeMillis()
        val filtered = playedHistory.filterValues { time -> (now - time) < FIVE_DAYS_MS }
        if (filtered.size != playedHistory.size) {
            playedHistory.clear()
            playedHistory.putAll(filtered)
            settingsManager.playedHistory = playedHistory
        }
    }

    fun scanLocalFolders(context: Context, settingsManager: SettingsManager) {
        playlists.clear()
        playlists[1] = queryMediaStoreForFolder(context, settingsManager.getFolderName("music1", "Music1"), 1)
        playlists[2] = queryMediaStoreForFolder(context, settingsManager.getFolderName("music2", "Music2"), 2)
        playlists[3] = queryMediaStoreForFolder(context, settingsManager.getFolderName("music3", "Music3"), 3)
        playlists[4] = queryMediaStoreForFolder(context, settingsManager.getFolderName("music4", "Music4"), 4)
        playlists[5] = queryMediaStoreForFolder(context, settingsManager.getFolderName("music5", "Music5"), 5)
        playlists[6] = queryMediaStoreForFolder(context, settingsManager.getFolderName("music6", "Music6"), 6)
        playlists[0] = queryMediaStoreForFolder(context, settingsManager.getFolderName("jingles", "Jingles"), 0) 
        // Cartella dedicata agli Spot/Commercials
        playlists[7] = queryMediaStoreForFolder(context, settingsManager.getFolderName("spot", "Spot"), 7)
    }

    private fun queryMediaStoreForFolder(context: Context, folderName: String, playlistId: Int): List<Track> {
        val trackList = mutableListOf<Track>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE)
        val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/$folderName/%")
        
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn)
                    val title = cursor.getString(titleColumn) ?: File(path).nameWithoutExtension
                    trackList.add(Track(id = path, title = title, url = path, playlistId = playlistId))
                }
            }
        } catch (e: Exception) { Log.e("PlaylistManager", "Errore: $folderName", e) }
        return trackList
    }

    fun addToQueue(track: Track) {
        manualQueue.add(track)
    }

    fun addNextTrack(track: Track) {
        manualQueue.add(0, track)
    }

    fun getQueue(): List<Track> = manualQueue.toList()

    fun getRandomJingle(): Track? {
        val jingles = playlists[0] ?: emptyList()
        return jingles.randomOrNull()
    }

    fun getRandomSpot(): Track? {
        val spots = playlists[7] ?: emptyList()
        return spots.randomOrNull()
    }

    fun rollNextTrack(settingsManager: SettingsManager) {
        if (manualQueue.isNotEmpty()) {
            upcomingTrack = manualQueue.first()
            return
        }
        val allTracksInMode = getTracksForCurrentMode()
        val now = System.currentTimeMillis()
        
        val availableTracks = allTracksInMode.filter { track ->
            val lastPlayed = playedHistory[track.id] ?: 0L
            (now - lastPlayed) >= FIVE_DAYS_MS
        }
        
        upcomingTrack = if (availableTracks.isEmpty()) {
            allTracksInMode.minByOrNull { playedHistory[it.id] ?: 0L }
        } else {
            availableTracks.randomOrNull()
        }
    }

    fun consumeNextMusicTrack(settingsManager: SettingsManager): Track? {
        if (upcomingTrack == null) rollNextTrack(settingsManager)
        val track = upcomingTrack
        
        if (track != null) {
            if (manualQueue.isNotEmpty() && manualQueue.first() == track) {
                manualQueue.removeAt(0)
            } else {
                playedHistory[track.id] = System.currentTimeMillis()
                settingsManager.playedHistory = playedHistory
            }
        }
        
        rollNextTrack(settingsManager) 
        return track
    }

    private fun getTracksForCurrentMode(): List<Track> {
        return when (currentMode) {
            PlayMode.PLAYLIST_1 -> playlists[1] ?: emptyList()
            PlayMode.PLAYLIST_2 -> playlists[2] ?: emptyList()
            PlayMode.PLAYLIST_3 -> playlists[3] ?: emptyList()
            PlayMode.PLAYLIST_4 -> playlists[4] ?: emptyList()
            PlayMode.PLAYLIST_5 -> playlists[5] ?: emptyList()
            PlayMode.PLAYLIST_6 -> playlists[6] ?: emptyList()
            PlayMode.RANDOM_1_5 -> {
                val combined = mutableListOf<Track>()
                for (i in 1..5) combined.addAll(playlists[i] ?: emptyList())
                combined
            }
        }
    }
}
