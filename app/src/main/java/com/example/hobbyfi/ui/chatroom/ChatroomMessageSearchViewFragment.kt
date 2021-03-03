package com.example.hobbyfi.ui.chatroom

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.example.hobbyfi.adapters.message.ChatroomMessageSearchListAdapter
import com.example.hobbyfi.databinding.FragmentMessageSearchViewBinding
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.ui.UIMessage
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isConnected
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageSearchViewFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

@ExperimentalPagingApi
@ExperimentalCoroutinesApi
class ChatroomMessageSearchViewFragment : ChatroomMessageFragment() {
    @ExperimentalCoroutinesApi
    @ExperimentalPagingApi
    override val viewModel: ChatroomMessageSearchViewFragmentViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var binding: FragmentMessageSearchViewBinding

    override val messageListAdapter: ChatroomMessageSearchListAdapter by lazy {
        ChatroomMessageSearchListAdapter(
            activityViewModel.chatroomUsers.value ?: arrayListOf(),
        ) { _, message ->
            navController.previousBackStackEntry?.savedStateHandle?.set(Constants.searchMessage, message)
            navController.popBackStack()
        }
    }

    // custom adapter for RV (takes pagingdata again but displays different cards)
    // and can filter through stuff
    // initially always empty but on searchview get injected data (?)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMessageSearchViewBinding.inflate(layoutInflater, container, false)

        with(binding) {
            initMessageListAdapter()
            observeMessagesState()
            observeUsers()

            val searchManager = requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager
            searchView.apply {
                // Assumes current hosting activity is the searchable activity
                setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
                setIconifiedByDefault(false) // Do not iconify the widget; expand it by default
            }

            return@onCreateView root
        }
    }

    @ExperimentalPagingApi
    @ExperimentalCoroutinesApi
    override fun observeUsers() {
        activityViewModel.chatroomUsers.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            Log.i("ChatroomMListFragment", "SEARCH Adapter users: $it")
            messageListAdapter.setCurrentUsers(it)
        })
    }

    override fun initMessageListAdapter() {
        with(binding) {
            searchList.addItemDecoration(VerticalSpaceItemDecoration(15))
            searchList.adapter = messageListAdapter.withLoadStateFooter(loadStateAdapter)
        }
    }

    suspend fun filterMessages(query: String) {
        if(!connectivityManager.isConnected()) {
            // if the user is not connected, work with whatever data there already exists
            val currentMessages: List<Message>? =
                navController.previousBackStackEntry?.savedStateHandle?.get(Constants.currentMessages)

            currentMessages?.filter {
                it.message.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) // super simple for now
            }?.let { messageListAdapter.submitData(PagingData.from(it.map { message -> UIMessage.MessageItem(message) })) }
        } else {
            viewModel.setCurrentMessages(null)
            activityViewModel.authChatroom.value?.let {
                viewModel.sendIntent(
                    MessageListIntent.FetchMessages(
                        it.id,
                        query
                    )
                )
            }
        }
    }

    // FIXME: copied from ChatroomModelFragment
    // TODO: Refactor with navdestinationchanged for navcontroller
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as ChatroomActivity)
            .binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        // hacky way to fix drawer but... so is life. Use toolbars and navviews on individual fragments, kids!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @ExperimentalCoroutinesApi
    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as ChatroomActivity)
            .binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }
}