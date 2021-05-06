package com.example.hobbyfi.ui.chatroom

import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.message.ChatroomMessageAdapter
import com.example.hobbyfi.models.ui.UIMessage
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest

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
            viewModel.mainState.collectLatestWithNonIdleReset(
                listOf(MessageListState.Idle::class, MessageListState.OnData.MessagesResult::class),
                viewModel::resetMessageListState
            ) {
                when(it) {
                    is MessageListState.Idle -> {

                    }
                    is MessageListState.OnData.MessagesResult -> {
                        lifecycleScope.launchWhenCreated {
                            it.messages.catch { e ->
                                e.printStackTrace()
                                if(e.isCritical) {
                                    context?.showFailureToast(getString(R.string.reauth_error))
                                    (requireActivity() as ChatroomActivity).handleAuthActionableError(
                                        e.message,
                                        false,
                                        shouldExit = true,
                                        context = requireContext()
                                    )
                                } else if(e !is CancellationException) {
                                    Log.i("ChatroomMListFragment", "it.messages collect() received a normal exception: $e")
                                    context?.showFailureToast(e.message ?: getString(R.string.something_wrong))
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
                    }
                }
            }
        }
    }

    protected open fun onPostMessageListCollect(currentMessages: PagingData<UIMessage>, qMessageId: Long? = null) {
        // does nothing by default
    }
}