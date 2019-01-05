package com.dokidevs.pholder.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ImageView
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.dokidevs.pholder.PholderApplication.Companion.animateGif
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.FileTag
import com.dokidevs.pholder.data.PholderTagUtil

/*--- GlideAppModule ---*/
@GlideModule
class GlideAppModule : AppGlideModule() {
    // empty class for GlideApp generated API
}


/*--- GlideUtil ---*/
class GlideUtil {

    /*--- LoadListener ---*/
    interface LoadListener {

        fun onLoadSuccess(): Boolean

        fun onLoadFailed(ex: GlideException?, loadPath: String): Boolean

    }

    /* companion object */
    companion object {

        // loadFolderLayout
        fun loadFolderLayout(
            imageView: ImageView,
            thumbnailPath: String,
            loadListener: LoadListener? = null
        ) {
            load(
                imageView,
                thumbnailPath,
                R.drawable.ic_folder_white_48dp,
                false,
                loadListener,
                false,
                CenterCrop(),
                RoundedCorners(
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8.0f,
                        imageView.resources.displayMetrics
                    ).toInt()
                )
            )
        }

        // loadFileLayout
        fun loadFileLayout(
            imageView: ImageView,
            fileTag: FileTag,
            loadListener: LoadListener? = null
        ) {
            load(
                imageView,
                fileTag.getGlideLoadPath(),
                R.drawable.ic_broken_image_white_48dp,
                animateGif,
                loadListener
            )
        }

        // loadSlideshowImage
        fun loadSlideshowImage(
            imageView: ImageView,
            loadPath: String,
            animateGif: Boolean,
            loadListener: LoadListener? = null
        ) {
            load(
                imageView,
                loadPath,
                R.drawable.ic_broken_image_white_48dp,
                animateGif,
                loadListener
            )
        }

        // loadSlideshowVideo
        fun loadSlideshowVideo(
            imageView: ImageView,
            loadPath: String,
            loadListener: LoadListener? = null
        ) {
            load(
                imageView,
                loadPath,
                R.drawable.ic_broken_image_white_48dp,
                false,
                loadListener,
                true
            )
        }

        // loadSlideshowVideoReturn
        fun loadSlideshowVideoReturn(
            imageView: ImageView,
            loadPath: String,
            loadListener: LoadListener? = null
        ) {
            load(
                imageView,
                loadPath,
                R.drawable.ic_broken_image_white_48dp,
                false,
                loadListener,
                false
            )
        }

        // loadImage
        @SuppressLint("CheckResult")
        private fun load(
            imageView: ImageView,
            loadPath: String,
            errorResId: Int = 0,
            animateGif: Boolean = true,
            loadListener: LoadListener? = null,
            loadVideoFirstFrame: Boolean = false,
            vararg transformation: Transformation<Bitmap>
        ) {
            val context = imageView.context
            if (context != null) {
                val glideRequest = GlideApp.with(context)
                    .load(loadPath)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .error(errorResId)
                if (PholderTagUtil.isGif(loadPath)) {
                    if (animateGif) {
                        glideRequest.signature(ObjectKey("animateGif=true"))
                    } else {
                        glideRequest.signature(ObjectKey("animateGif=false")).dontAnimate()
                    }
                }
                if (loadListener != null) {
                    glideRequest.listener(object : RequestListener<Drawable> {
                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return loadListener.onLoadSuccess()
                        }

                        override fun onLoadFailed(
                            ex: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return loadListener.onLoadFailed(ex, loadPath)
                        }
                    })
                }
                if (transformation.isNotEmpty()) {
                    glideRequest.transforms(*transformation)
                }
                if (PholderTagUtil.isVideo(loadPath)) {
                    if (loadVideoFirstFrame) {
                        glideRequest.frame(1L)
                        glideRequest.signature(ObjectKey("loadVideoFirstFrame=true"))
                    } else {
                        glideRequest.signature(ObjectKey("loadVideoFirstFrame=false"))
                    }
                }
                glideRequest.into(imageView)
            }
        }

    }

}