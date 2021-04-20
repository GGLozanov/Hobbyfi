package com.example.hobbyfi.shared

import android.animation.ValueAnimator
import android.app.*
import android.content.*
import android.content.Context.ACTIVITY_SERVICE
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.GridView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.models.data.*
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.ui.auth.LoginFragmentDirections
import com.example.hobbyfi.ui.auth.RegisterFragmentDirections
import com.example.hobbyfi.ui.chatroom.ChatroomActivity
import com.example.hobbyfi.ui.chatroom.ChatroomEditFragmentDirections
import com.example.hobbyfi.ui.chatroom.ChatroomMessageListFragmentDirections
import com.example.hobbyfi.ui.chatroom.EventCreateFragmentDirections
import com.example.hobbyfi.ui.main.ChatroomCreateFragmentDirections
import com.example.hobbyfi.ui.main.ChatroomListFragmentDirections
import com.example.hobbyfi.ui.main.JoinedChatroomListFragmentDirections
import com.example.hobbyfi.ui.main.UserProfileFragmentDirections
import com.example.hobbyfi.ui.shared.LoadingFragment
import com.example.hobbyfi.utils.ColourUtils
import com.example.hobbyfi.utils.TokenUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject
import java.io.IOException
import java.lang.reflect.Field
import kotlin.reflect.KClass


inline fun <reified T> Gson.fromJson(json: String?) = fromJson<T>(
    json,
    object : TypeToken<T>() {}.type
)

inline fun <reified T> Gson.fromJson(json: JsonElement?) = fromJson<T>(
    json,
    object : TypeToken<T>() {}.type
)

// original function by Zhuinden; modified to work for INvalidation purposes instead of validation
fun invalidateBy(vararg liveDatas: LiveData<Boolean>): LiveData<Boolean> = MediatorLiveData<Boolean>().also { mediator ->
    mediator.value = liveDatas.all { it.value == false }

    for (current in liveDatas) {
        mediator.addSource(current) { valid ->
            var isInvalid = valid
            if (!isInvalid) {
                for (liveData in liveDatas) {
                    if (liveData !== current) {
                        if (liveData.value != false) {
                            isInvalid = true
                            break
                        }
                    }
                }
            }

            mediator.value = isInvalid
        }
    }
}

fun <T : Comparable<*>> T?.equalsOrBiggerThan(comp: T?): Boolean =
    compareValues(this, comp).run {
        this >= 0
    }

fun <T : Comparable<*>> T?.equalsOrLessThan(comp: T?): Boolean =
    compareValues(this, comp).run {
        this <= 0
    }

private fun findField(name: String, type: Class<*>): Field? {
    for (declaredField in type.declaredFields) {
        if (declaredField.name == name) {
            return declaredField
        }
    }
    return if (type.superclass != null) {
        findField(name, type.superclass)
    } else null
}

fun ConnectivityManager.isConnected(): Boolean {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        with(
            getNetworkCapabilities(
                activeNetwork
            )
        ) {
            return this != null &&
                    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    } else {
        return activeNetworkInfo?.isConnected == true // null safety requires explicit check
    }
}

fun <T> MutableList<T>.addAllDistinct(list: List<T>) {
    list.forEach {
        if(!this.contains(it)) {
            this.add(it)
        }
    }
}

fun <T> List<T>.newListWithDistinct(selectedTags: List<T>): MutableList<T> {
    val newTags = this.toMutableList()
    Log.i("getNewListWith", "Original list: $newTags")
    newTags.addAll(selectedTags)
    return newTags.distinct().toMutableList()
}

fun <T> List<T>.replaceOrAdd(newValue: T, predicate: (T) -> Boolean): List<T> {
    val newList =  map {
        if (predicate(it)) newValue else it
    }

    return if(!newList.contains(newValue)) newList + newValue else newList
}

val FragmentManager.currentNavigationFragment: Fragment?
    get() = primaryNavigationFragment?.childFragmentManager?.fragments?.first()

val FragmentManager.previousNavigationFragment: Fragment?
    get() = primaryNavigationFragment?.childFragmentManager?.fragments?.get(1)

inline fun <reified T : Fragment> FragmentManager.findFragmentByType(): T? = fragments.firstOrNull { it is T } as T?

fun NavigationView.clearCurrentMenuAndInflate(menuId: Int) {
    menu.clear()
    inflateMenu(menuId)
}

