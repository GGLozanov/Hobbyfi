package com.example.hobbyfi.adapters.chatroom

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import coil.load
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.ChatroomTagListAdapter
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.setHeightBasedOnChildren
import com.example.hobbyfi.utils.ColourUtils


class ChatroomListAdapter(
    private inline val onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
    private inline val onLeaveChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null) :
    PagingDataAdapter<Chatroom, ChatroomListAdapter.ChatroomListViewHolder>(DIFF_CALLBACK) {

    private var shouldDisplayLeaveChatroomButtons: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomListViewHolder {
        return ChatroomListViewHolder.getInstance(parent)
    }

    override fun onBindViewHolder(holder: ChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)

        with(holder) {
            bind(chatroom)
            binding.isAuthUserChatroom = shouldDisplayLeaveChatroomButtons
            binding.chatroomJoinButton.setOnClickListener {
                if (chatroom != null) {
                    onJoinChatroomButton?.invoke(it, chatroom)
                }
            }
            binding.chatroomLeaveButton.setOnClickListener {
                if(chatroom != null) {
                    onLeaveChatroomButton?.invoke(it, chatroom)
                }
            }
        }
    }

    class ChatroomListViewHolder(val context: Context, val binding: ChatroomCardBinding, view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomListViewHolder {
                val binding: ChatroomCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.chatroom_card,
                    parent, false
                )
                return ChatroomListViewHolder(parent.context, binding, binding.root)
            }
        }


        fun bind(chatroom: Chatroom?) {
            binding.chatroom = chatroom
            with(binding) {
                if(chatroom?.photoUrl != null) {
                    chatroomImage.load(
                        chatroom.photoUrl
                    )
                }
                if(chatroom?.tags != null) {
                    val adapter = ChatroomTagListAdapter(chatroom.tags, itemView.context, R.layout.chatroom_tag_card)
                    tagsGridView.setHeightBasedOnChildren(chatroom.tags.size)

                    tagsGridView.adapter = adapter
                }
            }

            // TODO: Handle possible performance issue from notifications with pagination
            // todo: databinding init here
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
        shouldDisplayLeaveChatroomButtons = shouldDisplay
        notifyDataSetChanged() // eh, not really true but have to notify somehow
    }
}