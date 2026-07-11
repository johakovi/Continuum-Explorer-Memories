package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.utils.getUriForUniversalFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

object AudioManager {
    private var exoPlayer: ExoPlayer? = null
    private var appContext: Context? = null

    fun getExoPlayer(context: Context): ExoPlayer {
        init(context)
        return exoPlayer!!
    }
    
    var currentTrack by mutableStateOf<UniversalFile?>(null)
    var currentTitle by mutableStateOf<String?>(null)
    var currentArtist by mutableStateOf<String?>(null)
    var isAudioPlaying by mutableStateOf(false)
    var isShuffleEnabled by mutableStateOf(false)
    var isRepeatEnabled by mutableStateOf(false)
    var isMinimized by mutableStateOf(false)
    var isExpanded by mutableStateOf(false)
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var currentIndex by mutableIntStateOf(-1)
    
    private var progressJob: Job? = null
    val playlist = mutableStateListOf<UniversalFile>()

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        MusicMetadataManager.init(applicationContext)
        if (exoPlayer == null) {
            val mediaSourceFactory = DefaultMediaSourceFactory(applicationContext)
            
            exoPlayer = ExoPlayer.Builder(applicationContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                shuffleModeEnabled = isShuffleEnabled
                repeatMode = if (isRepeatEnabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        isAudioPlaying = isPlayingNow
                        if (isPlayingNow) {
                            startProgressUpdate()
                            context.applicationContext.startService(Intent(context.applicationContext, com.troikoss.continuum_explorer.services.PlaybackService::class.java))
                        } else {
                            stopProgressUpdate()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            this@AudioManager.duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                        } else if (state == Player.STATE_ENDED) {
                            playNext()
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        currentIndex = exoPlayer?.currentMediaItemIndex ?: -1
                        if (currentIndex != -1 && currentIndex < playlist.size) {
                            currentTrack = playlist[currentIndex]
                        }
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        currentTitle = mediaMetadata.title?.toString()
                        currentArtist = mediaMetadata.artist?.toString()
                    }
                })
            }
        }
    }

    fun play(context: Context, file: UniversalFile, siblings: List<UniversalFile> = emptyList()) {
        init(context)
        isMinimized = false

        // If we are playing a file from the CURRENT playlist, don't clear it.
        // Otherwise, siblings is a new list from the file explorer.
        if (siblings !== playlist) {
            playlist.clear()
            playlist.addAll(siblings.ifEmpty { listOf(file) })
        }
        
        // Find the index of the clicked file. 
        // We match by name AND parentId/path to be as specific as possible.
        var index = playlist.indexOfFirst { 
            it.providerId == file.providerId || 
            (it.name == file.name && it.parentId == file.parentId) 
        }
        
        if (index == -1) index = 0
        this.currentIndex = index
        
        context.applicationContext.startService(Intent(context.applicationContext, com.troikoss.continuum_explorer.services.PlaybackService::class.java))

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            
            val mediaItems = playlist.map { track ->
                val songMetadata = MusicMetadataManager.getSongMetadata(track.providerId)
                val metadata = MediaMetadata.Builder()
                    .setTitle(songMetadata?.title ?: track.name)
                    .setDisplayTitle(songMetadata?.title ?: track.name)
                    .setArtist(songMetadata?.artist)
                    .setAlbumTitle(songMetadata?.album)
                    .setArtworkUri(songMetadata?.album?.let { albumName ->
                        MusicMetadataManager.getCoverPath(albumName, context)?.let { path ->
                            Uri.fromFile(File(path))
                        }
                    })
                    .build()

                MediaItem.Builder()
                    .setUri(getUriForUniversalFile(context, track))
                    .setMediaId(track.providerId)
                    .setMediaMetadata(metadata)
                    .setTag(track)
                    .build()
            }
            
            player.setMediaItems(mediaItems, index, 0L)
            player.prepare()
            player.play()
            currentTrack = playlist[index]
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        exoPlayer?.shuffleModeEnabled = isShuffleEnabled
    }

    fun toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled
        exoPlayer?.repeatMode = if (isRepeatEnabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    fun playNext() {
        exoPlayer?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNext()
            } else if (playlist.isNotEmpty()) {
                // If we're at the end and repeat is off, manually wrap around for the button click
                it.seekToDefaultPosition(0)
            }
        }
    }

    fun playPrevious() {
        exoPlayer?.let {
            if (it.hasPreviousMediaItem()) {
                it.seekToPrevious()
            } else if (playlist.isNotEmpty()) {
                it.seekToDefaultPosition(playlist.size - 1)
            }
        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        currentPosition = position
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        currentTrack = null
        currentTitle = null
        currentArtist = null
        isAudioPlaying = false
        currentPosition = 0L
        duration = 0L
        playlist.clear()
        currentIndex = -1
        stopProgressUpdate()
        appContext?.let {
            it.stopService(Intent(it, com.troikoss.continuum_explorer.services.PlaybackService::class.java))
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isAudioPlaying) {
                currentPosition = exoPlayer?.currentPosition ?: 0L
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressUpdate()
        appContext?.let {
            it.stopService(Intent(it, com.troikoss.continuum_explorer.services.PlaybackService::class.java))
        }
        exoPlayer?.release()
        exoPlayer = null
    }
}
