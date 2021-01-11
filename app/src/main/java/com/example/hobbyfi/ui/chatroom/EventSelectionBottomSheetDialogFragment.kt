package com.example.hobbyfi.ui.chatroom

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.databinding.FragmentEventSelectionBottomSheetDialogBinding
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.ui.base.BaseActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class EventSelectionBottomSheetDialogFragment : ChatroomBottomSheetDialogFragment() {
    // TODO: Pass in events directly (FOR NOW) because this will ONLY be avaialble for admin user

    private val eventListAdapter: EventListAdapter = EventListAdapter(
        activityViewModel.authEvents.value ?: emptyList(),
        { view: View, event: Event ->

        },
        { view: View, event: Event ->

        }
    )

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