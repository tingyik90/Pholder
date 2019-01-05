package com.dokidevs.pholder.gallery

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.dokidevs.dokilog.d
import com.dokidevs.pholder.PholderApplication.Companion.isDarkTheme
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseDialogFragment
import com.dokidevs.pholder.base.BaseFragment
import com.dokidevs.pholder.camera.CameraActivity
import com.dokidevs.pholder.data.FileTag
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.data.PholderDatabase.Companion.ALL_VIDEOS_FOLDER
import com.dokidevs.pholder.data.PholderDatabase.Companion.PHOLDER_FOLDER
import com.dokidevs.pholder.data.PholderDatabase.Companion.PUBLIC_ROOT
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.dialog.ConfirmationDialog
import com.dokidevs.pholder.dialog.FolderNameDialog
import com.dokidevs.pholder.dialog.SortDialog
import com.dokidevs.pholder.dialog.folderpicker.FolderPickerDialog
import com.dokidevs.pholder.gallery.AlbumFragment.Companion.ALBUM_ROOT
import com.dokidevs.pholder.gallery.StarFragment.Companion.STAR_ROOT
import com.dokidevs.pholder.gallery.layout.GalleryAdapter.Companion.LAYOUT_GRID
import com.dokidevs.pholder.gallery.layout.GalleryAdapter.Companion.LAYOUT_LIST
import com.dokidevs.pholder.service.FileIntentService
import com.dokidevs.pholder.settings.SettingsActivity
import com.dokidevs.pholder.utils.*
import com.dokidevs.pholder.utils.ColorUtils.Companion.alertDialogTheme
import com.dokidevs.pholder.utils.ColorUtils.Companion.navigationBarTranslucent
import com.dokidevs.pholder.utils.ColorUtils.Companion.statusBarTranslucent
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_GALLERY_VIEW_TYPE
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_IS_FIRST_UPDATE_DATA
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_LIST_BROWSING_MODE
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_LIST_BROWSING_MODE_ALBUM
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_LIST_BROWSING_MODE_FILE_EXPLORER
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOWED_ALBUM_MODE_DIALOG
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOWED_FILE_EXPLORER_MODE_DIALOG
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_CAMERA_BUTTONS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_USE_EXIT_CONFIRMATION
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_USE_FULL_NATIVE_CAMERA
import com.google.android.material.tabs.TabLayout
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import kotlinx.android.synthetic.main.activity_gallery.*
import org.jetbrains.anko.longToast
import java.io.File
import androidx.core.util.Pair as AndroidPair

/*--- GalleryActivity ---*/
class GalleryActivity : GalleryBaseActivity(), BaseDialogFragment.DialogListener {

    /* companion object */
    companion object {

        /* saved instance states */
        private const val SAVED_CURRENT_TAB_POSITION = "SAVED_CURRENT_TAB_POSITION"
        private const val SAVED_LAST_UPDATE_TIME = "SAVED_LAST_UPDATE_TIME"

        /* dialog */
        private const val REQUEST_CODE_FOLDER_PICKER_DIALOG_MOVE = 1001
        private const val REQUEST_CODE_FOLDER_PICKER_DIALOG_CAMERA = 1002

    }

    /* views */
    private val main by lazy { galleryActivity_main }
    private val splashScreen by lazy { galleryActivity_splashScreen }
    private val logo by lazy { galleryActivity_logo }
    private val firstUpdateText by lazy { galleryActivity_firstUpdate }
    private val appBarLayout by lazy { galleryActivity_appBarLayout }
    private val toolbar by lazy { galleryActivity_toolbar }
    private val lockableViewPager by lazy { galleryActivity_lockableViewpager }
    private val fabVideo by lazy { galleryActivity_fab_video }
    private val fabCamera by lazy { galleryActivity_fab_camera }
    private val tabLayout by lazy { galleryActivity_tabs }
    private lateinit var toolbarTitle: TextView
    private lateinit var tabFragmentAdapter: GalleryTabAdapter
    private lateinit var snackProgressBarManager: SnackProgressBarManager

    /* layout */
    private var browsingMode = ""
    private var galleryViewType = LAYOUT_GRID

    /* menus */
    private lateinit var menuCreateFolder: MenuItem
    private lateinit var menuSortBy: MenuItem
    private lateinit var menuGridView: MenuItem
    private lateinit var menuListView: MenuItem
    private lateinit var menuStarFolder: MenuItem
    private lateinit var menuRemoveStarFolder: MenuItem
    private lateinit var menuShare: MenuItem
    private lateinit var menuDelete: MenuItem
    private lateinit var menuSelectAll: MenuItem
    private lateinit var menuMoveTo: MenuItem
    private lateinit var menuRename: MenuItem
    private lateinit var menuFolderInfo: MenuItem
    private lateinit var menuSettings: MenuItem

    /* splash */
    private val hideSplashHandler = Handler()
    private val hideSplashRunnable = Runnable { hideSplash(false) }
    private val hideSplashDelay by lazy { resources.getInteger(R.integer.animation_duration_shape_change) * 2L }
    private val logoAnimation by lazy { AnimationUtils.loadAnimation(this, R.anim.anim_logo) }

    /* backPress */
    private var wasBackPressed = false
    private var backPressHandler = Handler()
    private var backPressRunnable = Runnable { wasBackPressed = false }

    /* parameters */
    private val updateTimeLimit = 5 * TIME_MINUTE
    private var lastUpdateTime = -1L
    private var cameraLastFilePath = ""
    private var isImageCamera = true
    private var currentTabPosition = 0
    private var isRequestingPermission = false
    private lateinit var broadcastReceiver: BroadcastReceiver

    // onCreatePreAction
    override fun onCreatePreAction(savedInstanceState: Bundle?) {
        // Set theme
        if (isDarkTheme) {
            setTheme(R.style.AppTheme_NoActionBar)
        } else {
            setTheme(R.style.AppTheme_Light_NoActionBar)
        }
    }

