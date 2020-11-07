package com.example.hobbyfi.adapters.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.User

class ChatroomUserListAdapter : PagingDataAdapter<User, ChatroomUserListAdapter.ChatroomUserViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomUserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_card, parent, false)
        return ChatroomUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatroomUserViewHolder, position: Int) {
//        holder.idView.text = item.id
//        holder.contentView.text = item.content
    }

    class ChatroomUserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomListAdapter.ChatroomListViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.chatroom_card, parent, false)
                return ChatroomListAdapter.ChatroomListViewHolder(view)
            }
        }


        fun bind(chatroom: Chatroom?) {
            //loads image from network using coil extension function
            // todo: databinding init here
        }
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<User>() {
            // Chatroom details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(oldUser: User,
                                         newUser: User
            ) = oldUser.id == newUser.id

            override fun areContentsTheSame(oldUser: User,
                                            newUser: User
            ) = oldUser == newUser
        }

    }
}