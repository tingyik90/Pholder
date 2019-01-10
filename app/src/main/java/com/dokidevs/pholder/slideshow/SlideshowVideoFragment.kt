package com.dokidevs.pholder.slideshow

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.bumptech.glide.load.engine.GlideException
import com.dokidevs.dokilog.d
import com.dokidevs.dokilog.e
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.InsetsConstraintLayout
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.utils.GlideUtil
import com.dokidevs.pholder.utils.setDimensions
import com.dokidevs.pholder.utils.setMargins
import com.sprylab.android.widget.TextureVideoView
import org.jetbrains.anko.doAsync

/*--- SlideshowVideoFragment ---*/
class SlideshowVideoFragment : SlideshowBaseFragment(), View.OnClickListener {

    /* companion object */
    companion object {

        /* tag */
        const val FRAGMENT_CLASS = "SlideshowVideoFragment"

        /* intent */
        private const val VIDEO_DURATION = "VIDEO_DURATION"

        /* saved instance state */
        private const val SAVED_IS_PLAYING = "SAVED_IS_PLAYING"
        private const val SAVED_IS_FINISHED = "SAVED_IS_FINISHED"
        private const val SAVED_SAVED_POSITION = "SAVED_SAVED_POSITION"

        /* durations */
        private const val SEEKBAR_UPDATE_DURATION = 50L
        private const val HIDE_CONTROL_LAYOUT_DELAY = 3000L
        private const val HIDE_CONTROL_LAYOUT_MAX_DURATION = 4000L

        // newInstance
        fun newInstance(filePath: String, videoDuration: Int): SlideshowVideoFragment {
            val fragment = SlideshowVideoFragment()
            val bundle = getBaseBundle(filePath)
            bundle.putInt(VIDEO_DURATION, videoDuration)
            fragment.arguments = bundle
            return fragment
        }

    }

    /* views */
    private lateinit var main: InsetsConstraintLayout
    private lateinit var video: TextureVideoView
    private lateinit var image: ImageView
    private lateinit var playPause: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var seekBarLayout: ConstraintLayout
    private lateinit var time: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var duration: TextView
    private lateinit var bottomGradient: View

    /* parameters */
    private val seekBarHandler = Handler()
    private val seekBarRunnable = Runnable { updateSeekBarRepeat() }
    private val hideControlLayoutHandler = Handler()
    private val hideControlLayoutRunnable = Runnable { fragmentListener?.onVideoHideControlLayout(this) }
    var isPrepared = false
        private set
    private var isPlaying = false
    private var isFinished = false
    private var savedPosition = 1
    private var onSeekBarChangeListener: SeekBar.OnSeekBarChangeListener? = null

    // getFragmentClass
    override fun getFragmentClass(): String {
        return FRAGMENT_CLASS
    }

    // getVideoDuration
    private fun getVideoDuration(): Int {
        return arguments?.getInt(VIDEO_DURATION) ?: 0
    }

    // onCreateAction
    override fun onCreateAction(savedInstanceState: Bundle?) {
        // When resuming from rotation
        if (savedInstanceState != null) {
            isPlaying = savedInstanceState.getBoolean(SAVED_IS_PLAYING, isPlaying)
            isFinished = savedInstanceState.getBoolean(SAVED_IS_FINISHED, isFinished)
            savedPosition = savedInstanceState.getInt(SAVED_SAVED_POSITION, 1)
        }
    }

