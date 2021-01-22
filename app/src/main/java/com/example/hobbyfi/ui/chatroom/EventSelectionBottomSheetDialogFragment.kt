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
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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
abstract class EventSelectionBottomSheetDialogFragment : ChatroomBottomSheetDialogFragment() {
    // TODO: Pass in events directly (FOR NOW) because this will ONLY be avaialble for admin user

    protected abstract val eventListAdapter: EventListAdapter
    protected lateinit var binding: FragmentEventSelectionBottomSheetDialogBinding
    protected abstract val eventsSource: LiveData<List<Event>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentEventSelectionBottomSheetDialogBinding.inflate(
            layoutInflater,
            container,
            false
        )

        val initialEvents = eventsSource.value ?: emptyList()

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

            observeEvents()

            return@onCreateView root
        }
    }

    private fun observeEvents() {
        eventsSource.observe(viewLifecycleOwner, Observer {
            setViewsVisibilityOnEvents(it)

            eventListAdapter.setEvents(it)
        })
    }

    protected open fun setViewsVisibilityOnEvents(events: List<Event>) {
        with(binding) {
            eventScroll.isVisible = events.isNotEmpty()
            noEventsText.isVisible = events.isEmpty()
        }
    }
}