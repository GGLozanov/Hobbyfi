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
import com.example.hobbyfi.databinding.MessageCardReceiveBinding
import com.example.hobbyfi.databinding.MessageCardSendBinding
import com.example.hobbyfi.databinding.MessageCardTimelineBinding
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.GlideUtils
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

abstract class ChatroomMessageAdapter(
    protected var currentUsers: List<User>
): PagingDataAdapter<Message, BaseViewHolder<Message>>(DIFF_CALLBACK), KodeinAware {

    override val kodein: Kodein by kodein(MainApplication.applicationContext) // FIXME: Kodein w/ appcontext bad???

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")

    protected enum class MessageType {
        SEND, RECEIVE, TIMELINE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Message> {
        return when(viewType) {
            MessageType.TIMELINE.ordinal -> getTimelineMessageViewHolderInstance(parent)
            MessageType.RECEIVE.ordinal -> getReceiveMessageViewHolderInstance(parent)
            MessageType.SEND.ordinal -> getSendMessageViewHolderInstance(parent)
            else -> throw IllegalArgumentException(Constants.invalidViewTypeError)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<Message>, position: Int) {
        val message = getItem(position)

        holder.bind(message, position)
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        if(message?.isTimeline == true) return MessageType.TIMELINE.ordinal

        return if(message?.userSentId == prefConfig.getAuthUserIdFromToken()) MessageType.SEND.ordinal
        else MessageType.RECEIVE.ordinal
    }

    protected abstract fun getTimelineMessageViewHolderInstance(parent: ViewGroup): BaseTimelineMessageViewHolder
    protected abstract fun getReceiveMessageViewHolderInstance(parent: ViewGroup): BaseUserChatroomMessageViewHolder
    protected abstract fun getSendMessageViewHolderInstance(parent: ViewGroup): BaseUserChatroomMessageViewHolder

    protected abstract class BaseUserChatroomMessageViewHolder(
        rootView: View,
        val messageCardBinding: MessageCardBinding,
        protected val users: List<User>,
        protected val prefConfig: PrefConfig
    ) : BaseViewHolder<Message>(rootView) {
        override fun bind(model: Message?, position: Int) {
            Log.i("ChatroomMListAdapter", "Message: $model")
            val userSentMessage =
                users.find { model?.userSentId == it.id }

            // DATA BINDING GO BRRRRRR????
            messageCardBinding.userName.text = userSentMessage?.name ?: "[Unknown User]"
            messageCardBinding.userMessage.text = model?.message
            handleImageMessageBind(model, userSentMessage, position)
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
    ) : BaseViewHolder<Message>(binding.root) {
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

        override fun bind(model: Message?, position: Int) {
            binding.message = model
        }
    }

    @JvmName("setCurrentUsers1")
    fun setCurrentUsers(users: List<User>) {
        if(currentUsers != users) {
            currentUsers = users
            notifyDataSetChanged()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<Message>() {
            // Chatroom details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(
                oldMessage: Message,
                newMessage: Message
            ) = oldMessage.id == newMessage.id

            override fun areContentsTheSame(
                oldMessage: Message,
                newMessage: Message
            ) = oldMessage == newMessage
        }
    }
}