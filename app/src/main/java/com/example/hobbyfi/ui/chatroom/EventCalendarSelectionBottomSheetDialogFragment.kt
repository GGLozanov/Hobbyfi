package com.example.hobbyfi.ui.chatroom

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.navigation.fragment.findNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.event.EventListAdapter
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addDistinctFragmentToBackStack
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EventCalendarSelectionBottomSheetDialogFragment : EventSelectionBottomSheetDialogFragment() {
    override val eventsSource: LiveData<List<Event>>
        by lazy { activityViewModel.authEvents.map {
            it.filter { event -> (requireArguments()[Constants.CALENDAR_DAY] as CalendarDay) ==
                    event.calendarDayFromDate }
        } } // livedata filter {} go brrr

    override val eventListAdapter: EventListAdapter by lazy {
        EventListAdapter(
            eventsSource.value ?: emptyList(),
            { v: View, event: Event ->
                v.isEnabled = false

                parentFragmentManager.addDistinctFragmentToBackStack(
                    event.id.toString(),
                    R.id.nav_host_fragment
                ) { EventDetailsFragment.newInstance(event) }
                dismiss()

                v.postDelayed({
                    v.isEnabled = true
                }, 1000) // event card tap antispam
            }, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.findViewById<MaterialButton>(R.id.delete_old_events_button).isVisible = false
        view.findViewById<MaterialTextView>(R.id.current_events_header).isVisible = false
        return view
    }

    companion object {
        fun newInstance(calendarDay: CalendarDay): EventCalendarSelectionBottomSheetDialogFragment {
            val instance = EventCalendarSelectionBottomSheetDialogFragment()
            val args = Bundle()
            args.putParcelable(Constants.CALENDAR_DAY, calendarDay)
            instance.arguments = args

            return instance
        }
    }
}