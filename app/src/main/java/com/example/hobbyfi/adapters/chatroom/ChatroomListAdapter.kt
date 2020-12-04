package com.example.hobbyfi.adapters.chatroom

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import coil.load
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.ChatroomTagListAdapter
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.setHeightBasedOnChildren


class ChatroomListAdapter(
    private val onJoinChatroomButtonCallback: OnJoinChatroomButtonPressed) :
    PagingDataAdapter<Chatroom, ChatroomListAdapter.ChatroomListViewHolder>(DIFF_CALLBACK) {

    interface OnJoinChatroomButtonPressed {
        fun onJoinChatroomButtonPress(view: View, chatroom: Chatroom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomListViewHolder {
        return ChatroomListViewHolder.getInstance(parent)
    }

    override fun onBindViewHolder(holder: ChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)

        holder.bind(chatroom)
        holder.binding.chatroomJoinButton.setOnClickListener {
            if (chatroom != null) {
                onJoinChatroomButtonCallback.onJoinChatroomButtonPress(it, chatroom)
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
            //loads image from network using coil extension function
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
}