package com.example.hobbyfi.adapters.chatroom

import android.view.View
import android.view.ViewGroup
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.databinding.ChatroomCardBinding
import com.example.hobbyfi.databinding.JoinedChatroomCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.shared.PrefConfig

class JoinedChatroomListAdapter(
    onJoinChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null,
    private inline val onLeaveChatroomButton: ((view: View, chatroom: Chatroom) -> Unit)? = null
): BaseChatroomListAdapter<JoinedChatroomListAdapter.JoinedChatroomListViewHolder>(onJoinChatroomButton) {

    private var userOwnedChatroomIds: List<Long> = emptyList()

    class JoinedChatroomListViewHolder(
        binding: JoinedChatroomCardBinding,
        private val prefConfig: PrefConfig
    ) : BaseViewHolder<Chatroom>(binding.root) {

        override fun bind(model: Chatroom?, position: Int) {
            TODO("Not yet implemented")
        }
    }

    override fun onBindViewHolder(holder: JoinedChatroomListViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): JoinedChatroomListViewHolder {
        TODO("Not yet implemented")
    }

    fun setUserOwnedChatroomIds(userOwnedChatroomIds: List<Long>) {
        this.userOwnedChatroomIds = userOwnedChatroomIds
    }
}