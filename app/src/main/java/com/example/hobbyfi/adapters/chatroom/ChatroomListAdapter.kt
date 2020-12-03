package com.example.hobbyfi.adapters.chatroom

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.models.Chatroom


class ChatroomListAdapter(
    private val onJoinChatroomButtonCallback: OnJoinChatroomButtonPressed) :
    PagingDataAdapter<Chatroom, ChatroomListAdapter.ChatroomListViewHolder>(DIFF_CALLBACK) {

    interface OnJoinChatroomButtonPressed {
        fun onJoinChatroomButtonPress(view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomListViewHolder {
        return ChatroomListViewHolder.getInstance(parent)
    }

    override fun onBindViewHolder(holder: ChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)
        (holder as? ChatroomListViewHolder)?.bind(chatroom)
//        holder.binding.chatroomImage.load(
//
//        )
        // TODO: Viewbinding Coil init, GridLayoutManager & Simple tag adapter
    }

    class ChatroomListViewHolder(val binding: ChatroomCardBinding, view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomListViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: ChatroomCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.chatroom_card,
                    parent, false
                )
                return ChatroomListViewHolder(binding, binding.root)
            }
        }


        fun bind(chatroom: Chatroom?) {
            binding.chatroom = chatroom
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