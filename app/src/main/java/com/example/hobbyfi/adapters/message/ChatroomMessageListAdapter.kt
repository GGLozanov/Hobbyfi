package com.example.hobbyfi.adapters.message

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.example.hobbyfi.R
import com.example.hobbyfi.models.Message


class ChatroomMessageListAdapter : PagingDataAdapter<Message, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomSendMessageViewHolder {
        // TODO: databinding & switch viewType for different message cards!
        when(viewType) {}

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_card_send, parent, false)
        return ChatroomSendMessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        // TODO:
        // (holder as? ChatroomSendMessageViewHolder)?.bind(message)
//        holder.idView.text = item.id
//        holder.contentView.text = item.content
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        // TODO: Check if message user sent id = auth user id
        // TODO: Check if message is notification for user leaving/joining or an event occuring

        return super.getItemViewType(position)
    }

    class ChatroomSendMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomSendMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.message_card_send, parent, false)
                return ChatroomSendMessageViewHolder(view)
            }
        }


        fun bind(message: Message?) {
            // todo: databinding init here & handle image as
        }
    }

    class ChatroomReceiveMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomSendMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.message_card_receive, parent, false)
                return ChatroomSendMessageViewHolder(view)
            }
        }


        fun bind(message: Message?) {
            // todo: databinding init here & handle image as
        }
    }

    class ChatroomTimelineMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomSendMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.message_card_timeline, parent, false)
                return ChatroomSendMessageViewHolder(view)
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