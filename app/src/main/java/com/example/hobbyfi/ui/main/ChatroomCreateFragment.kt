package com.example.hobbyfi.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
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
import kotlinx.coroutines.flow.collectLatest
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
            chatroomInfo.chatroomImage.setOnClickListener {
                Callbacks.requestImage(this@ChatroomCreateFragment)
            }

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            chatroomInfo.buttonBar.leftButton.setOnClickListener { // tag select button
                navController.navigate(ChatroomCreateFragmentDirections.actionChatroomCreateFragmentToTagNavGraph(
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
            viewModel!!.base64Image.loadUriIntoWithoutSignature(requireContext(), chatroomInfo.chatroomImage)
        }

        lifecycleScope.launch {
            viewModel.mainState.collectLatestWithLoading(navController,
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
                        navController.navigate(ChatroomCreateFragmentDirections.actionChatroomCreateFragmentToChatroomActivity(
                            activityViewModel.authUser.value,
                            it.response
                        ))
                        viewModel.resetState()
                    }
                    is ChatroomState.Error -> {
                        Toast.makeText(context, it.error, Toast.LENGTH_LONG)
                            .show()
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

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).binding.bottomNav.isVisible = false
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
            Glide.with(requireContext())
                .load(data!!.data!!)
                .into(binding.chatroomInfo.chatroomImage)
            viewModel.base64Image.setOriginalUri(data.data!!.toString())
        }
    }
}