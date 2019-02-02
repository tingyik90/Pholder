package com.dokidevs.pholder.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.os.ResultReceiver
import androidx.core.app.JobIntentService
import androidx.exifinterface.media.ExifInterface
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.d
import com.dokidevs.dokilog.e
import com.dokidevs.pholder.base.BaseResultReceiver
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.utils.GEOCODER_INTENT_SERVICE_JOB_ID
import com.google.android.gms.maps.model.LatLng
import java.util.*

class GeocoderIntentService : JobIntentService(), DokiLog {

    /* companion object */
    companion object {

        /* intents */
        const val FILE_PATH = "FILE_PATH"
        const val LAT = "LAT"
        const val LNG = "LNG"
        private const val RESULT_RECEIVER = "RESULT_RECEIVER"

        /* results */
        const val RESULT_FILE_PATH = "RESULT_FILE_PATH"
        const val RESULT_LAT = "RESULT_LAT"
        const val RESULT_LNG = "RESULT_LNG"
        const val RESULT_ADDRESS = "RESULT_ADDRESS"

        // getAddress
        fun getAddress(context: Context, filePath: String, latLng: LatLng, resultReceiver: BaseResultReceiver) {
            val intent = Intent()
            intent.putExtra(FILE_PATH, filePath)
            intent.putExtra(LAT, latLng.latitude)
            intent.putExtra(LNG, latLng.longitude)
            intent.putExtra(RESULT_RECEIVER, resultReceiver)
            enqueueWork(context, intent)
        }

        // enqueueWork
        private fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, GeocoderIntentService::class.java, GEOCODER_INTENT_SERVICE_JOB_ID, intent)
        }
    }

    // onHandleWork
    override fun onHandleWork(intent: Intent) {
        d()
        val filePath = intent.getStringExtra(FILE_PATH) ?: ""
        var lat = intent.getDoubleExtra(LAT, 0.0)
        var lng = intent.getDoubleExtra(LNG, 0.0)
        // Cannot be casted to BaseResultReceiver as ResultReceiver parcel creator is not redefined in BaseResultReceiver
        // It will throw ClassCastException, see https://stackoverflow.com/a/26384664
        val resultReceiver = intent.getParcelableExtra<ResultReceiver>(RESULT_RECEIVER)
        if (resultReceiver != null) {
            // Get latLng from exif for jpg if MediaStore failed
            if (lat == 0.0 && lng == 0.0 && PholderTagUtil.isJpg(filePath)) {
                try {
                    val exifInterface = ExifInterface(filePath)
                    val latLong = exifInterface.latLong
                    if (latLong != null) {
                        d("exif latLng is available")
                        lat = latLong[0]
                        lng = latLong[1]
                    }
                } catch (ex: Exception) {
                    e(ex)
                }
            }
            val resultLatLng = LatLng(lat, lng)
            var resultAddress = ""
            if (resultLatLng.latitude != 0.0 && resultLatLng.longitude != 0.0) {
                try {
                    val addresses = Geocoder(this, Locale.getDefault()).getFromLocation(
                        resultLatLng.latitude,
                        resultLatLng.longitude,
                        5
                    )
                    var cityName = ""
                    var stateName = ""
                    var countryName = ""
                    for (address in addresses) {
                        // See https://stackoverflow.com/a/26960384/3584439
                        // stateName is usually available but cityName may not.
                        // As long as we hit a cityName, we return the result. Else, this will use whatever stateName
                        // which is available last.
                        if (!address.countryName.isNullOrEmpty()) {
                            countryName = address.countryName
                        }
                        if (!address.adminArea.isNullOrEmpty()) {
                            stateName = address.adminArea
                        }
                        if (!address.locality.isNullOrEmpty()) {
                            cityName = address.locality
                            break
                        }
                        /* For debugging
                        d("address = ${address.getAddressLine(0)}")
                        d("featureName = ${address.featureName}")
                        d("premises = ${address.premises}")
                        d("thoroughfare = ${address.thoroughfare}")
                        d("subThoroughfare = ${address.subThoroughfare}")
                        d("locality = ${address.locality}")
                        d("subLocality = ${address.subLocality}")
                        d("adminArea = ${address.adminArea}")
                        d("subAdminArea = ${address.subAdminArea}")
                        */
                    }
                    resultAddress =
                        when {
                            !cityName.isEmpty() && !stateName.isEmpty() -> {
                                "$cityName, $stateName"
                            }
                            !cityName.isEmpty() -> {
                                cityName
                            }
                            !stateName.isEmpty() -> {
                                stateName
                            }
                            !countryName.isEmpty() -> {
                                countryName
                            }
                            else -> {
                                ""
                            }
                        }
                } catch (ex: Exception) {
                    e(ex)
                }
            }
            // Send result
            val bundle = Bundle()
            bundle.putString(RESULT_FILE_PATH, filePath)
            bundle.putDouble(RESULT_LAT, resultLatLng.latitude)
            bundle.putDouble(RESULT_LNG, resultLatLng.longitude)
            bundle.putString(RESULT_ADDRESS, resultAddress)
            resultReceiver.send(Activity.RESULT_OK, bundle)
        }
    }

    // onDestroy
    override fun onDestroy() {
        d()
        super.onDestroy()
    }

}