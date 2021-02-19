package com.example.hobbyfi.shared

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.GridView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.paging.PagingDataAdapter
import com.example.hobbyfi.R
import com.example.hobbyfi.models.*
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.utils.TokenUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.lang.reflect.Field

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

fun<T : Comparable<*>> T?.equalsOrBiggerThan(comp: T?): Boolean =
    compareValues(this, comp).run {
        this >= 0
    }

fun<T : Comparable<*>> T?.equalsOrLessThan(comp: T?): Boolean =
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

fun NavigationView.clearCurrentMenuAndInflate(menuId: Int) {
    menu.clear()
    inflateMenu(menuId)
}

fun Context.buildYesNoAlertDialog(
    dialogMessage: String,
    onConfirm: DialogInterface.OnClickListener, onCancel: DialogInterface.OnClickListener,
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

fun AppCompatActivity.comeFromAuthDeepLink(): Boolean = (intent.extras?.get("+clicked_branch_link") as String?)?.toBoolean() == true

fun comeFromAuthDeepLink(intent: Intent): Boolean = (intent.extras?.get("+clicked_branch_link") as String?)?.toBoolean() == true

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
        Constants.DELETED_MODEL_USER_SENT_ID, (data[Constants.USER_SENT_ID] ?: error("Data User sent ID must not be null!"))
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

fun android.content.Intent.getDestructedMapExtra(): Map<String?, String?> {
    val keys = extras?.get(Constants.DATA_KEYS) as Array<String>
    val values = extras?.get(Constants.DATA_VALUES) as Array<String>
    return keys.zip(values)
        .toMap()
}

fun android.content.Intent.getDeletedModelIdExtra(): Long = extras?.getLong(Constants.DELETED_MODEL_ID)!!

fun android.content.Intent.getEventIdsExtra(): List<Long> {
    return Constants.tagJsonConverter.fromJson(
        extras?.getString(
            Constants.EVENT_IDS
        )
    )!!
}

val Throwable.isCritical get() = this is Repository.ReauthenticationException || this is InstantiationException ||
        this is io.jsonwebtoken.lang.InstantiationException || this is Repository.NetworkException ||
        this is Repository.UnknownErrorException || this is TokenUtils.InvalidStoredTokenException

fun <T : Model> PagingDataAdapter<T, *>.extractModelListFromCurrentPagingData(): List<T> {
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

fun <T : Model> PagingDataAdapter<T, *>.findItemFromCurrentPagingData(predicate: (T) -> Boolean): T? {
    return extractModelListFromCurrentPagingData().find(predicate)
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
        ) else Log.e("Extensions",
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