fun Context.buildYesNoAlertDialog(
    dialogMessage: String,
    onConfirm: DialogInterface.OnClickListener, onCancel: DialogInterface.OnClickListener?,
    onDismiss: DialogInterface.OnDismissListener? = null
) {
    val dialogBuilder = AlertDialog.Builder(this)
        .setMessage(dialogMessage)
        .setPositiveButton(getString(R.string.yes), onConfirm)
        .setNegativeButton(getString(R.string.no), onCancel)

    onDismiss?.let {
        dialogBuilder.setOnDismissListener(it)
    }

    dialogBuilder.create().apply {
        window!!.setBackgroundDrawableResource(R.color.colorBackground)
        show()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun Context.createNotificationChannel(): NotificationChannel {
    val channelId = resources.getString(R.string.default_notification_channel_id)
    val name = getString(R.string.channel_name)
    val descriptionText = getString(R.string.channel_description)
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(
        channelId,
        name,
        importance
    ).apply {
        description = descriptionText
    }

    // Register the channel with the system
    val notificationManager: NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
    return channel
}

inline fun <reified T : DialogFragment> FragmentManager.showDistinctDialog(
    tag: String,
    noinline instanceGenerator: () -> T,
    targetFragment: Fragment? = null
) {
    val dialog = (findFragmentByTag(tag) as T?)
        ?: instanceGenerator()
    targetFragment?.let {
        dialog.setTargetFragment(it, 400)
    }
    dialog.show(this, tag)
}

inline fun <reified T : Fragment> FragmentManager.addDistinctFragmentToBackStack(
    tag: String,
    containerId: Int,
    noinline instanceGenerator: () -> T
) {
    val fragment = (findFragmentByTag(tag) as T?)
        ?: instanceGenerator()
    commit {
        setReorderingAllowed(true)
        add(containerId, fragment, tag)
        addToBackStack(tag)
    }
}

fun <T, K, R> LiveData<T>.combineWith(
    liveData: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block(this.value, liveData.value)
    }
    return result
}

fun View.setParamsBasedOnScreenOrientation(
    activity: Activity,
    divisorHeightPortrait: Int,
    divisorWidthPortrait: Int,
    divisorHeightLandscape: Int,
    divisorWidthLandscape: Int
) {
    val displayMetrics = activity.resources.displayMetrics
    if(activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        layoutParams.height = displayMetrics.heightPixels / divisorHeightPortrait
        layoutParams.width = displayMetrics.widthPixels / divisorWidthPortrait
    } else {
        layoutParams.height = displayMetrics.heightPixels / divisorHeightLandscape
        layoutParams.width = displayMetrics.widthPixels / divisorWidthLandscape
    }
}

fun Marker.animateMarker(newLatLng: LatLng) {
    val startValues: DoubleArray =
        doubleArrayOf(position.latitude, position.longitude)
    val endValues: DoubleArray = doubleArrayOf(newLatLng.latitude, newLatLng.longitude)
    val latLngAnimator: ValueAnimator =
        ValueAnimator.ofObject(DoubleArrayEvaluator(), startValues, endValues)
    latLngAnimator.duration = 600
    latLngAnimator.interpolator = android.view.animation.DecelerateInterpolator()
    latLngAnimator.addUpdateListener { animation ->
        val animatedValue = animation.animatedValue as DoubleArray
        position = LatLng(animatedValue[0], animatedValue[1])
    } // lerp the anim
    latLngAnimator.start()
}

fun <T> MutableLiveData<T>.forceObserve() {
    this.value = this.value
}

fun Context.convertDrawableResToBitmap(@DrawableRes drawableId: Int, width: Int?, height: Int?): Bitmap {
    val d: Drawable = ContextCompat.getDrawable(this, drawableId) ?: throw Resources.NotFoundException()
    return d.toBitmap(width ?: d.intrinsicWidth, height ?: d.intrinsicHeight)
}

fun AppCompatActivity.comeFromAuthDeepLink(): Boolean = (intent.extras?.get("+clicked_branch_link") as String?)?.toBoolean() == true

fun comeFromAuthDeepLink(intent: Intent): Boolean = (intent.extras?.get("+clicked_branch_link") as String?)?.toBoolean() == true

@ExperimentalCoroutinesApi
private fun addLoadingAwareNavListener(navController: NavController, activity: AppCompatActivity): NavController {
    navController.addOnDestinationChangedListener { _, destination, _ ->
        if(activity !is ChatroomActivity) {
            activity.supportActionBar?.title = destination.label // reaffirm due to config changes
        }

        if(destination.id == R.id.loadingFragment ||
            destination.id == R.id.authWrapperFragment) activity.supportActionBar?.hide() else activity.supportActionBar?.show()
    }
    return navController
}

// yes, this does look weird, but due to possible race conditions w/ navController instances, previous destinations are directly passed as references
fun NavController.getCurrentDestinationToLoadingNavGraphActionId(defaultActionId: NavDirections): NavDirections {
    fun getLoadingNavGraphAction(destinationId: Int? = currentDestination?.id, recDepth: Int = 0): NavDirections {
        return when(destinationId) {
            R.id.loginFragment -> LoginFragmentDirections.actionLoginFragmentToLoadingNavGraph(
                R.id.loginFragment)
            R.id.registerFragment -> RegisterFragmentDirections.actionRegisterFragmentToLoadingNavGraph(
                R.id.registerFragment)
            R.id.chatroomCreateFragment -> ChatroomCreateFragmentDirections.actionChatroomCreateFragmentToLoadingNavGraph(
                R.id.chatroomCreateFragment)
            R.id.userProfileFragment -> UserProfileFragmentDirections.actionUserProfileFragmentToLoadingNavGraph(
                R.id.userProfileFragment)
            R.id.joinedChatroomListFragment -> JoinedChatroomListFragmentDirections.actionJoinedChatroomListFragmentToLoadingNavGraph(
                R.id.joinedChatroomListFragment)
            R.id.chatroomListFragment -> ChatroomListFragmentDirections.actionChatroomListFragmentToLoadingNavGraph(
                R.id.chatroomListFragment)
            R.id.chatroomEditFragment -> ChatroomEditFragmentDirections.actionChatroomEditFragmentToLoadingNavGraph(
                R.id.chatroomEditFragment)
            R.id.eventCreateFragment -> EventCreateFragmentDirections.actionGlobalLoadingNavGraph(
                R.id.eventCreateFragment)
            R.id.chatroomMessageListFragment -> ChatroomMessageListFragmentDirections.actionChatroomMessageListFragmentToLoadingNavGraph(
                R.id.chatroomMessageListFragment)
            R.id.loadingFragment -> getLoadingNavGraphAction(previousBackStackEntry?.destination?.id, recDepth + 1)
            else -> if(recDepth != 0) defaultActionId else getLoadingNavGraphAction(previousBackStackEntry?.destination?.id, recDepth + 1)
        }
    }

    return getLoadingNavGraphAction()
}

fun NavController.getCurrentDestinationToLoadingNavGraphActionIdNoRec(defaultActionId: NavDirections?): NavDirections? {
    fun getLoadingNavGraphAction(destinationId: Int? = currentDestination?.id): NavDirections? {
        return when(destinationId) {
            R.id.loginFragment -> LoginFragmentDirections.actionLoginFragmentToLoadingNavGraph(
                R.id.loginFragment)
            R.id.registerFragment -> RegisterFragmentDirections.actionRegisterFragmentToLoadingNavGraph(
                R.id.registerFragment)
            R.id.chatroomCreateFragment -> ChatroomCreateFragmentDirections.actionChatroomCreateFragmentToLoadingNavGraph(
                R.id.chatroomCreateFragment)
            R.id.userProfileFragment -> UserProfileFragmentDirections.actionUserProfileFragmentToLoadingNavGraph(
                R.id.userProfileFragment)
            R.id.joinedChatroomListFragment -> JoinedChatroomListFragmentDirections.actionJoinedChatroomListFragmentToLoadingNavGraph(
                R.id.joinedChatroomListFragment)
            R.id.chatroomListFragment -> ChatroomListFragmentDirections.actionChatroomListFragmentToLoadingNavGraph(
                R.id.chatroomListFragment)
            R.id.chatroomEditFragment -> ChatroomEditFragmentDirections.actionChatroomEditFragmentToLoadingNavGraph(
                R.id.chatroomEditFragment)
            R.id.eventCreateFragment -> EventCreateFragmentDirections.actionGlobalLoadingNavGraph(
                R.id.eventCreateFragment)
            R.id.chatroomMessageListFragment -> ChatroomMessageListFragmentDirections.actionChatroomMessageListFragmentToLoadingNavGraph(
                R.id.chatroomMessageListFragment)
            else -> defaultActionId
        }
    }

    return getLoadingNavGraphAction()
}

@ExperimentalCoroutinesApi
fun AppCompatActivity.findLoadingDestinationAwareNavController(): NavController? =
    try {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        addLoadingAwareNavListener(navHostFragment.navController, this)
    } catch(ex: ClassCastException) {
        Log.w("NavigationActivity", "User has triggered ClassCastException on Activity restart from onStart(), possibly because they've nav'd to a fragment whose behaviour is not managed by Android Navigation.")
        null
    }

@ExperimentalCoroutinesApi
fun Fragment.findLoadingDestinationAwareNavController(): NavController {
    val navController = findNavController()
    return addLoadingAwareNavListener(navController, requireActivity() as AppCompatActivity)
}

suspend fun<T : Any> StateFlow<T>.collectLatestWithLoading(lifecycleOwner: LifecycleOwner, navController: NavController, defaultActionId: NavDirections,
                                                           loadingCls: KClass<*>, onDestinationPop: () -> Unit,
                                                           action: suspend (value: T) -> Unit) {
    collectLatest {
        Log.i("Extensions", "collectLatestWithLoading -> Current State in Flow collect: ${it.toString()} w/ loadingCls: ${loadingCls.simpleName}")

        navController.currentBackStackEntry?.savedStateHandle?.set(LoadingFragment.LOADING_KEY, true)
        val loading = loadingCls == it::class
        
        if(loading) {
            if(navController.currentDestination?.id != R.id.loadingFragment &&
                    navController.getCurrentDestinationToLoadingNavGraphActionIdNoRec(null)
                            != null) {
                navController.currentBackStackEntry?.savedStateHandle
                    ?.getLiveData(LoadingFragment.BACK_KEY, false)?.observe(lifecycleOwner, Observer { backed ->
                        Log.i("Extensions", "collectLatestWithLoading -> BACK_KEY received w/ backed: ${backed}")
                        if(backed) {
                            onDestinationPop()
                        }
                    })
                navController.navigate(navController.getCurrentDestinationToLoadingNavGraphActionId(defaultActionId))
            }
            navController.currentBackStackEntry?.savedStateHandle?.set(LoadingFragment.LOADING_KEY, false)
        } else action(it)
    }
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

@Suppress("DEPRECATION") // Deprecated for third party apps. Still returns active user services tho
fun <T> Context.isServiceForegrounded(service: Class<T>) =
    (getSystemService(ACTIVITY_SERVICE) as? ActivityManager)
        ?.getRunningServices(Integer.MAX_VALUE)
        ?.find { it.service.className == service.name }
        ?.foreground == true

// credit to Utsav Branwal from SO https://stackoverflow.com/questions/6005245/how-to-have-a-gridview-that-adapts-its-height-when-items-are-added
fun GridView.setHeightBasedOnChildren(noOfColumns: Int) {
    val gridViewAdapter = adapter ?: return // adapter is not set yet
    var totalHeight: Int //total height to set on grid view
    val items = gridViewAdapter.count //no. of items in the grid
    val rows: Int //no. of rows in grid
    val listItem: View = gridViewAdapter.getView(0, null, this)
    listItem.measure(0, 0)
    totalHeight = listItem.measuredHeight
    val x: Float
    if (items > noOfColumns) {
        x = (items / noOfColumns).toFloat()

        //Check if exact no. of rows of rows are available, if not adding 1 extra row
        rows = if (items % noOfColumns != 0) {
            (x + 1).toInt()
        } else {
            x.toInt()
        }
        totalHeight *= rows

        //Adding any vertical space set on grid view
        totalHeight += verticalSpacing * rows
    }

    //Setting height on grid view
    val params = layoutParams
    params.height = totalHeight
    layoutParams = params
}

@Throws(IOException::class)
fun Context.downloadAndSaveImage(url: String, name: String) {
    // bitmap code:
    val downloadManagerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == DownloadManager.ACTION_NOTIFICATION_CLICKED) {
                val dm = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                this@downloadAndSaveImage.startActivity(dm)
            } else {
                Log.w(
                    "Extensions",
                    "downloadAndSaveImage -> received broadcast w/ invalid intent action!"
                )
            }
            this@downloadAndSaveImage.unregisterReceiver(this)
        }
    }

    registerReceiver(
        downloadManagerReceiver,
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE).apply {
            addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        }
    )

    val downloadManagerRequest: DownloadManager.Request = DownloadManager.Request(Uri.parse(url))
        .setAllowedNetworkTypes(
            DownloadManager.Request.NETWORK_WIFI or
                    DownloadManager.Request.NETWORK_MOBILE
        )
        .setAllowedOverRoaming(false)
        .setTitle("$name.jpg")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setDescription("Downloading image...").setMimeType("image/jpg")
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/Hobbyfi/")
    val downloadManager: DownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(downloadManagerRequest)
}

