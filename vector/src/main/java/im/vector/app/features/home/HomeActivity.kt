/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.SpaceStateHandler
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.extensions.restart
import im.vector.app.core.extensions.validateBackPressed
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.ActivityHomeBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.analytics.accountdata.AnalyticsAccountDataViewModel
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.disclaimer.DisclaimerDialog
import im.vector.app.features.home.room.list.actions.RoomListSharedAction
import im.vector.app.features.home.room.list.actions.RoomListSharedActionViewModel
import im.vector.app.features.home.room.list.home.layout.HomeLayoutSettingBottomDialogFragment
import im.vector.app.features.home.room.list.home.release.ReleaseNotesActivity
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.matrixto.OriginOfMatrixTo
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.onboarding.AuthenticationDescription
import im.vector.app.features.permalink.NavigationInterceptor
import im.vector.app.features.permalink.PermalinkHandler
import im.vector.app.features.permalink.PermalinkHandler.Companion.MATRIX_TO_CUSTOM_SCHEME_URL_BASE
import im.vector.app.features.permalink.PermalinkHandler.Companion.ROOM_LINK_PREFIX
import im.vector.app.features.permalink.PermalinkHandler.Companion.USER_LINK_PREFIX
import im.vector.app.features.popup.DefaultVectorAlert
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.popup.VerificationVectorAlert
import im.vector.app.features.rageshake.ReportType
import im.vector.app.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.spaces.SpaceCreationActivity
import im.vector.app.features.spaces.SpacePreviewActivity
import im.vector.app.features.spaces.SpaceSettingsMenuBottomSheet
import im.vector.app.features.spaces.invite.SpaceInviteBottomSheet
import im.vector.app.features.spaces.share.ShareSpaceBottomSheet
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.usercode.UserCodeActivity
import im.vector.app.features.workers.signout.ServerBackupStatusViewModel
import im.vector.app.timeshare.MovableFloatingActionButton
import im.vector.app.timeshare.TSSessionManager
import im.vector.app.timeshare.TSUtils.MYUtil.random
import im.vector.app.timeshare.TSUtils.MyDialog
import im.vector.app.timeshare.api_request_body.GetCategoryRequest
import im.vector.app.timeshare.api_response_body.GetCategoryResponse
import im.vector.app.timeshare.api_response_body.UploadImage
import im.vector.app.timeshare.auth.EmailVerifyActivity
import im.vector.app.timeshare.categ.Category
import im.vector.app.timeshare.categ.CategoryActivity
import im.vector.app.timeshare.categ.SingleRecyclerViewAdapter
import im.vector.app.timeshare.categ.SubCategory
import im.vector.app.timeshare.categ.SubCategoryActivity
import im.vector.app.timeshare.categ.SubCategorySingleSelectionAdapter
import im.vector.app.timeshare.friends.FriendsFragment
import im.vector.app.timeshare.home.HomeFragment
import im.vector.app.timeshare.menu.MenuFragment
import im.vector.app.timeshare.myactivities.ImageListAdapter
import im.vector.app.timeshare.webservices.ApiUtils
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import org.matrix.android.sdk.api.session.sync.initialSyncStrategy
import org.matrix.android.sdk.api.util.MatrixItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

@Parcelize
data class HomeActivityArgs(
        val clearNotification: Boolean,
        val authenticationDescription: AuthenticationDescription? = null,
        val hasExistingSession: Boolean = false,
        val inviteNotificationRoomId: String? = null
) : Parcelable

@AndroidEntryPoint
class HomeActivity :
        VectorBaseActivity<ActivityHomeBinding>(),
        NavigationInterceptor,
        SpaceInviteBottomSheet.InteractionListener,
        MatrixToBottomSheet.InteractionListener,
        View.OnClickListener,
        SingleRecyclerViewAdapter.SingleClickListener,
        SubCategorySingleSelectionAdapter.SingleClickListener,
        VectorMenuProvider {

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel
    private lateinit var roomListSharedActionViewModel: RoomListSharedActionViewModel

    private val homeActivityViewModel: HomeActivityViewModel by viewModel()

    @Suppress("UNUSED")
    private val analyticsAccountDataViewModel: AnalyticsAccountDataViewModel by viewModel()

    @Suppress("UNUSED")
    private val userColorAccountDataViewModel: UserColorAccountDataViewModel by viewModel()

    private val serverBackupStatusViewModel: ServerBackupStatusViewModel by viewModel()

    @Inject lateinit var vectorUncaughtExceptionHandler: VectorUncaughtExceptionHandler
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var popupAlertManager: PopupAlertManager
    @Inject lateinit var shortcutsHandler: ShortcutsHandler
    @Inject lateinit var permalinkHandler: PermalinkHandler
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var initSyncStepFormatter: InitSyncStepFormatter
    @Inject lateinit var spaceStateHandler: SpaceStateHandler
    @Inject lateinit var unifiedPushHelper: UnifiedPushHelper
    @Inject lateinit var nightlyProxy: NightlyProxy
    @Inject lateinit var disclaimerDialog: DisclaimerDialog
    @Inject lateinit var notificationPermissionManager: NotificationPermissionManager

    private var isNewAppLayoutEnabled: Boolean = false // delete once old app layout is removed

    private val createSpaceResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val spaceId = SpaceCreationActivity.getCreatedSpaceId(activityResult.data)
            val defaultRoomId = SpaceCreationActivity.getDefaultRoomId(activityResult.data)
            val isJustMe = SpaceCreationActivity.isJustMeSpace(activityResult.data)
            views.drawerLayout.closeDrawer(GravityCompat.START)

            val postSwitchOption: Navigator.PostSwitchSpaceAction = if (defaultRoomId != null) {
                Navigator.PostSwitchSpaceAction.OpenDefaultRoom(defaultRoomId, !isJustMe)
            } else if (isJustMe) {
                Navigator.PostSwitchSpaceAction.OpenAddExistingRooms
            } else {
                Navigator.PostSwitchSpaceAction.None
            }
            // Here we want to change current space to the newly created one, and then immediately open the default room
            if (spaceId != null) {
                navigator.switchToSpace(
                        context = this,
                        spaceId = spaceId,
                        postSwitchOption,
                )
                roomListSharedActionViewModel.post(RoomListSharedAction.CloseBottomSheet)
            }
        }
    }

    private val postPermissionLauncher = registerForPermissionsResult { _, _ ->
        // Nothing to do with the result.
    }

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            if (f is MatrixToBottomSheet) {
                f.interactionListener = this@HomeActivity
            }
            super.onFragmentResumed(fm, f)
        }

        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
            if (f is MatrixToBottomSheet) {
                f.interactionListener = null
            }
            super.onFragmentPaused(fm, f)
        }
    }

    private val drawerListener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerOpened(drawerView: View) {
            analyticsTracker.screen(MobileScreen(screenName = MobileScreen.ScreenName.Sidebar))
        }

        override fun onDrawerStateChanged(newState: Int) {
            hideKeyboard()
        }
    }

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun getBinding() = ActivityHomeBinding.inflate(layoutInflater)
    var tsSessionManager: TSSessionManager? = null
    val mAPIService = ApiUtils.getAPIService()
  //  lateinit var fab:MovableFloatingActionButton
     lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    val cameraRequest = 100
    val PICK_IMAGE_MULTIPLE = 101
    var gallerybitmap: Bitmap? = null
    var filePart: MultipartBody.Part? = null
    var filePartList: List<MultipartBody.Part> = ArrayList<MultipartBody.Part>()
    var call: Call<UploadImage>? = null

    var myDialog:MyDialog?=null
    var edit_venu: EditText? = null
    var calendarView_startdate: CalendarView? = null
    var calendarView_enddate: CalendarView? = null
    var rl_select_category: RelativeLayout? = null
    var rl_select_subcategory:RelativeLayout? = null
    var ll_category_space: LinearLayout? = null
    var ll_subcategory_space:LinearLayout? = null
    var ll_start_time: LinearLayout? = null
    var ll_end_time:LinearLayout? = null
    var timePicker1: TimePicker? = null
    var timePicker2:TimePicker? = null
    var tv_time_cancel1: TextView? = null
    var tv_time_cancel2:TextView? = null
    var tv_time_ok1:TextView? = null
    var tv_time_ok2:TextView? = null
    var ll_calendar_view_start_date: LinearLayout? = null
    var ll_calendar_view_end_date:android.widget.LinearLayout? = null
    var tv_calendar_cancel1: TextView? = null
    var tv_calendar_ok1:TextView? = null
    var tv_calendar_cancel2:TextView? = null
    var tv_calendar_ok2:TextView? = null
    var tv_activity_start_date: TextView? = null
    var tv_activity_end_date:TextView? = null
    var iv_close: ImageView? = null
    var img_select_start_date:ImageView? = null
    var img_select_end_date:ImageView? = null

    var rv_category: RecyclerView? = null
    var rv_spinnersubcategory:RecyclerView? = null
    var categoryList: ArrayList<Category> = ArrayList<Category>()
    var subCategoryList: ArrayList<SubCategory> = ArrayList<SubCategory>()
    var singleRecyclerViewAdapter: SingleRecyclerViewAdapter? = null
    var subCategorySingleSelectionAdapter: SubCategorySingleSelectionAdapter? = null
    var et_limits: EditText? = null
    var switch_limits: Switch? = null
    var switch_make_public:Switch? = null

    var activityName: String? = null
    var activityDescription:String? = null
    var numberOfFiles:String? = "1"
    var categoryName:String? = ""
    var subCategory:String? = ""
    var userUuid:String? = null
    var email_id:String? = null
    var postLocation:String? = null
    var startDate:String? = null
    var endDate:String? = null
    var email:String? = null

    //multipart enstances
    var activityname: RequestBody? = null
    var activitydesc:RequestBody? = null
    var numberoffiles:RequestBody? = null
    var category:RequestBody? = null
    var subcategory:RequestBody? = null
    var user_uuid:RequestBody? = null
    var location:RequestBody? = null
    var start_date_and_time:RequestBody? = null
    var end_date_and_time:RequestBody? = null


    var isHome = false
    var isFriend = false
    var isChat = false
    var isMenu = false
    var iv_home: ImageView? = null
    var iv_users:ImageView? = null
    var iv_chat:ImageView? = null
    var iv_menu:ImageView? = null
