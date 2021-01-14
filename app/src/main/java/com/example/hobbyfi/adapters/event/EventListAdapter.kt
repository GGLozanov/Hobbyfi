package com.example.hobbyfi.adapters.event

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.adapters.chatroom.JoinedChatroomListAdapter
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
        private val binding: EventCardBinding,
        private val prefConfig: PrefConfig
    ) : BaseViewHolder<Event>(binding.root) {
        override fun bind(model: Event?, position: Int) {
            binding.event = model

        }

        fun initOnManagePressButton(event: Event, onManagePressButton: (View, Event) -> Unit) {
            binding.manageButton.setOnClickListener {
                onManagePressButton(it, event)
            }
        }

        companion object {
            fun getInstance(parent: ViewGroup, prefConfig: PrefConfig): EventViewHolder {
                val binding: EventCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.event_card,
                    parent, false
                )
                return EventViewHolder(binding, prefConfig)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder = EventViewHolder.getInstance(parent, prefConfig)

    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int
    ) {
        val event = events[position]

        with(holder) {
            bind(event, position)

        }
    }

    override fun getItemCount(): Int = events.size

    fun setEvents(events: List<Event>) {
        this.events = events
    }
}