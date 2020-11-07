package com.example.hobbyfi.adapters.message

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.example.hobbyfi.R
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message


class ChatroomMessageListAdapter : PagingDataAdapter<Message, ChatroomMessageListAdapter.ChatroomMessageViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomMessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_card, parent, false)
        return ChatroomMessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatroomMessageViewHolder, position: Int) {
        val message = getItem(position)
        (holder as? ChatroomMessageViewHolder)?.bind(message)
//        holder.idView.text = item.id
//        holder.contentView.text = item.content
    }

    class ChatroomMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.message_card, parent, false)
                return ChatroomMessageViewHolder(view)
            }
        }


        fun bind(message: Message?) {
            // todo: databinding init here & handle image as
        }
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<Message>() {
            // Chatroom details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(oldMessage: Message,
                                         newMessage: Message
            ) = oldMessage.id == newMessage.id

            override fun areContentsTheSame(oldMessage: Message,
                                            newMessage: Message
            ) = oldMessage == newMessage
        }

    }
}