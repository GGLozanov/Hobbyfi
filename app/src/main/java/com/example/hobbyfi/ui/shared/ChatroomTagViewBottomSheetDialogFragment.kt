package com.example.hobbyfi.ui.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import androidx.navigation.navArgs
import com.example.hobbyfi.databinding.FragmentTagViewBottomSheetDialogBinding
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.reinitChipsByTags
import com.example.hobbyfi.ui.chatroom.ChatroomActivity
import com.example.hobbyfi.ui.chatroom.ChatroomActivityArgs
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChatroomTagViewBottomSheetDialogFragment : TagViewBottomSheetDialogFragment() {
    private val activityViewModel: ChatroomActivityViewModel by activityViewModels(factoryProducer = {
        val activityArgs: ChatroomActivityArgs by (requireActivity() as ChatroomActivity).navArgs()
        AuthUserChatroomViewModelFactory(requireActivity().application, activityArgs.user, activityArgs.chatroom)
    })

    private val tagsSource: LiveData<List<Tag>?> get() {
        return activityViewModel.authChatroom.map { it?.tags }
    }

    override fun onStart() {
        super.onStart()
        observeTags()
    }

    private fun observeTags() {
        tagsSource.observe(viewLifecycleOwner, Observer {
            binding.noTagsText.isVisible = !binding.tagGroup.reinitChipsByTags(it)
        })
    }

    companion object {
        fun newInstance(name: String): ChatroomTagViewBottomSheetDialogFragment {
            val bundle = Bundle().apply {
                putString(Constants.NAME, name)
            }

            return ChatroomTagViewBottomSheetDialogFragment().apply {
                arguments = bundle
            }
        }
    }
}