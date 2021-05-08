package com.example.hobbyfi.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChatroomCreateBinding
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.main.ChatroomCreateFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomCreateFragment : MainFragment(), TextFieldInputValidationOnus {
    private val viewModel: ChatroomCreateFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChatroomCreateBinding

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentChatroomCreateBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        observeCombinedObserversInvalidity()

        with(binding) {
            chatroomInfo.chatroomImage.galleryOption.setOnClickListener {
                Callbacks.requestImage(this@ChatroomCreateFragment)
            }

            chatroomInfo.chatroomImage.cameraOption.setOnClickListener {
                navController.navigate(R.id.action_chatroomCreateFragment_to_camera_capture_nav_graph)
            }

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            chatroomInfo.buttonBar.leftButton.setOnClickListener { // tag select button
                navController.safeNavigate(ChatroomCreateFragmentDirections.actionChatroomCreateFragmentToTagNavGraph(
                    viewModel!!.tagBundle.selectedTags.toTypedArray(),
                    viewModel!!.tagBundle.tags.toTypedArray(),
                ))
            }

            chatroomInfo.buttonBar.rightButton.setOnClickListener { // confirm button
                lifecycleScope.launch {
                    viewModel!!.sendIntent(ChatroomIntent.CreateChatroom(activityViewModel.authUser.value!!.id))
                }
            }

            // load after fragment switch & stuff
            viewModel!!.base64Image.loadUriIntoWithoutSignature(requireContext(), chatroomInfo.chatroomImage.image)
        }

        lifecycleScope.launch {
            viewModel.mainState.collectLatestWithLoadingAndNonIdleReset(listOf(ChatroomState.Idle::class),
                    viewModel::resetState,
                    viewLifecycleOwner, navController,
                    ChatroomCreateFragmentDirections.actionChatroomCreateFragmentToLoadingNavGraph(R.id.chatroomCreateFragment),
                    ChatroomState.Loading::class) {
                when(it) {
                    is ChatroomState.Idle -> {

                    }
                    is ChatroomState.OnData.ChatroomCreateResult -> {
                        activityViewModel.sendIntent(
                            UserIntent.UpdateUserCache(mapOf(
                                Pair(Constants.CHATROOM_IDS, Constants.jsonConverter.toJson(
                                    activityViewModel.authUser.value!!.chatroomIds?.plus(it.response.id) ?: listOf(it.response.id))
                                )
                            )
                        )) // trigger for joinedChatroom observer in ChatroomListFragment

                        viewModel.base64Image.originalUri?.let { image ->
                            WorkerUtils.buildAndEnqueueImageUploadWorker(
                                it.response.id,
                                prefConfig.getAuthUserToken()!!,
                                Constants.CHATROOMS,
                                image,
                                requireContext(),
                                R.string.pref_last_chatrooms_fetch_time
                            )
                        }

                        activityViewModel.setJoinedChatroom(true)
                        prefConfig.writeLastEnteredChatroomId(it.response.id)
                        navController.safeNavigate(ChatroomCreateFragmentDirections.actionChatroomCreateFragmentToChatroomActivity(
                            activityViewModel.authUser.value,
                            it.response
                        ))
                    }
                    is ChatroomState.Error -> {
                        context?.showFailureToast(it.error ?: getString(R.string.something_wrong))
                        if(it.shouldExit) {
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

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Uri?>(Constants.CAMERA_URI)
            ?.observe(viewLifecycleOwner, Observer { uri ->
                uri?.let {
                    binding.chatroomInfo.chatroomImage.image.loadUriIntoGlideAndSaveInImageHolder(it, viewModel.base64Image)
                }
            })
    }

    override fun observePredicateValidators() {
        viewModel.name.invalidity.observe(
            viewLifecycleOwner,
            TextInputLayoutFocusValidatorObserver(binding.chatroomInfo.nameInputField, getString(R.string.name_input_error))
        )

        viewModel.description.invalidity.observe(
            viewLifecycleOwner,
            TextInputLayoutFocusValidatorObserver(binding.chatroomInfo.descriptionInputField, getString(R.string.description_input_error))
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
            // FIXME: Small code dup on the callback with the other Fragments...
            binding.chatroomInfo.chatroomImage.image.loadUriIntoGlideAndSaveInImageHolder(
                data!!.data!!, viewModel.base64Image
            )
        }
    }
}