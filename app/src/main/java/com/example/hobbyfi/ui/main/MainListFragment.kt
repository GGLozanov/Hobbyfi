package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.chatroom.BaseChatroomListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomListBinding
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.RefreshConnectionAware
import com.example.hobbyfi.ui.chatroom.ChatroomActivity
import com.example.hobbyfi.ui.chatroom.ChatroomMessageBottomSheetDialogFragment
import com.example.hobbyfi.ui.shared.TagViewBottomSheetDialogFragment
import com.example.hobbyfi.viewmodels.main.ChatroomListFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.google.android.gms.tasks.OnFailureListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
abstract class MainListFragment<T: BaseChatroomListAdapter<*>> : MainFragment(), RefreshConnectionAware {
    protected val viewModel: ChatroomListFragmentViewModel by viewModels()
    protected lateinit var binding: FragmentChatroomListBinding

    protected var updateJob: Job? = null

    protected val chatroomFlowCollectExceptionHandler: suspend FlowCollector<PagingData<Chatroom>>.(cause: Throwable) -> Unit = { e: Throwable ->
        e.printStackTrace()
        if(e !is CancellationException) {
            Log.i("ChatroomListFragment", "state.chatrooms collect() received a normal exception: $e")
            binding.root.showFailureSnackbar(e.message ?: getString(R.string.something_wrong))
            if(e.isCritical) {
                (requireActivity() as MainActivity).logout()
            }
        }
    }

    protected val onChatroomJoinButton: (View, Chatroom) -> Unit = { _: View, chatroom: Chatroom ->
        viewModel.setButtonSelectedChatroom(chatroom)
        val userChatroomIds = activityViewModel.authUser.value?.chatroomIds

        // if user does not have a chatroom (kinda redundant check)
        if (userChatroomIds == null || !userChatroomIds.contains(chatroom.id)) {
            // If user join chatroom is successful, delete other chatrooms from cache (+remote keys) and load only their chatroom from cache
            lifecycleScope.launch {
                activityViewModel.sendIntent(
                    UserIntent.UpdateUser(
                        mapOf(
                            Pair(
                                Constants.CHATROOM_ID, chatroom.id.toString()
                            )
                        )
                    )
                )
            }
        } else {
            // otherwise simply allow the user to join their chatroom
            updateJob = lifecycleScope.launch {
                prefConfig.writeLastEnteredChatroomId(chatroom.id)
                navigateToChatroomPerDeepLinkExtras()
            }
        }
    }

    protected val onTagsViewButton: ((view: View, chatroom: Chatroom) -> Unit) = { _: View, chatroom: Chatroom ->
        parentFragmentManager.showDistinctDialog("ChatroomSheet" + chatroom.id.toString(), {
            TagViewBottomSheetDialogFragment.newInstance(
                chatroom.tags,
                chatroom.name
            )
        })
    }

    protected abstract val chatroomListAdapter: T

    protected abstract fun observeAuthUser()

    protected abstract fun observeChatroomsState()

    protected abstract fun observeChatroomEntryState()

    protected var loadStateAdapter: DefaultLoadStateAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_chatroom_list,
            container, false
        )

        binding.viewModel = viewModel

        with(binding) {
            lifecycleScope.launch {
                chatroomListAdapter.loadStateFlow
                    // Only emit when REFRESH LoadState for RemoteMediator changes.
                    .distinctUntilChangedBy { loadState -> loadState.refresh }
                    // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                    .filter { loadState -> loadState.refresh is LoadState.NotLoading }
                    .collect { binding.swiperefresh.isRefreshing = false }
            }

            initChatroomListAdapter()
            observeConnectionRefresh(savedInstanceState, (requireActivity() as BaseActivity).refreshConnectivityMonitor)

            return@onCreateView root
        }
    }

    override fun onStart() {
        super.onStart()
        observeChatroomEntryState()
        observeAuthUser()
        observeChatroomsState()
    }

    private fun initChatroomListAdapter() {
        with(binding) {
            chatroomList.addItemDecoration(VerticalSpaceItemDecoration(20))
            loadStateAdapter = DefaultLoadStateAdapter({
                chatroomListAdapter.retry()
            }, {
                navigateToChatroomCreate()
            })

            chatroomList.adapter = chatroomListAdapter.withLoadStateFooter(loadStateAdapter!!)

            swiperefresh.setOnRefreshListener {
                if(chatroomListAdapter.itemCount == 0) {
                    swiperefresh.isRefreshing = false
                    return@setOnRefreshListener
                }

                chatroomListAdapter.refresh()
                // should trickle down to remote mediator and VM
            }
        }
    }

    protected fun navigateToChatroomPerDeepLinkExtras() {
        if(activityViewModel.deepLinkExtras != null &&
            viewModel.buttonSelectedChatroom?.id ==
                activityViewModel.deepLinkExtras?.getDouble(Constants.CHATROOM_ID)?.toLong()) {
            startActivity(android.content.Intent(requireContext(), ChatroomActivity::class.java).apply {
                putExtras(activityViewModel.deepLinkExtras!!)
            })

            activityViewModel.setDeepLinkExtras(null)
            ActivityCompat.finishAffinity(requireActivity())
        } else {
            navigateToChatroom()
        }
    }

    protected open fun navigateToChatroom() {
        (requireActivity() as MainActivity).disconnectServerSocket()
    }

    protected abstract fun navigateToChatroomCreate()

    override fun observeConnectionRefresh(savedState: Bundle?, refreshConnectivityMonitor: RefreshConnectivityMonitor) {
        super.observeConnectionRefresh(savedState, refreshConnectivityMonitor)
        refreshConnectivityMonitor.observe(viewLifecycleOwner, Observer { connectionRefreshed ->
            if(connectionRefreshed) {
                Log.i("MainListFragment", "MainListFragment CONNECTED")
                refreshDataOnConnectionRefresh()
            } else {
                Log.i("MainListFragment", "MainListFragment DIS-CONNECTED")
            }
        })
    }

    override fun refreshDataOnConnectionRefresh() {
        chatroomListAdapter.refresh()
    }
}