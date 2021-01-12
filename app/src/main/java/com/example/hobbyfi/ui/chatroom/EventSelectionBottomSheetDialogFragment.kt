package com.example.hobbyfi.ui.chatroom

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.databinding.FragmentEventSelectionBottomSheetDialogBinding
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.BaseActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

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
            deleteOldEventsButton.setOnClickListener {
                Constants.buildDeleteAlertDialog(
                    requireContext(),
                    requireContext().getString(R.string.delete_events_batch),
                    { dialogInterface: DialogInterface, _: Int ->
                        lifecycleScope.launch {
                            activityViewModel.sendEventsIntent(EventListIntent.DeleteOldEventsCache)
                        }
                        dialogInterface.dismiss()
                    },
                    { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                )
            }

            observeChatroom()
            observeEvents()

            return@onCreateView binding.root
        }
    }
    
    private fun observeEvents() {
        activityViewModel.authEvents.observe(viewLifecycleOwner, Observer { 
            eventListAdapter.setEvents(it)
        })
    }
    
    private fun observeChatroom() {
        activityViewModel.authChatroom.observe(viewLifecycleOwner, Observer {

        })
    }
}