fun RecyclerView.isLinearFirstVisible(): Boolean {
    val layoutManager = layoutManager as LinearLayoutManager
    val pos = layoutManager.findFirstCompletelyVisibleItemPosition()
    return pos == 0
}

fun JSONObject.toPlainStringMap(): Map<String, String> = keys().asSequence().associateWith {
    this[it].toString()
}

fun android.content.Intent.putDestructedMapExtra(data: Map<String, String>) {
    putExtra(Constants.DATA_KEYS, data.keys.toTypedArray())
    putExtra(Constants.DATA_VALUES, data.values.toTypedArray())
}

fun android.content.Intent.putDeletedModelIdExtra(data: Map<String, String>) =
    putExtra(
        Constants.DELETED_MODEL_ID, (data[Constants.ID] ?: error("Data ID must not be null!"))
            .toLong()
    )

fun android.content.Intent.putDeletedModelUserSentIdExtra(data: Map<String, String>) =
    putExtra(
        Constants.DELETED_MODEL_USER_SENT_ID,
        (data[Constants.USER_SENT_ID] ?: error("Data User sent ID must not be null!"))
            .toLong()
    )

// generic go rippppp
fun android.content.Intent.putParcelableUserExtra(data: Map<String, String>) {
    putExtra(Constants.PARCELABLE_MODEL, User(data))
}