//    lateinit var mHomeSelected:View
//    lateinit var mUsersSelected:View
//    lateinit var mChatSelected:View
//    lateinit var mMenuSelected:View



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isNewAppLayoutEnabled = vectorPreferences.isNewAppLayoutEnabled()
        analyticsScreenName = MobileScreen.ScreenName.Home
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
        sharedActionViewModel = viewModelProvider[HomeSharedActionViewModel::class.java]
        roomListSharedActionViewModel = viewModelProvider[RoomListSharedActionViewModel::class.java]

        tsSessionManager = TSSessionManager(this@HomeActivity)
        iv_home = findViewById(R.id.iv_home)
        iv_users = findViewById(R.id.iv_users)
        iv_chat = findViewById(R.id.iv_chat)
        iv_menu = findViewById(R.id.iv_menu)
      //  fab = findViewById(R.id.fab_add_activity)

        views.drawerLayout.addDrawerListener(drawerListener)
        if (isFirstCreation()) {
            if (vectorPreferences.isNewAppLayoutEnabled()) {
                views.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
               // replaceFragment(views.homeDetailFragmentContainer, NewHomeDetailFragment::class.java)
                replaceFragment(views.homeDetailFragmentContainer, HomeFragment::class.java)
            } else {
                replaceFragment(views.homeDetailFragmentContainer, HomeDetailFragment::class.java)
                replaceFragment(views.homeDrawerFragmentContainer, HomeDrawerFragment::class.java)
                views.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }

        if(tsSessionManager!!.isEmailVerified()) {

            if (tsSessionManager!!.isLoggedIn()) {

                var user: HashMap<String, String>
                user = tsSessionManager!!.getUserDetails()
                userUuid = user.get(TSSessionManager.KEY_user_uuid)
                email_id = user.get(TSSessionManager.KEY_email_id)

              //  System.out.println("email_id>>" + email_id)
               // System.out.println("subcateg>>" + tsSessionManager!!.isSubCategory)

                if (tsSessionManager!!.isCategory && !tsSessionManager!!.isSubCategory) {
                    MaterialAlertDialogBuilder(this@HomeActivity)
                            .setTitle("Alert!")
                            .setCancelable(false)
                            .setMessage("Please click on 'continue' to add category and sub-category.")
                            .setPositiveButton("Continue", { dialog, _ ->
                                val intent = Intent(applicationContext, SubCategoryActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                               // intent.putExtra("email",email_id)
                                applicationContext.startActivity(intent)
                                dialog.dismiss()
                            })
                            .show()
                } else if (!tsSessionManager!!.isCategory && !tsSessionManager!!.isSubCategory) {
                    MaterialAlertDialogBuilder(this@HomeActivity)
                            .setTitle("Alert!")
                            .setCancelable(false)
                            .setMessage("Please click on 'continue' to add category and sub-category.")
                            .setPositiveButton("Continue", { dialog, _ ->
                                val intent = Intent(applicationContext, CategoryActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                intent.putExtra("email",email_id)
                                applicationContext.startActivity(intent)
                                dialog.dismiss()
                            })
                            .show()
                }
            }
        } else {
            var user: HashMap<String, String>
            user = tsSessionManager!!.getUserEmail()
           val email = user.get(TSSessionManager.KEY_email)
            Log.d("email>>",""+email)
            MaterialAlertDialogBuilder(this@HomeActivity)
                    .setTitle("Alert!")
                    .setCancelable(false)
                    .setMessage("Please verify your email")
                    .setPositiveButton("Continue", { dialog, _ ->
                        val intent = Intent(applicationContext, EmailVerifyActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.putExtra("email", email)
                        applicationContext.startActivity(intent)
                        dialog.dismiss()
                    })
                    .show()
        }


       // Log.d("userid>>",""+userUuid)
      //  Log.d("email_id>>",""+email_id)

      /*  fab.setOnClickListener {
            startCreatingNewEvents()
           // val intent = Intent(this, PhotoActivity::class.java)
           // startActivity(intent)
        }*/

        // Get view id's and perform listener's for Bottom items
        val rl_home = findViewById<RelativeLayout>(R.id.rl_home)
        rl_home.setOnClickListener {
            if (isFirstCreation()) {
                if (vectorPreferences.isNewAppLayoutEnabled()) {
                    isHome = true
                    isFriend = false
                    isChat = false
                    isMenu = false

                    iv_home?.setImageResource(R.drawable.ic_home_selected)
                    iv_users?.setImageResource(R.drawable.ic_users)
                    iv_chat?.setImageResource(R.drawable.ic_chat_ts)
                    iv_menu?.setImageResource(R.drawable.ic_menu)


//                    mHomeSelected?.visibility = View.VISIBLE
//                    mUsersSelected?.visibility = View.GONE
//                    mChatSelected?.visibility = View.GONE
//                    mMenuSelected?.visibility = View.GONE



                    views.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    replaceFragment(views.homeDetailFragmentContainer, HomeFragment::class.java)
                }
            }
        }

        val rl_users = findViewById<RelativeLayout>(R.id.rl_users);
        rl_users.setOnClickListener {
            if (isFirstCreation()) {
                if (vectorPreferences.isNewAppLayoutEnabled()) {
                    isHome = false
                    isFriend = true
                    isChat = false
                    isMenu = false

                    iv_home?.setImageResource(R.drawable.ic_home)
                    iv_users?.setImageResource(R.drawable.ic_users_selected)
                    iv_chat?.setImageResource(R.drawable.ic_chat_ts)
                    iv_menu?.setImageResource(R.drawable.ic_menu)

//                    mHomeSelected?.visibility = View.GONE
//                    mUsersSelected?.visibility = View.VISIBLE
//                    mChatSelected?.visibility = View.GONE
//                    mMenuSelected?.visibility = View.GONE
                    views.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    replaceFragment(views.homeDetailFragmentContainer, FriendsFragment::class.java)
                }
            }
        }
        val rl_chat = findViewById<RelativeLayout>(R.id.rl_chat);
        rl_chat.setOnClickListener {
            if (isFirstCreation()) {
                if (vectorPreferences.isNewAppLayoutEnabled()) {
                    isHome = false
                    isFriend = false
                    isChat = true
                    isMenu = false

                    iv_home?.setImageResource(R.drawable.ic_home)
                    iv_users?.setImageResource(R.drawable.ic_users)
                    iv_chat?.setImageResource(R.drawable.ic_chat_selected)
                    iv_menu?.setImageResource(R.drawable.ic_menu)


//                    mHomeSelected?.visibility = View.GONE
//                    mUsersSelected?.visibility = View.GONE
//                    mChatSelected?.visibility = View.VISIBLE
//                    mMenuSelected?.visibility = View.GONE
                    views.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                   // replaceFragment(views.homeDetailFragmentContainer, ChatFragment::class.java)
                    replaceFragment(views.homeDetailFragmentContainer, NewHomeDetailFragment::class.java)
                }
            }
        }

        val rl_menu = findViewById<RelativeLayout>(R.id.rl_menu);
        rl_menu.setOnClickListener {
            if (isFirstCreation()) {
                if (vectorPreferences.isNewAppLayoutEnabled()) {
                    isHome = false
                    isFriend = false
                    isChat = false
                    isMenu = true

                    iv_home?.setImageResource(R.drawable.ic_home)
                    iv_users?.setImageResource(R.drawable.ic_users)
                    iv_chat?.setImageResource(R.drawable.ic_chat_ts)
                    iv_menu?.setImageResource(R.drawable.ic_menu_selected)


//                    mHomeSelected?.visibility = View.GONE
//                    mUsersSelected?.visibility = View.GONE
//                    mChatSelected?.visibility = View.GONE
//                    mMenuSelected?.visibility = View.VISIBLE
                    views.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    replaceFragment(views.homeDetailFragmentContainer, MenuFragment::class.java)
                }
            }
        }


        sharedActionViewModel
                .stream()
                .onEach { sharedAction ->
                    when (sharedAction) {
                        is HomeActivitySharedAction.OpenDrawer -> views.drawerLayout.openDrawer(GravityCompat.START)
                        is HomeActivitySharedAction.CloseDrawer -> views.drawerLayout.closeDrawer(GravityCompat.START)
                        is HomeActivitySharedAction.OpenSpacePreview -> startActivity(SpacePreviewActivity.newIntent(this, sharedAction.spaceId))
                        is HomeActivitySharedAction.AddSpace -> createSpaceResultLauncher.launch(SpaceCreationActivity.newIntent(this))
                        is HomeActivitySharedAction.ShowSpaceSettings -> showSpaceSettings(sharedAction.spaceId)
                        is HomeActivitySharedAction.OpenSpaceInvite -> openSpaceInvite(sharedAction.spaceId)
                        HomeActivitySharedAction.SendSpaceFeedBack -> bugReporter.openBugReportScreen(this, ReportType.SPACE_BETA_FEEDBACK)
                        HomeActivitySharedAction.OnCloseSpace -> onCloseSpace()
                    }
                }
                .launchIn(lifecycleScope)

        val args = intent.getParcelableExtraCompat<HomeActivityArgs>(Mavericks.KEY_ARG)

        if (args?.clearNotification == true) {
            notificationDrawerManager.clearAllEvents()
        }
        if (args?.inviteNotificationRoomId != null) {
            activeSessionHolder.getSafeActiveSession()?.permalinkService()?.createPermalink(args.inviteNotificationRoomId)?.let {
                navigator.openMatrixToBottomSheet(this, it, OriginOfMatrixTo.NOTIFICATION)
            }
        }

        homeActivityViewModel.observeViewEvents {
          /*  when (it) {
                is HomeActivityViewEvents.AskPasswordToInitCrossSigning -> handleAskPasswordToInitCrossSigning(it)
                is HomeActivityViewEvents.CurrentSessionNotVerified -> handleOnNewSession(it)
                is HomeActivityViewEvents.CurrentSessionCannotBeVerified -> handleCantVerify(it)
                HomeActivityViewEvents.PromptToEnableSessionPush -> handlePromptToEnablePush()
                HomeActivityViewEvents.StartRecoverySetupFlow -> handleStartRecoverySetup()
                is HomeActivityViewEvents.ForceVerification -> {
                    if (it.sendRequest) {
                        navigator.requestSelfSessionVerification(this)
                    } else {
                        navigator.waitSessionVerification(this)
                    }
                }
                is HomeActivityViewEvents.OnCrossSignedInvalidated -> handleCrossSigningInvalidated(it)
                HomeActivityViewEvents.ShowAnalyticsOptIn -> handleShowAnalyticsOptIn()
                HomeActivityViewEvents.ShowNotificationDialog -> handleShowNotificationDialog()
                HomeActivityViewEvents.ShowReleaseNotes -> handleShowReleaseNotes()
                HomeActivityViewEvents.NotifyUserForThreadsMigration -> handleNotifyUserForThreadsMigration()
                is HomeActivityViewEvents.MigrateThreads -> migrateThreadsIfNeeded(it.checkSession)
                is HomeActivityViewEvents.AskUserForPushDistributor -> askUserToSelectPushDistributor()
            }*/
        }
        homeActivityViewModel.onEach { renderState(it) }

        shortcutsHandler.observeRoomsAndBuildShortcuts(lifecycleScope)

        if (isFirstCreation()) {
            handleIntent(intent)
        }
        homeActivityViewModel.handle(HomeActivityViewActions.ViewStarted)



    }

    private fun startCreatingNewEvents() {

         val builder = BottomSheetDialog(this);
        builder.setCancelable(false);

        val bottomSheet = LayoutInflater.from(this).inflate(R.layout.layout_start_activity, null);
        builder.setContentView(bottomSheet);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
       // val displayMetrics:DisplayMetrics = getResources().getDisplayMetrics();
     //   val height:Int = displayMetrics.heightPixels;
      //  val maxHeight:Int = (height*0.93).toInt();
      //  bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
       // bottomSheetBehavior.setPeekHeight(maxHeight);
        val tv_continue = builder.findViewById<TextView>(R.id.tv_continue);
        val iv_close = builder.findViewById<ImageView>(R.id.iv_close_activity);
        val et_activity_name = builder.findViewById<EditText>(R.id.et_activity_name);
        val et_activity_desc = builder.findViewById<EditText>(R.id.et_activity_desc);

        tv_continue?.setOnClickListener {
            activityName = et_activity_name?.getText().toString().trim();
            activityDescription = et_activity_desc?.getText().toString().trim();
            if (validateActivity(activityName.toString(), activityDescription.toString(),et_activity_name,et_activity_desc)) {
               // activityname = RequestBody.create("text/plain".toMediaTypeOrNull(), activityName.toString());
               // activitydesc = RequestBody.create("text/plain".toMediaTypeOrNull(), activityDescription.toString());
                uploadMediaDialog();
                builder.dismiss()
            }

        }

        iv_close?.setOnClickListener {

            builder.dismiss()
        }


        builder.show();
    }

    private fun uploadMediaDialog() {
        val builder = BottomSheetDialog(this);
        builder.setCancelable(false);

        val bottomSheet = LayoutInflater.from(this).inflate(R.layout.layout_upload_photo, null);
        builder.setContentView(bottomSheet);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        // val displayMetrics:DisplayMetrics = getResources().getDisplayMetrics();
        //   val height:Int = displayMetrics.heightPixels;
        //  val maxHeight:Int = (height*0.93).toInt();
        //  bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        // bottomSheetBehavior.setPeekHeight(maxHeight);
        tv_continue_upload_photo = builder.findViewById<TextView>(R.id.tv_continue_upload_photo);
        val iv_back = builder.findViewById<ImageView>(R.id.iv_back_upload_media);
        val recyclerView = builder.findViewById<RecyclerView>(R.id.rv_imageList);
        val tv_add_media = builder.findViewById<TextView>(R.id.tv_add_media);
         ll_content_area = builder.findViewById<LinearLayout>(R.id.ll_content_area);


        recyclerView?.layoutManager = LinearLayoutManager(this@HomeActivity)
        imageListAdapter = ImageListAdapter(this@HomeActivity)
        recyclerView?.setAdapter(imageListAdapter)

        tv_continue_upload_photo?.setOnClickListener(View.OnClickListener {
            moreInfoDialog()
            builder.dismiss()
        })


        tv_add_media?.setOnClickListener(View.OnClickListener {
            choosePhoto()
        })


        iv_back?.setOnClickListener {

            builder.dismiss()
        }


        builder.show();
    }

    private fun moreInfoDialog() {
        val builder = BottomSheetDialog(this);
        builder.setCancelable(false);

        val bottomSheet = LayoutInflater.from(this).inflate(R.layout.layout_more_details, null);
        builder.setContentView(bottomSheet);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        // val displayMetrics:DisplayMetrics = getResources().getDisplayMetrics();
        //   val height:Int = displayMetrics.heightPixels;
        //  val maxHeight:Int = (height*0.93).toInt();
        //  bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        // bottomSheetBehavior.setPeekHeight(maxHeight);
        val iv_back = builder.findViewById<ImageView>(R.id.iv_close_more_details);
         edit_venu = builder.findViewById<EditText>(R.id.edit_venu);
        val tv_continue = builder.findViewById<TextView>(R.id.tv_continue);
         calendarView_startdate = builder.findViewById<CalendarView>(R.id.calendarView_startdate);
         calendarView_enddate = builder.findViewById<CalendarView>(R.id.calendarView_enddate);

        rl_select_category = builder.findViewById<RelativeLayout>(R.id.rl_select_category)
        rl_select_subcategory = builder.findViewById<RelativeLayout>(R.id.rl_select_subcategory)

        ll_category_space = builder.findViewById<LinearLayout>(R.id.ll_category_space)
        ll_subcategory_space = builder.findViewById<LinearLayout>(R.id.ll_subcategory_space)

        rv_category = builder.findViewById<RecyclerView>(R.id.rv_spinnercategory)
        rv_spinnersubcategory = builder.findViewById<RecyclerView>(R.id.rv_spinnersubcategory)

        ll_calendar_view_start_date = builder.findViewById<LinearLayout>(R.id.ll_calendar_view_start_date)
        ll_calendar_view_end_date = builder.findViewById<LinearLayout>(R.id.ll_calendar_view_end_date)


        img_select_start_date = builder.findViewById<ImageView>(R.id.img_select_start_date)
        img_select_end_date = builder.findViewById<ImageView>(R.id.img_select_end_date)

        tv_calendar_cancel1 = builder.findViewById<TextView>(R.id.tv_calendar_cancel1)
        tv_calendar_cancel2 = builder.findViewById<TextView>(R.id.tv_calendar_cancel2)

        tv_calendar_ok1 = builder.findViewById<TextView>(R.id.tv_calendar_ok1)
        tv_calendar_ok2 = builder.findViewById<TextView>(R.id.tv_calendar_ok2)

        tv_activity_start_date = builder.findViewById<TextView>(R.id.tv_activity_start_date)
        tv_activity_end_date = builder.findViewById<TextView>(R.id.tv_activity_end_date)


        ll_start_time = builder.findViewById<LinearLayout>(R.id.ll_start_time)
        ll_end_time = builder.findViewById<LinearLayout>(R.id.ll_end_time)

        timePicker1 = builder.findViewById<TimePicker>(R.id.timePicker1)
        timePicker2 = builder.findViewById<TimePicker>(R.id.timePicker2)

        tv_time_cancel1 = builder.findViewById<TextView>(R.id.tv_time_cancel1)
        tv_time_cancel2 = builder.findViewById<TextView>(R.id.tv_time_cancel2)
        tv_time_ok1 = builder.findViewById<TextView>(R.id.tv_time_ok1)
        tv_time_ok2 = builder.findViewById<TextView>(R.id.tv_time_ok2)

        et_limits = builder.findViewById<EditText>(R.id.et_limits)

        switch_limits = builder.findViewById<Switch>(R.id.switch_limits)
        switch_make_public = builder.findViewById<Switch>(R.id.switch_make_public)

        getCategories(email_id)

        /* DateFormat dateFormat = new SimpleDateFormat("mm-dd-yyyy");
        Date date = new Date();
        tv_activity_start_date.setText(dateFormat.format(date));
        tv_activity_end_date.setText(dateFormat.format(date));*/
        val calendar = Calendar.getInstance(TimeZone.getDefault())

        val currentYear = calendar[Calendar.YEAR]
        val currentMonth = calendar[Calendar.MONTH] + 1
        val currentDay = calendar[Calendar.DAY_OF_MONTH]

        val hour: String
        val minute: String
        val hour2: String
        val minute2: String
        @Suppress("DEPRECATION")
        if (timePicker1!!.currentHour < 10) {
            hour = "0" + timePicker1!!.currentHour
            hour2 = "0" + timePicker2!!.currentHour
        } else {
            hour = "" + timePicker1!!.currentHour
            hour2 = "" + timePicker2!!.currentHour
        }
        @Suppress("DEPRECATION")
        if (timePicker1!!.currentMinute < 10) {
            minute = "0" + timePicker1!!.currentMinute
            minute2 = "0" + timePicker2!!.currentMinute
        } else {
            minute = "" + timePicker1!!.currentMinute
            minute2 = "" + timePicker2!!.currentMinute
        }

        val currentTime_start = "$hour:$minute"
        val currentTime2_end = "$hour2:$minute2"

        tv_activity_start_date?.setText(splitDate(StringBuilder().append(currentYear).append("-").append(currentMonth).append("-").append(currentDay).append("")) + "-" + currentTime_start)
        tv_activity_end_date?.setText(splitDate(StringBuilder().append(currentYear).append("-").append(currentMonth).append("-").append(currentDay).append("")) + "-" + currentTime2_end)

        //Listener's
        tv_continue!!.setOnClickListener(this)
        img_select_start_date!!.setOnClickListener(this)
        img_select_end_date!!.setOnClickListener(this)

        tv_calendar_cancel1!!.setOnClickListener(this)
        tv_calendar_ok1!!.setOnClickListener(this)
        tv_calendar_cancel2!!.setOnClickListener(this)
        tv_calendar_ok2!!.setOnClickListener(this)

        tv_time_ok1!!.setOnClickListener(this)
        tv_time_ok2!!.setOnClickListener(this)


        tv_time_cancel1!!.setOnClickListener(this)
        tv_time_cancel1!!.setOnClickListener(this)

        rl_select_category!!.setOnClickListener(this)
        rl_select_subcategory!!.setOnClickListener(this)



        iv_back?.setOnClickListener {
            builder.dismiss()
        }

        tv_continue.setOnClickListener {
            myDialog?.showProgresbar(this@HomeActivity)
            categoryList.clear()
            subCategoryList.clear()
            hideKeyboard()
            postLocation = edit_venu!!.text.toString().trim { it <= ' ' }
            startDate = tv_activity_start_date!!.text.toString().trim { it <= ' ' }
            endDate = tv_activity_end_date!!.text.toString().trim { it <= ' ' }
            @Suppress("DEPRECATION")
            if (validateMoreInfo(postLocation!!, startDate!!, endDate!!, categoryName.toString(), subCategory.toString(), edit_venu!!)) {
                numberoffiles = RequestBody.create("text/plain".toMediaTypeOrNull(), numberOfFiles!!)
                category = RequestBody.create("text/plain".toMediaTypeOrNull(), categoryName!!)
                subcategory = RequestBody.create("text/plain".toMediaTypeOrNull(), subCategory!!)
                user_uuid = RequestBody.create("text/plain".toMediaTypeOrNull(), userUuid!!)
                location = RequestBody.create("text/plain".toMediaTypeOrNull(), postLocation!!)
                start_date_and_time = RequestBody.create("text/plain".toMediaTypeOrNull(), startDate!!)
                end_date_and_time = RequestBody.create("text/plain".toMediaTypeOrNull(), endDate!!)

                // System.out.println("error>> udid " + uuid);
                if (filePartList.size > 0) {
                    call = mAPIService.createPostImages(
                            filePartList, activityname, activitydesc, numberoffiles, category, subcategory, user_uuid,
                            location, start_date_and_time, end_date_and_time
                    )
                } else {
                    call = mAPIService.createPost(
                            filePart, activityname, activitydesc, numberoffiles, category, subcategory, user_uuid,
                            location, start_date_and_time, end_date_and_time
                    )
                }
    /*            call.enqueue(object : Callback<UploadImage?> {
                    override fun onResponse(call: Call<UploadImage?>, response: Response<UploadImage?>) {
                        println("msg>>  response: $response")
                        myDialog!!.hideDialog(this@HomeActivity)
                        SingleRecyclerViewAdapter.sSelected_category = -1
                        SubCategorySingleSelectionAdapter.sSelected_subcategory = -1
                        if (response.body() != null) {
                            val uploadImage: UploadImage? = response.body()
                            val Activity_UUID: String = uploadImage.getActivity_UUID()
                            println("Activity_UUID>>$Activity_UUID")
                            if (Activity_UUID != null) {
                                // inviteDialog(Activity_UUID) // pass the Activity_UUID for invite friend api
                                builder.dismiss()
                            }
                        } else {
                            Toast.makeText(this@HomeActivity, "Invalid response from server", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UploadImage?>, t: Throwable) {
                        myDialog!!.hideDialog(this@HomeActivity)
                        println("msg>> failure: " + t.cause)
                        //  Toast.makeText(MainActivity.this, "" + t.getCause(), Toast.LENGTH_SHORT).show();
                    }
                })*/
            }
        }

        builder.show()
    }

    private fun validateMoreInfo(postLocation: String, startDate: String, endDate: String, categoryName: String, subCategory: String, edit_venu: EditText): Boolean {
        if (postLocation.isEmpty()) {
            edit_venu.error = "Please enter location!"
            edit_venu.requestFocus()
            return false
        } else if (startDate == "") {
            Toast.makeText(this@HomeActivity, "Please select activity start date and time", Toast.LENGTH_SHORT).show()
            return false
        } else if (endDate == "") {
            Toast.makeText(this@HomeActivity, "Please select activity end date and time", Toast.LENGTH_SHORT).show()
            return false
        } else if (categoryName == "") {
            Toast.makeText(this@HomeActivity, "Please select category name", Toast.LENGTH_SHORT).show()
            return false
        } else if (subCategory == "") {
            Toast.makeText(this@HomeActivity, "Please select sub-category name", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun splitDate(strDate: java.lang.StringBuilder): String? {
        var finalDate = ""
        val objDate:Date?;
        try {
            //current date format
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            objDate = dateFormat.parse(strDate.toString())

            //Expected date format
            val dateFormat2 = SimpleDateFormat("MMMM dd, yyyy")
            finalDate= objDate?.let { dateFormat2.format(it) }.toString()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return finalDate
    }
    private fun getCategories(emailId: String?) {
      //  Log.d("email_id>>",""+emailId)
        val getCategoryRequest = GetCategoryRequest(emailId)
        val call: Call<GetCategoryResponse> = mAPIService.getCategory(getCategoryRequest)
        call.enqueue(object : Callback<GetCategoryResponse?> {
            override fun onResponse(call: Call<GetCategoryResponse?>, response: Response<GetCategoryResponse?>) {
                //  System.out.println("error>>  response " + response.toString());
                if (response.body() != null) {
                    val categoryResponse = response.body()
                    val message = categoryResponse?.msg
                    val status = categoryResponse?.status
                    if (status == "1") {
                        rv_category!!.layoutManager = GridLayoutManager(this@HomeActivity, 2)
                        rv_category!!.setHasFixedSize(true)
                        // add static data in eventlist

                        // add static data in eventlist
                        categoryList.add(Category(R.drawable.ic_ideas, "Travelling"))
                        categoryList.add(Category(R.drawable.ic_ideas, "Education"))
                        categoryList.add(Category(R.drawable.ic_ideas, "Sports"))
                        categoryList.add(Category(R.drawable.ic_ideas, "Cars"))
                        categoryList.add(Category(R.drawable.ic_ideas, "Movie"))

                        singleRecyclerViewAdapter = SingleRecyclerViewAdapter(this@HomeActivity, categoryList)
                        rv_category!!.adapter = singleRecyclerViewAdapter
                        singleRecyclerViewAdapter!!.setOnItemClickListener(this@HomeActivity)
                        Toast.makeText(this@HomeActivity, "" + message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@HomeActivity, "" + message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<GetCategoryResponse?>, t: Throwable) {
                println("error>>" + t.cause)
            }
        })
    }

    private fun choosePhoto() {
        val builder = BottomSheetDialog(this);
        builder.setCancelable(false);
        builder.setContentView(R.layout.fragment_custom_dialog_image__camera_bottom_sheet)
        val tv_gallery = builder.findViewById<TextView>(R.id.tv_gallery_multiple)
        val tv_camera = builder.findViewById<TextView>(R.id.tv_camera)
        val tv_cancel_dialog = builder.findViewById<TextView>(R.id.tv_cancel_dialog)


        @Suppress("DEPRECATION")
        tv_camera?.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, cameraRequest)
            ll_content_area?.setVisibility(View.GONE)
            builder.dismiss()
        }
        @Suppress("DEPRECATION")
        tv_gallery?.setOnClickListener {
            val intentGallery = Intent()
            intentGallery.type = "image/*"
            intentGallery.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intentGallery.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intentGallery, getString(R.string.select_image)), PICK_IMAGE_MULTIPLE)
            ll_content_area?.setVisibility(View.GONE)
            builder.dismiss()
        }

        tv_cancel_dialog?.setOnClickListener {
            builder.dismiss()
        }

        builder.show()

    }
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest && resultCode == RESULT_OK) {
            val imageBitmap: Bitmap = data?.extras?.get("data") as Bitmap
            imageListAdapter?.addItem(imageBitmap)

        }else if(requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK){
            try {
                if (data!!.clipData != null) {
                    val cout = data.clipData!!.itemCount
                    for (i in 0 until cout) {
                        val imageUrl = data.clipData!!.getItemAt(i).uri
                        gallerybitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUrl)
                        imageListAdapter?.addItem(gallerybitmap)
                        val defaultFile = File(getExternalFilesDir(null), "XXX" + "XXX")
                        if (!defaultFile.exists()) defaultFile.mkdirs()
                        val random: String = random() //need to random file name
                        val fileName = "IMG_$random"
                        //   System.out.println("filename>>"+fileName);
                        var file = File(defaultFile, fileName)
                        if (file.exists()) {
                            file.delete()
                            file = File(defaultFile, fileName)
                        }
                        uploadPhoto(file, defaultFile, fileName)
                    }
                } else {
                    val imageUrl = data.data
                    gallerybitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUrl)
                    imageListAdapter?.addItem(gallerybitmap)
                    val defaultFile = File(getExternalFilesDir(null), "XXX" + "XXX")
                    if (!defaultFile.exists()) defaultFile.mkdirs()
                    val d = Date()
                    val s = DateFormat.format("dd-MM-yyyy_hh:mm:ss", d.time)
                    val fileName = "IMG_$s" //need to implement
                    //   System.out.println("filename>>"+fileName);
                    var file = File(defaultFile, fileName)
                    if (file.exists()) {
                        file.delete()
                        file = File(defaultFile, fileName)
                    }
                    val output = FileOutputStream(file)
                   // gallerybitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    output.flush()
                    output.close()
                    val imgFile = File(defaultFile.absolutePath + "/" + fileName)
                    if (imgFile.exists()) {
                       // filePart = MultipartBody.Part.createFormData.createFormData("media_file", imgFile.name, RequestBody.create(MediaType.parse("image/*"), imgFile))
                    }
                }
            }catch(e: Exception){
            }
        }
    }

    private fun uploadPhoto(file: File, defaultFile: File, fileName: String) {
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    val output = FileOutputStream(file)
                    gallerybitmap!!.compress(Bitmap.CompressFormat.PNG, 100, output)
                    output.flush()
                    output.close()
                    val imgFile = File(defaultFile.absolutePath + "/" + fileName)
                    if (imgFile.exists()) {
                      //  filePart = MultipartBody.Part.createFormData("media_file", imgFile.name, RequestBody.create(MediaType.parse("image/*"), imgFile))
                      //  filePartList.add(filePart)
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }

        thread.start()
    }

    private fun validateActivity(activityName: String, activityDescription: String, etActivityName: EditText?, etActivityDesc: EditText?): Boolean {
        if (activityName.isEmpty()) {
            etActivityName?.setError("Please enter activity name!")
            etActivityName?.requestFocus()
            return false
        } else if (activityDescription == "") {
            etActivityDesc?.setError("Please enter activity description!")
            etActivityDesc?.requestFocus()
            return false
        }
        return true
    }



    override fun onPostResume() {
        super.onPostResume()

        // fab.setVisibility(View.VISIBLE);
        if (isHome) {
            val newCase: Fragment = HomeFragment()
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newCase)
                    .addToBackStack(null)
                    .commit()
            iv_home?.setImageResource(R.drawable.ic_home_selected)
            iv_users?.setImageResource(R.drawable.ic_users)
            iv_chat?.setImageResource(R.drawable.ic_chat_ts)
            iv_menu?.setImageResource(R.drawable.ic_menu)
//            mHomeSelected?.setVisibility(View.VISIBLE)
//            mUsersSelected?.setVisibility(View.GONE)
//            mChatSelected?.setVisibility(View.GONE)
//            mMenuSelected?.setVisibility(View.GONE)
        } else if (isFriend) {
            iv_home?.setImageResource(R.drawable.ic_home)
            iv_users?.setImageResource(R.drawable.ic_users_selected)
            iv_chat?.setImageResource(R.drawable.ic_chat_ts)
            iv_menu?.setImageResource(R.drawable.ic_menu)
//            mHomeSelected?.setVisibility(View.GONE)
//            mUsersSelected?.setVisibility(View.VISIBLE)
//            mChatSelected?.setVisibility(View.GONE)
//            mMenuSelected?.setVisibility(View.GONE)

            /* Fragment newCase = new FriendsFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newCase)
                    .addToBackStack(null)
                    .commit();*/
        } else if (isChat) {
            iv_home?.setImageResource(R.drawable.ic_home)
            iv_users?.setImageResource(R.drawable.ic_users)
            iv_chat?.setImageResource(R.drawable.ic_chat_selected)
            iv_menu?.setImageResource(R.drawable.ic_menu)
//            mHomeSelected?.setVisibility(View.GONE)
//            mUsersSelected?.setVisibility(View.GONE)
//            mChatSelected?.setVisibility(View.VISIBLE)
//            mMenuSelected?.setVisibility(View.GONE)
          //  mActivity.startActivity(Intent(mActivity, MainActivity::class.java))

            /* Fragment newCase = new ChatFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newCase)
                    .addToBackStack(null)
                    .commit();*/
        } else if (isMenu) {
            iv_home?.setImageResource(R.drawable.ic_home)
            iv_users?.setImageResource(R.drawable.ic_users)
            iv_chat?.setImageResource(R.drawable.ic_chat_ts)
            iv_menu?.setImageResource(R.drawable.ic_menu_selected)
//            mHomeSelected?.setVisibility(View.GONE)
//            mUsersSelected?.setVisibility(View.GONE)
//            mChatSelected?.setVisibility(View.GONE)
//            mMenuSelected?.setVisibility(View.VISIBLE)

            /* Fragment newCase = new MenuFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newCase)
                    .addToBackStack(null)
                    .commit();*/
        }
    }

    private fun askUserToSelectPushDistributor() {
        unifiedPushHelper.showSelectDistributorDialog(this) { selection ->
            homeActivityViewModel.handle(HomeActivityViewActions.RegisterPushDistributor(selection))
        }
    }

    private fun handleShowNotificationDialog() {
        notificationPermissionManager.eventuallyRequestPermission(this, postPermissionLauncher)
    }

    private fun handleShowReleaseNotes() {
        startActivity(Intent(this, ReleaseNotesActivity::class.java))
    }

    private fun showSpaceSettings(spaceId: String) {
        // open bottom sheet
        SpaceSettingsMenuBottomSheet
                .newInstance(spaceId, object : SpaceSettingsMenuBottomSheet.InteractionListener {
                    override fun onShareSpaceSelected(spaceId: String) {
                        ShareSpaceBottomSheet.show(supportFragmentManager, spaceId)
                    }
                })
                .show(supportFragmentManager, "SPACE_SETTINGS")
    }

    private fun showLayoutSettings() {
        HomeLayoutSettingBottomDialogFragment()
                .show(supportFragmentManager, "LAYOUT_SETTINGS")
    }

    private fun openSpaceInvite(spaceId: String) {
        SpaceInviteBottomSheet.newInstance(spaceId)
                .show(supportFragmentManager, "SPACE_INVITE")
    }

    private fun onCloseSpace() {
        views.drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun handleShowAnalyticsOptIn() {
        navigator.openAnalyticsOptIn(this)
    }

    /**
     * Migrating from old threads io.element.thread to new m.thread needs an initial sync to
     * sync and display existing messages appropriately.
     */
    private fun migrateThreadsIfNeeded(checkSession: Boolean) {
        if (checkSession) {
            // We should check session to ensure we will only clear cache if needed
            val args = intent.getParcelableExtraCompat<HomeActivityArgs>(Mavericks.KEY_ARG)
            if (args?.hasExistingSession == true) {
                // existingSession --> Will be true only if we came from an existing active session
                Timber.i("----> Migrating threads from an existing session..")
                handleThreadsMigration()
            } else {
                // We came from a new session and not an existing one,
                // so there is no need to migrate threads while an initial synced performed
                Timber.i("----> No thread migration needed, we are ok")
                vectorPreferences.setShouldMigrateThreads(shouldMigrate = false)
            }
        } else {
            // Proceed with migration
            handleThreadsMigration()
        }
    }

    /**
     * Clear cache and restart to invoke an initial sync for threads migration.
     */
    private fun handleThreadsMigration() {
        Timber.i("----> Threads Migration detected, clearing cache and sync...")
        vectorPreferences.setShouldMigrateThreads(shouldMigrate = false)
        MainActivity.restartApp(this, MainActivityArgs(clearCache = true))
    }

    private fun handleNotifyUserForThreadsMigration() {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.threads_notice_migration_title)
                .setMessage(R.string.threads_notice_migration_message)
                .setCancelable(true)
                .setPositiveButton(R.string.sas_got_it) { _, _ -> }
                .show()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.dataString?.let { deepLink ->
            val resolvedLink = when {
                // Element custom scheme is not handled by the sdk, convert it to matrix.to link for compatibility
                deepLink.startsWith(MATRIX_TO_CUSTOM_SCHEME_URL_BASE) -> {
                    when {
                        deepLink.startsWith(USER_LINK_PREFIX) -> deepLink.substring(USER_LINK_PREFIX.length)
                        deepLink.startsWith(ROOM_LINK_PREFIX) -> deepLink.substring(ROOM_LINK_PREFIX.length)
                        else -> null
                    }?.let { permalinkId ->
                        activeSessionHolder.getSafeActiveSession()?.permalinkService()?.createPermalink(permalinkId)
                    }
                }
                else -> deepLink
            }

            lifecycleScope.launch {
                val isHandled = permalinkHandler.launch(
                        fragmentActivity = this@HomeActivity,
                        deepLink = resolvedLink,
                        navigationInterceptor = this@HomeActivity,
                        buildTask = true
                )
                if (!isHandled) {
                    val isMatrixToLink = deepLink.startsWith(PermalinkService.MATRIX_TO_URL_BASE) ||
                            deepLink.startsWith(MATRIX_TO_CUSTOM_SCHEME_URL_BASE)
                    MaterialAlertDialogBuilder(this@HomeActivity)
                            .setTitle(R.string.dialog_title_error)
                            .setMessage(if (isMatrixToLink) R.string.permalink_malformed else R.string.universal_link_malformed)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                }
            }
        }
    }

    private fun handleStartRecoverySetup() {
        // To avoid IllegalStateException in case the transaction was executed after onSaveInstanceState
        lifecycleScope.launchWhenResumed {
            navigator.open4SSetup(this@HomeActivity, SetupMode.NORMAL)
        }
    }

    private fun renderState(state: HomeActivityViewState) {
        when (val status = state.syncRequestState) {
            is SyncRequestState.InitialSyncProgressing -> {
                val initSyncStepStr = initSyncStepFormatter.format(status.initialSyncStep)
                Timber.v("$initSyncStepStr ${status.percentProgress}")
                views.waitingView.root.setOnClickListener {
                    // block interactions
                }
                views.waitingView.waitingHorizontalProgress.apply {
                    isIndeterminate = false
                    max = 100
                    progress = status.percentProgress
                    isVisible = true
                }
                views.waitingView.waitingStatusText.apply {
                    text = initSyncStepStr
                    isVisible = true
                }
                views.waitingView.root.isVisible = true
            }
            else -> {
                // Idle or Incremental sync status
                views.waitingView.root.isVisible = false
            }
        }
    }

    private fun handleAskPasswordToInitCrossSigning(events: HomeActivityViewEvents.AskPasswordToInitCrossSigning) {
        // We need to ask
        promptSecurityEvent(
                uid = PopupAlertManager.UPGRADE_SECURITY_UID,
                userItem = events.userItem,
                titleRes = R.string.upgrade_security,
                descRes = R.string.security_prompt_text,
        ) {
            it.navigator.upgradeSessionSecurity(it, true)
        }
    }

    private fun handleCrossSigningInvalidated(event: HomeActivityViewEvents.OnCrossSignedInvalidated) {
        // We need to ask
        promptSecurityEvent(
                uid = PopupAlertManager.VERIFY_SESSION_UID,
                userItem = event.userItem,
                titleRes = R.string.crosssigning_verify_this_session,
                descRes = R.string.confirm_your_identity,
        ) {
            it.navigator.waitSessionVerification(it)
        }
    }

    private fun handleOnNewSession(event: HomeActivityViewEvents.CurrentSessionNotVerified) {
        // We need to ask
        promptSecurityEvent(
                uid = PopupAlertManager.VERIFY_SESSION_UID,
                userItem = event.userItem,
                titleRes = R.string.crosssigning_verify_this_session,
                descRes = R.string.confirm_your_identity,
        ) {
            if (event.waitForIncomingRequest) {
                it.navigator.waitSessionVerification(it)
            } else {
                it.navigator.requestSelfSessionVerification(it)
            }
        }
    }

    private fun handleCantVerify(event: HomeActivityViewEvents.CurrentSessionCannotBeVerified) {
        // We need to ask
        promptSecurityEvent(
                uid = PopupAlertManager.UPGRADE_SECURITY_UID,
                userItem = event.userItem,
                titleRes = R.string.crosssigning_cannot_verify_this_session,
                descRes = R.string.crosssigning_cannot_verify_this_session_desc,
        ) {
            it.navigator.open4SSetup(it, SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET)
        }
    }

    private fun handlePromptToEnablePush() {
        popupAlertManager.postVectorAlert(
                DefaultVectorAlert(
                        uid = PopupAlertManager.ENABLE_PUSH_UID,
                        title = getString(R.string.alert_push_are_disabled_title),
                        description = getString(R.string.alert_push_are_disabled_description),
                        iconId = R.drawable.ic_room_actions_notifications_mutes,
                        shouldBeDisplayedIn = {
                            it is HomeActivity
                        }
                ).apply {
                    colorInt = ThemeUtils.getColor(this@HomeActivity, R.attr.vctr_notice_secondary)
                    contentAction = Runnable {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                            // action(it)
                            homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                            it.navigator.openSettings(it, VectorSettingsActivity.EXTRA_DIRECT_ACCESS_NOTIFICATIONS)
                        }
                    }
                    dismissedAction = Runnable {
                        homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                    }
                    addButton(getString(R.string.action_dismiss), {
                        homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                    }, true)
                    addButton(getString(R.string.settings), {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                            // action(it)
                            homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                            it.navigator.openSettings(it, VectorSettingsActivity.EXTRA_DIRECT_ACCESS_NOTIFICATIONS)
                        }
                    }, true)
                }
        )
    }

    private fun promptSecurityEvent(
            uid: String,
            userItem: MatrixItem.UserItem,
            titleRes: Int,
            descRes: Int,
            action: ((VectorBaseActivity<*>) -> Unit),
    ) {
        popupAlertManager.postVectorAlert(
                VerificationVectorAlert(
                        uid = uid,
                        title = getString(titleRes),
                        description = getString(descRes),
                        iconId = R.drawable.ic_shield_warning
                ).apply {
                    viewBinder = VerificationVectorAlert.ViewBinder(userItem, avatarRenderer)
                    colorInt = ThemeUtils.getColor(this@HomeActivity, R.attr.colorPrimary)
                    contentAction = Runnable {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                            action(it)
                        }
                    }
                    dismissedAction = Runnable {}
                }
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val parcelableExtra = intent?.getParcelableExtraCompat<HomeActivityArgs>(Mavericks.KEY_ARG)
        if (parcelableExtra?.clearNotification == true) {
            notificationDrawerManager.clearAllEvents()
        }
        if (parcelableExtra?.inviteNotificationRoomId != null) {
            activeSessionHolder.getSafeActiveSession()
                    ?.permalinkService()
                    ?.createPermalink(parcelableExtra.inviteNotificationRoomId)?.let {
                        navigator.openMatrixToBottomSheet(this, it, OriginOfMatrixTo.NOTIFICATION)
                    }
        }
        handleIntent(intent)
    }

    override fun onDestroy() {
        views.drawerLayout.removeDrawerListener(drawerListener)
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        //reload timeline's
     //   replaceFragment(views.homeDetailFragmentContainer, HomeFragment::class.java)

        if (vectorUncaughtExceptionHandler.didAppCrash()) {
            vectorUncaughtExceptionHandler.clearAppCrashStatus()

            MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.send_bug_report_app_crashed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ -> bugReporter.openBugReportScreen(this) }
                    .setNegativeButton(R.string.no) { _, _ -> bugReporter.deleteCrashFile() }
                    .show()
        } else {
            disclaimerDialog.showDisclaimerDialog(this)
        }

        // Force remote backup state update to update the banner if needed
        serverBackupStatusViewModel.refreshRemoteStateIfNeeded()

        // Check nightly
        if (nightlyProxy.canDisplayPopup()) {
            nightlyProxy.updateApplication()
        }

        checkNewAppLayoutFlagChange()
    }

    private fun checkNewAppLayoutFlagChange() {
        if (vectorPreferences.isNewAppLayoutEnabled() != isNewAppLayoutEnabled) {
            restart()
        }
    }

    override fun getMenuRes() = if (vectorPreferences.isNewAppLayoutEnabled()) R.menu.menu_new_home else R.menu.menu_home

    override fun handlePrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_home_init_sync_legacy).isVisible = vectorPreferences.developerMode()
        menu.findItem(R.id.menu_home_init_sync_optimized).isVisible = vectorPreferences.developerMode()
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home_suggestion -> {
                bugReporter.openBugReportScreen(this, ReportType.SUGGESTION)
                true
            }
            R.id.menu_home_report_bug -> {
                bugReporter.openBugReportScreen(this, ReportType.BUG_REPORT)
                true
            }
            R.id.menu_home_init_sync_legacy -> {
                // Configure the SDK
                initialSyncStrategy = InitialSyncStrategy.Legacy
                // And clear cache
                MainActivity.restartApp(this, MainActivityArgs(clearCache = true))
                true
            }
            R.id.menu_home_init_sync_optimized -> {
                // Configure the SDK
                initialSyncStrategy = InitialSyncStrategy.Optimized()
                // And clear cache
                MainActivity.restartApp(this, MainActivityArgs(clearCache = true))
                true
            }
            R.id.menu_home_filter -> {
                navigator.openRoomsFiltering(this)
                true
            }
            R.id.menu_home_setting -> {
                navigator.openSettings(this)
                true
            }
            R.id.menu_home_layout_settings -> {
                showLayoutSettings()
                true
            }
            R.id.menu_home_invite_friends -> {
                launchInviteFriends()
                true
            }
            R.id.menu_home_qr -> {
                launchQrCode()
                true
            }
            else -> false
        }
    }

    private fun launchQrCode() {
        startActivity(UserCodeActivity.newIntent(this, sharedActionViewModel.session.myUserId))
    }

    private fun launchInviteFriends() {
        activeSessionHolder.getSafeActiveSession()?.permalinkService()?.createPermalink(sharedActionViewModel.session.myUserId)?.let { permalink ->
            analyticsTracker.screen(MobileScreen(screenName = MobileScreen.ScreenName.InviteFriends))
            val text = getString(R.string.invite_friends_text, permalink)

            startSharePlainTextIntent(
                    context = this,
                    activityResultLauncher = null,
                    chooserTitle = getString(R.string.invite_friends),
                    text = text,
                    extraTitle = getString(R.string.invite_friends_rich_title)
            )
        }
    }

    override fun onBackPressed() {
        if (views.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            views.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            validateBackPressed {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }

    override fun navToMemberProfile(userId: String, deepLink: Uri): Boolean {
        // TODO check if there is already one??
        MatrixToBottomSheet.withLink(deepLink.toString(), OriginOfMatrixTo.LINK)
                .show(supportFragmentManager, "HA#MatrixToBottomSheet")
        return true
    }

    override fun navToRoom(roomId: String?, eventId: String?, deepLink: Uri?, rootThreadEventId: String?): Boolean {
        if (roomId == null) return false
        MatrixToBottomSheet.withLink(deepLink.toString(), OriginOfMatrixTo.LINK)
                .show(supportFragmentManager, "HA#MatrixToBottomSheet")
        return true
    }

    override fun spaceInviteBottomSheetOnAccept(spaceId: String) {
        navigator.switchToSpace(this, spaceId, Navigator.PostSwitchSpaceAction.OpenRoomList)
    }

    override fun spaceInviteBottomSheetOnDecline(spaceId: String) {
        // nop
    }

    companion object {
        var imageListAdapter: ImageListAdapter? = null
        @JvmField  var tv_continue_upload_photo: TextView? =null
        @JvmField var ll_content_area: LinearLayout? =null
        fun newIntent(
                context: Context,
                firstStartMainActivity: Boolean,
                clearNotification: Boolean = false,
                authenticationDescription: AuthenticationDescription? = null,
                existingSession: Boolean = false,
                inviteNotificationRoomId: String? = null
        ): Intent {
            val args = HomeActivityArgs(
                    clearNotification = clearNotification,
                    authenticationDescription = authenticationDescription,
                    hasExistingSession = existingSession,
                    inviteNotificationRoomId = inviteNotificationRoomId
            )

            val intent = Intent(context, HomeActivity::class.java)//HomeActivity
                    .apply {
                        putExtra(Mavericks.KEY_ARG, args)
                    }

            return if (firstStartMainActivity) {
                MainActivity.getIntentWithNextIntent(context, intent)
            } else {
                intent
            }
        }
    }

    override fun mxToBottomSheetNavigateToRoom(roomId: String, trigger: ViewRoom.Trigger?) {
        navigator.openRoom(this, roomId, trigger = trigger)
    }

    override fun mxToBottomSheetSwitchToSpace(spaceId: String) {
        navigator.switchToSpace(this, spaceId, Navigator.PostSwitchSpaceAction.OpenRoomList)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.img_select_start_date ->{
                hideKeyboard()
                if (ll_calendar_view_start_date!!.visibility != View.VISIBLE) {
                    ll_calendar_view_start_date!!.visibility = View.VISIBLE
                } else {
                    ll_calendar_view_start_date!!.visibility = View.GONE
                }
            }
            R.id.img_select_end_date ->{
                hideKeyboard()
                if (ll_calendar_view_end_date!!.visibility != View.VISIBLE) {
                    ll_calendar_view_end_date!!.visibility = View.VISIBLE
                } else {
                    ll_calendar_view_end_date!!.visibility = View.GONE
                }
            }
            R.id.tv_calendar_cancel1 ->{
                ll_calendar_view_start_date!!.visibility = View.GONE
            }
            R.id.tv_calendar_cancel2 ->{
                ll_calendar_view_end_date!!.visibility = View.GONE
            }
            R.id.tv_calendar_ok1 ->{
                ll_calendar_view_start_date!!.visibility = View.GONE
                ll_start_time!!.visibility = View.VISIBLE
            }

            R.id.tv_calendar_ok2 ->{
                ll_calendar_view_end_date!!.visibility = View.GONE
                ll_end_time!!.visibility = View.VISIBLE
            }

            R.id.tv_time_ok1 ->{
                val hour: String
                val minute: String
                ll_start_time!!.visibility = View.GONE
                @Suppress("DEPRECATION")
                hour = if (timePicker1!!.currentHour < 10) {
                    "0" + timePicker1!!.currentHour
                } else {
                    "" + timePicker1!!.currentHour
                }
                @Suppress("DEPRECATION")
                minute = if (timePicker1!!.currentMinute < 10) {
                    "0" + timePicker1!!.currentMinute
                } else {
                    "" + timePicker1!!.currentMinute
                }

                val currentTime = "$hour:$minute"
                if (tv_activity_start_date!!.text.toString().length>0) {
                    tv_activity_start_date!!.text = tv_activity_start_date!!.text.toString() + "-" + currentTime
                } else {
                    val dateFormat: java.text.DateFormat = SimpleDateFormat("mm-dd-yyyy")
                    val date = Date()
                    tv_activity_start_date!!.text = dateFormat.format(date) + "-" + currentTime
                }
            }

            R.id.tv_time_ok2 ->{
                val hour: String
                val minute: String
                ll_end_time!!.visibility = View.GONE
                @Suppress("DEPRECATION")
                hour = if (timePicker2!!.currentHour < 10) {
                    "0" + timePicker2!!.currentHour
                } else {
                    "" + timePicker2!!.currentHour
                }
                @Suppress("DEPRECATION")
                minute = if (timePicker2!!.currentMinute < 10) {
                    "0" + timePicker2!!.currentMinute
                } else {
                    "" + timePicker2!!.currentMinute
                }
                val currentTime = "$hour:$minute"
                if (tv_activity_end_date!!.text.toString().length>0) {
                    tv_activity_end_date!!.text = tv_activity_end_date!!.text.toString() + "-" + currentTime
                } else {
                    val dateFormat: java.text.DateFormat = SimpleDateFormat("mm-dd-yyyy")
                    val date = Date()
                    tv_activity_end_date!!.text = dateFormat.format(date) + "-" + currentTime
                }
            }

            R.id.tv_time_cancel1 ->{
                ll_start_time!!.visibility = View.GONE
            }

            R.id.tv_time_cancel2 ->{
                ll_end_time!!.visibility = View.GONE
            }

            R.id.rl_select_category ->{
                hideKeyboard()
                if (ll_category_space!!.visibility == View.VISIBLE) {
                    ll_category_space!!.visibility = View.GONE
                    ll_category_space!!.startAnimation(AnimationUtils.loadAnimation(this@HomeActivity, R.anim.slide_up))
                } else {
                    ll_category_space!!.visibility = View.VISIBLE
                    ll_category_space!!.startAnimation(AnimationUtils.loadAnimation(this@HomeActivity, R.anim.slide_down))
                }
            }

            R.id.rl_select_subcategory ->{
                if (ll_subcategory_space!!.visibility == View.VISIBLE) {
                    ll_subcategory_space!!.visibility = View.GONE
                    ll_subcategory_space!!.startAnimation(AnimationUtils.loadAnimation(this@HomeActivity, R.anim.slide_up))
                } else {
                    ll_subcategory_space!!.visibility = View.VISIBLE
                    ll_subcategory_space!!.startAnimation(AnimationUtils.loadAnimation(this@HomeActivity, R.anim.slide_down))
                }
            }
        }
    }

    override fun onItemClickListener(position: Int, view: View?) {
        singleRecyclerViewAdapter!!.selectedItem()
        // Toast.makeText(this@HomeActivity, ""+categoryList.get(position).getCategory(), Toast.LENGTH_SHORT).show();
        categoryName = categoryList[position].category
        getSubCategories(categoryList[position].category)
    }

    private fun getSubCategories(category: String?) {
        subCategoryList.clear()
        // add static data in subCategory
        if (category == "Travelling") {
            subCategoryList.add(SubCategory("Mountains", 0, "Travelling"))
            subCategoryList.add(SubCategory("Desert", 0, "Travelling"))
            subCategoryList.add(SubCategory("Sea", 0, "Travelling"))
            subCategoryList.add(SubCategory("Plains", 0, "Travelling"))
            subCategoryList.add(SubCategory("City", 0, "Travelling"))
        }
        if (category == "Education") {
            subCategoryList.add(SubCategory("Engineering", 1, "Education"))
            subCategoryList.add(SubCategory("Medical", 1, "Education"))
            subCategoryList.add(SubCategory("Astrophysics", 1, "Education"))
            subCategoryList.add(SubCategory("Science", 1, "Education"))
            subCategoryList.add(SubCategory("Economics", 1, "Education"))
        }
        if (category == "Sports") {
            subCategoryList.add(SubCategory("Cricket", 2, "Sports"))
            subCategoryList.add(SubCategory("Hockey", 2, "Sports"))
            subCategoryList.add(SubCategory("Tenis", 2, "Sports"))
            subCategoryList.add(SubCategory("Football", 2, "Sports"))
            subCategoryList.add(SubCategory("Chess", 2, "Sports"))
        }
        if (category == "Cars") {
            subCategoryList.add(SubCategory("BMW", 3, "Cars"))
            subCategoryList.add(SubCategory("Mercedes ", 3, "Cars"))
            subCategoryList.add(SubCategory("Maruti Suzuki ", 3, "Cars"))
            subCategoryList.add(SubCategory("Honda", 3, "Cars"))
            subCategoryList.add(SubCategory("Ferrari ", 3, "Cars"))
        }
        if (category == "Movie") {
            subCategoryList.add(SubCategory("Action", 4, "Movie"))
            subCategoryList.add(SubCategory("Drama ", 4, "Movie"))
            subCategoryList.add(SubCategory("Horror ", 4, "Movie"))
            subCategoryList.add(SubCategory("Thrill", 4, "Movie"))
            subCategoryList.add(SubCategory("3D ", 4, "Movie"))
        }
        rv_spinnersubcategory!!.layoutManager = GridLayoutManager(this@HomeActivity, 2)
        rv_spinnersubcategory!!.setHasFixedSize(true)
        subCategorySingleSelectionAdapter = SubCategorySingleSelectionAdapter(this@HomeActivity, subCategoryList)
        rv_spinnersubcategory!!.adapter = subCategorySingleSelectionAdapter
        subCategorySingleSelectionAdapter!!.setOnItemClickListener(this@HomeActivity)

    }

     override fun onSubCategoryListener(position: Int, view: View?) {
        subCategorySingleSelectionAdapter!!.selectedItem()
          Toast.makeText(this@HomeActivity, ""+subCategoryList.get(position).getSub_categ_name(), Toast.LENGTH_SHORT).show();
         subCategory = subCategoryList[position].sub_categ_name
    }
}
