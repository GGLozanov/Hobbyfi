package com.example.hobbyfi.adapters.event

import android.view.View
import android.view.ViewGroup
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.adapters.user.ChatroomUserListAdapter
import com.example.hobbyfi.databinding.EventCardBinding
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.PrefConfig
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class EventListAdapter(
    private var events: List<Event>,
    private val onManagePressButton: (View, Event) -> Unit,
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>(), KodeinAware {

    @ExperimentalPagingApi
    override val kodein: Kodein by kodein(MainApplication.applicationContext)

    private val prefConfig: PrefConfig by instance(tag = "prefConfig")

    class EventViewHolder(
        binding: EventCardBinding,
        private val prefConfig: PrefConfig
    ) : BaseViewHolder<Event>(binding.root) {
        override fun bind(model: Event?, position: Int) {
            TODO("Not yet implemented")
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int = events.size

    fun setEvents(events: List<Event>) {
        this.events = events
    }
}