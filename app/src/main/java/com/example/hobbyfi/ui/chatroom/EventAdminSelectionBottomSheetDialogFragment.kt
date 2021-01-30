package com.example.hobbyfi.ui.chatroom

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.buildYesNoAlertDialog
import com.example.hobbyfi.shared.showDistinctDialog
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.viewmodels.chatroom.EventSelectionBottomSheetDialogFragmentViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EventAdminSelectionBottomSheetDialogFragment : EventSelectionBottomSheetDialogFragment() {
    private val viewModel: EventSelectionBottomSheetDialogFragmentViewModel by viewModels()

    override val eventsSource: LiveData<List<Event>>
        get() = activityViewModel.authEvents

    override val eventListAdapter: EventListAdapter by lazy {
        EventListAdapter(
            eventsSource.value ?: emptyList(),
            { v: View, event: Event ->
                v.isEnabled = false
                parentFragmentManager.showDistinctDialog(
                    event.id.toString(),
                    { EventEditDialogFragment.newInstance(event) },
                    this
                )
                v.postDelayed({
                    v.isEnabled = true
                }, 1000) // event card tap antispam
            }, { _: View, event: Event ->
                requireContext().buildYesNoAlertDialog(
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
            }, ownerDisplay = true)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       val view = super.onCreateView(inflater, container, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.delete_old_events_button).setOnClickListener {
            requireContext().buildYesNoAlertDialog(
                requireContext().getString(R.string.delete_events_batch),
                { dialogInterface: DialogInterface, _: Int ->
                    if(areThereOldEventsToDelete()) {
                        lifecycleScope.launch {
                            activityViewModel.sendEventsIntent(EventListIntent.DeleteOldEvents)
                        }
                    } else {
                        Toast.makeText(requireContext(), "No old events to delete!", Toast.LENGTH_LONG)
                            .show()
                    }
                    dialogInterface.dismiss()
                },
                { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            )
        }

        observeEventState()

        return view
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
                        ).show()
                    }
                    is EventState.Error -> {
                        // TODO: Handle error
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    override fun setViewsVisibilityOnEvents(events: List<Event>) {
        super.setViewsVisibilityOnEvents(events)
        binding.deleteOldEventsButton.isVisible = events.isNotEmpty()
        binding.currentEventsHeader.isVisible = events.isNotEmpty()
    }

    private fun areThereOldEventsToDelete(): Boolean {
        eventsSource.value?.forEach {
            if(it.calculateDateDiff().isNegative) {
                return true
            }
        }
        return false
    }

    companion object {
        fun newInstance() = EventAdminSelectionBottomSheetDialogFragment()
    }
}