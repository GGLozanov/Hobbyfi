package com.example.hobbyfi.adapters.chatroom

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.adapters.tag.ChatroomTagListAdapter
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.setHeightBasedOnChildren
import com.example.hobbyfi.utils.GlideUtils
import com.google.android.material.button.MaterialButton
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class ChatroomListAdapter(
    onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null
) : BaseChatroomListAdapter<ChatroomListAdapter.ChatroomListViewHolder>(onJoinChatroomButton) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomListViewHolder {
        return ChatroomListViewHolder.getInstance(parent, prefConfig)
    }

    override fun onBindViewHolder(holder: ChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)

        with(holder) {
            bind(chatroom, position)
            initChatroomJoinButtonListener(chatroom, onJoinChatroomButton)
        }
    }

    class ChatroomListViewHolder(
        private val binding: ChatroomCardBinding,
        prefConfig: PrefConfig
    ) : BaseChatroomViewHolder(binding.root, prefConfig) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup, prefConfig: PrefConfig): ChatroomListViewHolder {
                val binding: ChatroomCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.chatroom_card,
                    parent, false
                )
                return ChatroomListViewHolder(binding, prefConfig)
            }
        }

        override fun bind(chatroom: Chatroom?, position: Int) {
            binding.chatroomLeaveButton.isVisible =
                false
            binding.chatroom = chatroom
            bindChatroomPhotoAndTags(chatroom, position)
        }

        override val chatroomJoinButton: MaterialButton
            get() = binding.chatroomJoinButton
        override val chatroomImageView: ImageView
            get() = binding.chatroomImage
        override val tagsGridView: GridView
            get() = binding.tagsGridView
    }
}