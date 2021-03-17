package com.example.hobbyfi.adapters.message

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.databinding.MessageCardBinding
import com.example.hobbyfi.databinding.MessageCardTimelineBinding
import com.example.hobbyfi.databinding.MessageSeparatorBinding
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.models.ui.UIMessage
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.GlideUtils
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

abstract class ChatroomMessageAdapter(
    protected var currentUsers: List<User>
): PagingDataAdapter<UIMessage, BaseViewHolder<UIMessage>>(DIFF_CALLBACK), KodeinAware {

    override val kodein: Kodein by kodein(MainApplication.applicationContext) // FIXME: Kodein w/ appcontext bad???

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")

    protected enum class MessageType {
        SEND, RECEIVE, TIMELINE, SEPARATOR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<UIMessage> {
        return when(viewType) {
            MessageType.TIMELINE.ordinal -> getTimelineMessageViewHolderInstance(parent)
            MessageType.RECEIVE.ordinal -> getReceiveMessageViewHolderInstance(parent)
            MessageType.SEND.ordinal -> getSendMessageViewHolderInstance(parent)
            MessageType.SEPARATOR.ordinal -> getSeparatorMessageViewHolderInstance(parent)
            else -> throw IllegalArgumentException(Constants.invalidViewTypeError)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<UIMessage>, position: Int) {
        val message = getItem(position)

        holder.bind(message, position)
    }

    override fun getItemViewType(position: Int): Int {
        when (val message = getItem(position)) {
            is UIMessage.MessageSeparatorItem -> {
                return MessageType.SEPARATOR.ordinal
            }
            is UIMessage.MessageItem -> {
                if(message.message.isTimeline) return MessageType.TIMELINE.ordinal

                return if(message.message.userSentId == prefConfig.getAuthUserIdFromToken()) MessageType.SEND.ordinal
                else MessageType.RECEIVE.ordinal
            }
            else -> {
                throw UnsupportedOperationException("Unknown view type for ChatroomMessageAdapter")
            }
        }
    }

    protected abstract fun getTimelineMessageViewHolderInstance(parent: ViewGroup): BaseTimelineMessageViewHolder
    protected abstract fun getReceiveMessageViewHolderInstance(parent: ViewGroup): BaseUserChatroomMessageViewHolder
    protected abstract fun getSendMessageViewHolderInstance(parent: ViewGroup): BaseUserChatroomMessageViewHolder

    private fun getSeparatorMessageViewHolderInstance(parent: ViewGroup): BaseSeparatorMessageViewHolder =
        BaseSeparatorMessageViewHolder.getInstance(parent)

    protected open class BaseSeparatorMessageViewHolder(
        rootView: View,
        val messageSeparatorBinding: MessageSeparatorBinding
    ): BaseViewHolder<UIMessage>(rootView) {
        companion object {
            fun getInstance(parent: ViewGroup): BaseSeparatorMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageSeparatorBinding =
                    MessageSeparatorBinding.inflate(
                        inflater,
                        parent, false
                    )
                return BaseSeparatorMessageViewHolder(binding.root, binding) // empty (does nothing special)
            }
        }

        override fun bind(model: UIMessage?, position: Int) {
            messageSeparatorBinding.separatorDate.text = (model as UIMessage.MessageSeparatorItem?)?.dateText
        }
    }

    protected abstract class BaseUserChatroomMessageViewHolder(
        rootView: View,
        val messageCardBinding: MessageCardBinding,
        protected val users: List<User>,
        protected val prefConfig: PrefConfig
    ) : BaseViewHolder<UIMessage>(rootView) {
        override fun bind(model: UIMessage?, position: Int) {
            Log.i("ChatroomMListAdapter", "Message: $model")
            val message = (model as UIMessage.MessageItem?)?.message
            val userSentMessage =
                users.find { message?.userSentId == it.id }

            // DATA BINDING GO BRRRRRR????
            messageCardBinding.userName.text = userSentMessage?.name ?: "[Unknown User]"
            messageCardBinding.userMessage.text = message?.message
            handleImageMessageBind(message, userSentMessage, position)
        }

        private fun handleImageMessageBind(
            message: Message?, userSentMessage: User?,
            position: Int,
        ) {
            val isMessageImage = Constants.imageRegex
                .matches(message?.message!!)

            val glide = Glide.with(itemView.context)
            if(isMessageImage) {
                loadMessageImage(message.message, glide)
            }

            messageCardBinding.userImage.isVisible = isMessageImage
            messageCardBinding.userMessage.isVisible = !isMessageImage

            loadUserMessageImage(userSentMessage?.photoUrl, position, glide)
        }

        // edit user notifications received in activity either way, so using signature shouldn't matter
        // TODO: This means that, on user list, message list, AND message search list, normal EDIT_USER notifications trigger
        // TODO: Reload of images. While this is somewhat tolerable only on image updates, it is not AT ALL acceptable
        // TODO: For simple, normal updates. Therefore, define separate prefs for image updates & use them for signature
        protected open fun loadUserMessageImage(photoUrl: String?, position: Int, glide: RequestManager) {
            glide
                .load(photoUrl)
                .placeholder(R.drawable.user_default_pic)
                .signature(
                    GlideUtils.getPagingObjectKey(
                        prefConfig,
                        position,
                        R.string.pref_last_chatroom_users_fetch_time,
                        Constants.messagesPageSize
                    )
                )
                .into(messageCardBinding.userProfileImage)
        }

        protected open fun loadMessageImage(messageUrl: String, glide: RequestManager) {
            glide
                .load(messageUrl)
                .placeholder(R.drawable.ic_baseline_image_42)
                .into(messageCardBinding.userImage)
        }
    }

    protected open class BaseTimelineMessageViewHolder(
        val binding: MessageCardTimelineBinding,
    ) : BaseViewHolder<UIMessage>(binding.root) {
        companion object {
            fun getInstance(parent: ViewGroup): BaseTimelineMessageViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding: MessageCardTimelineBinding =
                    DataBindingUtil.inflate(
                        inflater, R.layout.message_card_timeline,
                        parent, false
                    )
                return BaseTimelineMessageViewHolder(binding) // empty (does nothing special)
            }
        }

        override fun bind(model: UIMessage?, position: Int) {
            binding.message = (model as UIMessage.MessageItem?)?.message
        }
    }

    @JvmName("setCurrentUsers1")
    fun setCurrentUsers(users: List<User>) {
        currentUsers = users
        notifyDataSetChanged()
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<UIMessage>() {
            // Chatroom details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(
                oldMessage: UIMessage,
                newMessage: UIMessage
            ) = (oldMessage is UIMessage.MessageItem && newMessage is UIMessage.MessageItem &&
                        oldMessage.message.id == newMessage.message.id) ||
                        (oldMessage is UIMessage.MessageSeparatorItem && newMessage is UIMessage.MessageSeparatorItem &&
                                oldMessage.dateText == newMessage.dateText)


            override fun areContentsTheSame(
                oldMessage: UIMessage,
                newMessage: UIMessage
            ) = oldMessage == newMessage
        }
    }
}