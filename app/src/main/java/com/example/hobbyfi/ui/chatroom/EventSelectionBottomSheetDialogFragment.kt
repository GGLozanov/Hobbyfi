package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.databinding.FragmentEventSelectionBottomSheetDialogBinding
import com.example.hobbyfi.models.data.Event
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
abstract class EventSelectionBottomSheetDialogFragment : ChatroomBottomSheetDialogFragment() {
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

        val initialEvents = eventsSource.value ?: arrayListOf()

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

            return@onCreateView root
        }
    }

    override fun onStart() {
        super.onStart()
        observeEvents()
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