package com.machina.playtify.view

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.machina.playtify.R
import com.machina.playtify.databinding.ActivityMainBinding
import com.machina.playtify.player.isPlayEnabled
import com.machina.playtify.player.isPlaying
import com.machina.playtify.player.toSong
import com.machina.playtify.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var viewModel: HomeViewModel
    private lateinit var navController: NavController

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupView()
        setupObserver()


        binding.currentTrackBottomBar.setOnClickListener {
            val action = HomeFragmentDirections.globalActionToCurrentTrackFragment()
            navController.navigate(action)
        }

        binding.currentTrackPlaybackControl.setOnClickListener {
            viewModel.currentPlayingSong.value?.toSong()?.let { song ->
                viewModel.playOrToggleSong(song, true)
            }
        }
    }

    private fun setupView() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.homeFragment -> showBottomBar()
                else -> hideBottomBar()
            }
        }
    }

    private fun setupObserver() {
        viewModel.playbackState.observe(this) { state ->
            if (state?.isPlaying == true) {
                binding.currentTrackPlaybackControl.setImageResource(R.drawable.ic_baseline_pause_24)
            } else if (state?.isPlayEnabled == true) {
                binding.currentTrackPlaybackControl.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            }
        }

        viewModel.currentPlayingSong.observe(this) { currentSong ->
            if (currentSong != null) {
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    Glide.with(this).asBitmap()
                        .load(currentSong.description.iconUri)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onLoadFailed(errorDrawable: Drawable?) {  }

                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                Timber.d("Glide load for palette completed")
                                binding.currentTrackBottomBarConstraintLayout
                                    .loadBitMapAsDarkMutedBackground(resource)
                                binding.currentTrackImage.setImageBitmap(resource)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {  }
                        })
                    binding.currentTrackBottomBar.isVisible = true
                    binding.currentTrackTitle.text = currentSong.description?.title.toString()
                    binding.currentTrackArtist.text = currentSong.description?.subtitle.toString()
                    val maxProgress = currentSong.getLong(METADATA_KEY_DURATION).toInt()
                    binding.currentTrackLinearProgress.max = maxProgress
                } else {
                    binding.currentTrackBottomBar.isVisible = false
                }

            } else {
                binding.currentTrackBottomBar.isVisible = true
            }
        }

        viewModel.currentPlayerPosition.observe(this) { position ->
//            Timber.d("CurrentPosition $position")
            if (navController.currentDestination?.id == R.id.homeFragment) {
                binding.currentTrackLinearProgress.progress = position.toInt()
            }
        }
    }

    private fun showBottomBar() {
        binding.currentTrackBottomBar.isVisible = true
    }

    private fun hideBottomBar() {
        binding.currentTrackBottomBar.isVisible = false
    }
}