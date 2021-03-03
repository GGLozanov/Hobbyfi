package com.example.hobbyfi.adapters.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.MessageCardBinding
import com.example.hobbyfi.databinding.MessageCardReceiveBinding
import com.example.hobbyfi.databinding.MessageCardSendBinding
import com.example.hobbyfi.databinding.MessageCardTimelineBinding
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.models.ui.UIMessage
import com.example.hobbyfi.shared.PrefConfig

class ChatroomMessageSearchListAdapter(
    currentUsers: List<User>,
    private val onMessagePress: (View, Message) -> Unit
): ChatroomMessageAdapter(currentUsers) {
    private abstract class ChatroomUserMessageSearchViewHolder(
        rootView: View,
        binding: MessageCardBinding,
        users: List<User>,
        prefConfig: PrefConfig,
        protected val onMessagePress: (View, Message) -> Unit
    ): BaseUserChatroomMessageViewHolder(rootView, binding, users, prefConfig) {
        override fun bind(model: UIMessage?, position: Int) {
            super.bind(model, position)
            messageCardBinding.messageCardLayout.setOnClickListener {
                onMessagePress(it, (model as UIMessage.MessageItem).message)
            }
        }
    }

    private class ChatroomReceiveMessageSearchViewHolder(
        binding: MessageCardReceiveBinding,
        users: List<User>,
        prefConfig: PrefConfig,
        onMessagePress: (View, Message) -> Unit
    ): ChatroomUserMessageSearchViewHolder(
        binding.root,
        binding.messageCardReceive,
        users,
        prefConfig,
        onMessagePress
    ) {
        companion object {
            fun getInstance(
                parent: ViewGroup,
                users: List<User>,
                prefConfig: PrefConfig,
                onMessagePress: (View, Message) -> Unit
            ): ChatroomReceiveMessageSearchViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardReceiveBinding =
                    DataBindingUtil.inflate(inflater, R.layout.message_card_receive, parent, false)
                return ChatroomReceiveMessageSearchViewHolder(
                    binding, users,
                    prefConfig, onMessagePress
                )
            }
        }
    }

    private class ChatroomSendMessageSearchViewHolder(
        binding: MessageCardSendBinding,
        users: List<User>,
        prefConfig: PrefConfig,
        onMessagePress: (View, Message) -> Unit
    ): ChatroomUserMessageSearchViewHolder(
        binding.root,
        binding.messageCardSend,
        users,
        prefConfig,
        onMessagePress
    ) {
        companion object {
            fun getInstance(
                parent: ViewGroup,
                users: List<User>,
                prefConfig: PrefConfig,
                onMessagePress: (View, Message) -> Unit
            ): ChatroomSendMessageSearchViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardSendBinding =
                    DataBindingUtil.inflate(inflater, R.layout.message_card_send, parent, false)
                return ChatroomSendMessageSearchViewHolder(
                    binding, users,
                    prefConfig, onMessagePress
                )
            }
        }

        override fun bind(model: UIMessage?, position: Int) {
            super.bind(model, position)
//            messageCardBinding.messageLayout.layoutDirection = View.LAYOUT_DIRECTION_LTR // forcibly set the gravity for search messages
        }
    }

    private class ChatroomTimelineMessageSearchViewHolder(
        binding: MessageCardTimelineBinding,
        private val onMessagePress: (View, Message) -> Unit
    ) : BaseTimelineMessageViewHolder(binding) {
        companion object {
            fun getInstance(
                parent: ViewGroup,
                users: List<User>,
                prefConfig: PrefConfig,
                onMessagePress: (View, Message) -> Unit
            ): ChatroomTimelineMessageSearchViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardTimelineBinding =
                    DataBindingUtil.inflate(
                        inflater, R.layout.message_card_timeline,
                        parent, false
                    )
                return ChatroomTimelineMessageSearchViewHolder(
                    binding, onMessagePress
                )
            }
        }

        override fun bind(model: UIMessage?, position: Int) {
            super.bind(model, position)
            binding.messageCardTimelineLayout.setOnClickListener {
                onMessagePress(it, (model as UIMessage.MessageItem).message)
            }
        }
    }

    override fun getTimelineMessageViewHolderInstance(parent: ViewGroup): BaseTimelineMessageViewHolder =
        ChatroomTimelineMessageSearchViewHolder.getInstance(
            parent,
            currentUsers,
            prefConfig,
            onMessagePress
        )

    override fun getReceiveMessageViewHolderInstance(parent: ViewGroup): BaseUserChatroomMessageViewHolder =
        ChatroomReceiveMessageSearchViewHolder.getInstance(
            parent,
            currentUsers,
            prefConfig,
            onMessagePress
        )

    override fun getSendMessageViewHolderInstance(parent: ViewGroup): BaseUserChatroomMessageViewHolder =
        ChatroomSendMessageSearchViewHolder.getInstance(
            parent,
            currentUsers,
            prefConfig,
            onMessagePress
        )
}