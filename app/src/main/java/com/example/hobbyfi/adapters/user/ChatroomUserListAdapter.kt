package com.example.hobbyfi.adapters.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.databinding.UserCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.User

class ChatroomUserListAdapter : PagingDataAdapter<User, ChatroomUserListAdapter.ChatroomUserViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomUserViewHolder {
        return ChatroomUserViewHolder.getInstance(parent)
    }

    override fun onBindViewHolder(holder: ChatroomUserViewHolder, position: Int) {

    }

    class ChatroomUserViewHolder(binding: UserCardBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomUserViewHolder {
                val binding = UserCardBinding.inflate(LayoutInflater.from(parent.context))
                return ChatroomUserViewHolder(binding)
            }
        }


        fun bind(user: User?) {
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