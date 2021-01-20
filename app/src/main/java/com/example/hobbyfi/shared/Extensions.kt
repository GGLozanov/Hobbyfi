package com.example.hobbyfi.shared

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.GridView
import androidx.core.util.Predicate
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.models.*
import com.example.hobbyfi.repositories.Repository
import com.example.spendidly.utils.PredicateTextWatcher
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.fromJson(json: String?) = fromJson<T>(
    json,
    object : TypeToken<T>() {}.type
)

inline fun <reified T> Gson.fromJson(json: JsonElement?) = fromJson<T>(
    json,
    object : TypeToken<T>() {}.type
)

fun TextInputLayout.addTextChangedListener(errorText: String, predicate: Predicate<String>): PredicateTextWatcher {
    val watcher = PredicateTextWatcher(
        this,
        errorText,
        predicate
    )
    this.editText!!.addTextChangedListener(
        watcher
    )
    return watcher
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

fun <T> List<T>.replace(newValue: T, predicate: (T) -> Boolean): List<T> {
    return map {
        if (predicate(it)) newValue else it
    }
}

val FragmentManager.currentNavigationFragment: Fragment?
    get() = primaryNavigationFragment?.childFragmentManager?.fragments?.first()

fun NavigationView.clearCurrentMenuAndInflate(menuId: Int) {
    menu.clear()
    inflateMenu(menuId)
}

fun RecyclerView.listIsAtTop(): Boolean {
    return if (childCount == 0) true else
        getChildAt(0).top == 0 &&
                (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() == 0
}

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

fun android.content.Intent.getEventIdsExtra(): List<Long> = Constants.tagJsonConverter.fromJson(
    extras?.getString(
        Constants.EVENT_IDS
    )
)!!

val Exception.isCritical get() = this is Repository.ReauthenticationException || this is InstantiationException ||
        this is InstantiationError || this is Repository.NetworkException ||
        this is Repository.UnknownErrorException

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

/**
 * Manages the various graphs needed for a [BottomNavigationView].
 *
 * This sample is a workaround until the Navigation Component supports multiple back stacks.
 */
fun BottomNavigationView.setupWithNavController(
    navGraphIds: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int,
    intent: android.content.Intent
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

        if (index == 0) {
            firstFragmentGraphId = graphId
        }

        // Save to the map
        graphIdToTagMap[graphId] = fragmentTag

        // Attach or detach nav host fragment depending on whether it's the selected item.
        if (this.selectedItemId == graphId) {
            // Update livedata with the selected graph
            selectedNavController.value = navHostFragment.navController
            attachNavHostFragment(fragmentManager, navHostFragment, index == 0)
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