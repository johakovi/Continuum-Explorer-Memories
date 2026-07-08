package com.troikoss.continuum_explorer.managers

import android.content.Context
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.troikoss.continuum_explorer.providers.ProviderDataSource
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.utils.getUriForUniversalFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AudioManager {
    private var exoPlayer: ExoPlayer? = null
    
    var currentTrack by mutableStateOf<UniversalFile?>(null)
    var isAudioPlaying by mutableStateOf(false)
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var currentIndex by mutableIntStateOf(-1)
    
    private var progressJob: Job? = null
    val playlist = mutableStateListOf<UniversalFile>()

    fun init(context: Context) {
        if (exoPlayer == null) {
            val mediaSourceFactory = DefaultMediaSourceFactory(context)
            
            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        isAudioPlaying = isPlayingNow
                        if (isPlayingNow) {
                            startProgressUpdate()
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
                })
            }
        }
    }

    fun play(context: Context, file: UniversalFile, siblings: List<UniversalFile> = emptyList()) {
        init(context)
        
        playlist.clear()
        playlist.addAll(siblings.ifEmpty { listOf(file) })
        
        // Find the index of the clicked file. 
        // We match by name AND parentId/path to be as specific as possible.
        var index = playlist.indexOfFirst { 
            it.providerId == file.providerId || 
            (it.name == file.name && it.parentId == file.parentId) 
        }
        
        if (index == -1) index = 0
        this.currentIndex = index
        
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            
            val mediaItems = playlist.map { track ->
                MediaItem.Builder()
                    .setUri(getUriForUniversalFile(context, track))
                    .setMediaId(track.providerId)
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

    fun playNext() {
        if (exoPlayer?.hasNextMediaItem() == true) {
            exoPlayer?.seekToNext()
        }
    }

    fun playPrevious() {
        if (exoPlayer?.hasPreviousMediaItem() == true) {
            exoPlayer?.seekToPrevious()
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
        isAudioPlaying = false
        currentPosition = 0L
        duration = 0L
        playlist.clear()
        currentIndex = -1
        stopProgressUpdate()
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
        exoPlayer?.release()
        exoPlayer = null
    }
}
