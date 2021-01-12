package com.example.hobbyfi.adapters.chatroom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.databinding.JoinedChatroomCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.PrefConfig
import com.google.android.material.button.MaterialButton

class JoinedChatroomListAdapter(
    onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
    private inline val onLeaveChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null
) : BaseChatroomListAdapter<JoinedChatroomListAdapter.JoinedChatroomListViewHolder>(onJoinChatroomButton) {

    private var userOwnedChatroomIds: List<Long> = emptyList()

    class JoinedChatroomListViewHolder(
        private val binding: JoinedChatroomCardBinding,
        prefConfig: PrefConfig
    ) : BaseChatroomViewHolder(binding.root, prefConfig) {

        companion object {
            fun getInstance(parent: ViewGroup, prefConfig: PrefConfig): JoinedChatroomListViewHolder {
                val binding: JoinedChatroomCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.joined_chatroom_card,
                    parent, false
                )
                return JoinedChatroomListViewHolder(binding, prefConfig)
            }
        }

        override val chatroomJoinButton: MaterialButton
            get() = binding.joinChatroomButtonBar.rightButton
        override val chatroomImageView: ImageView
            get() = binding.chatroomImage
        override val tagsGridView: GridView
            get() = binding.tagsGridView

        override fun bind(model: Chatroom?, position: Int) {
            binding.chatroom = model
            bindChatroomPhotoAndTags(model, position)
            binding.expandCardButton.setOnClickListener {
                if(binding.subCardLayout.visibility == View.GONE) {
                    TransitionManager.beginDelayedTransition(binding.cardLayout, AutoTransition())
                    binding.subCardLayout.isVisible = true
                    it.setBackgroundResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
                } else {
                    binding.subCardLayout.isVisible = false
                    it.setBackgroundResource(R.drawable.ic_baseline_chevron_right_24)
                }
            }
        }

        fun initLeaveChatroomButtonListener(chatroom: Chatroom?, onLeaveChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null) {
            binding.joinChatroomButtonBar.leftButton.setOnClickListener {
                if(chatroom != null) {
                    onLeaveChatroomButton?.invoke(it, chatroom)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: JoinedChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)

        with(holder) {
            bind(chatroom, position)
            initLeaveChatroomButtonListener(chatroom, onLeaveChatroomButton)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): JoinedChatroomListViewHolder {
        return JoinedChatroomListViewHolder.getInstance(parent, prefConfig)
    }

    fun setUserOwnedChatroomIds(userOwnedChatroomIds: List<Long>) {
        this.userOwnedChatroomIds = userOwnedChatroomIds
    }
}