package com.example.hobbyfi.ui.chatroom

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChatroomEditBinding
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.TextInputLayoutFocusValidatorObserver
import com.example.hobbyfi.shared.ViewReverseEnablerObserver
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.chatroom.ChatroomEditFragmentViewModel
import com.example.hobbyfi.viewmodels.factories.TagListViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomEditFragment : ChatroomModelFragment(), TextFieldInputValidationOnus {
    private lateinit var binding: FragmentChatroomEditBinding
    val viewModel: ChatroomEditFragmentViewModel by viewModels(factoryProducer = {
        TagListViewModelFactory(requireActivity().application,
            activityViewModel.authChatroom.value?.tags ?: arrayListOf())
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil
            .inflate(layoutInflater, R.layout.fragment_chatroom_edit, container, false)

        binding.viewModel = viewModel
        observeCombinedObserversInvalidity()

        with(binding) {
            lifecycleOwner = this@ChatroomEditFragment

            chatroomInfo.chatroomImage.setOnClickListener {
                Callbacks.requestImage(this@ChatroomEditFragment)
            }

            viewModel!!.base64Image.loadUriIntoWithoutSignature(requireContext(), chatroomInfo.chatroomImage)

            chatroomInfo.buttonBar.rightButton.setOnClickListener {
                val fieldMap: MutableMap<String, String?> = mutableMapOf()

                if(activityViewModel.authChatroom.value?.name != viewModel!!.name.value) {
                    fieldMap[Constants.NAME] = viewModel!!.name.value
                }

                if(activityViewModel.authChatroom.value?.description != viewModel!!.description.value) {
                    fieldMap[Constants.DESCRIPTION] = viewModel!!.description.value
                }

                if((activityViewModel.authChatroom.value?.tags ?: arrayListOf()) != viewModel!!.tagBundle.selectedTags) {
                    fieldMap[Constants.TAGS + "[]"] = Constants.jsonConverter
                        .toJson(viewModel!!.tagBundle.selectedTags)
                }

                if(viewModel!!.base64Image.originalUri != null) { // means chatroom has changed pfp
                    fieldMap[Constants.IMAGE] = viewModel!!.base64Image.originalUri
                }

                Log.i("ChatroomEditDFragment", "FieldMap update: ${fieldMap}")
                if(fieldMap.isEmpty()) {
                    Toast.makeText(requireContext(), Constants.noUpdateFields, Toast.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                } else if(fieldMap.size == 1 && fieldMap.containsKey(Constants.IMAGE)) {
                    WorkerUtils.buildAndEnqueueImageUploadWorker(
                        activityViewModel.authChatroom.value!!.id,
                        prefConfig.getAuthUserToken()!!,
                        Constants.EDIT_CHATROOM_TYPE,
                        viewModel!!.base64Image.originalUri!!,
                        requireContext(),
                        R.string.pref_last_chatrooms_fetch_time
                    )
                    return@setOnClickListener
                }

                fieldMap[Constants.ID] = activityViewModel.authChatroom.value?.id.toString()

                lifecycleScope.launch {
                    activityViewModel.sendChatroomIntent(ChatroomIntent.UpdateChatroom(fieldMap))
                }
            }

            activityViewModel.authChatroom.observe(viewLifecycleOwner, Observer {
                if(it != null) {
                    viewModel!!.name.value = it.name
                    viewModel!!.description.value = it.description
                    it.tags?.let { selectedTags ->
                        viewModel!!.tagBundle.setSelectedTags(selectedTags)
                        viewModel!!.tagBundle.appendNewSelectedTagsToTags(selectedTags)
                    }

                    if (it.photoUrl != null) {
                        Log.i("UserProfileFragment", "User photo url: ${it.photoUrl}")
                        Glide.with(this@ChatroomEditFragment).load(
                            it.photoUrl!!
                        ).signature(
                            ObjectKey(prefConfig.readLastPrefFetchTime(R.string.pref_last_chatrooms_fetch_time))
                        ).placeholder(chatroomInfo.chatroomImage.drawable)
                        .into(chatroomInfo.chatroomImage)
                    } else {
                        // load default img (needed if image deletion is added because image will be sent null/"0" or whatever)
                    }
                }
            })

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

        binding.chatroomInfo.buttonBar.leftButton.setOnClickListener {
            navController.navigate(ChatroomEditFragmentDirections.actionChatroomEditDialogFragmentToTagNavGraph(
                viewModel.tagBundle.selectedTags.toTypedArray(),
                viewModel.tagBundle.tags.toTypedArray()
            ))
        }
    }

    override fun observePredicateValidators() {
        viewModel.name.invalidity.observe(
            viewLifecycleOwner,
            TextInputLayoutFocusValidatorObserver(binding.chatroomInfo.nameInputField, Constants.nameInputError)
        )

        viewModel.description.invalidity.observe(
            viewLifecycleOwner,
            TextInputLayoutFocusValidatorObserver(binding.chatroomInfo.descriptionInputField, Constants.descriptionInputError)
        )
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.chatroomInfo.buttonBar.rightButton))
    }

    override fun onStart() {
        super.onStart()
        observePredicateValidators()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Callbacks.handleImageRequestWithPermission(
            requireActivity(),
            requestCode,
            resultCode,
            data
        ) {
            binding.chatroomInfo.chatroomImage.setImageBitmap(it)
            viewModel.base64Image.setOriginalUri(data!!.data!!.toString())
        }
    }
}