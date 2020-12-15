package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChatroomMessageListBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.currentNavigationFragment
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.android.synthetic.main.activity_chatroom.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.Observer

@ExperimentalCoroutinesApi
class ChatroomMessageListFragment : ChatroomFragment() {
    // TODO: Init adapter, loader
    private val viewModel: ChatroomMessageListFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChatroomMessageListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_chatroom_message_list, container, false)
        binding.viewModel = viewModel

        // TODO: Handle expired token error & logout

        // TODO: BroadcastReceiver here triggered => insert message and remote keys (calculate them based on adapter dataset itemCount divided by page size)
        // TODO: They will be used later in the RemoteMediator
        // TODO: If notification for new message is received => insert message into database => trigger APPEND
        // TODO: In RemoteMediator, check if REFRESH loadstate => has new message in database with its remote key =>
        // TODO: return Mediator Success and load new list with new message?
        // TODO: Append message to room db so that it can be pushed to the paging data automatically;

        with(binding) {
            messageList.addItemDecoration(VerticalSpaceItemDecoration(10))

            return@onCreateView root
        }
    }

    override fun onStart() {
        super.onStart()
        activityViewModel.authChatroom.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            setToolbarProperties(it)
        })
        activityViewModel.isAuthUserChatroomOwner.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if(it) {
                activity?.toolbar
                    ?.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_admin_panel_settings_24)
            }
        })
    }

    private fun setToolbarProperties(chatroom: Chatroom?) {
        val activity = requireActivity() as ChatroomActivity
        activity.title = chatroom?.name
    }
}