fun android.content.Intent.putParcelableMessageExtra(data: Map<String, String>) {
    putExtra(Constants.PARCELABLE_MODEL, Message(data))
}

fun android.content.Intent.putParcelableChatroomExtra(data: Map<String, String>) {
    putExtra(Constants.PARCELABLE_MODEL, Chatroom(data))
}

fun android.content.Intent.putParcelableEventExtra(data: Map<String, String>) {
    putExtra(Constants.PARCELABLE_MODEL, Event(data))
}

fun android.content.Intent.getDestructedMapExtra(): Map<String, String?> {
    val keys = extras?.get(Constants.DATA_KEYS) as Array<String>
    val values = extras?.get(Constants.DATA_VALUES) as Array<String>
    return keys.zip(values)
        .toMap()
}

fun android.content.Intent.getDeletedModelIdExtra(): Long = extras?.getLong(Constants.DELETED_MODEL_ID)!!

fun android.content.Intent.getEventIdsExtra(): List<Long> {
    return Constants.jsonConverter.fromJson(
        extras?.getString(
            Constants.EVENT_IDS
        )
    )!!
}

fun ChipGroup.reinitChipsByTags(tags: List<Tag>?): Boolean {
    removeAllViews()
    return if(tags != null && tags.isNotEmpty()) {
        tags.toList().forEach { tag ->
            val chip = Chip(context).apply {
                text = tag.name
                isCheckable = false
                layoutDirection = View.LAYOUT_DIRECTION_LOCALE
                chipBackgroundColor = ColorStateList(
                    arrayOf(
                        IntArray(0)
                    ),
                    IntArray(1) {
                        ColourUtils.getColourOrGreen(tag.colour)
                    }
                )
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }

            addView(chip)
        }
        true
    } else {
        false
    }
}

