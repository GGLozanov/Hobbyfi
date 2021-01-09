package com.example.hobbyfi.adapters.chatroom

import android.view.View
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.models.Chatroom

abstract class BaseChatroomListAdapter<VH : RecyclerView.ViewHolder>(
    protected inline val onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null
) : PagingDataAdapter<Chatroom, VH>(DIFF_CALLBACK) {

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