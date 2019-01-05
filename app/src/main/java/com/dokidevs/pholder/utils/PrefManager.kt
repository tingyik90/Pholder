package com.dokidevs.pholder.utils

import android.content.Context
import androidx.preference.PreferenceManager
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.v
import com.dokidevs.pholder.R
import com.google.gson.Gson

/*--- PrefManager ---*/
class PrefManager(context: Context) : DokiLog {

    /* init */
    init {
        // set default values
        PreferenceManager.setDefaultValues(context, R.xml.pref_general, true)
    }

    /* companion object */
    companion object {

        /* preferences */
        const val PREF_IS_DARK_THEME = "PREF_IS_DARK_THEME"
        const val PREF_ALL_VIDEOS_FOLDER_STAR = "PREF_ALL_VIDEOS_FOLDER_STAR"
        const val PREF_ANIMATE_GIF = "PREF_ANIMATE_GIF"
        const val PREF_ARRAY_CREATED_FOLDER_PATHS = "PREF_ARRAY_CREATED_FOLDER_PATHS"
        const val PREF_ARRAY_EXCLUDED_FOLDER_PATHS = "PREF_ARRAY_EXCLUDED_FOLDER_PATHS"
        const val PREF_ARRAY_INCLUDED_FOLDER_PATHS = "PREF_ARRAY_INCLUDED_FOLDER_PATHS"
        const val PREF_AUTOMATICALLY_STAR_CREATED_FOLDER = "PREF_AUTOMATICALLY_STAR_CREATED_FOLDER"
        const val PREF_DATABASE_LAST_UPDATE_TIME = "PREF_DATABASE_LAST_UPDATE_TIME"
        const val PREF_ENABLE_CAMERA_LOCATION = "PREF_ENABLE_CAMERA_LOCATION"
        const val PREF_GALLERY_VIEW_TYPE = "PREF_GALLERY_VIEW_TYPE"
        const val PREF_IS_FIRST_UPDATE_DATA = "PREF_IS_FIRST_UPDATE_DATA"
        const val PREF_IS_PHOLDER_FOLDER_CREATED = "PREF_IS_PHOLDER_FOLDER_CREATED"
        const val PREF_IS_SUGGESTED_STAR_FOLDER_PATHS_ADDED = "PREF_IS_SUGGESTED_STAR_FOLDER_PATHS_ADDED"
        const val PREF_LIST_BROWSING_MODE = "PREF_LIST_BROWSING_MODE"
        const val PREF_LIST_BROWSING_MODE_ALBUM = "PREF_LIST_BROWSING_MODE_ALBUM"
        const val PREF_LIST_BROWSING_MODE_FILE_EXPLORER = "PREF_LIST_BROWSING_MODE_FILE_EXPLORER"
        const val PREF_SHOW_ALL_VIDEOS_FOLDER = "PREF_SHOW_ALL_VIDEOS_FOLDER"
        const val PREF_SHOW_CAMERA_BUTTONS = "PREF_SHOW_CAMERA_BUTTONS"
        const val PREF_SHOW_EMPTY_FOLDERS = "PREF_SHOW_EMPTY_FOLDERS"
        const val PREF_SHOWED_ALBUM_MODE_DIALOG = "PREF_SHOWED_ALBUM_MODE_DIALOG"
        const val PREF_SHOWED_FILE_EXPLORER_MODE_DIALOG = "PREF_SHOWED_FILE_EXPLORER_MODE_DIALOG"
        const val PREF_SORT_ORDER_FOLDER = "PREF_SORT_ORDER_FOLDER"
        const val PREF_SORT_ORDER_MEDIA = "PREF_SORT_ORDER_MEDIA"
        const val PREF_USE_EXIT_CONFIRMATION = "PREF_USE_EXIT_CONFIRMATION"
        const val PREF_USE_FULL_NATIVE_CAMERA = "PREF_USE_FULL_NATIVE_CAMERA"

        /* debug */
        private const val PUT = "PUT"
        private const val GET = "GET"

    }

    /* parameters */
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor = sharedPreferences.edit()
    private var toLog = false

    // contains
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    // put
    fun put(key: String, value: Any?) {
        when (value) {
            is String -> {
                editor.putString(key, value).commit()
            }
            is Boolean -> {
                editor.putBoolean(key, value).commit()
            }
            is Int -> {
                editor.putInt(key, value).commit()
            }
            is Long -> {
                editor.putLong(key, value).commit()
            }
            is Float -> {
                editor.putFloat(key, value).commit()
            }
            else -> {
                throw UnsupportedOperationException("Not yet implemented")
            }
        }
        logger(PUT, key, value)
    }

    // putStringArray
    fun putStringArray(key: String, stringArray: Array<String>) {
        val json = Gson().toJson(stringArray)
        put(key, json)
    }

    // get
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, defaultValue: T): T {
        val value: T
        when (defaultValue) {
            is String -> {
                value = sharedPreferences.getString(key, defaultValue) as T
            }
            is Boolean -> {
                value = sharedPreferences.getBoolean(key, defaultValue) as T
            }
            is Int -> {
                value = sharedPreferences.getInt(key, defaultValue) as T
            }
            is Long -> {
                value = sharedPreferences.getLong(key, defaultValue) as T
            }
            is Float -> {
                value = sharedPreferences.getFloat(key, defaultValue) as T
            }
            else -> {
                throw UnsupportedOperationException("Not yet implemented")
            }
        }
        logger(GET, key, value)
        return value
    }

    // getStringArray
    fun getStringArray(key: String): Array<String> {
        val json = get(key, "")
        return if (json.isNotEmpty()) {
            Gson().fromJson(json, Array<String>::class.java)!!
        } else {
            arrayOf()
        }
    }

    // clearAll
    fun clearAll() {
        editor.clear().commit()
    }

    // remove
    fun remove(key: String) {
        editor.remove(key).commit()
    }

    // logger
    private fun logger(type: String, key: String, value: Any?) {
        if (toLog) v("$type $key = $value")
    }

}