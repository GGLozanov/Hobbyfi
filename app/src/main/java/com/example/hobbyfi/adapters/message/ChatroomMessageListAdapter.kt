package com.example.hobbyfi.adapters.message

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.databinding.MessageCardBinding
import com.example.hobbyfi.databinding.MessageCardReceiveBinding
import com.example.hobbyfi.databinding.MessageCardSendBinding
import com.example.hobbyfi.databinding.MessageCardTimelineBinding
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.GlideUtils
import com.google.android.material.textview.MaterialTextView
import com.example.hobbyfi.models.User
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.android.x.androidXContextTranslators
import org.kodein.di.generic.instance
import java.lang.IllegalArgumentException


class ChatroomMessageListAdapter(
    private var currentUsers: List<User>,
    private inline val onMessageLongPress: (View, Message) -> Boolean)
    : PagingDataAdapter<Message, BaseViewHolder<Message>>(DIFF_CALLBACK), KodeinAware {

    override val kodein: Kodein by kodein(MainApplication.applicationContext) // FIXME: Kodein w/ appcontext bad???

    private val prefConfig: PrefConfig by instance(tag = "prefConfig")

    private enum class MessageType {
        SEND, RECEIVE, TIMELINE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Message> {
        return when(viewType) {
            MessageType.TIMELINE.ordinal -> {
                ChatroomTimelineMessageViewHolder.getInstance(parent)
            }
            MessageType.RECEIVE.ordinal -> {
                ChatroomReceiveMessageViewHolder.getInstance(parent, onMessageLongPress)
            }
            MessageType.SEND.ordinal -> {
                ChatroomSendMessageViewHolder.getInstance(parent, onMessageLongPress)
            }
            else -> throw IllegalArgumentException(Constants.invalidViewType)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<Message>, position: Int) {
        val message = getItem(position)

        holder.bind(message, position)

        when(holder) {
            is ChatroomReceiveMessageViewHolder, is ChatroomSendMessageViewHolder -> {
                handleImageMessageBind(
                    message,
                    position,
                    (holder as ChatroomMessageViewHolder).messageCardBinding,
                    holder.itemView.context
                )
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        if(message?.isTimeline == true) return MessageType.TIMELINE.ordinal

        return if(message?.userSentId == prefConfig.getAuthUserIdFromToken()) MessageType.SEND.ordinal
            else MessageType.RECEIVE.ordinal
    }

    abstract class ChatroomMessageViewHolder(
        rootView: View,
        val messageCardBinding: MessageCardBinding,
        private val onMessageLongPress: (View, Message) -> Boolean
    ) : BaseViewHolder<Message>(rootView) {
        override fun bind(message: Message?, position: Int) {
            messageCardBinding.message = message // DATA BINDING GO BRRRRRR????
            messageCardBinding.messageCardLayout.setOnLongClickListener {
                onMessageLongPress(it, message!!)
            }
        }
    }

    // TODO: Gesture detection for edit message and delete message callbacks
    class ChatroomSendMessageViewHolder(
        binding: MessageCardSendBinding,
        onMessageLongPress: (View, Message) -> Boolean
    ) : ChatroomMessageViewHolder(binding.root, binding.messageCardSend, onMessageLongPress) {
        companion object {
            fun getInstance(parent: ViewGroup, onMessageLongPress: (View, Message) -> Boolean): ChatroomSendMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardSendBinding =
                    DataBindingUtil.inflate(inflater, R.layout.message_card_send, parent, false)
                return ChatroomSendMessageViewHolder(binding, onMessageLongPress)
            }
        }
    }

    class ChatroomReceiveMessageViewHolder(
        binding: MessageCardReceiveBinding,
        onMessageLongPress: (View, Message) -> Boolean
    ) : ChatroomMessageViewHolder(binding.root, binding.messageCardReceive, onMessageLongPress) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup, onMessageLongPress: (View, Message) -> Boolean): ChatroomReceiveMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardReceiveBinding =
                    DataBindingUtil.inflate(inflater, R.layout.message_card_receive, parent, false)
                return ChatroomReceiveMessageViewHolder(binding, onMessageLongPress)
            }
        }
    }

    class ChatroomTimelineMessageViewHolder(private val binding: MessageCardTimelineBinding) : BaseViewHolder<Message>(binding.root) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): ChatroomTimelineMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardTimelineBinding =
                    DataBindingUtil.inflate(inflater, R.layout.message_card_timeline,
                        parent, false)
                return ChatroomTimelineMessageViewHolder(binding)
            }
        }

        override fun bind(message: Message?, position: Int) {
            binding.message = message
        }
    }

    private fun handleImageMessageBind(message: Message?,
                                       position: Int, messageBinding: MessageCardBinding,
                                       itemViewContext: Context) {
        Log.i("ChatroomMListAdapter", "MESSAGE: $message")
        val isMessageImage = Constants.imageRegex
            .matches(message?.message!!)
        Log.i("ChatroomMListAdapter", "MESSAGE ISIMAGE: $isMessageImage")

        val glide = Glide.with(itemViewContext)
        if(isMessageImage) {
            loadCachedPagingImageInto(glide, message.message,
                position, messageBinding.userImage, R.string.pref_last_chatroom_messages_fetch_time)
        }

        messageBinding.userImage.isVisible = isMessageImage
        messageBinding.userMessage.isVisible = !isMessageImage

        val userSentMessage = currentUsers.find { message.userSentId == it.id }
        messageBinding.userName.text = userSentMessage?.name ?: "[Unknown User]"

        loadCachedPagingImageInto(
            glide,
            userSentMessage?.photoUrl,
            position,
            messageBinding.userProfileImage,
            R.string.pref_last_chatroom_users_fetch_time
        )
    }

    private fun loadCachedPagingImageInto(glide: RequestManager, url: String?,
                                          position: Int, imageView: ImageView, cachePrefId: Int) {
        glide
            .load(url)
            .placeholder(R.drawable.default_pic)
            .signature(
                GlideUtils.getPagingObjectKey(
                    prefConfig,
                    position,
                    cachePrefId,
                    Constants.messagesPageSize
                )
            )
            .into(imageView)
    }

    fun setCurrentUsers(users: List<User>) {
        currentUsers = users
        notifyDataSetChanged()
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<Message>() {
            // Chatroom details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(oldMessage: Message,
                                         newMessage: Message
            ) = oldMessage.id == newMessage.id

            override fun areContentsTheSame(oldMessage: Message,
                                            newMessage: Message
            ) = oldMessage == newMessage
        }
    }
}