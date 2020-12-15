package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.databinding.FragmentChatroomEditDialogBinding
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.chatroom.ChatroomEditDialogFragmentViewModel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

class ChatroomEditDialogFragment : ChatroomDialogFragment() {
    private lateinit var binding: FragmentChatroomEditDialogBinding
    private val viewModel: ChatroomEditDialogFragmentViewModel by viewModels()

    @ExperimentalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Handle expired token error & logout
        binding = DataBindingUtil
            .inflate(layoutInflater, R.layout.fragment_chatroom_edit_dialog, container, false)
        binding.viewModel = viewModel

        with(binding) {

            buttonBar.leftButton.setOnClickListener {
                dismiss()
            }

            buttonBar.rightButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                val fieldMap: MutableMap<String?, String?> = mutableMapOf()

                if(activityViewModel.authChatroom.value?.name != viewModel!!.name.value) {
                    fieldMap[Constants.USERNAME] = viewModel!!.name.value
                }

                if(activityViewModel.authChatroom.value?.description != viewModel!!.description.value) {
                    fieldMap[Constants.USERNAME] = viewModel!!.description.value
                }

                if((activityViewModel.authChatroom.value?.tags ?: emptyList()) != viewModel!!.tagBundle.selectedTags) {
                    fieldMap[Constants.TAGS + "[]"] = (GsonBuilder()
                        .registerTypeAdapter(Tag::class.java, TagTypeAdapter())
                        .create()) // TODO: Extract into DI/singleton/static var
                        .toJson(viewModel!!.tagBundle.selectedTags)
                }

                Log.i("ChatroomEditDFragment", "FieldMap update: ${fieldMap}")
                if(fieldMap.isEmpty()) {
                    Toast.makeText(requireContext(), Constants.noUpdateFields, Toast.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    activityViewModel.sendChatroomIntent(ChatroomIntent.UpdateChatroom(fieldMap))
                }
                dismiss()
            }

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)
            ?.observe(viewLifecycleOwner, Observer {
                viewModel.tagBundle.setSelectedTags(it)
                viewModel.tagBundle.appendNewSelectedTagsToTags(it)
        })

        binding.tagSelectButton.setOnClickListener {
            navController.navigate(ChatroomEditDialogFragmentDirections.actionChatroomEditDialogFragmentToTagNavGraph(
                viewModel.tagBundle.selectedTags.toTypedArray(),
                viewModel.tagBundle.tags.toTypedArray()
            ))
        }
    }

    override fun initTextFieldValidators() {
        with(binding) {

        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(textInputUsername) ||
                    FieldUtils.isTextFieldInvalid(textInputDescription)
        }
    }

}