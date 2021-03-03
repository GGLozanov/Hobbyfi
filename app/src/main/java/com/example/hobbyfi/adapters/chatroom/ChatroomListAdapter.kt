package com.example.hobbyfi.adapters.chatroom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.shared.PrefConfig
import com.google.android.material.button.MaterialButton


class ChatroomListAdapter(
    onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null
) : BaseChatroomListAdapter<ChatroomListAdapter.ChatroomListViewHolder>(onJoinChatroomButton) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomListViewHolder {
        return ChatroomListViewHolder.getInstance(parent, prefConfig)
    }

    override fun onBindViewHolder(holder: ChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)

        with(holder) {
            bind(chatroom, position)
            initChatroomJoinButtonListener(chatroom, onJoinChatroomButton)
        }
    }

    class ChatroomListViewHolder(
        private val binding: ChatroomCardBinding,
        prefConfig: PrefConfig
    ) : BaseChatroomViewHolder(binding.root, prefConfig) {
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
            super.bind(chatroom, position)
            binding.chatroomLeaveButton.isVisible =
                false
            binding.chatroom = chatroom
        }

        override val chatroomJoinButton: MaterialButton = binding.chatroomJoinButton
        override val mainImageView: ImageView = binding.chatroomImage
        override val tagsGridView: GridView = binding.tagsGridView
    }
}