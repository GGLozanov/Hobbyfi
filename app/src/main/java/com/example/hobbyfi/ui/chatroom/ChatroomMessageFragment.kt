package com.example.hobbyfi.ui.chatroom

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.message.ChatroomMessageAdapter
import com.example.hobbyfi.adapters.message.ChatroomMessageListAdapter
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class ChatroomMessageFragment : ChatroomFragment() {
    @ExperimentalCoroutinesApi
    @ExperimentalPagingApi
    protected abstract val viewModel: ChatroomMessageViewModel

    protected abstract val messageListAdapter: ChatroomMessageAdapter

    protected val loadStateAdapter: DefaultLoadStateAdapter = DefaultLoadStateAdapter(
        { messageListAdapter.retry() },
        null,
        showOnlyProgessBar = true
    )

    protected abstract fun initMessageListAdapter()

    @ExperimentalPagingApi
    @ExperimentalCoroutinesApi
    protected abstract fun observeUsers()

    @ExperimentalCoroutinesApi
    @ExperimentalPagingApi
    protected fun observeMessagesState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collectLatest {
                when(it) {
                    is MessageListState.Idle, is MessageListState.Loading -> {

                    }
                    is MessageListState.OnData.MessagesResult -> {
                        lifecycleScope.launchWhenCreated {
                            it.messages.catch { e ->
                                e.printStackTrace()
                                if(e.isCritical) {
                                    Toast.makeText(requireContext(), Constants.reauthError, Toast.LENGTH_LONG)
                                        .show()
                                    // TODO: Switch to `startActivityForResult` calls cuz process death
                                    localBroadcastManager.sendBroadcast(Intent(Constants.LOGOUT))
                                } else if(e !is CancellationException) {
                                    Log.i("ChatroomMListFragment", "it.messages collect() received a normal exception: $e")
                                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG)
                                        .show()
                                }
                            }.collectLatest { data ->
                                Log.i("ChatroomMessageFragment", "Collecting message paging data ${data}")
                                messageListAdapter.submitData(data)
                                onPostMessageListCollect(data, it.queriedMessageId)
                                // TODO: Add on initial fetch scroll, not on every
                                // binding.messageList.smoothScrollToPosition(0)
                            }
                        }
                    }
                    is MessageListState.Error -> {
                        (requireActivity() as ChatroomActivity).handleAuthActionableError(
                            it.error,
                            it.shouldExit,
                            context = requireContext()
                        ) // TODO: Might make this a bit too coupled to the activity. . .
                        viewModel.resetMessageListState()
                    }
                }
            }
        }
    }

    protected open fun onPostMessageListCollect(currentMessages: PagingData<Message>, qMessageId: Long? = null) {
        // does nothing by default
    }
}