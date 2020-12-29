package com.example.hobbyfi.adapters.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.databinding.UserCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.User
import com.example.hobbyfi.viewmodels.base.BaseViewModel

// Discord doesn't do pagination for their guild users...
// ...so neither will I!
class ChatroomUserListAdapter(private var users: List<User>) :
    RecyclerView.Adapter<ChatroomUserListAdapter.ChatroomUserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomUserViewHolder {
        return ChatroomUserViewHolder.getInstance(parent)
    }

    override fun onBindViewHolder(holder: ChatroomUserViewHolder, position: Int) {

    }

    override fun getItemCount(): Int {
        return users.size
    }

    class ChatroomUserViewHolder(binding: UserCardBinding) : BaseViewHolder<User>(binding.root) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomUserViewHolder {
                val binding = UserCardBinding.inflate(LayoutInflater.from(parent.context))
                return ChatroomUserViewHolder(binding)
            }
        }


        override fun bind(user: User?, position: Int) {
        }
    }

    fun setUsers(users: List<User>) {
        this.users = users
    }
}