    // onCreateAction
    override fun onCreateAction(savedInstanceState: Bundle?) {
        super.onCreateAction(savedInstanceState)
        val visibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility
        window.navigationBarColor = navigationBarTranslucent
        window.statusBarColor = statusBarTranslucent
        setContentView(R.layout.activity_gallery)
        // Apply margin so that gallery is not chipped when rotated
        ViewCompat.setOnApplyWindowInsetsListener(main) { view, insets ->
            // If possible, apply the margin to rootView so that snackProgressBar will use the correct margin
            // if phone is using virtual navigationBar.
            val content = window.decorView.findViewById<View>(android.R.id.content)
            if (content != null) {
                content.setMargins(left = insets.systemWindowInsetLeft, right = insets.systemWindowInsetRight)
            } else {
                // Fallback in case we can't find the root
                view.setMargins(left = insets.systemWindowInsetLeft, right = insets.systemWindowInsetRight)
            }
            insets
        }
        galleryViewType = prefManager.get(PREF_GALLERY_VIEW_TYPE, galleryViewType)
        if (savedInstanceState == null) {
            // This is a new instance, animate logo while background processing
            animateLogo(true)
        } else {
            // Rotation is already handled by onConfigurationChanged.
            // This call is only possible when activity is destroyed due to Android releasing memory.
            hideSplash(false)
            currentTabPosition = savedInstanceState.getInt(SAVED_CURRENT_TAB_POSITION, 0)
            lastUpdateTime = savedInstanceState.getLong(SAVED_LAST_UPDATE_TIME, lastUpdateTime)
        }
        setSnackProgressBarManager()
        setToolbar()
        setTabFragmentAdapter()
        setViewPager()
        setTab()
        setBroadcastReceiver()
        setFab()
        showFab(true)
    }

    // animateLogo
    private fun animateLogo(animate: Boolean) {
        if (animate) {
            logo.startAnimation(logoAnimation)
        } else {
            logoAnimation.cancel()
            logo.clearAnimation()
        }
    }

    // hideSplash
    private fun hideSplash(withDelay: Boolean) {
        if (splashScreen.isVisible) {
            if (withDelay) {
                hideSplashHandler.postDelayed(hideSplashRunnable, hideSplashDelay)
            } else {
                hideSplashHandler.removeCallbacks(hideSplashRunnable)
                splashScreen.isVisible = false
                animateLogo(false)
            }
        }
    }

    // setSnackProgressBarManager
    private fun setSnackProgressBarManager() {
        snackProgressBarManager = SnackProgressBarManager(main)
        snackProgressBarManager.setViewsToMove(arrayOf(fabCamera, fabVideo))
    }

