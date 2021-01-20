package com.example.hobbyfi.ui.chatroom

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.databinding.FragmentEventSelectionBottomSheetDialogBinding
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.listIsAtTop
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.viewmodels.chatroom.EventSelectionBottomSheetDialogFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class EventSelectionBottomSheetDialogFragment : ChatroomBottomSheetDialogFragment() {
    // TODO: Pass in events directly (FOR NOW) because this will ONLY be avaialble for admin user
    private val viewModel: EventSelectionBottomSheetDialogFragmentViewModel by viewModels()

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

        val initialEvents = activityViewModel.authEvents.value ?: emptyList()
        eventListAdapter = EventListAdapter(
            initialEvents,
            { _: View, event: Event ->
                val dialog = (parentFragmentManager.findFragmentByTag(event.id.toString())
                        as EventEditDialogFragment?)
                    ?: EventEditDialogFragment.newInstance(event)
                dialog.setTargetFragment(this, 400)
                dialog.show(parentFragmentManager, event.id.toString())
            }, { _: View, event: Event ->
                Constants.buildYesNoAlertDialog(
                    requireContext(),
                    requireContext().getString(R.string.delete_event),
                    { dialogInterface: DialogInterface, _: Int ->
                        lifecycleScope.launch {
                            viewModel.sendIntent(
                                EventIntent.DeleteEvent(event.id)
                            )
                        }
                        dialogInterface.dismiss()
                    },
                    { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                )
            })

        setViewsVisibilityOnEvents(initialEvents)

        with(binding) {
            val behaviour = BottomSheetBehavior.from(bottomSheet)
            behaviour.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            bottomSheet.requestLayout() // reinit layout for RV notifyDataSetChanged()
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {

                    }
                })
            }

            eventList.addItemDecoration(VerticalSpaceItemDecoration(15))
            eventList.adapter = eventListAdapter
            // TODO: Recyclerview height responsive size (if it doesn't work - static height)
            // scaleViewByScreenSizeAndReLayout(eventList, behaviour, bottomSheet, bottomSheetCoordinator, 3)

            deleteOldEventsButton.setOnClickListener {
                Constants.buildYesNoAlertDialog(
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
            observeEventState()
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

    private fun observeEventState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                when(it) {
                    is EventState.Idle -> {

                    }
                    is EventState.Loading -> {

                    }
                    is EventState.OnData.EventDeleteResult -> {
                        activityViewModel.sendEventsIntent(EventListIntent.DeleteAnEventCache(it.eventId))
                        Toast.makeText(
                            requireContext(),
                            "Event successfuly deleted!",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                    is EventState.Error -> {
                        // TODO: Handle error
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
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