package com.example.hobbyfi.adapters.chatroom

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.adapters.tag.ChatroomTagListAdapter
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.setHeightBasedOnChildren
import com.example.hobbyfi.utils.GlideUtils
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class ChatroomListAdapter(
    private inline val onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
    private inline val onLeaveChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null) :
    PagingDataAdapter<Chatroom, ChatroomListAdapter.ChatroomListViewHolder>(DIFF_CALLBACK), KodeinAware {

    @ExperimentalPagingApi
    override val kodein: Kodein by kodein(MainApplication.applicationContext)

    private val prefConfig: PrefConfig by instance(tag = "prefConfig")

    private var shouldDisplayLeaveChatroomButton: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomListViewHolder {
        return ChatroomListViewHolder.getInstance(parent, prefConfig)
    }

    override fun onBindViewHolder(holder: ChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)

        with(holder) {
            bind(chatroom, position)
            initChatroomLeaveButtonListener(chatroom, onLeaveChatroomButton, shouldDisplayLeaveChatroomButton)
            initChatroomJoinButtonListener(chatroom, onJoinChatroomButton)
        }
    }

    class ChatroomListViewHolder(private val binding: ChatroomCardBinding, private val prefConfig: PrefConfig) : BaseViewHolder<Chatroom>(binding.root) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup, prefConfig: PrefConfig): ChatroomListViewHolder {
                val binding: ChatroomCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.chatroom_card,
                    parent, false
                )
                return ChatroomListViewHolder(binding, prefConfig)
            }
        }


        override fun bind(chatroom: Chatroom?, position: Int) {
            binding.chatroom = chatroom
            with(binding) {
                Log.i("ChatroomListAdapter", "Chatroom w/ id ${chatroom?.id} profile picture url: ${chatroom?.photoUrl}")
                if(chatroom?.photoUrl != null) {
                    Glide.with(itemView.context)
                        .load(chatroom.photoUrl) // TODO: Find a way to inject PrefConfig's singleton instance
                        .signature(
                            GlideUtils.getPagingObjectKey(
                                prefConfig,
                                position,
                                R.string.pref_last_chatrooms_fetch_time,
                                Constants.chatroomPageSize
                            )
                        )
                        // calculate current page based on item position
                        .into(binding.chatroomImage)
                } else {
                    Glide.with(itemView.context)
                        .load(
                            R.drawable.chatroom_default_pic
                        )
                        .into(binding.chatroomImage)
                }
                if(chatroom?.tags != null) {
                    val adapter = ChatroomTagListAdapter(chatroom.tags!!, itemView.context, R.layout.chatroom_tag_card)
                    tagsGridView.setHeightBasedOnChildren(chatroom.tags!!.size)

                    tagsGridView.adapter = adapter
                }
            }
        }

        fun initChatroomLeaveButtonListener(chatroom: Chatroom?,
                    onLeaveChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)?, shouldDisplayLeaveChatroomButton: Boolean) {
            binding.chatroomLeaveButton.isVisible =
                shouldDisplayLeaveChatroomButton
            binding.chatroomLeaveButton.setOnClickListener {
                if(chatroom != null) {
                    onLeaveChatroomButton?.invoke(it, chatroom)
                }
            }
        }

        fun initChatroomJoinButtonListener(chatroom: Chatroom?, onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)?) {
            binding.chatroomJoinButton.setOnClickListener {
                if (chatroom != null) {
                    onJoinChatroomButton?.invoke(it, chatroom)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<Chatroom>() {
            // Chatroom details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(oldChatroom: Chatroom,
                                         newChatroom: Chatroom) = oldChatroom.id == newChatroom.id

            override fun areContentsTheSame(oldChatroom: Chatroom,
                                            newChatroom: Chatroom) = oldChatroom == newChatroom
        }

    }

    fun setLeaveChatroomButtonVisibility(shouldDisplay: Boolean) {
        shouldDisplayLeaveChatroomButton = shouldDisplay
        notifyDataSetChanged() // eh, not really true but have to notify somehow
    }
}