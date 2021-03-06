package com.machina.playtify.viewmodels

import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machina.playtify.core.Constants.MY_MEDIA_ROOT_ID
import com.machina.playtify.core.Constants.POSITION_UPDATE_INTERVAL_MILLIS
import com.machina.playtify.model.Resource
import com.machina.playtify.model.Song
import com.machina.playtify.player.*
import com.machina.playtify.player.MediaPlaybackService.Companion.currentSongDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
): ViewModel() {

    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> =  _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val currentPlayingSong = musicServiceConnection.currentPlayingSong
    val currentQueue = musicServiceConnection.currentQueue
    val playbackState = musicServiceConnection.playbackState
    val shuffleMode = musicServiceConnection.shuffleMode
    val repeatMode = musicServiceConnection.repeatMode

    private val _currentPlayerPosition = MutableLiveData<Long>()
    val currentPlayerPosition: LiveData<Long> = _currentPlayerPosition

    private val _currentSongDuration = MutableLiveData<Long>()
    val currentSongDuration: LiveData<Long> = _currentSongDuration

    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(
            MY_MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: MutableList<MediaBrowserCompat.MediaItem>
                ) {
                    super.onChildrenLoaded(parentId, children)
                    val items = children.map {
                        Song(
                            it.mediaId.toString(),
                            it.description.title.toString(),
                            it.description.subtitle.toString(),
                            it.description.iconUri.toString(),
                            it.description.mediaUri.toString()
                        )
                    }
                    _mediaItems.postValue(Resource.success(items))
                }
            }
        )
        checkPlaybackPosition()
    }



    private fun checkPlaybackPosition() {
        viewModelScope.launch {
            while (true) {
                val position = playbackState.value?.currentPlayPosition ?: 0L
//                Timber.d("Current Playback Position $position")
//                Timber.d("Current Playback Duration ${MediaPlaybackService.currentSongDuration}")
                if (currentPlayerPosition.value != position) {
                    _currentPlayerPosition.postValue(position)
                    _currentSongDuration.postValue(MediaPlaybackService.currentSongDuration)
                }

                delay(POSITION_UPDATE_INTERVAL_MILLIS)
            }
        }
    }


    fun shuffleMode(toggle: Boolean = false) {
        with(musicServiceConnection.transportControls) {
            if (toggle) {
                setShuffleMode(SHUFFLE_MODE_ALL)
            } else {
                setShuffleMode(SHUFFLE_MODE_NONE)
            }
        }
    }

    fun repeatMode(toggle: Boolean = false) {
        with(musicServiceConnection.transportControls) {
            if (toggle) {
                setRepeatMode(REPEAT_MODE_ALL)
            }
            else {
                setRepeatMode(REPEAT_MODE_NONE)
            }
        }
    }

    fun skipToNextSong() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPrevious() {
        val currentPos = playbackState.value?.currentPlayPosition ?: 0
        Timber.d("Current Playback Position $currentPos")
        if (currentPos < 10000) {
            musicServiceConnection.transportControls.skipToPrevious()
        } else {
            musicServiceConnection.transportControls.seekTo(0)
        }
    }

    fun seekTo(pos: Long) {
        musicServiceConnection.transportControls.seekTo(pos)
    }

    fun playOrToggleSong(mediaItem: Song, toggle: Boolean = false) {
        val isPrepared = playbackState.value?.isPrepared ?: false

        if (isPrepared && mediaItem.id ==
            currentPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)
        ) {
            playbackState.value?.let { state ->
                when {
                    state.isPlaying -> if (toggle) musicServiceConnection.transportControls.pause()
                    state.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else -> Unit
                }
            }
        } else {
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.id, null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(
            MY_MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {  }
        )
    }

}