package com.dokidevs.pholder.utils

import com.dokidevs.pholder.BuildConfig

/* files */
const val FILE_PROVIDER = BuildConfig.APPLICATION_ID + ".fileprovider"
// From https://stackoverflow.com/a/2703882/3584439
const val FILEPATH_RESERVED_CHARACTERS = "%|\\?*<\":>+[]/'"

/* time */
const val TIME_SECOND = 1000L
const val TIME_MINUTE = 60 * TIME_SECOND
const val TIME_HOUR = 60 * TIME_MINUTE

/* format */
const val JPG = "jpg"
const val JPEG = "jpeg"
const val PNG = "png"
const val BMP = "bmp"
const val GIF = "gif"
const val WEBP = "webp"
const val MP4 = "mp4"
const val WEBM = "webm"
const val TGP = "3gp"
const val MIME_IMAGE_ANY = "image/%"
const val MIME_IMAGE_MIX = "image/*"
const val MIME_IMAGE_JPG = "image/jpeg"
const val MIME_IMAGE_PNG = "image/png"
const val MIME_IMAGE_BMP = "image/bmp"
const val MIME_IMAGE_GIF = "image/gif"
const val MIME_IMAGE_WEBP = "image/webp"
const val MIME_VIDEO_MIX = "video/*"
const val MIME_VIDEO_MP4 = "video/mp4"
const val MIME_VIDEO_WEBM = "video/webm"
const val MIME_VIDEO_TGP = "video/3gpp"

/* request code */
const val REQUEST_ACCESS_FINE_LOCATION = 510
const val REQUEST_ACTION_APPLICATION_DETAILS_SETTINGS = 237
const val REQUEST_CAMERA_ACTIVITY = 294
const val REQUEST_CAMERA_CAPTURE = 145
const val REQUEST_GOOGLE_PLAY_SERVICES = 644
const val REQUEST_SETTINGS_ACTIVITY = 702
const val REQUEST_SLIDESHOW_ACTIVITY = 199
const val REQUEST_WRITE_EXTERNAL_STORAGE = 848