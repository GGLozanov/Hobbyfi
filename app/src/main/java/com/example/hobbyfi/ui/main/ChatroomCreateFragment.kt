package com.example.hobbyfi.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.databinding.FragmentChatroomCreateBinding
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.factories.TagListViewModelFactory
import com.example.hobbyfi.viewmodels.main.ChatroomCreateFragmentViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomCreateFragment : MainFragment(), TextFieldInputValidationOnus {
    private val viewModel: ChatroomCreateFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChatroomCreateBinding

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (requireActivity() as MainActivity).bottom_nav.isVisible = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentChatroomCreateBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        initTextFieldValidators()

        with(binding) {
            chatroomImage.setOnClickListener {
                Callbacks.requestImage(this@ChatroomCreateFragment)
            }

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            buttonPair.leftButton.setOnClickListener { // tag select button
                navController.navigate(ChatroomCreateFragmentDirections.actionChatroomCreateFragmentToTagNavGraph(
                    viewModel!!.tagBundle.selectedTags.toTypedArray(),
                    viewModel!!.tagBundle.tags.toTypedArray(),
                ))
            }

            buttonPair.rightButton.setOnClickListener { // confirm button
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel!!.sendIntent(ChatroomIntent.CreateChatroom)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.mainState.collect {
                when(it) {
                    is ChatroomState.Idle -> {

                    }
                    is ChatroomState.Loading -> {
                        // TODO: Progressbar
                    }
                    is ChatroomState.OnData.ChatroomCreateResult -> {
                        activityViewModel.updateAndSaveUser(mapOf(
                            Pair(Constants.CHATROOM_ID, it.response!!.id.toString())
                        ))

                        navController.navigate(ChatroomCreateFragmentDirections.actionChatroomCreateFragmentToChatroomActivity(
                            activityViewModel.authUser.value,
                            Chatroom(
                                it.response.id,
                                viewModel.name.value!!,
                                viewModel.description.value,
                                BuildConfig.BASE_URL + "uploads/" + Constants.chatroomProfileImageDir(it.response.id)
                                        + "/" + it.response.id + ".jpg",
                                viewModel.tagBundle.selectedTags,
                                activityViewModel.authUser.value!!.id,
                                null
                            )
                        ))
                    }
                    is ChatroomState.Error -> {
                        Toast.makeText(context, it.error, Toast.LENGTH_LONG)
                            .show()
                        if(it.shouldReauth) {
                            (requireActivity() as MainActivity).logout()
                        }
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }

        // TODO: Fix code dup with other tag fragments
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)
            ?.observe(viewLifecycleOwner) { selectedTags ->
                viewModel.tagBundle.appendNewSelectedTagsToTags(selectedTags)
                viewModel.tagBundle.setSelectedTags(selectedTags)
            }
    }

    override fun initTextFieldValidators() {
        with(binding) {
            // TODO: Fix code dup with other layouts like these and find a way to extract this in a single method call or something
            textInputName.addTextChangedListener(
                Constants.nameInputError,
                Constants.namePredicate
            )

            textInputDescription.addTextChangedListener(
                Constants.descriptionInputError,
                Constants.descriptionPredicate
            )
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(textInputName) || FieldUtils.isTextFieldInvalid(textInputDescription)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).bottom_nav.isVisible = true // hacky solution but oh well, nested nav graphs do that
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Callbacks.handleImageRequestWithPermission(
            this,
            requireActivity(),
            requestCode,
            resultCode,
            data
        ) {
            // FIXME: Small code dup on the callback with the other Fragments...
            binding.chatroomImage.load(
                it
            ) // set the new image resource to be decoded from the bitmap
            viewModel.setProfileImageBase64(
                ImageUtils.encodeImage(it)
            )
        }
    }
}