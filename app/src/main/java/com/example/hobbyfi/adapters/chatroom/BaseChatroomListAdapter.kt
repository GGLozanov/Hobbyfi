package com.example.hobbyfi.adapters.chatroom

import android.content.Context
import android.view.View
import android.widget.GridView
import android.widget.ImageView
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.adapters.tag.ChatroomTagListAdapter
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

abstract class BaseChatroomListAdapter<VH : RecyclerView.ViewHolder>(
    protected inline val onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null
) : PagingDataAdapter<Chatroom, VH>(DIFF_CALLBACK), KodeinAware {

    @ExperimentalPagingApi
    override val kodein: Kodein by kodein(MainApplication.applicationContext)

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")

    abstract class BaseChatroomViewHolder(
        itemView: View,
        protected val prefConfig: PrefConfig
    ) : BaseViewHolder<Chatroom>(itemView) {
        protected fun bindChatroomPhotoAndTags(
            chatroom: Chatroom?,
            position: Int
        ) {
            if(chatroom?.photoUrl != null) {
                Glide.with(itemView.context)
                    .load(chatroom.photoUrl)
                    .signature(
                        GlideUtils.getPagingObjectKey(
                            prefConfig,
                            position,
                            R.string.pref_last_chatrooms_fetch_time,
                            Constants.chatroomPageSize
                        )
                    )
                    // calculate current page based on item position
                    .into(chatroomImageView)
            } else {
                Glide.with(itemView.context)
                    .load(
                        R.drawable.chatroom_default_pic
                    )
                    .into(chatroomImageView)
            }
            if(chatroom?.tags != null) {
                val adapter = ChatroomTagListAdapter(chatroom.tags!!, itemView.context, R.layout.chatroom_tag_card)
                tagsGridView.setHeightBasedOnChildren(chatroom.tags!!.size)

                tagsGridView.adapter = adapter
            }
        }

        fun initChatroomJoinButtonListener(
            chatroom: Chatroom?,
            onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)?,
        ) {
            chatroomJoinButton.setOnClickListener {
                if (chatroom != null) {
                    onJoinChatroomButton?.invoke(it, chatroom)
                }
            }
        }

        abstract val chatroomImageView: ImageView
        abstract val tagsGridView: GridView
        abstract val chatroomJoinButton: MaterialButton
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