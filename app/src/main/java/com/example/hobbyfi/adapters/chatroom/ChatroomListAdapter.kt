package com.example.hobbyfi.adapters.chatroom

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.ChatroomTagListAdapter
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.setHeightBasedOnChildren
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein


class ChatroomListAdapter(
    private inline val onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
    private inline val onLeaveChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null) :
    PagingDataAdapter<Chatroom, ChatroomListAdapter.ChatroomListViewHolder>(DIFF_CALLBACK) {

    private var shouldDisplayLeaveChatroomButton: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomListViewHolder {
        return ChatroomListViewHolder.getInstance(parent)
    }

    override fun onBindViewHolder(holder: ChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)

        with(holder) {
            bind(chatroom, position)
            binding.chatroomLeaveButton.isVisible =
                shouldDisplayLeaveChatroomButton
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


        fun bind(chatroom: Chatroom?, position: Int) {
            binding.chatroom = chatroom
            with(binding) {
                Log.i("ChatroomListAdapter", "Chatroom w/ id ${chatroom?.id} profile picture url: ${chatroom?.photoUrl}")
                if(chatroom?.photoUrl != null) {
                    Glide.with(itemView.context)
                        .load(chatroom.photoUrl) // TODO: Find a way to inject PrefConfig's singleton instance
                        .signature(ObjectKey(
                            PrefConfig(context).readLastPrefFetchTime(R.string.pref_last_chatrooms_fetch_time) +
                                if(position % Constants.chatroomPageSize == 0)
                                    position / Constants.chatroomPageSize else (position / Constants.chatroomPageSize) + 1))
                        // calculate current page based on item position
                        .into(binding.chatroomImage)
                } else {
                    Glide.with(itemView.context)
                        .load(
                            R.drawable.chatroom_default_pic
                        )
                        .into(binding.chatroomImage)
                }
                if(chatroom?.tags != null) {
                    val adapter = ChatroomTagListAdapter(chatroom.tags!!, itemView.context, R.layout.chatroom_tag_card)
                    tagsGridView.setHeightBasedOnChildren(chatroom.tags!!.size)

                    tagsGridView.adapter = adapter
                }
            }
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
                                            newChatroom: Chatroom) = oldChatroom.id == newChatroom.id
        }

    }

    fun setLeaveChatroomButtonVisibility(shouldDisplay: Boolean) {
        shouldDisplayLeaveChatroomButton = shouldDisplay
        notifyDataSetChanged() // eh, not really true but have to notify somehow
    }
}