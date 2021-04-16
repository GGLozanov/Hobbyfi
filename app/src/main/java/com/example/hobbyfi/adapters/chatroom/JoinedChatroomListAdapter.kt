package com.example.hobbyfi.adapters.chatroom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.JoinedChatroomCardBinding
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.addAllDistinct
import com.google.android.material.button.MaterialButton

class JoinedChatroomListAdapter(
    onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
    onTagsViewButton: ((view: View, chatroom: Chatroom) -> Unit),
    private inline val onLeaveChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
    private var _userOwnedChatroomIds: MutableList<Long> = mutableListOf()
) : BaseChatroomListAdapter<JoinedChatroomListAdapter.JoinedChatroomListViewHolder>(onJoinChatroomButton, onTagsViewButton) {

    val userOwnedChatroomIds: List<Long> get() = _userOwnedChatroomIds

    class JoinedChatroomListViewHolder(
        private val binding: JoinedChatroomCardBinding,
        prefConfig: PrefConfig,
        onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
        onTagsViewButton: ((view: View, chatroom: Chatroom) -> Unit),
    ) : BaseChatroomViewHolder(binding.root, prefConfig, onJoinChatroomButton, onTagsViewButton) {

        companion object {
            fun getInstance(parent: ViewGroup, prefConfig: PrefConfig, onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
                            onTagsViewButton: ((view: View, chatroom: Chatroom) -> Unit)): JoinedChatroomListViewHolder {
                val binding: JoinedChatroomCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.joined_chatroom_card,
                    parent, false
                )
                return JoinedChatroomListViewHolder(binding, prefConfig, onJoinChatroomButton, onTagsViewButton)
            }
        }

        override val chatroomJoinButton: MaterialButton = binding.joinChatroomButtonBar.rightButton
        override val mainImageView: ImageView = binding.chatroomImage
        override val tagsViewButton: AppCompatImageButton = binding.tagsViewButton

        override fun bind(model: Chatroom?, position: Int) {
            super.bind(model, position)
            binding.chatroom = model
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

        fun initLeaveChatroomButtonListener(
            chatroom: Chatroom?,
            onLeaveChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
            userOwnedChatroomIds: List<Long>
        ) {
            with(binding.joinChatroomButtonBar.leftButton) {
                isVisible = !userOwnedChatroomIds.contains(chatroom?.id)
                setOnClickListener {
                    if(chatroom != null) {
                        onLeaveChatroomButton?.invoke(it, chatroom)
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: JoinedChatroomListViewHolder, position: Int) {
        val chatroom = getItem(position)

        with(holder) {
            bind(chatroom, position)
            initLeaveChatroomButtonListener(chatroom, onLeaveChatroomButton, _userOwnedChatroomIds)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): JoinedChatroomListViewHolder {
        return JoinedChatroomListViewHolder.getInstance(parent, prefConfig, onJoinChatroomButton, onTagsViewButton)
    }

    fun addDistinctUserOwnedChatroomIds(userOwnedChatroomIds: List<Long>) {
        _userOwnedChatroomIds.addAllDistinct(userOwnedChatroomIds)
        notifyDataSetChanged()
    }
}