package com.dokidevs.pholder.slideshow

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.bumptech.glide.load.engine.GlideException
import com.dokidevs.dokilog.d
import com.dokidevs.dokilog.e
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.InsetsConstraintLayout
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.utils.GlideUtil
import com.dokidevs.pholder.utils.setDimensions
import com.github.chrisbanes.photoview.PhotoView
import java.io.File

/*--- SlideshowImageFragment ---*/
class SlideshowImageFragment : SlideshowBaseFragment() {

    /* companion object */
    companion object {

        /* tag */
        const val FRAGMENT_CLASS = "SlideshowImageFragment"

        // newInstance
        fun newInstance(filePath: String): SlideshowImageFragment {
            val fragment = SlideshowImageFragment()
            fragment.arguments = getBaseBundle(filePath)
            return fragment
        }

    }

    /* views */
    private lateinit var main: InsetsConstraintLayout
    private lateinit var bottomGradient: View
    private lateinit var image: PhotoView
    private lateinit var gifTransition: ImageView
    private lateinit var sharedElementImageView: ImageView

    /* parameters */
    private var hasError = false

    // getFragmentClass
    override fun getFragmentClass(): String {
        return FRAGMENT_CLASS
    }

    // onCreateViewAction
    override fun onCreateViewAction(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_slideshow_image, container, false)
    }

    // onViewCreatedAction
    override fun onViewCreatedAction(view: View, savedInstanceState: Bundle?) {
        main = view.findViewById(R.id.slideshowImageFragment_main)
        image = view.findViewById(R.id.slideshowImageFragment_image)
        gifTransition = view.findViewById(R.id.slideshowImageFragment_transition_gif)
        bottomGradient = view.findViewById(R.id.slideshowImageFragment_gradient_bottom)
        setImage()
        super.onViewCreatedAction(view, savedInstanceState)
    }

    // getMainView
    override fun getMainView(): View {
        return main
    }

    // insetsUpdated
    override fun insetsUpdated(insets: WindowInsetsCompat) {
        setBottomGradient(insets.systemWindowInsetBottom)
    }

    // setBottomGradient
    private fun setBottomGradient(bottomInsets: Int) {
        val height = resources.getDimension(R.dimen.slideshowImageFragment_gradient_bottom_height).toInt()
        bottomGradient.setDimensions(height = height + bottomInsets)
    }

    // setImage
    private fun setImage() {
        // Set zoom level
        image.maximumScale = 5.0f
        image.mediumScale = 2.2f
        // Set transition name
        val filePath = getFilePath()
        image.transitionName = filePath
        gifTransition.transitionName = filePath
        sharedElementImageView = image
        image.setOnMatrixChangeListener {
            fragmentListener?.enableSwipe(this, image.scale == 1f)
        }
        image.setOnViewTapListener { _, _, _ ->
            fragmentListener?.onMediaTap(this)
        }
    }

    // prepareEnterTransition
    override fun prepareEnterTransition() {
        // Assign transition resources accordingly
        // For gif, use gifTransition instead because the image will be frozen upon transition and not animate.
        d(getFragmentTag())
        var target: ImageView = image
        if (PholderTagUtil.isGif(getFilePath())) {
            gifTransition.isVisible = true
            sharedElementImageView = gifTransition
            target = gifTransition
        }
        GlideUtil.loadSlideshowImage(target, getFilePath(), false, object : GlideUtil.LoadListener {
            override fun onLoadSuccess(): Boolean {
                fragmentListener?.onFragmentReady(getFragmentTag(), this@SlideshowImageFragment)
                return false
            }

            override fun onLoadFailed(ex: GlideException?, loadPath: String): Boolean {
                e(ex)
                hasError = true
                fragmentListener?.onFragmentReady(getFragmentTag(), this@SlideshowImageFragment)
                return false
            }
        })
    }

    // loadImage
    @SuppressLint("CheckResult")
    override fun loadImage() {
        d(getFragmentTag())
        sharedElementImageView = image
        GlideUtil.loadSlideshowImage(image, getFilePath(), true, object : GlideUtil.LoadListener {
            override fun onLoadSuccess(): Boolean {
                image.post { gifTransition.isVisible = false }
                return false
            }

            override fun onLoadFailed(ex: GlideException?, loadPath: String): Boolean {
                e(ex)
                hasError = true
                image.post { gifTransition.isVisible = false }
                return false
            }
        })
    }

    // postEnterTransition
    override fun postEnterTransition() {
        // Reload media for gif only
        if (PholderTagUtil.isGif(getFilePath())) {
            loadImage()
        }
    }

    // onDragStart
    override fun onDragStart() {
        // Do nothing
    }

    // onDragIdle
    override fun onDragIdle(isCurrentItem: Boolean) {
        if (isCurrentItem && hasError) {
            fragmentListener?.onImageError(this@SlideshowImageFragment, File(getFilePath()).exists())
        }
    }

    // onSelected
    override fun onSelected() {
        // Do nothing
    }

    // onUnselected
    override fun onUnselected() {
        // Do nothing
    }

    // prepareExitTransition
    override fun prepareExitTransition() {
        // Do nothing
    }

    // showControlLayout
    override fun showControlLayout(show: Boolean) {
        bottomGradient.isVisible = show
    }

    // getSharedElementImageView
    override fun getTransitionViews(): List<View> {
        return listOf(sharedElementImageView)
    }

    // getControlLayoutViews
    override fun getControlLayoutViews(): List<View> {
        return listOf(bottomGradient)
    }

    // onDestroyViewAction
    override fun onDestroyViewAction() {
        // Clear listeners, there is a bug with viewpager when swiped too quickly, some touch is intercepted by random views.
        // Here, swiping too quickly may randomly trigger onMatrixChangeListener of already detached fragment, where
        // getSlideshowActivity will return null. This caused the implementation of null check currently.
        // See https://stackoverflow.com/a/29217727/3584439
        image.setOnMatrixChangeListener(null)
        image.setOnViewTapListener(null)
    }

    // onBackPressed
    override fun onBackPressed(): Boolean {
        // For zoomed in image, scale back to fit screen
        if (image.scale != 1f) {
            image.setScale(1f, true)
            return true
        }
        return false
    }

}