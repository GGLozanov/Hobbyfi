package com.example.hobbyfi.adapters.event

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.ImageLoaderViewHolder
import com.example.hobbyfi.databinding.EventCardBinding
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.PrefConfig
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class EventListAdapter(
    private var events: List<Event>,
    private val onManagePress: (View, Event) -> Unit,
    private val onDeletePressButton: (View, Event) -> Unit
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>(), KodeinAware {

    @ExperimentalPagingApi
    override val kodein: Kodein by kodein(MainApplication.applicationContext)

    private val prefConfig: PrefConfig by instance(tag = "prefConfig")

    class EventViewHolder(
        private val binding: EventCardBinding,
        prefConfig: PrefConfig,
    ) : ImageLoaderViewHolder<Event>(binding.root, prefConfig) {
        override fun bind(model: Event?, position: Int) {
            binding.event = model
            bindImage(model, position)
        }

        fun initOnDeletePressButton(event: Event, onDeletePressButton: (View, Event) -> Unit) {
            binding.deleteButton.setOnClickListener {
                onDeletePressButton(it, event)
            }
        }

        fun initOnManagePress(event: Event, onManagePress: (View, Event) -> Unit) {
            binding.eventCard.setOnClickListener {
                onManagePress(it, event)
            }
        }

        override val mainImageView: ImageView = binding.eventImage
        override val signatureGenerator: (position: Int) -> ObjectKey = { _: Int ->
            ObjectKey(prefConfig.readLastPrefFetchTime(R.string.pref_last_events_fetch_time)) }
        override val defaultPicResId: Int = R.drawable.event_default_pic

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
            initOnDeletePressButton(event, onDeletePressButton)
            initOnManagePress(event, onManagePress)
        }
    }

    override fun getItemCount(): Int = events.size

    fun setEvents(events: List<Event>) {
        this.events = events
        notifyDataSetChanged()
    }
}