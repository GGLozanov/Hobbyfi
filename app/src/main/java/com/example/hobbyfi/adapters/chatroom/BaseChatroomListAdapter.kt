package com.example.hobbyfi.adapters.chatroom

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Button
import android.widget.GridView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.ImageLoaderViewHolder
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.Tag
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
    protected inline val onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
    protected inline val onTagsViewButton: ((view: View, chatroom: Chatroom) -> Unit)
) : PagingDataAdapter<Chatroom, VH>(DIFF_CALLBACK), KodeinAware {

    @ExperimentalPagingApi
    override val kodein: Kodein by kodein(MainApplication.applicationContext)

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")

    abstract class BaseChatroomViewHolder(
        itemView: View,
        prefConfig: PrefConfig,
        private inline val onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)?,
        private inline val onTagsViewButton: (view: View, chatroom: Chatroom) -> Unit
    ) : ImageLoaderViewHolder<Chatroom>(itemView, prefConfig) {
        override fun bind(model: Chatroom?, position: Int) {
            bindImage(model, position)
            initChatroomButtonListener(model, chatroomJoinButton) {
                onJoinChatroomButton?.invoke(
                    tagsViewButton,
                    model!!
                )
            }

            initChatroomButtonListener(model, tagsViewButton) {
                onTagsViewButton.invoke(
                    tagsViewButton,
                    model!!
                )
            }
        }

        private fun initChatroomButtonListener(
            chatroom: Chatroom?,
            button: View,
            callback: () -> Unit
        ) {
            button.setOnClickListener {
                if (chatroom != null) {
                    callback()
                }
            }
        }

        override val signatureGenerator: (position: Int) -> ObjectKey = { position ->
            GlideUtils.getPagingObjectKey(
                prefConfig,
                position,
                R.string.pref_last_chatrooms_fetch_time,
                Constants.chatroomPageSize
            )
        }
        override val defaultPicDrawable: Drawable by lazy {
            ContextCompat.getDrawable(itemView.context, R.drawable.chatroom_default_pic)!!
        }

        abstract val tagsViewButton: AppCompatImageButton
        abstract val chatroomJoinButton: MaterialButton
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<Chatroom>() {
            // Chatroom details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(oldChatroom: Chatroom,
                                         newChatroom: Chatroom
            ) = oldChatroom.id == newChatroom.id

            override fun areContentsTheSame(oldChatroom: Chatroom,
                                            newChatroom: Chatroom
            ) = oldChatroom == newChatroom
        }

    }
}