val Throwable.isCritical get() = this is Repository.ReauthenticationException || this is InstantiationException ||
        this is io.jsonwebtoken.lang.InstantiationException || this is Repository.NetworkException ||
        this is Repository.UnknownErrorException || this is TokenUtils.InvalidStoredTokenException

fun <T : Any> PagingDataAdapter<T, *>.extractListFromCurrentPagingData(): List<T> {
    val list = mutableListOf<T>()
    for(i in 0..itemCount) {
        try {
            val model = peek(i)
            if(model != null) {
                list.add(model)
            }
        } catch (ex: IndexOutOfBoundsException) {
            Log.i("extractListFromPData", "Skipping out of bounds")
        }
    }
    return list
}

fun <T : Any> PagingDataAdapter<T, *>.findItemFromCurrentPagingData(predicate: (T?) -> Boolean): T? =
    snapshot().find(predicate)

fun <T : Any> PagingDataAdapter<T, *>.findItemPositionFromCurrentPagingData(item: T): Int? {
    val snapshot = snapshot()
    snapshot.items.forEachIndexed { index, t ->
        Log.i("findIPositionFCPData", "item: ${t}")
        if(t == item) {
            return index
        }
    }
    return null
}

fun Bundle.toReadable(): String {
    var string = "{"
    for (key in keySet()) {
        string += " " + key + "=>" + get(key) + ";"
    }
    string += " }"
    return string
}

