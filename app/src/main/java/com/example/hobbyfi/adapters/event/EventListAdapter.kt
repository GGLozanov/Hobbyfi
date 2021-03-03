package com.example.hobbyfi.adapters.event

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.ImageLoaderViewHolder
import com.example.hobbyfi.databinding.EventCardBinding
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.shared.PrefConfig
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


// TODO: Change onRightButtonPress back to nullable if only admin can share links for event
class EventListAdapter(
    private var events: List<Event>,
    private val onCardPress: (View, Event) -> Unit,
    private val onRightButtonPress: (View, Event) -> Unit, // handles both share to facebook button and delete event button
    private val ownerDisplay: Boolean = false
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

        fun initOnRightButtonPress(
            event: Event,
            onUserSelectionPress: ((View, Event) -> Unit)?,
            ownerDisplay: Boolean
        ) {
            if(ownerDisplay) {
                binding.deleteButton.setOnClickListener {
                    onUserSelectionPress?.invoke(it, event)
                }
            } else {
                // TODO: Change to facebook button without using set share content
                    // otherwise having the right buttons as separate is fucking stupid
                binding.facebookShareButton.setOnClickListener {
                    onUserSelectionPress?.invoke(it, event)
                }
            }
        }

        fun initOnCardPress(event: Event, onCardPress: (View, Event) -> Unit) {
            binding.eventCard.setOnClickListener {
                onCardPress(it, event)
            }
        }

        override val mainImageView: ImageView = binding.eventImage
        override val signatureGenerator: (position: Int) -> ObjectKey = { _: Int ->
            ObjectKey(prefConfig.readLastPrefFetchTime(R.string.pref_last_events_fetch_time)) }
        override val defaultPicDrawable: Drawable by lazy {
            ContextCompat.getDrawable(itemView.context, R.drawable.event_default_pic)!!
        }

        companion object {
            fun getInstance(parent: ViewGroup, prefConfig: PrefConfig, ownerDisplay: Boolean): EventViewHolder {
                val binding: EventCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.event_card,
                    parent, false
                )
                binding.ownerDisplay = ownerDisplay
                return EventViewHolder(binding, prefConfig)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder = EventViewHolder.getInstance(parent, prefConfig, ownerDisplay)

    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int
    ) {
        val event = events[position]

        with(holder) {
            bind(event, position)
            initOnRightButtonPress(event, onRightButtonPress, ownerDisplay)
            initOnCardPress(event, onCardPress)
        }
    }

    override fun getItemCount(): Int = events.size

    fun setEvents(events: List<Event>) {
        this.events = events
        notifyDataSetChanged()
    }
}