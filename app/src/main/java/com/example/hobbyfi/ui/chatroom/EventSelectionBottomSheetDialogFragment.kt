package com.example.hobbyfi.ui.chatroom

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.databinding.FragmentEventSelectionBottomSheetDialogBinding
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.ui.base.BaseActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class EventSelectionBottomSheetDialogFragment : ChatroomBottomSheetDialogFragment() {
    private val eventListAdapter: EventListAdapter = EventListAdapter(
        activityViewModel.authEvents.value ?: emptyList(),
        { view: View, event: Event ->

        },
        { view: View, event: Event ->

        }
    )

    // TODO: When user taps leave/join button, resend FetchEvent intent to resync data
    // TODO: if user requests to leave/join a deleted event => don't allow them
    // TODO: if user requests to leave/join an updated event => send the event with the new data and save in VM
    // TODO: Use cache if offline => use remote if connected
    // TODO: Require connection for user taps on buttons

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentEventSelectionBottomSheetDialogBinding = FragmentEventSelectionBottomSheetDialogBinding.inflate(
            layoutInflater,
            container,
            false
        )

        with(binding) {
            return@onCreateView binding.root
        }
    }
    
    private fun observeEvents() {
        activityViewModel.authEvents.observe(viewLifecycleOwner, Observer { 
            
        })
    }
    
    private fun observeChatroom() {
        activityViewModel.authChatroom.observe(viewLifecycleOwner, Observer {

        })
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // viewModel.setAuthEvents(null) // retrigger fetch from either cache or network
    }

    private fun observeConnectionRefresh() {
        (requireActivity() as BaseActivity).refreshConnectivityMonitor.observe(viewLifecycleOwner, Observer { connectionRefreshed ->
            if(connectionRefreshed) {
                Log.i("ChatroomListFragment", "ChatroomListFragment CONNECTED")
                // TODO: refresh events send intent
            } else {
                Log.i("ChatroomListFragment", "ChatroomListFragment DIS-CONNECTED")
            }
        })
    }
}