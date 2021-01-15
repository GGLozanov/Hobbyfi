package com.example.hobbyfi.ui.chatroom

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.databinding.FragmentEventSelectionBottomSheetDialogBinding
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.BaseActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EventSelectionBottomSheetDialogFragment : ChatroomBottomSheetDialogFragment() {
    // TODO: Pass in events directly (FOR NOW) because this will ONLY be avaialble for admin user

    private lateinit var eventListAdapter: EventListAdapter

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

        eventListAdapter = EventListAdapter(
                activityViewModel.authEvents.value ?: emptyList(),
        ) { _: View, event: Event ->
            findNavController().navigate(
                EventSelectionBottomSheetDialogFragmentDirections
                    .actionEventSelectionBottomSheetDialogFragmentToEventEditDialogFragment(
                        event
                    )
            )
        }

        with(binding) {
            BottomSheetBehavior.from(bottomSheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            eventList.layoutParams.height = DisplayMetrics().heightPixels / 3

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
            if(it.isEmpty()) {

            }

            eventListAdapter.setEvents(it)
        })
    }
    
    private fun observeChatroom() {
        activityViewModel.authChatroom.observe(viewLifecycleOwner, Observer {

        })
    }
}