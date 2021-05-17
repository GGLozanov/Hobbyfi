package com.example.hobbyfi.adapters.chatroom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.PrefConfig
import com.google.android.material.button.MaterialButton


class ChatroomListAdapter(
    onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
    onTagsViewButton: ((view: View, chatroom: Chatroom) -> Unit)
) : BaseChatroomListAdapter<ChatroomListAdapter.ChatroomListViewHolder>(onJoinChatroomButton, onTagsViewButton) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomListViewHolder {
        return ChatroomListViewHolder.getInstance(parent, prefConfig, onJoinChatroomButton, onTagsViewButton)
    }

    override fun onBindViewHolder(holder: ChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)

        with(holder) {
            bind(chatroom, position)
        }
    }

    class ChatroomListViewHolder(
        private val binding: ChatroomCardBinding,
        prefConfig: PrefConfig,
        onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
        onTagsViewButton: ((view: View, chatroom: Chatroom) -> Unit)
    ) : BaseChatroomViewHolder(binding.root, prefConfig, onJoinChatroomButton, onTagsViewButton) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup, prefConfig: PrefConfig, onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
                            onTagsViewButton: ((view: View, chatroom: Chatroom) -> Unit)): ChatroomListViewHolder {
                val binding: ChatroomCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.chatroom_card,
                    parent, false
                )
                return ChatroomListViewHolder(binding, prefConfig, onJoinChatroomButton, onTagsViewButton)
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
        override val tagsViewButton: AppCompatImageButton = binding.tagsViewButton
    }
}