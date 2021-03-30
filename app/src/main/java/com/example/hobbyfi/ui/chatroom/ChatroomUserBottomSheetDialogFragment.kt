package com.example.hobbyfi.ui.chatroom

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChatroomUserBottomSheetDialogBinding
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.buildYesNoAlertDialog
import com.example.hobbyfi.shared.reinitChipsByTags
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomUserBottomSheetDialogFragment : ChatroomBottomSheetDialogFragment(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: FragmentChatroomUserBottomSheetDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater,
            R.layout.fragment_chatroom_user_bottom_sheet_dialog, container, false)

        with(binding) {
            tagGroup.setChipSpacing(10)
            bottomSheet.apply {
                BottomSheetBehavior.from(this).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            root.requestLayout()

            observeUser()
            observeChatroomOwnership()
            initAdminActions()

            return@onCreateView root
        }
    }

    private fun observeUser() {
        activityViewModel.chatroomUsers.map {
            it.find { user -> user == requireArguments()[Constants.USER] }
        }.observe(viewLifecycleOwner, Observer {
            binding.user = it

            it?.photoUrl?.let { photoUrl ->
                Glide.with(requireContext())
                    .load(photoUrl)
                    .placeholder(binding.userImage.drawable)
                    .signature(
                        ObjectKey(R.string.pref_last_chatroom_users_fetch_time)
                    )
                    .into(binding.userImage)
            }

            binding.tagGroup.reinitChipsByTags(it?.tags)
        })
    }

    private fun observeChatroomOwnership() {
        activityViewModel.isAuthUserChatroomOwner.observe(viewLifecycleOwner, Observer {
            val user = requireArguments()[Constants.USER] as User
            binding.isChatroomAdmin = user.id == activityViewModel.authChatroom.value?.ownerId
            binding.canChatroomAdminUseActions = it &&
                user != activityViewModel.authUser.value
        })
    }

    private fun initAdminActions() {
        binding.bottomSheetAdminActions.setNavigationItemSelectedListener(this@ChatroomUserBottomSheetDialogFragment)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_kick -> {
                requireContext().buildYesNoAlertDialog(
                    getString(R.string.kick_user_confirm),
                    { dialogInterface: DialogInterface, _: Int ->
                        lifecycleScope.launch {
                            activityViewModel.sendUsersIntent(
                                UserListIntent.KickUser(
                                    (requireArguments()[Constants.USER] as User).id
                                )
                            )
                        }
                        dialogInterface.dismiss()
                    },
                    { dialogInterface: DialogInterface, _: Int ->
                        dialogInterface.dismiss()
                    }
                )
            }
        }
        return true
    }

    companion object {
        @JvmStatic
        fun newInstance(user: User) =
            ChatroomUserBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(Constants.USER, user)
                }
            }
    }
}