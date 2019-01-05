package com.dokidevs.pholder.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.dokidevs.dokilog.d
import com.dokidevs.dokilog.e
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseActivity
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.data.PholderDatabase.Companion.PHOLDER_FOLDER
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.utils.*
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ENABLE_CAMERA_LOCATION
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/*--- CameraActivity ---*/
class CameraActivity : BaseActivity() {

    /* companion object */
    companion object {

        /* saved instance states */
        private const val SAVED_FILE_PATH = "SAVED_FILE_PATH"

        /* intents */
        const val LAST_FILE_PATH = "LAST_FILE_PATH"
        private const val ROOT_PATH = "ROOT_PATH"
        private const val IS_IMAGE = "IS_IMAGE"

        /* files */
        private const val IMAGE_PREFIX = "IMG"
        private const val IMAGE_SUFFIX = ".jpg"
        private const val VIDEO_PREFIX = "VID"
        private const val VIDEO_SUFFIX = ".mp4"

        // newIntent
        fun newIntent(context: Context, rootPath: String, isImage: Boolean): Intent {
            val intent = Intent(context, CameraActivity::class.java)
            intent.putExtra(ROOT_PATH, rootPath)
            intent.putExtra(IS_IMAGE, isImage)
            return intent
        }

    }

    /* parameters */
    private val resultIntent = Intent()
    private var rootPath = PHOLDER_FOLDER.absolutePath
    private var filePath = ""
    private var lastFilePath = ""
    private var isImage = true
    private var canStartCamera = true
    private var isBackFromCamera = false
    private var requestedPlayServiceUpdate = false
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var location: Location? = null