    // onCreateViewAction
    override fun onCreateViewAction(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_slideshow_video, container, false)
    }

    // onViewCreatedAction
    override fun onViewCreatedAction(view: View, savedInstanceState: Bundle?) {
        main = view.findViewById(R.id.slideshowVideoFragment_main)
        video = view.findViewById(R.id.slideshowVideoFragment_video)
        image = view.findViewById(R.id.slideshowVideoFragment_image)
        playPause = view.findViewById(R.id.slideshowVideoFragment_playPause)
        progressBar = view.findViewById(R.id.slideshowVideoFragment_progressBar)
        seekBarLayout = view.findViewById(R.id.slideshowVideoFragment_seekBarLayout)
        time = view.findViewById(R.id.slideshowVideoFragment_time)
        seekBar = view.findViewById(R.id.slideshowVideoFragment_seekBar)
        duration = view.findViewById(R.id.slideshowVideoFragment_duration)
        bottomGradient = view.findViewById(R.id.slideshowVideoFragment_gradient_bottom)
        main.setOnClickListener(this)
        // Set duration first based on information we have in fileTag
        setDuration(getVideoDuration())
        setImage()
        setVideo()
        super.onViewCreatedAction(view, savedInstanceState)
    }

    // getMainView
    override fun getMainView(): View {
        return main
    }

    // insetsUpdated
    override fun insetsUpdated(insets: WindowInsetsCompat) {
        setSeekBarLayout(insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        setBottomGradient(insets.systemWindowInsetBottom)
    }

    // setSeekBarLayout
    private fun setSeekBarLayout(insetRight: Int, insetBottom: Int) {
        val bottomPadding = resources.getDimension(R.dimen.seekControl_bottom_margin).toInt()
        seekBarLayout.setMargins(
            right = insetRight,
            bottom = bottomPadding + insetBottom
        )
    }

    // setBottomGradient
    private fun setBottomGradient(insetBottom: Int) {
        val height = resources.getDimension(R.dimen.slideshowVideoFragment_gradient_bottom_height).toInt()
        bottomGradient.setDimensions(height = height + insetBottom)
    }

    // onClick
    override fun onClick(v: View?) {
        fragmentListener?.onMediaTap(this)
    }

    // setImage
    private fun setImage() {
        image.transitionName = getFilePath()
    }

    // setVideo
    private fun setVideo() {
        video.setOnClickListener(this)
    }

    // setDuration
    private fun setDuration(videoDuration: Int) {
        seekBar.max = videoDuration
        duration.text = PholderTagUtil.videoMillisToDuration(videoDuration)
    }

    // setPlayPause
    private fun setPlayPause() {
        // This is only called once video is prepared
        progressBar.isVisible = false
        playPause.isVisible = true
        playPause.setOnClickListener {
            if (isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    // prepareEnterTransition
    override fun prepareEnterTransition() {
        isPlaying = true
        loadImage()
    }

    // loadImage
    override fun loadImage() {
        GlideUtil.loadSlideshowVideo(image, getFilePath(), object : GlideUtil.LoadListener {
            override fun onLoadSuccess(): Boolean {
                fragmentListener?.onFragmentReady(getFragmentTag(), this@SlideshowVideoFragment)
                return false
            }

            override fun onLoadFailed(ex: GlideException?, loadPath: String): Boolean {
                fragmentListener?.onFragmentReady(getFragmentTag(), this@SlideshowVideoFragment)
                return false
            }
        })
    }

    // postEnterTransition
    override fun postEnterTransition() {
        loadVideo()
    }

    // loadVideo
    fun loadVideo() {
        d(getFragmentTag())
        if (!isPrepared) {
            video.setOnPreparedListener { mediaPlayer ->
                onPrepared(mediaPlayer)
            }
            video.setOnCompletionListener {
                onCompletion()
            }
            video.setOnErrorListener { _, _, _ ->
                onError()
                // Return true to notify error is handled and stop onCompletionListener from being called
                true
            }
            video.setVideoPath(getFilePath())
            video.requestFocus()
        }
    }

    // onPrepared
    private fun onPrepared(mediaPlayer: MediaPlayer) {
        d(getFragmentTag())
        // Upon video.start(), there can be a 200ms delay before video first frame is actually rendered by
        // videoView. If we hide the image too early, there will be a flicker since videoView is still black.
        // This flag is used instead to hide image. See https://stackoverflow.com/a/26625164/3584439
        mediaPlayer.setOnInfoListener { _, what, _ ->
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                d(getFragmentTag())
                // When coming in, frame at time = 0 was used so that the video will look continuous.
                // Once the video starts playing, hide the transition image and load a return image for it
                image.post {
                    image.alpha = 0f
                    GlideUtil.loadSlideshowVideoReturn(image, getFilePath())
                }
                true
            } else {
                // For other cases, allow mediaPlayer to handle the info
                false
            }
        }
        isPrepared = true
        // Set duration again based on videoView loaded information
        setDuration(video.duration)
        // Set playPause now so that user can interact with it
        setPlayPause()
        setOnSeekBarChangeListener()
        // Check to make sure savedPosition within bound
        if (savedPosition > video.duration) {
            savedPosition = video.duration
        }
        if (savedPosition == 0) {
            savedPosition = 1
        }
        try {
            video.seekTo(savedPosition)
        } catch (ex: Exception) {
            e(getFragmentTag(), ex)
        }
        if (isPlaying) {
            play()
        } else {
            if (isFinished) {
                // Show that video has ended
                updateSeekBar(video.duration)
            }
        }
    }

    // onCompletion
    private fun onCompletion() {
        d(getFragmentTag())
        isFinished = true
        pause()
        updateSeekBar(video.duration)
        fragmentListener?.onVideoFinish(this)
    }

    // onError
    private fun onError() {
        d(getFragmentTag())
        playPause.setImageResource(R.drawable.ic_clear_white_24dp)
        progressBar.isVisible = false
        playPause.isVisible = true
        fragmentListener?.onVideoError(this)
    }

    // onStartAction
    override fun onStartAction() {
        // This is for when resuming from onStop
        if (isPrepared) {
            setOnSeekBarChangeListener()
        }
    }

    // setOnSeekBarChangeListener
    private fun setOnSeekBarChangeListener() {
        if (onSeekBarChangeListener == null) {
            onSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser && isPrepared) {
                        try {
                            video.seekTo(progress)
                        } catch (ex: Exception) {
                            e(getFragmentTag(), ex)
                        }
                        time.text = PholderTagUtil.videoMillisToDuration(progress)
                        isFinished = progress >= video.duration
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    d(getFragmentTag())
                    internalPause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    d(getFragmentTag())
                    if (isPlaying) {
                        play()
                    }
                }
            }
        }
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)
    }

    // updateSeekBar
    private fun updateSeekBar(position: Int) {
        var currentPosition = position
        // Sometimes, video.currentPosition skip backwards in the beginning of video.
        // Avoid this when playing, but only for small interval of 200ms.
        // Anything bigger can be due to actual frame search after user used seekBar.
        if (isPlaying && seekBar.progress > position && Math.abs(seekBar.progress - position) < 200) {
            currentPosition = seekBar.progress
        }
        // savedPosition can be more than duration due to frame search, adjust to show on seekBar correctly
        if (position > video.duration) {
            currentPosition = video.duration
        }
        seekBar.progress = currentPosition
        time.text = PholderTagUtil.videoMillisToDuration(currentPosition)
    }

    // updateSeekBarAuto
    private fun updateSeekBarRepeat() {
        updateSeekBar(video.currentPosition)
        seekBarHandler.postDelayed(seekBarRunnable, SEEKBAR_UPDATE_DURATION)
    }

    // getSeekBarLayout
    fun getSeekBarLayout(): View {
        return seekBarLayout
    }

    // onDragStart
    override fun onDragStart() {
        pause()
    }

    // onDragIdle
    override fun onDragIdle(isCurrentItem: Boolean) {
        // Only load video once drag is idle to make scrolling faster
        if (isCurrentItem) {
            loadVideo()
        }
    }

    // onSelected
    override fun onSelected() {
        // Once user let go of scrolling, start the progressBar
        if (!isPrepared) {
            playPause.isVisible = false
            progressBar.isVisible = true
        }
    }

    // onUnselected
    override fun onUnselected() {
        // When user let go of scrolling, hide the progressBar
        progressBar.isVisible = false
        playPause.isVisible = true
        reset()
    }

    // play
    private fun play() {
        d(getFragmentTag())
        if (isPrepared) {
            if (isFinished) {
                reset()
                isFinished = false
            }
            isPlaying = true
            playPause.setImageResource(R.drawable.ic_pause_white_48dp)
            if (playPause.isVisible) {
                if (video.duration - video.currentPosition > HIDE_CONTROL_LAYOUT_MAX_DURATION) {
                    hideControlLayoutHandler.postDelayed(hideControlLayoutRunnable, HIDE_CONTROL_LAYOUT_DELAY)
                }
            }
            try {
                video.start()
                updateSeekBarRepeat()
                fragmentListener?.onVideoPlay(this)
            } catch (ex: Exception) {
                e(ex)
                onError()
            }
        }
    }

    // internalPause
    private fun internalPause() {
        try {
            // MediaPlayer will throw IllegalStateException sometimes due to state not ready
            video.pause()
        } catch (ex: Exception) {
            e(ex)
        }
        seekBarHandler.removeCallbacks(seekBarRunnable)
        hideControlLayoutHandler.removeCallbacks(hideControlLayoutRunnable)
        playPause.setImageResource(R.drawable.ic_play_white_48dp)
    }

    // pause
    fun pause() {
        d(getFragmentTag())
        if (isPrepared) {
            if (isPlaying) {
                isPlaying = false
                fragmentListener?.onVideoPause(this)
            }
            internalPause()
            // Show the control depending on original condition, this will show the play and pause icon as required
            showControlLayout(playPause.isVisible)
        }
    }

    // reset
    private fun reset() {
        d(getFragmentTag())
        if (isPrepared) {
            pause()
            isFinished = false
            try {
                video.seekTo(1)
                updateSeekBar(1)
            } catch (ex: Exception) {
                e(getFragmentTag(), ex)
            }
        }
    }

    // release()
    private fun release() {
        d(getFragmentTag())
        if (isPrepared) {
            isPrepared = false
            doAsync {
                try {
                    video.stopPlayback()
                } catch (ex: IllegalStateException) {
                    e(ex)
                } catch (ex: Exception) {
                    e(ex)
                }
            }
        }
    }

    // onSaveInstanceState
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_IS_PLAYING, isPlaying)
        outState.putBoolean(SAVED_IS_FINISHED, isFinished)
        try {
            val currentPosition = video.currentPosition
            outState.putInt(SAVED_SAVED_POSITION, currentPosition)
        } catch (ex: Exception) {
            e(getFragmentTag(), ex)
        }
    }

    // onStopAction
    override fun onStopAction() {
        pause()
        seekBar.setOnSeekBarChangeListener(null)
    }

    // prepareExitTransition
    override fun prepareExitTransition() {
        pause()
        // Hide video and show image
        video.isVisible = false
        image.alpha = 1f
        seekBarHandler.removeCallbacks(seekBarRunnable)
        playPause.setImageResource(R.drawable.ic_play_white_48dp)
        // Memory of mediaPlayer and surfaceView needs to be released. If stopPlayback() is not called,
        // Android should somehow clear them in OnDestroy and this will cause GalleryActivity to be laggy with frame drops.
        // Therefore, release them before transition back and do this via async to concurrently begin transition.
        release()
    }

    // onDestroyAction
    override fun onDestroyAction() {
        // Release memory when out of viewPager
        release()
    }

    // showControlLayout
    override fun showControlLayout(show: Boolean) {
        hideControlLayoutHandler.removeCallbacks(hideControlLayoutRunnable)
        seekBarLayout.isVisible = show
        bottomGradient.isVisible = show
        playPause.isGone = !show && isPlaying
    }

    // getSharedElementImageView
    override fun getTransitionViews(): List<View> {
        return listOf(image, playPause)
    }

    // getControlLayoutViews
    override fun getControlLayoutViews(): List<View> {
        return listOf(bottomGradient, time, seekBar, duration)
    }

}