package com.example.hobbyfi.adapters.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.RequestManager
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.MessageCardBinding
import com.example.hobbyfi.databinding.MessageCardReceiveBinding
import com.example.hobbyfi.databinding.MessageCardSendBinding
import com.example.hobbyfi.databinding.MessageCardTimelineBinding
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.models.ui.UIMessage
import com.example.hobbyfi.shared.PrefConfig


class
ChatroomMessageListAdapter(
    currentUsers: List<User>,
    private var isAuthUserChatroomOwner: Boolean,
    private inline val onMessageLongPress: (View, Message) -> Boolean,
    private inline val onImageMessagePress: (MessageCardBinding) -> Unit
): ChatroomMessageAdapter(currentUsers) {
    private abstract class UserChatroomMessageViewHolder(
        rootView: View,
        messageCardBinding: MessageCardBinding,
        protected val onMessageLongPress: (View, Message) -> Boolean,
        protected val onImageMessagePress: (MessageCardBinding) -> Unit,
        users: List<User>,
        protected val isAuthUserChatroomOwner: Boolean,
        prefConfig: PrefConfig,
    ) : BaseUserChatroomMessageViewHolder(rootView, messageCardBinding, users, prefConfig) {
        override fun bind(model: UIMessage?, position: Int) {
            super.bind(model, position)
            val message = (model as UIMessage.MessageItem?)?.message
            val authUserSentMessage =
                users.find { message?.userSentId == prefConfig.getAuthUserIdFromToken() }

            if(authUserSentMessage != null || isAuthUserChatroomOwner) {
                messageCardBinding.messageCardLayout.setOnLongClickListener {
                    onMessageLongPress(it, message!!)
                }
            }
        }

        override fun loadMessageImage(messageUrl: String, glide: RequestManager) {
            super.loadMessageImage(messageUrl, glide)
            messageCardBinding.messageCardLayout.setOnClickListener {
                onImageMessagePress(messageCardBinding)
            }
        }
    }

    // TODO: Gesture detection for edit message and delete message callbacks
    private class ChatroomSendMessageViewHolder(
        binding: MessageCardSendBinding,
        onMessageLongPress: (View, Message) -> Boolean,
        onImageMessagePress: (MessageCardBinding) -> Unit,
        users: List<User>,
        isAuthUserChatroomOwner: Boolean,
        prefConfig: PrefConfig,
    ) : UserChatroomMessageViewHolder(
        binding.root, binding.messageCardSend, onMessageLongPress, onImageMessagePress,
        users, isAuthUserChatroomOwner, prefConfig
    ) {
        companion object {
            fun getInstance(
                parent: ViewGroup, onMessageLongPress: (View, Message) -> Boolean, onImageMessagePress: (MessageCardBinding) -> Unit,
                users: List<User>, isAuthUserChatroomOwner: Boolean, prefConfig: PrefConfig
            ): ChatroomSendMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardSendBinding =
                    DataBindingUtil.inflate(inflater, R.layout.message_card_send, parent, false)
                return ChatroomSendMessageViewHolder(
                    binding, onMessageLongPress, onImageMessagePress,
                    users, isAuthUserChatroomOwner, prefConfig
                )
            }
        }
    }

    private class ChatroomReceiveMessageViewHolder(
        binding: MessageCardReceiveBinding,
        onMessageLongPress: (View, Message) -> Boolean,
        onImageMessagePress: (MessageCardBinding) -> Unit,
        users: List<User>,
        isAuthUserChatroomOwner: Boolean,
        prefConfig: PrefConfig,
    ) : UserChatroomMessageViewHolder(
        binding.root, binding.messageCardReceive, onMessageLongPress, onImageMessagePress,
        users, isAuthUserChatroomOwner, prefConfig
    ) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(
                parent: ViewGroup, onMessageLongPress: (View, Message) -> Boolean, onImageMessagePress: (MessageCardBinding) -> Unit,
                users: List<User>, isAuthUserChatroomOwner: Boolean, prefConfig: PrefConfig
            ): ChatroomReceiveMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardReceiveBinding =
                    DataBindingUtil.inflate(inflater, R.layout.message_card_receive, parent, false)
                return ChatroomReceiveMessageViewHolder(
                    binding, onMessageLongPress, onImageMessagePress,
                    users, isAuthUserChatroomOwner, prefConfig
                )
            }
        }
    }

    private class ChatroomTimelineMessageViewHolder(
        binding: MessageCardTimelineBinding,
        private val isAuthUserChatroomOwner: Boolean,
        private val onMessageLongPress: (View, Message) -> Boolean
    ) : BaseTimelineMessageViewHolder(binding) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(
                parent: ViewGroup, isAuthUserChatroomOwner: Boolean,
                onMessageLongPress: (View, Message) -> Boolean
            ): ChatroomTimelineMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardTimelineBinding =
                    DataBindingUtil.inflate(
                        inflater, R.layout.message_card_timeline,
                        parent, false
                    )
                return ChatroomTimelineMessageViewHolder(
                    binding,
                    isAuthUserChatroomOwner,
                    onMessageLongPress
                )
            }
        }

        override fun bind(model: UIMessage?, position: Int) {
            val message = (model as UIMessage.MessageItem?)?.message
            binding.message = message

            if(isAuthUserChatroomOwner) {
                binding.messageCardTimelineLayout.setOnLongClickListener {
                    onMessageLongPress(it, message!!)
                }
            }
        }
    }

    fun setAuthUserChatroomOwner(isOwner: Boolean) {
        if(isAuthUserChatroomOwner != isOwner) {
            isAuthUserChatroomOwner = isOwner
            notifyDataSetChanged()
        }
    }

    override fun getTimelineMessageViewHolderInstance(parent: ViewGroup): BaseTimelineMessageViewHolder =
        ChatroomTimelineMessageViewHolder.getInstance(parent, isAuthUserChatroomOwner, onMessageLongPress)

    override fun getReceiveMessageViewHolderInstance(parent: ViewGroup): BaseUserChatroomMessageViewHolder =
        ChatroomReceiveMessageViewHolder.getInstance(parent, onMessageLongPress, onImageMessagePress, currentUsers,
            isAuthUserChatroomOwner, prefConfig)

    override fun getSendMessageViewHolderInstance(parent: ViewGroup): BaseUserChatroomMessageViewHolder =
        ChatroomSendMessageViewHolder.getInstance(parent, onMessageLongPress, onImageMessagePress, currentUsers,
            isAuthUserChatroomOwner, prefConfig)
}