    // onCreateAction
    override fun onCreateAction(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_camera)
        rootPath = intent.getStringExtra(ROOT_PATH) ?: rootPath
        isImage = intent.getBooleanExtra(IS_IMAGE, true)
        if (savedInstanceState != null) {
            // Activity was destroyed due to low memory, save the last photo and finish
            val filePath = savedInstanceState.getString(SAVED_FILE_PATH) ?: ""
            savePicture(filePath, location)
            canStartCamera = false
            finish()
        } else {
            // Show toast during first entry only.
            if (isImage) {
                shortToast(
                    preResId = R.string.toast_camera_save_location_photo_pre,
                    message = rootPath,
                    postResId = R.string.toast_camera_save_location_photo_post
                )
            } else {
                shortToast(
                    preResId = R.string.toast_camera_save_location_video_pre,
                    message = rootPath,
                    postResId = R.string.toast_camera_save_location_video_post
                )
            }
            if (prefManager.get(PREF_ENABLE_CAMERA_LOCATION, true)) {
                prepareLocationProvider()
                checkGpsPermission()
            } else {
                // Proceed to onResume and start camera
            }
        }
    }

    // prepareLocationProvider
    private fun prepareLocationProvider() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult != null) {
                    location = locationResult.lastLocation
                    d("location updated = $location")
                }
            }
        }
    }

    // checkGpsPermission
    private fun checkGpsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            d("PERMISSION_GRANTED = true")
            checkGooglePlayServices()
        } else {
            canStartCamera = false
            // Else, request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    // onRequestPermissionsResult
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ACCESS_FINE_LOCATION -> {
                // Proceed if permission granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    d("PERMISSION_GRANTED = true")
                    checkGpsPermission()
                } else {
                    // Show permission dialog
                    d("PERMISSION_GRANTED = false")
                    val alertDialogBuilder = AlertDialog.Builder(ContextThemeWrapper(this, ColorUtils.alertDialogTheme))
                    alertDialogBuilder.setMessage(R.string.permissionDialog_gps_message)
                    alertDialogBuilder.setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                checkGpsPermission()
                            } else {
                                // Redirect to app settings, shouldShowRequestPermissionRationale return false if user denied permission permanently
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivityForResult(intent, REQUEST_ACTION_APPLICATION_DETAILS_SETTINGS)
                            }
                        } else {
                            checkGpsPermission()
                        }
                    }
                    alertDialogBuilder.setNegativeButton(android.R.string.cancel) { dialogInterface, _ ->
                        prefManager.put(PREF_ENABLE_CAMERA_LOCATION, false)
                        dialogInterface.dismiss()
                        shortToast(R.string.toast_camera_gps_off)
                        canStartCamera = true
                        takePicture()
                    }
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.show()
                }
            }
        }
    }

    // checkGooglePlayServices
    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val result = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (result == ConnectionResult.SUCCESS) {
            // Take picture in onResume later
            canStartCamera = true
            startLocationUpdate()
        } else {
            // If not available, show error dialog if not prompt before
            if (googleApiAvailability.isUserResolvableError(result) && !requestedPlayServiceUpdate) {
                canStartCamera = false
                requestedPlayServiceUpdate = true
                googleApiAvailability.showErrorDialogFragment(this, result, REQUEST_GOOGLE_PLAY_SERVICES)
            } else {
                prefManager.put(PREF_ENABLE_CAMERA_LOCATION, false)
                shortToast(R.string.toast_google_play_services_not_available_gps)
                // Take picture in onResume later
                canStartCamera = true
            }
        }
    }

    // startLocationUpdate
    private fun startLocationUpdate() {
        val fusedLocationProviderClient = this.fusedLocationProviderClient
        if (fusedLocationProviderClient != null &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest().apply {
                interval = 5000
                fastestInterval = 1000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }

    // onActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CAMERA_CAPTURE -> {
                // Normal lifecycle is onActivityResult --> onStart --> onResume
                // If cameraActivity is killed due to memory, then lifecycle is onCreate --> onStart --> onActivityResult --> onResume
                d(logActivityResult(resultCode))
                isBackFromCamera = true
            }
            REQUEST_ACTION_APPLICATION_DETAILS_SETTINGS, REQUEST_GOOGLE_PLAY_SERVICES -> {
                checkGpsPermission()
            }
        }
    }

    // onResumeAction
    override fun onResumeAction() {
        // Process photo taken in onResume, after location is updated once looper is alive again
        if (isBackFromCamera) {
            isBackFromCamera = false
            val isSaved = savePicture(filePath, location)
            if (!isSaved) {
                canStartCamera = false
                finish()
            }
        }
        // Take picture flow as below:
        // (1) All ok, checkGpsPermission --> checkGooglePlayServices --> startLocationUpdate --> onResume --> takePicture
        // (2) No permission, checkGpsPermission --> requestPermissions --> onRequestPermissionsResult --> OK --> (1)
        // (3) Permission denied, onRequestPermissionsResult --> DENY --> show dialog --> OK --> (2)
        // (4) Dialog cancelled, show dialog --> CANCEL --> disable GPS --> takePicture
        // (5) PlayService issue, checkGooglePlayServices --> showErrorDialogFragment --> onActivityResult -> (1)
        if (canStartCamera) {
            takePicture()
        }
    }

    // takePicture
    private fun takePicture() {
        val intent = getCameraIntent(isImage)
        if (intent.resolveActivity(packageManager) != null) {
            val uri = FileProvider.getUriForFile(this, FILE_PROVIDER, createNewFile())
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivityForResult(intent, REQUEST_CAMERA_CAPTURE)
        } else {
            shortToast(R.string.toast_camera_not_found)
            finish()
        }
    }

    // getCameraIntent
    private fun getCameraIntent(isImage: Boolean): Intent {
        return if (isImage) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        } else {
            Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        }
    }

    // savePicture
    private fun savePicture(filePath: String, location: Location?): Boolean {
        var isSaved = false
        val file = File(filePath)
        if (file.exists() && file.length() != 0L) {
            isSaved = true
            lastFilePath = filePath
            if (PholderTagUtil.isJpg(filePath) &&
                prefManager.get(PREF_ENABLE_CAMERA_LOCATION, true) &&
                location != null
            ) {
                try {
                    val exifInterface = ExifInterface(filePath)
                    exifInterface.setLatLong(location.latitude, location.longitude)
                    exifInterface.saveAttributes()
                } catch (ex: Exception) {
                    e(ex)
                }
            }
            doAsync {
                if (File(filePath).exists()) {
                    if (PholderTagUtil.isJpg(filePath) || location == null) {
                        PholderDatabase.addMediaTaken(applicationContext, filePath)
                    } else {
                        // For video with location
                        PholderDatabase.addMediaTaken(
                            applicationContext,
                            filePath,
                            location.latitude,
                            location.longitude
                        )
                    }
                }
            }
        } else {
            // File is not used by camera due to zero length, delete it
            file.delete()
        }
        return isSaved
    }

    // createNewFile
    @SuppressLint("SimpleDateFormat")
    private fun createNewFile(): File {
        var i = 0
        var file: File? = null
        while (file == null) {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
            dateFormat.timeZone = TimeZone.getDefault()
            // Adjust by seconds to get unique file name in case of collision
            val dateTime = dateFormat.format(System.currentTimeMillis() + 1000L * i)
            file = File(rootPath, "${getFileNamePrefix(isImage)}_$dateTime" + getFileNameSuffix(isImage))
            if (!file.exists()) {
                // Create empty file and set file path
                file.createNewFile()
                filePath = file.absolutePath
            } else {
                // Clear file
                file = null
            }
            i++
        }
        return file
    }

    // getFileNamePrefix
    private fun getFileNamePrefix(isImage: Boolean): String {
        return if (isImage) {
            IMAGE_PREFIX
        } else {
            VIDEO_PREFIX
        }
    }

    // getFileNameSuffix
    private fun getFileNameSuffix(isImage: Boolean): String {
        return if (isImage) {
            IMAGE_SUFFIX
        } else {
            VIDEO_SUFFIX
        }
    }

    // onFinishAction
    override fun onFinishAction() {
        resultIntent.putExtra(LAST_FILE_PATH, lastFilePath)
        setResult(Activity.RESULT_OK, resultIntent)
    }

    // onSaveInstanceState
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVED_FILE_PATH, filePath)
    }

    // onDestroyAction
    override fun onDestroyAction() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
    }

}