fun JSONObject.toBundle(): Bundle? {
    val bundle = Bundle()
    val it = keys()
    while (it.hasNext()) {
        val key = it.next()
        val arr = optJSONArray(key)
        val num = optDouble(key)
        val str = optString(key)
        if (arr != null && arr.length() <= 0) bundle.putStringArray(
            key,
            arrayOf()
        ) else if (arr != null && !java.lang.Double.isNaN(arr.optDouble(0))) {
            val newarr = DoubleArray(arr.length())
            for (i in 0 until arr.length()) newarr[i] = arr.optDouble(i)
            bundle.putDoubleArray(key, newarr)
        } else if (arr != null && arr.optString(0) != null) {
            val newarr = arrayOfNulls<String>(arr.length())
            for (i in 0 until arr.length()) newarr[i] = arr.optString(i)
            bundle.putStringArray(key, newarr)
        } else if (!num.isNaN()) bundle.putDouble(key, num) else if (str != null) bundle.putString(
            key,
            str
        ) else Log.e(
            "Extensions",
            "JsonObject toBundle() -> unable to transform json to bundle $key"
        )
    }
    return bundle
}


/**
 * Manages the various graphs needed for a [BottomNavigationView].
 *
 * This sample is a workaround until the Navigation Component supports multiple back stacks.
 */
fun BottomNavigationView.setupWithNavController(
    navGraphIds: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int,
    intent: android.content.Intent,
    defaultIndex: Int = 0
): LiveData<NavController> {

    // Map of tags
    val graphIdToTagMap = SparseArray<String>()
    // Result. Mutable live data with the selected controlled
    val selectedNavController = MutableLiveData<NavController>()

    var firstFragmentGraphId = 0

    // First create a NavHostFragment for each NavGraph ID
    navGraphIds.forEachIndexed { index, navGraphId ->
        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment = obtainNavHostFragment(
            fragmentManager,
            fragmentTag,
            navGraphId,
            containerId
        )

        // Obtain its id
        val graphId = navHostFragment.navController.graph.id

        if (index == defaultIndex) {
            firstFragmentGraphId = graphId
        }

        // Save to the map
        graphIdToTagMap[graphId] = fragmentTag

        // Attach or detach nav host fragment depending on whether it's the selected item.
        if (this.selectedItemId == graphId) {
            // Update livedata with the selected graph
            selectedNavController.value = navHostFragment.navController
            attachNavHostFragment(fragmentManager, navHostFragment, index == defaultIndex)
        } else {
            detachNavHostFragment(fragmentManager, navHostFragment)
        }
    }

    // Now connect selecting an item with swapping Fragments
    var selectedItemTag = graphIdToTagMap[this.selectedItemId]
    val firstFragmentTag = graphIdToTagMap[firstFragmentGraphId]
    var isOnFirstFragment = selectedItemTag == firstFragmentTag

    // When a navigation item is selected
    setOnNavigationItemSelectedListener { item ->
        // Don't do anything if the state is state has already been saved.
        if (fragmentManager.isStateSaved) {
            false
        } else {
            val newlySelectedItemTag = graphIdToTagMap[item.itemId]
            if (selectedItemTag != newlySelectedItemTag) {
                // Pop everything above the first fragment (the "fixed start destination")
                fragmentManager.popBackStack(
                    firstFragmentTag,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                        as NavHostFragment

                // Exclude the first fragment tag because it's always in the back stack.
                if (firstFragmentTag != newlySelectedItemTag) {
                    // Commit a transaction that cleans the back stack and adds the first fragment
                    // to it, creating the fixed started destination.
                    fragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.nav_default_enter_anim,
                            R.anim.nav_default_exit_anim,
                            R.anim.nav_default_pop_enter_anim,
                            R.anim.nav_default_pop_exit_anim
                        )
                        .attach(selectedFragment)
                        .setPrimaryNavigationFragment(selectedFragment)
                        .apply {
                            // Detach all other Fragments
                            graphIdToTagMap.forEach { _, fragmentTagIter ->
                                if (fragmentTagIter != newlySelectedItemTag) {
                                    detach(fragmentManager.findFragmentByTag(firstFragmentTag)!!)
                                }
                            }
                        }
                        .addToBackStack(firstFragmentTag)
                        .setReorderingAllowed(true)
                        .commit()
                }
                selectedItemTag = newlySelectedItemTag
                isOnFirstFragment = selectedItemTag == firstFragmentTag
                selectedNavController.value = selectedFragment.navController
                true
            } else {
                false
            }
        }
    }

    // Optional: on item reselected, pop back stack to the destination of the graph
    setupItemReselected(graphIdToTagMap, fragmentManager)

    // Handle deep link
    setupDeepLinks(navGraphIds, fragmentManager, containerId, intent)

    // Finally, ensure that we update our BottomNavigationView when the back stack changes
    fragmentManager.addOnBackStackChangedListener {
        if (!isOnFirstFragment && !fragmentManager.isOnBackStack(firstFragmentTag)) {
            this.selectedItemId = firstFragmentGraphId
        }

        // Reset the graph if the currentDestination is not valid (happens when the back
        // stack is popped after using the back button).
        selectedNavController.value?.let { controller ->
            if (controller.currentDestination == null) {
                controller.navigate(controller.graph.id)
            }
        }
    }
    return selectedNavController
}

