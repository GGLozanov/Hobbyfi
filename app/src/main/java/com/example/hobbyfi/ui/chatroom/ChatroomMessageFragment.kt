package com.example.hobbyfi.ui.chatroom

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.map
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.message.ChatroomMessageAdapter
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.ui.UIMessage
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

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
                                    (requireActivity() as ChatroomActivity).handleAuthActionableError(
                                        e.message,
                                        false,
                                        shouldExit = true,
                                        context = requireContext()
                                    )
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
                            false,
                            shouldExit = it.shouldExit,
                            context = requireContext()
                        ) // TODO: Might make this a bit too coupled to the activity. . .
                        viewModel.resetMessageListState()
                    }
                }
            }
        }
    }

    protected open fun onPostMessageListCollect(currentMessages: PagingData<UIMessage>, qMessageId: Long? = null) {
        // does nothing by default
    }
}