    // setToolbar
    private fun setToolbar() {
        setSupportActionBar(toolbar)
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { view, insets ->
            view.setMargins(top = insets.systemWindowInsetTop)
            insets
        }
        // Hack to enable ellipsize marquee to work in ToolBar. See https://stackoverflow.com/a/8748802/3584439
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_toolbar_title_marquee, toolbar, false)
        supportActionBar?.customView = view
        toolbarTitle = view.findViewById(R.id.toolbarTitleMarqueeLayout_title)
        toolbarTitle.isSelected = true
        toolbarTitle.text = getString(R.string.app_name)
    }

    // updateToolbar
    fun updateToolbar(fragment: GalleryBaseFragment?) {
        if (fragment != null) {
            d(fragment.getFragmentTag())
            // Set UP button
            if (!fragment.isSelectionMode) {
                when (fragment.getFragmentClass()) {
                    AlbumFragment.FRAGMENT_CLASS -> {
                        // Show back button if not yet at root file
                        showToolbarUpButton(fragment.rootFile != ALBUM_ROOT)
                    }
                    StarFragment.FRAGMENT_CLASS -> {
                        // Show back button if not yet at root file
                        showToolbarUpButton(fragment.rootFile != STAR_ROOT)
                    }
                    FolderFragment.FRAGMENT_CLASS -> {
                        // Show back button if not yet at root file
                        showToolbarUpButton(fragment.rootFile != PUBLIC_ROOT)
                    }
                    DateFragment.FRAGMENT_CLASS -> {
                        showToolbarUpButton(false)
                    }
                }
            } else {
                // For selection mode, show 'X' instead
                supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
            // Set new toolbarTitle
            this.toolbarTitle.text = fragment.getToolbarTitle()
            // Refresh menu
            invalidateOptionsMenu()
        }
    }

    // showToolbarUpButton
    private fun showToolbarUpButton(show: Boolean) {
        // Use default back icon
        supportActionBar?.setHomeAsUpIndicator(0)
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
    }

    // setTabFragmentAdapter
    private fun setTabFragmentAdapter() {
        browsingMode = prefManager.get(PREF_LIST_BROWSING_MODE, "")
        tabFragmentAdapter = GalleryTabAdapter(browsingMode, supportFragmentManager)
    }

    // setViewPager
    private fun setViewPager() {
        lockableViewPager.offscreenPageLimit = 2
        lockableViewPager.canSwipe = true
        lockableViewPager.currentItem = currentTabPosition
        lockableViewPager.adapter = tabFragmentAdapter
    }

    // setTab
    private fun setTab() {
        tabLayout.setupWithViewPager(lockableViewPager)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                // Do nothing
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                val galleryFragment = tabFragmentAdapter.getGalleryBaseFragment(tab.position)
                if (galleryFragment != null && galleryFragment.isSelectionMode) {
                    galleryFragment.endSelectionMode()
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                // Use this marker because both lockableViewPager.currentItem() and tabFragmentAdapter.primaryItem
                // are only updated after this listener is finished calling.
                d("position = ${tab.position}")
                currentTabPosition = tab.position
                updateToolbar(getCurrentGalleryBaseFragment())
            }
        })
        when (browsingMode) {
            PREF_LIST_BROWSING_MODE_ALBUM -> {
                tabLayout.getTabAt(0)?.setIcon(R.drawable.ic_album_white_24dp)
                tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_image_white_24dp)
            }
            PREF_LIST_BROWSING_MODE_FILE_EXPLORER -> {
                tabLayout.getTabAt(0)?.setIcon(R.drawable.ic_star_white_24dp)
                tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_folder_white_48dp)
                tabLayout.getTabAt(2)?.setIcon(R.drawable.ic_image_white_24dp)
            }
        }
    }

    // setBroadcastReceiver
    @Suppress("UNCHECKED_CAST")
    private fun setBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                d("action = $action")
                when (action) {
                    FileIntentService.ACTION_FILES_UPDATED -> {
                        filesUpdated()
                    }
                    FileIntentService.ACTION_FILES_DELETED -> {
                        val resultPairs =
                            intent.getSerializableExtra(FileIntentService.FILES_ACTION_RESULT_PAIR_ARRAY) as Array<Pair<String, Int>>
                        filesDeleted(resultPairs)
                    }
                    FileIntentService.ACTION_FILES_MOVED -> {
                        val resultPairs =
                            intent.getSerializableExtra(FileIntentService.FILES_ACTION_RESULT_PAIR_ARRAY) as Array<Pair<String, Int>>
                        filesMoved(resultPairs)
                    }
                    FileIntentService.ACTION_FILES_RENAMED -> {
                        val resultPairs =
                            intent.getSerializableExtra(FileIntentService.FILES_ACTION_RESULT_PAIR_ARRAY) as Array<Pair<String, Int>>
                        filesRenamed(resultPairs)
                    }
                    FileIntentService.ACTION_FOLDER_CREATED -> {
                        val isFolderCreated = intent.getBooleanExtra(FileIntentService.FOLDER_CREATED_BOOLEAN, false)
                        val folderPath = intent.getStringExtra(FileIntentService.FOLDER_CREATED_PATH) ?: ""
                        folderCreated(isFolderCreated, folderPath)
                    }
                    FileIntentService.ACTION_FOLDER_STARRED -> {
                        filesUpdated()
                    }
                }
            }
        }
    }

    // setFab
    private fun setFab() {
        // Move fab towards bottom
        val fabMargin = resources.getDimension(R.dimen.grid_2x).toInt()
        // Right side of the margin is taken care by galleryActivity_main when rotated
        ViewCompat.setOnApplyWindowInsetsListener(fabCamera) { view, insets ->
            view.setMargins(
                bottom = insets.systemWindowInsetBottom + fabMargin
            )
            insets
        }
        fabVideo.setOnClickListener {
            isImageCamera = false
            startCamera()
        }
        fabCamera.setOnClickListener {
            isImageCamera = true
            startCamera()
        }
    }

    // showFab
    private fun showFab(show: Boolean) {
        if (show && prefManager.get(PREF_SHOW_CAMERA_BUTTONS, true)) {
            fabCamera.show()
            // Only show camera fab when using full native camera
            if (!prefManager.get(PREF_USE_FULL_NATIVE_CAMERA, false)) {
                fabVideo.show()
            } else {
                fabVideo.hide()
            }
        } else {
            fabVideo.hide()
            fabCamera.hide()
        }
    }

    // startCamera
    private fun startCamera() {
        if (prefManager.get(PREF_USE_FULL_NATIVE_CAMERA, false)) {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            if (intent.resolveActivity(packageManager) != null) {
                // Make time to exceed updateTimeLimit to force update after returning from camera app
                lastUpdateTime -= 2 * updateTimeLimit
                // If use full native camera, start the activity without result.
                // Using startActivityForResult will not return to Pholder if user presses back in camera app.
                // See https://stackoverflow.com/a/30041238/3584439
                startActivity(intent)
            } else {
                longToast(getString(R.string.toast_camera_not_found))
            }
        } else {
            val fragment = getCurrentGalleryBaseFragment()
            if (fragment != null) {
                val rootFile = File(fragment.getRootPath())
                when (rootFile) {
                    PUBLIC_ROOT,
                    ALBUM_ROOT,
                    STAR_ROOT -> {
                        if (isImageCamera) {
                            longToast(R.string.toast_startCamera_warn_root_not_allowed_image)
                        } else {
                            longToast(R.string.toast_startCamera_warn_root_not_allowed_video)
                        }
                    }
                    DateFragment.DATE_ROOT -> {
                        // Select save location
                        FolderPickerDialog
                            .newInstance(FolderPickerDialog.DIALOG_THUMBNAIL, REQUEST_CODE_FOLDER_PICKER_DIALOG_CAMERA)
                            .show(supportFragmentManager)
                        if (isImageCamera) {
                            shortToast(R.string.toast_select_folder_image)
                        } else {
                            shortToast(R.string.toast_select_folder_video)
                        }
                    }
                    else -> {
                        val intent = CameraActivity.newIntent(this, rootFile.absolutePath, isImageCamera)
                        startActivityForResult(intent, REQUEST_CAMERA_ACTIVITY)
                    }
                }
            }
        }
    }

    // onStartAction
    override fun onStartAction() {
        registerLocalBroadcastReceiver(broadcastReceiver, FileIntentService.ACTION_FILES_UPDATED)
        registerLocalBroadcastReceiver(broadcastReceiver, FileIntentService.ACTION_FILES_DELETED)
        registerLocalBroadcastReceiver(broadcastReceiver, FileIntentService.ACTION_FILES_RENAMED)
        registerLocalBroadcastReceiver(broadcastReceiver, FileIntentService.ACTION_FILES_MOVED)
        registerLocalBroadcastReceiver(broadcastReceiver, FileIntentService.ACTION_FOLDER_CREATED)
        registerLocalBroadcastReceiver(broadcastReceiver, FileIntentService.ACTION_FOLDER_STARRED)
    }

    // onCreateOptionsMenu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gallery, menu)
        menuCreateFolder = menu.findItem(R.id.galleryActivity_menu_createFolder)
        menuSortBy = menu.findItem(R.id.galleryActivity_menu_sortBy)
        menuGridView = menu.findItem(R.id.galleryActivity_menu_gridView)
        menuListView = menu.findItem(R.id.galleryActivity_menu_listView)
        menuStarFolder = menu.findItem(R.id.galleryActivity_menu_starFolder)
        menuRemoveStarFolder = menu.findItem(R.id.galleryActivity_menu_removeStarFolder)
        menuShare = menu.findItem(R.id.galleryActivity_menu_share)
        menuSelectAll = menu.findItem(R.id.galleryActivity_menu_selectAll)
        menuDelete = menu.findItem(R.id.galleryActivity_menu_delete)
        menuMoveTo = menu.findItem(R.id.galleryActivity_menu_moveTo)
        menuRename = menu.findItem(R.id.galleryActivity_menu_rename)
        menuFolderInfo = menu.findItem(R.id.galleryActivity_menu_folderInfo)
        menuSettings = menu.findItem(R.id.galleryActivity_menu_settings)
        return true
    }

    // onPrepareOptionsMenu
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val fragment = getCurrentGalleryBaseFragment()
        if (fragment != null && fragment.isSelectionMode) {
            val selectedFolderCount = fragment.getSelectedFolderCount()
            val selectedFileCount = fragment.getSelectedFileCount()
            when {
                // Selection of folders and files
                selectedFileCount > 0 && selectedFolderCount > 0 -> {
                    menuCreateFolder.isVisible = false
                    menuSortBy.isVisible = false
                    menuGridView.isVisible = false
                    menuListView.isVisible = false
                    menuStarFolder.isVisible = false
                    menuRemoveStarFolder.isVisible = false
                    menuShare.isVisible = false
                    menuDelete.isVisible = true
                    menuSelectAll.isVisible = true
                    menuMoveTo.isVisible = true
                    menuRename.isVisible = false
                    menuFolderInfo.isVisible = false
                    menuSettings.isVisible = false
                }
                // Selection of folders
                selectedFolderCount > 0 -> {
                    menuCreateFolder.isVisible = false
                    menuSortBy.isVisible = false
                    menuGridView.isVisible = false
                    menuListView.isVisible = false
                    val folderTag = fragment.getSelectedFolderTags()[0]
                    if (folderTag.isStarred) {
                        menuStarFolder.isVisible = false
                        menuRemoveStarFolder.isVisible = true
                    } else {
                        menuStarFolder.isVisible = true
                        menuRemoveStarFolder.isVisible = false
                    }
                    menuShare.isVisible = false
                    menuDelete.isVisible = true
                    menuSelectAll.isVisible = true
                    menuMoveTo.isVisible = true
                    menuRename.isVisible = true
                    menuFolderInfo.isVisible = false
                    menuSettings.isVisible = false
                }
                // Selection of files
                selectedFileCount > 0 -> {
                    menuCreateFolder.isVisible = false
                    menuSortBy.isVisible = false
                    menuGridView.isVisible = false
                    menuListView.isVisible = false
                    menuStarFolder.isVisible = false
                    menuRemoveStarFolder.isVisible = false
                    menuShare.isVisible = true
                    menuDelete.isVisible = true
                    menuSelectAll.isVisible = true
                    menuMoveTo.isVisible = true
                    menuRename.isVisible = false
                    menuFolderInfo.isVisible = false
                    menuSettings.isVisible = false
                }

            }
        } else {
            menuCreateFolder.isVisible = true
            // Mutate is used because drawables are shared everywhere in the app itself.
            // Else, the icon in another activity using the same icon will have the same alpha.
            // See https://stackoverflow.com/a/33697621/3584439 and https://android-developers.googleblog.com/2009/05/drawable-mutations.html
            if (canCreateFolder(fragment)) {
                menuCreateFolder.icon.mutate().alpha = 255
            } else {
                menuCreateFolder.icon.mutate().alpha = 127
            }
            menuSortBy.isVisible = true
            if (galleryViewType == LAYOUT_GRID) {
                menuGridView.isVisible = false
                menuListView.isVisible = true
            } else {
                menuGridView.isVisible = true
                menuListView.isVisible = false
            }
            menuStarFolder.isVisible = false
            menuRemoveStarFolder.isVisible = false
            menuShare.isVisible = false
            menuDelete.isVisible = false
            menuSelectAll.isVisible = false
            menuMoveTo.isVisible = false
            menuRename.isVisible = false
            menuFolderInfo.isVisible = canShowFolderInfo(fragment)
            menuSettings.isVisible = true
        }
        return true
    }

    // onOptionsItemSelected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.galleryActivity_menu_createFolder -> {
                createFolder()
            }
            R.id.galleryActivity_menu_sortBy -> {
                SortDialog.newInstance().show(supportFragmentManager)
            }
            R.id.galleryActivity_menu_gridView -> {
                galleryViewType = LAYOUT_GRID
                prefManager.put(PREF_GALLERY_VIEW_TYPE, galleryViewType)
                tabFragmentAdapter.forEachGalleryBaseFragment { _, galleryBaseFragment ->
                    galleryBaseFragment.switchGalleryViewType(galleryViewType)
                }
                menuGridView.isVisible = false
                menuListView.isVisible = true
            }
            R.id.galleryActivity_menu_listView -> {
                galleryViewType = LAYOUT_LIST
                prefManager.put(PREF_GALLERY_VIEW_TYPE, galleryViewType)
                tabFragmentAdapter.forEachGalleryBaseFragment { _, galleryBaseFragment ->
                    galleryBaseFragment.switchGalleryViewType(galleryViewType)
                }
                menuListView.isVisible = false
                menuGridView.isVisible = true
            }
            R.id.galleryActivity_menu_starFolder -> {
                starFolders(true)
            }
            R.id.galleryActivity_menu_removeStarFolder -> {
                starFolders(false)
            }
            R.id.galleryActivity_menu_share -> {
                val fragment = getCurrentGalleryBaseFragment()
                if (fragment != null) {
                    FileIntentService.shareFiles(this, fragment.getSelectedFileTags())
                }
            }
            R.id.galleryActivity_menu_delete -> {
                // Show dialog to confirm delete
                ConfirmationDialog.newInstance(ConfirmationDialog.DIALOG_DELETE).show(supportFragmentManager)
            }
            R.id.galleryActivity_menu_selectAll -> {
                getCurrentGalleryBaseFragment()?.selectAll()
            }
            R.id.galleryActivity_menu_moveTo -> {
                // Show dialog to select destination
                val rootPath = getCurrentGalleryBaseFragment()?.getRootPath() ?: ""
                FolderPickerDialog
                    .newInstance(
                        dialogType = FolderPickerDialog.DIALOG_THUMBNAIL,
                        requestCode = REQUEST_CODE_FOLDER_PICKER_DIALOG_MOVE,
                        rootPath = rootPath,
                        confirmationDialogType = ConfirmationDialog.DIALOG_MOVE
                    )
                    .show(supportFragmentManager)
            }
            R.id.galleryActivity_menu_rename -> {
                // Show dialog to get name
                FolderNameDialog.newRenameDialog().show(supportFragmentManager)
            }
            R.id.galleryActivity_menu_folderInfo -> {
                val fragment = getCurrentGalleryBaseFragment()
                if (fragment != null) {
                    snackProgressBarManager.longSnackBar(
                        this,
                        preResId = R.string.toast_folder_path_pre,
                        message = fragment.getRootPath(),
                        postResId = R.string.toast_folder_path_post
                    )
                }
            }
            R.id.galleryActivity_menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(intent, REQUEST_SETTINGS_ACTIVITY)
            }
        }
        return true
    }

    // canCreateFolder
    private fun canCreateFolder(fragment: GalleryBaseFragment?): Boolean {
        if (fragment == null) return false
        // Don't allow for virtual folders
        if (fragment.rootFile == ALL_VIDEOS_FOLDER) return false
        return when (fragment.getFragmentClass()) {
            AlbumFragment.FRAGMENT_CLASS -> {
                // Allow create folder if not at top directory.
                // When at album root, will default to create folder in Pholder folder.
                fragment.rootFile != PUBLIC_ROOT
            }
            StarFragment.FRAGMENT_CLASS -> {
                // Allow create folder if not at top directory
                fragment.rootFile != STAR_ROOT && fragment.rootFile != PUBLIC_ROOT
            }
            FolderFragment.FRAGMENT_CLASS -> {
                // Allow create folder if not at top directory
                fragment.rootFile != PUBLIC_ROOT
            }
            DateFragment.FRAGMENT_CLASS -> {
                // Don't allow create folder at all
                false
            }
            else -> {
                false
            }
        }
    }

    // canShowFolderInfo
    private fun canShowFolderInfo(fragment: GalleryBaseFragment?): Boolean {
        if (fragment == null) return false
        // Don't allow for virtual folders
        if (fragment.rootFile == ALL_VIDEOS_FOLDER) return false
        return when (fragment.getFragmentClass()) {
            AlbumFragment.FRAGMENT_CLASS -> {
                // Don't allow folder info at root file
                fragment.rootFile != ALBUM_ROOT
            }
            StarFragment.FRAGMENT_CLASS -> {
                // Don't allow folder info at root file
                fragment.rootFile != STAR_ROOT
            }
            FolderFragment.FRAGMENT_CLASS -> {
                // Allow folder info everywhere
                true
            }
            DateFragment.FRAGMENT_CLASS -> {
                // Don't allow folder info
                false
            }
            else -> {
                false
            }
        }
    }

    // onActivityResultAction
    override fun onActivityResultAction(requestCode: Int, resultCode: Int, data: Intent?) {
        // Handle result if not back from SlideshowActivity
        when (requestCode) {
            REQUEST_CAMERA_ACTIVITY -> {
                if (resultCode == Activity.RESULT_OK) {
                    cameraLastFilePath = data?.getStringExtra(CameraActivity.LAST_FILE_PATH) ?: ""
                }
            }
            REQUEST_SETTINGS_ACTIVITY -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.getBooleanExtra(SettingsActivity.TO_UPDATE_DATA, false) == true) {
                        // Animate refresh, but allow delay until all fragments are ready after onResume to start update
                        swipeRefresh(false)
                    }
                    showFab(data?.getBooleanExtra(SettingsActivity.TO_SHOW_CAMERA_BUTTONS, true) == true)
                }
            }
        }
    }

    // fragmentOnStart
    override fun fragmentOnStart(tag: String, fragment: BaseFragment) {
        // Do nothing
    }

    // getCurrentGalleryBaseFragment
    override fun getCurrentGalleryBaseFragment(): GalleryBaseFragment? {
        return tabFragmentAdapter.getGalleryBaseFragment(currentTabPosition)
    }

    // onGalleryBaseFragmentReady
    override fun onGalleryBaseFragmentReady(tabPosition: Int, fragment: GalleryBaseFragment, isFirstReady: Boolean) {
        // When a new fragment is created and it is currently showing, update toolbar.
        // Instead of using 'fragment == getCurrentGalleryBaseFragment', just compare the position directly.
        // It was found that when recreating the activity after it was destroyed due to memory,
        // source code of onCreate() actually recreate the fragments and attach them to activity first,
        // without going through ViewPager or PagerAdapter. Hence, getCurrentGalleryBaseFragment() will be null here.
        // After that only instantiateItem of PagerAdapter is called and then the reference is ready.
        if (tabPosition == currentTabPosition) {
            updateToolbar(fragment)
        }
        // isRequestingPermission flag to prevent checkWritePermission to triggering non-stop as a loop because
        // onPause is called when a dialogue appears. This causes loop for
        // checkWritePermission --> deny --> showDialog (user does not have chance to dismiss) --> checkWritePermission
        // Also, moved 'isReady' of tabFragment from onPause to onStop instead.
        if (isFirstReady && tabFragmentAdapter.areFragmentsReady() && !isRequestingPermission) {
            checkWritePermission()
        }
    }

    // checkWritePermission
    private fun checkWritePermission() {
        // Update files if permission is valid
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            d("PERMISSION_GRANTED = true")
            updateFiles()
        } else {
            d("PERMISSION_GRANTED = false")
            isRequestingPermission = true
            // Else, request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE
            )
        }
    }

    // onRequestPermissionsResult
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // Proceed if permission granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isRequestingPermission = false
                    d("PERMISSION_GRANTED = true")
                    updateFiles()
                } else {
                    // Show permission dialog
                    d("PERMISSION_GRANTED = false")
                    val alertDialogBuilder = AlertDialog.Builder(ContextThemeWrapper(this, alertDialogTheme))
                    alertDialogBuilder.setMessage(R.string.permissionDialog_writeAccess_message)
                    alertDialogBuilder.setPositiveButton(android.R.string.ok) { _, _ ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                checkWritePermission()
                            } else {
                                // Redirect to app settings, shouldShowRequestPermissionRationale return false if user denied permission permanently
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivityForResult(intent, REQUEST_ACTION_APPLICATION_DETAILS_SETTINGS)
                            }
                        } else {
                            checkWritePermission()
                        }
                    }
                    alertDialogBuilder.setNegativeButton(android.R.string.cancel) { dialogInterface, _ ->
                        dialogInterface.cancel()
                    }
                    alertDialogBuilder.setOnCancelListener {
                        finish()
                    }
                    alertDialogBuilder.setOnDismissListener {
                        isRequestingPermission = false
                    }
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.show()
                }
            }
        }
    }

    // swipeRefresh
    override fun swipeRefresh(updateNow: Boolean) {
        tabFragmentAdapter.forEachGalleryBaseFragment { _, galleryBaseFragment ->
            galleryBaseFragment.animateSwipeRefresh()
        }
        // Make time to exceed updateTimeLimit
        lastUpdateTime -= 2 * updateTimeLimit
        if (updateNow) {
            updateFiles()
        }
    }

    // updateFiles
    private fun updateFiles() {
        // For first update, delay the splash until data is updated. Show hint.
        if (prefManager.get(PREF_IS_FIRST_UPDATE_DATA, true)) {
            firstUpdateText.isVisible = true
        } else {
            // Fragment already updated their items when initialising before calling onGalleryBaseFragmentReady.
            // As long as write permission is cleared, we can hide the splash immediately to be responsive.
            hideSplash(false)
            // Introduce file explorer mode for first time immediately if user switched mode
            showFileExplorerModeDialog()
        }
        when {
            // Force update if more than time limit
            System.currentTimeMillis() - lastUpdateTime > updateTimeLimit -> {
                d("updateTimeLimit exceeded")
                FileIntentService.updateFiles(applicationContext)
            }
            // If file IO operation is carried out, but activity was in background and didn't receive broadcast
            lastUpdateTime != PholderDatabase.getLastUpdateTime() -> {
                d("UI showing older data")
                filesUpdated()
            }
            // Just resume from onPause and there was no change, call to force trigger whichever callback which should be called
            // when activity has focus e.g. user navigated away while swipeRefresh still ongoing.
            else -> {
                d("UI showing latest data")
                filesUpdated()
            }
        }
    }

    // filesUpdated
    private fun filesUpdated() {
        lastUpdateTime = PholderDatabase.getLastUpdateTime()
        d("lastUpdateTime = ${PholderTagUtil.millisToDateTime(lastUpdateTime)}")
        // Scroll to latest position if just back from CameraActivity
        val scrollToUid = cameraLastFilePath
        cameraLastFilePath = ""
        tabFragmentAdapter.forEachGalleryBaseFragment { _, galleryBaseFragment ->
            galleryBaseFragment.updateItems(true, scrollToUid)
        }
        // After data is updated for first time, hide splash
        if (prefManager.get(PREF_IS_FIRST_UPDATE_DATA, true)) {
            prefManager.put(PREF_IS_FIRST_UPDATE_DATA, false)
            firstUpdateText.text = getString(R.string.galleryActivity_firstUpdate_done)
            hideSplash(true)
            // Introduce browsing mode for first time after splash is hidden
            val handler = Handler()
            val runnable = Runnable { showAlbumModeDialog() }
            handler.postDelayed(runnable, hideSplashDelay + 100)
        }
    }

    // showAlbumModeDialog
    private fun showAlbumModeDialog() {
        if (browsingMode == PREF_LIST_BROWSING_MODE_ALBUM &&
            !prefManager.get(PREF_SHOWED_ALBUM_MODE_DIALOG, false)
        ) {
            prefManager.put(PREF_SHOWED_ALBUM_MODE_DIALOG, true)
            val alertDialogBuilder = AlertDialog.Builder(ContextThemeWrapper(this, alertDialogTheme))
            alertDialogBuilder.setTitle(getString(R.string.alertDialog_browsing_mode_album_title))
            alertDialogBuilder.setMessage(getString(R.string.alertDialog_browsing_mode_album_message))
            alertDialogBuilder.setPositiveButton(android.R.string.ok, null)
            alertDialogBuilder.show()
        }
    }

    // showFileExplorerModeDialog
    private fun showFileExplorerModeDialog() {
        if (browsingMode == PREF_LIST_BROWSING_MODE_FILE_EXPLORER &&
            !prefManager.get(PREF_SHOWED_FILE_EXPLORER_MODE_DIALOG, false)
        ) {
            prefManager.put(PREF_SHOWED_FILE_EXPLORER_MODE_DIALOG, true)
            val alertDialogBuilder = AlertDialog.Builder(ContextThemeWrapper(this, alertDialogTheme))
            alertDialogBuilder.setTitle(getString(R.string.alertDialog_browsing_mode_file_explorer_title))
            alertDialogBuilder.setMessage(getString(R.string.alertDialog_browsing_mode_file_explorer_message))
            alertDialogBuilder.setPositiveButton(android.R.string.ok, null)
            alertDialogBuilder.show()
        }
    }

    // startSelectionMode
    override fun startSelectionMode() {
        lockableViewPager.canSwipe = false
        showFab(false)
    }

    // onSelectionUpdated
    override fun onSelectionUpdated() {
        updateToolbar(getCurrentGalleryBaseFragment())
    }

    // endSelectionMode
    override fun endSelectionMode() {
        lockableViewPager.canSwipe = true
        updateToolbar(getCurrentGalleryBaseFragment())
        showFab(true)
    }

    // onDialogAction
    override fun onDialogAction(action: Int, dialogFragment: BaseDialogFragment, data: Bundle?) {
        when (dialogFragment.dialogType) {
            ConfirmationDialog.DIALOG_DELETE -> {
                if (action == BaseDialogFragment.CLICK_POSITIVE) {
                    deleteFiles()
                }
            }
            FolderPickerDialog.DIALOG_THUMBNAIL -> {
                if (action == BaseDialogFragment.CLICK_POSITIVE) {
                    dialogFragment as FolderPickerDialog
                    when (dialogFragment.requestCode) {
                        REQUEST_CODE_FOLDER_PICKER_DIALOG_MOVE -> {
                            moveFiles(dialogFragment.getDestinationRootPath())
                        }
                        REQUEST_CODE_FOLDER_PICKER_DIALOG_CAMERA -> {
                            val intent = CameraActivity.newIntent(
                                this@GalleryActivity, dialogFragment.getDestinationRootPath(), isImageCamera
                            )
                            startActivityForResult(intent, REQUEST_CAMERA_ACTIVITY)
                        }
                    }
                }
            }
            FolderNameDialog.DIALOG_RENAME -> {
                if (action == BaseDialogFragment.CLICK_POSITIVE) {
                    dialogFragment as FolderNameDialog
                    renameFiles(dialogFragment.folderName)
                }
            }
            SortDialog.DIALOG_SORT -> {
                if (action == BaseDialogFragment.CLICK_POSITIVE) {
                    dialogFragment as SortDialog
                    if (dialogFragment.hasSortOrderChanged) {
                        tabFragmentAdapter.forEachGalleryBaseFragment { _, galleryBaseFragment ->
                            // Only animate if size is acceptable
                            val calculateDiff = galleryBaseFragment.getItemSize() <= 200
                            galleryBaseFragment.updateItems(calculateDiff, "")
                        }
                    }
                }
            }
        }
    }

    // createFolder
    private fun createFolder() {
        val fragment = getCurrentGalleryBaseFragment()
        if (fragment != null) {
            if (canCreateFolder(fragment)) {
                var rootPath = fragment.getRootPath()
                // Replace album root with Pholder folder
                if (rootPath == ALBUM_ROOT.absolutePath) {
                    rootPath = PHOLDER_FOLDER.absolutePath
                }
                val folderNameDialog = FolderNameDialog.newCreateDialog(rootPath)
                folderNameDialog.show(supportFragmentManager)
                shortToast(
                    preResId = R.string.toast_createFolder_location_pre,
                    message = rootPath,
                    postResId = R.string.toast_createFolder_location_post
                )
            } else {
                snackProgressBarManager.shortSnackBar(this, R.string.toast_createFolder_warn_not_allowed)
            }
        }
    }

    // folderCreated
    private fun folderCreated(isFolderCreated: Boolean, folderPath: String) {
        if (isFolderCreated) {
            lastUpdateTime = PholderDatabase.getLastUpdateTime()
            tabFragmentAdapter.forEachGalleryBaseFragment { _, galleryBaseFragment ->
                galleryBaseFragment.updateItems(true, folderPath, true, true)
            }
            snackProgressBarManager.shortSnackBar(this, R.string.toast_createFolder_ok)
        } else {
            snackProgressBarManager.shortSnackBar(this, R.string.toast_createFolder_failed)
        }
    }

    // starFolders
    private fun starFolders(toStar: Boolean) {
        val fragment = getCurrentGalleryBaseFragment()
        if (fragment != null) {
            val selectedFolderTags = fragment.getSelectedFolderTags()
            if (selectedFolderTags.isNotEmpty()) {
                FileIntentService.starFolders(this, toStar, selectedFolderTags)
            }
            fragment.endSelectionMode()
        }
    }

    // deleteFiles
    private fun deleteFiles() {
        val fragment = getCurrentGalleryBaseFragment()
        if (fragment != null) {
            val selectedTags = fragment.getSelectedTags()
            FileIntentService.deleteFiles(applicationContext, selectedTags)
            fragment.endSelectionMode()
        }
    }

    // filesDeleted
    private fun filesDeleted(resultPairs: Array<Pair<String, Int>>) {
        var failedCount = 0
        var failedPath = ""
        var failedResult = 0
        resultPairs.forEach { resultPair ->
            if (resultPair.second != FileIntentService.ACTION_STATUS_OK) {
                failedCount++
                failedPath = resultPair.first
                failedResult = resultPair.second
                d("delete failed = $failedPath, result = $failedResult")
            }
        }
        if (failedCount > 1) {
            if (PholderTagUtil.isImage(failedPath) || PholderTagUtil.isVideo(failedPath)) {
                snackProgressBarManager.longSnackBar(this, R.string.toast_deleteFile_file_failed)
            } else {
                snackProgressBarManager.longSnackBar(this, R.string.toast_deleteFile_folder_failed)
            }
        } else if (failedCount == 1) {
            val fileName = PholderTagUtil.getFileNameWithExtension(failedPath)
            when (failedResult) {
                FileIntentService.ACTION_STATUS_SYSTEM_FOLDER_NOT_ALLOWED -> {
                    snackProgressBarManager.shortSnackBar(
                        this,
                        preResId = R.string.toast_deleteFile_warn_system_not_allowed_pre,
                        message = fileName,
                        postResId = R.string.toast_deleteFile_warn_system_not_allowed_post
                    )
                }
                FileIntentService.ACTION_STATUS_FAILED -> {
                    snackProgressBarManager.shortSnackBar(
                        this,
                        preResId = R.string.toast_deleteFile_failed_pre,
                        message = fileName,
                        postResId = R.string.toast_deleteFile_failed_post
                    )
                }
            }
        } else {
            snackProgressBarManager.shortSnackBar(this, R.string.toast_deleteFile_ok)
        }
        filesUpdated()
    }

    // moveFiles
    private fun moveFiles(destinationPath: String) {
        val fragment = getCurrentGalleryBaseFragment()
        if (fragment != null) {
            val rootPath = fragment.getRootPath()
            val selectedTags = fragment.getSelectedTags()
            if (destinationPath != rootPath) {
                FileIntentService.moveFiles(
                    applicationContext,
                    destinationPath,
                    selectedTags
                )
            }
            fragment.endSelectionMode()
        }
    }

    // filesMoved
    private fun filesMoved(resultPairs: Array<Pair<String, Int>>) {
        var failedCount = 0
        var failedPath = ""
        var failedResult = 0
        resultPairs.forEach { resultPair ->
            if (resultPair.second != FileIntentService.ACTION_STATUS_OK) {
                failedCount++
                failedPath = resultPair.first
                failedResult = resultPair.second
                d("move failed = $failedPath, result = $failedResult")
            }
        }
        when {
            failedCount > 1 -> {
                snackProgressBarManager.shortSnackBar(this, R.string.toast_move_failed)
            }
            failedCount == 1 -> {
                val fileName = PholderTagUtil.getFileNameWithExtension(failedPath)
                when (failedResult) {
                    FileIntentService.ACTION_STATUS_SYSTEM_FOLDER_NOT_ALLOWED -> {
                        snackProgressBarManager.shortSnackBar(
                            this,
                            preResId = R.string.toast_move_warn_system_not_allowed_pre,
                            message = fileName,
                            postResId = R.string.toast_move_warn_system_not_allowed_post
                        )
                    }
                    FileIntentService.ACTION_STATUS_MOVE_INTO_SELF -> {
                        snackProgressBarManager.shortSnackBar(
                            this,
                            preResId = R.string.toast_move_warn_self_pre,
                            message = fileName,
                            postResId = R.string.toast_move_warn_self_post
                        )
                    }
                    FileIntentService.ACTION_STATUS_FILE_COLLISION -> {
                        snackProgressBarManager.shortSnackBar(
                            this,
                            preResId = R.string.toast_move_warn_exist_pre,
                            message = fileName,
                            postResId = R.string.toast_move_warn_exist_post
                        )
                    }
                    else -> {
                        snackProgressBarManager.shortSnackBar(
                            this,
                            preResId = R.string.toast_move_failed_pre,
                            message = fileName,
                            postResId = R.string.toast_move_failed_post
                        )
                    }
                }
            }
            else -> {
                snackProgressBarManager.shortSnackBar(this, R.string.toast_move_ok)
            }
        }
        filesUpdated()
    }

    // renameFiles
    private fun renameFiles(folderName: String) {
        val fragment = getCurrentGalleryBaseFragment()
        if (fragment != null) {
            val selectedTags = fragment.getSelectedTags()
            FileIntentService.renameFiles(
                applicationContext,
                folderName,
                selectedTags
            )
            fragment.endSelectionMode()
        }
    }

    // filesRenamed
    private fun filesRenamed(resultPairs: Array<Pair<String, Int>>) {
        var failedCount = 0
        var failedPath = ""
        var failedResult = 0
        resultPairs.forEach { resultPair ->
            if (resultPair.second != FileIntentService.ACTION_STATUS_OK) {
                failedCount++
                failedPath = resultPair.first
                failedResult = resultPair.second
                d("rename failed = $failedPath, result = $failedResult")
            }
        }
        when {
            failedCount > 1 -> {
                snackProgressBarManager.shortSnackBar(this, R.string.toast_rename_failed)
            }
            failedCount == 1 -> {
                val fileName = PholderTagUtil.getFileNameWithExtension(failedPath)
                when (failedResult) {
                    FileIntentService.ACTION_STATUS_SYSTEM_FOLDER_NOT_ALLOWED -> {
                        snackProgressBarManager.shortSnackBar(
                            this,
                            preResId = R.string.toast_rename_warn_system_not_allowed_pre,
                            message = fileName,
                            postResId = R.string.toast_rename_warn_system_not_allowed_post
                        )
                    }
                    else -> {
                        snackProgressBarManager.shortSnackBar(
                            this,
                            preResId = R.string.toast_rename_failed_pre,
                            message = fileName,
                            postResId = R.string.toast_rename_failed_post
                        )
                    }
                }
            }
            else -> {
                snackProgressBarManager.shortSnackBar(this, R.string.toast_rename_ok)
            }
        }
        filesUpdated()
    }

    // startSlideshowPreparation
    override fun startSlideshowPreparation(filePath: String, view: View, fileTags: List<FileTag>) {
        showFab(false)
    }

    // slideshowFileNotExist
    override fun slideshowFileNotExist(filePath: String) {
        snackProgressBarManager.shortSnackBar(this, R.string.toast_startSlideshow_file_not_exist)
    }

    // postSlideshowBackTransition
    override fun postSlideshowBackTransition() {
        showFab(true)
    }

    // onPauseAction
    override fun onPauseAction() {
        // Reset backPress if navigated away
        wasBackPressed = false
        backPressHandler.removeCallbacks(backPressRunnable)
    }

    // onSaveInstanceState
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVED_CURRENT_TAB_POSITION, currentTabPosition)
        outState.putLong(SAVED_LAST_UPDATE_TIME, lastUpdateTime)
    }

    // onStopAction
    override fun onStopAction() {
        // In case GalleryActivity is destroyed when in SlideshowActivity due to Android releasing memory,
        // GalleryActivity is recreated by OnCreate -> OnStart -> OnResume -> OnPause -> OnActivityReenter
        // If clear fragmentReady in onPause, it will cause filesUpdated() call to be ineffective.
        unregisterLocalBroadcastReceiver(broadcastReceiver)
    }

    // onBackPressed
    override fun onBackPressed() {
        if (getCurrentGalleryBaseFragment()?.onBackPressed() == false) {
            // Check if exit confirmation is required and is first backPress
            if (prefManager.get(PREF_USE_EXIT_CONFIRMATION, true) && !wasBackPressed) {
                wasBackPressed = true
                // Inform user to click back again
                shortToast(R.string.toast_exit_confirmation)
                // Reset after some duration
                backPressHandler.postDelayed(backPressRunnable, 3000L)
            } else {
                super.onBackPressed()
            }
        }
    }

}