private fun BottomNavigationView.setupDeepLinks(
    navGraphIds: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int,
    intent: android.content.Intent
) {
    navGraphIds.forEachIndexed { index, navGraphId ->
        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment = obtainNavHostFragment(
            fragmentManager,
            fragmentTag,
            navGraphId,
            containerId
        )
        // Handle Intent
        if (navHostFragment.navController.handleDeepLink(intent)
            && selectedItemId != navHostFragment.navController.graph.id) {
            this.selectedItemId = navHostFragment.navController.graph.id
        }
    }
}

private fun BottomNavigationView.setupItemReselected(
    graphIdToTagMap: SparseArray<String>,
    fragmentManager: FragmentManager
) {
    setOnNavigationItemReselectedListener { item ->
        val newlySelectedItemTag = graphIdToTagMap[item.itemId]
        val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                as NavHostFragment
        val navController = selectedFragment.navController
        // Pop the back stack to the start destination of the current navController graph
        navController.popBackStack(
            navController.graph.startDestination, false
        )
    }
}

private fun detachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment
) {
    fragmentManager.beginTransaction()
        .detach(navHostFragment)
        .commitNow()
}

private fun attachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment,
    isPrimaryNavFragment: Boolean
) {
    fragmentManager.beginTransaction()
        .attach(navHostFragment)
        .apply {
            if (isPrimaryNavFragment) {
                setPrimaryNavigationFragment(navHostFragment)
            }
        }
        .commitNow()

}

private fun obtainNavHostFragment(
    fragmentManager: FragmentManager,
    fragmentTag: String,
    navGraphId: Int,
    containerId: Int
): NavHostFragment {
    // If the Nav Host fragment exists, return it
    val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
    existingFragment?.let { return it }

    // Otherwise, create it and return it.
    val navHostFragment = NavHostFragment.create(navGraphId)
    fragmentManager.beginTransaction()
        .add(containerId, navHostFragment, fragmentTag)
        .commitNow()
    return navHostFragment
}

private fun FragmentManager.isOnBackStack(backStackName: String): Boolean {
    val backStackCount = backStackEntryCount
    for (index in 0 until backStackCount) {
        if (getBackStackEntryAt(index).name == backStackName) {
            return true
        }
    }
    return false
}

private fun getFragmentTag(index: Int) = "bottomNavigation#$index"