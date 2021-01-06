package com.example.hobbyfi.adapters.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.adapters.chatroom.ChatroomListAdapter
import com.example.hobbyfi.databinding.UserCardBinding
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.GlideUtils
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

// Discord doesn't do pagination for their guild users...
// ...so neither will I!
class ChatroomUserListAdapter(
    private var users: List<User>,
    private val onUserCardPress: (View, User) -> Unit
) : RecyclerView.Adapter<ChatroomUserListAdapter.ChatroomUserViewHolder>(), KodeinAware {

    @ExperimentalPagingApi
    override val kodein: Kodein by kodein(MainApplication.applicationContext)

    private val prefConfig: PrefConfig by instance(tag = "prefConfig")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomUserViewHolder {
        return ChatroomUserViewHolder.getInstance(parent, prefConfig, onUserCardPress)
    }

    override fun onBindViewHolder(holder: ChatroomUserViewHolder, position: Int) {
        val user = users[position]

        holder.bind(user, position)
    }

    override fun getItemCount(): Int {
        return users.size
    }

    class ChatroomUserViewHolder(
        private val binding: UserCardBinding,
        private val prefConfig: PrefConfig,
        private val onUserCardPress: (View, User) -> Unit
    ) : BaseViewHolder<User>(binding.root) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup, prefConfig: PrefConfig, onUserCardPress: (View, User) -> Unit): ChatroomUserViewHolder {
                val binding: UserCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context), R.layout.user_card,
                    parent, false
                )
                return ChatroomUserViewHolder(binding, prefConfig, onUserCardPress)
            }
        }


        override fun bind(user: User?, position: Int) {
            binding.user = user
            if(user?.photoUrl != null) {
                Glide.with(itemView.context)
                    .load(user.photoUrl)
                    .signature(
                        ObjectKey(prefConfig.readLastPrefFetchTime(R.string.pref_last_chatroom_users_fetch_time))
                    )
                    // calculate current page based on item position
                    .into(binding.userProfileImage)
            } else {
                Glide.with(itemView.context)
                    .load(
                        R.drawable.chatroom_default_pic
                    )
                    .into(binding.userProfileImage)
            }
            binding.userCard.setOnClickListener {
                onUserCardPress(it, user!!)
            }
        }
    }

    fun setUsers(users: List<User>) {
        this.users = users
        notifyDataSetChanged()
    }
}