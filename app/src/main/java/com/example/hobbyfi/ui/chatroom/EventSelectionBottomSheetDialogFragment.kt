package com.example.hobbyfi.ui.chatroom

import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.databinding.FragmentEventSelectionBottomSheetDialogBinding
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EventSelectionBottomSheetDialogFragment : ChatroomBottomSheetDialogFragment() {
    // TODO: Pass in events directly (FOR NOW) because this will ONLY be avaialble for admin user

    private lateinit var eventListAdapter: EventListAdapter
    private lateinit var binding: FragmentEventSelectionBottomSheetDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEventSelectionBottomSheetDialogBinding.inflate(
            layoutInflater,
            container,
            false
        )

        eventListAdapter = EventListAdapter(
                activityViewModel.authEvents.value ?: emptyList(),
        ) { _: View, event: Event ->
            val dialog = (parentFragmentManager.findFragmentByTag(event.id.toString())
                    as EventEditDialogFragment?)
                ?: EventEditDialogFragment.newInstance(event)
            dialog.show(parentFragmentManager, event.id.toString())
        }

        setViewsVisibilityOnEvents(activityViewModel.authEvents.value ?: emptyList())

        with(binding) {
            eventList.layoutParams.height = DisplayMetrics().heightPixels / 3
            eventList.requestLayout()
            eventList.adapter = eventListAdapter

            BottomSheetBehavior.from(bottomSheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
            }

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
            observeEvents()

            return@onCreateView root
        }
    }
    
    private fun observeEvents() {
        activityViewModel.authEvents.observe(viewLifecycleOwner, Observer {
            setViewsVisibilityOnEvents(it)

            eventListAdapter.setEvents(it)
        })
    }

    private fun setViewsVisibilityOnEvents(events: List<Event>) {
        with(binding) {
            noEventsText.isVisible = events.isEmpty()
            deleteOldEventsButton.isVisible = events.isNotEmpty()
        }
    }

    companion object {
        fun newInstance() = EventSelectionBottomSheetDialogFragment()
    }
}