package com.example.hobbyfi.ui.chatroom

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.message.ChatroomMessageListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomMessageListBinding
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.MessageState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.RefreshConnectionAware
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.kroegerama.imgpicker.BottomSheetImagePicker
import com.kroegerama.imgpicker.ButtonType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.FileNotFoundException

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomMessageListFragment : ChatroomMessageFragment(), TextFieldInputValidationOnus,
        BottomSheetImagePicker.OnImagesSelectedListener,
        ChatroomMessageBottomSheetDialogFragment.OnMessageOptionSelected,
        RefreshConnectionAware {

    @ExperimentalCoroutinesApi
    @ExperimentalPagingApi
    public override val viewModel: ChatroomMessageListFragmentViewModel by viewModels()

    private lateinit var binding: FragmentChatroomMessageListBinding

    // props like these are nullable because Activity onDestroy being called on new app start from push notification
    // and causing crashes for unitialised properties before anything actually happens prior to activity restart

    override val messageListAdapter: ChatroomMessageListAdapter by lazy {
        ChatroomMessageListAdapter(
            activityViewModel.chatroomUsers.value ?: arrayListOf(),
            activityViewModel.isAuthUserChatroomOwner.value == true,
            { _, message ->
                // reuse fragment for distinct messages when they don't change
                parentFragmentManager.showDistinctDialog(message.message, {
                    ChatroomMessageBottomSheetDialogFragment.newInstance(
                        message
                    )
                })
                return@ChatroomMessageListAdapter true
            },
        ) { messageB ->
            navController.navigate(ChatroomMessageListFragmentDirections
                .actionChatroomMessageListFragmentToImageViewFragment(messageB.userMessage.text.toString()))
        }
    }

    private val onNormalSendMessage = View.OnClickListener {
        lifecycleScope.launch {
            viewModel.sendMessageIntent(
                MessageIntent.CreateMessage(
                    null,
                    activityViewModel.authUser.value!!.id,
                    activityViewModel.authChatroom.value!!.id
                )
            )
        }
    }

    private val onEditSendMessage = { editedMessage: Message ->
        View.OnClickListener {
            val messageMap = mutableMapOf<String?, String?>()

            if(editedMessage.message != viewModel.message.value) { // kinda bruh for the two-way databinding but I'm dumb
                messageMap[Constants.MESSAGE] = viewModel.message.value
            } else {
                Toast.makeText(requireContext(), "You can't edit a message with the same message!", Toast.LENGTH_LONG)
                    .show()
                return@OnClickListener
            }

            messageMap[Constants.ID] = editedMessage.id.toString()

            lifecycleScope.launch {
                viewModel.sendMessageIntent(
                    MessageIntent.UpdateMessage(
                        messageMap
                    )
                )
            }
        }
    }

    private var chatroomMessageBroadcastReceiverFactory: ChatroomMessageBroadcastReceiverFactory? = null

    private var createMessageReceiver: BroadcastReceiver? = null
    private var editMessageReceiver: BroadcastReceiver? = null
    private var deleteMessageReceiver: BroadcastReceiver? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = requireActivity() as ChatroomActivity

        // TODO: Move receiver registration in after chatroom messages fetch!!!
        chatroomMessageBroadcastReceiverFactory = ChatroomMessageBroadcastReceiverFactory
            .getInstance(viewModel, messageListAdapter, activityViewModel, activity)
        createMessageReceiver = chatroomMessageBroadcastReceiverFactory!!.createActionatedReceiver(Constants.CREATE_MESSAGE_TYPE)
        editMessageReceiver = chatroomMessageBroadcastReceiverFactory!!.createActionatedReceiver(Constants.EDIT_MESSAGE_TYPE)
        deleteMessageReceiver = chatroomMessageBroadcastReceiverFactory!!.createActionatedReceiver(Constants.DELETE_MESSAGE_TYPE)

        localBroadcastManager.registerReceiver(createMessageReceiver!!, IntentFilter(Constants.CREATE_MESSAGE_TYPE))
        localBroadcastManager.registerReceiver(editMessageReceiver!!, IntentFilter(Constants.EDIT_MESSAGE_TYPE))
        localBroadcastManager.registerReceiver(deleteMessageReceiver!!, IntentFilter(Constants.DELETE_MESSAGE_TYPE))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_chatroom_message_list, container, false)
        binding.viewModel = viewModel

        with(binding) {
            lifecycleOwner = this@ChatroomMessageListFragment
            selectImageButton.setOnClickListener {
                BottomSheetImagePicker.Builder(getString(R.string.file_provider))
                    .cameraButton(ButtonType.Button)
                    .galleryButton(ButtonType.Button)
                    .multiSelect(1, 4)
                    .multiSelectTitles(
                        R.plurals.pick_multi,
                        R.plurals.pick_multi_more,
                        R.string.pick_multi_limit
                    )
                    .peekHeight(R.dimen.peekHeight)
                    .columnSize(R.dimen.columnSize)
                    .show(childFragmentManager)
            }

            sendMessageButton.setOnClickListener(onNormalSendMessage)
            cancelHeader.setOnClickListener { sendMessageButton.setOnClickListener(onNormalSendMessage)
                viewModel!!.message.setValue(null)
                editMessageOptionsLayout.isVisible = false
            }

            initMessageListAdapter()
            observeUsers()
            observeMessageState()
            observeMessagesState()
            observeConnectionRefresh(savedInstanceState, (requireActivity() as BaseActivity)
                .refreshConnectivityMonitor
            )

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSearchMessage()
    }

    @ExperimentalPagingApi
    @ExperimentalCoroutinesApi
    override fun observeUsers() {
        activityViewModel.chatroomUsers.observe(viewLifecycleOwner, Observer {
            Log.i("ChatroomMListFragment", "Adapter users: $it")
            if(viewModel.areCurrentMessagesNull && it.isNotEmpty()) {
                // send message fetch intent
                activityViewModel.authChatroom.value?.let {
                    lifecycleScope.launch {
                        viewModel.sendIntent(MessageListIntent.FetchMessages(it.id))
                    }
                }
            }

            // update chatroomlistadapter users
            messageListAdapter.setCurrentUsers(it)
        })
    }

    private fun observeMessageState() {
        lifecycleScope.launchWhenCreated {
            viewModel.messageState.collectLatest {
                when(it) {
                    is MessageState.Idle -> {

                    }
                    is MessageState.Loading -> {
                        // TODO: stop button? Or do nothing?
                    }
                    is MessageState.OnData.MessageCreateResult -> {
                        viewModel.message.setValue(null) // reset msg
                    }
                    is MessageState.OnData.MessageUpdateResult -> {
                        binding.cancelHeader.callOnClick()
                    }
                    is MessageState.OnData.MessageDeleteResult -> {
                        Toast.makeText(requireContext(), "Successfully deleted message!", Toast.LENGTH_LONG)
                            .show()
                        viewModel.resetMessageState()
                    }
                    is MessageState.Error -> {
                        (requireActivity() as ChatroomActivity).handleAuthActionableError(
                            it.error,
                            it.shouldExit,
                            context = requireContext()
                        )
                        viewModel.resetMessageState()
                    }
                }
            }
        }
    }

    private fun observeSearchMessage() {
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Message?>(Constants.searchMessage)
            ?.observe(viewLifecycleOwner, Observer {
                Log.i("ChatroomMListFragment", "message received from navcontroller handle: ${it}")
                it?.let {
                    messageListAdapter.findItemPositionFromCurrentPagingData(it).run {
                        Log.i("ChatroomMListFragment", "POSITION received from navcontroller handle: ${this}")
                        if(this != null) {
                            binding.messageList.smoothScrollToPosition(this)
                            navController.currentBackStackEntry?.savedStateHandle?.set(Constants.searchMessage, null)
                        } else if(connectivityManager.isConnected()) { // assert whether there's any point to search through the messages
                            // delete messages cached so that new generation can be propagated (like Discord)
                            lifecycleScope.launch {
                                viewModel.sendIntent(
                                    MessageListIntent.DeleteCachedMessages
                                )
                            }
                        } else { // do nothing otherwise (this should some kind of a banner in the where it says there's no connection)
                            navController.currentBackStackEntry?.savedStateHandle?.set(Constants.searchMessage, null)
                        }
                    }
                }
            })
    }

    override fun onPostMessageListCollect(currentMessages: PagingData<Message>, qMessageId: Long?) {
        if(qMessageId != null) {
            navController.currentBackStackEntry?.savedStateHandle?.set(Constants.searchMessage, null)
//
//            navController.currentBackStackEntry?.savedStateHandle?.get<Message?>(Constants.searchMessage)?.run {
//                messageListAdapter.findItemPositionFromCurrentPagingData(
//                    this
//                ).let {
//                    // renderBottomGenerationsAfterSearchFetch()
//                }
//            }
        }
    }

    override fun observeConnectionRefresh(savedState: Bundle?, refreshConnectivityMonitor: RefreshConnectivityMonitor) {
        super.observeConnectionRefresh(savedState, refreshConnectivityMonitor)
        refreshConnectivityMonitor.observe(viewLifecycleOwner, Observer { connectionRefreshed ->
            // connectivityManager.isConnected() IMPORTANT TODO: Fix in order to refetch if user
            //  enter without internet (currently refetches old chatrooms in itiial joins)
            if(connectionRefreshed) {
                Log.i("ChatroomMListFragment", "ChatroomMessageListFragment CONNECTED")
                refreshDataOnConnectionRefresh()
            } else {
                Log.i("ChatroomMListFragment", "ChatroomMessageListFragment DIS-CONNECTED")
            }
        })
    }

    private fun renderBottomGenerationsAfterSearchFetch() {
        if(!viewModel.sentMessageIdFetchRequestPrior) {
            viewModel.setSentMessageIdFetchRequestPrior(true)
            lifecycleScope.launch {
                viewModel.sendIntent(
                    MessageListIntent.FetchMessages(
                        activityViewModel.authChatroom.value!!.id,
                    )
                )
            }
        }
    }

    override fun refreshDataOnConnectionRefresh() {
        messageListAdapter.refresh()
    }

    override fun initMessageListAdapter() {
        with(binding) {
            messageList.addItemDecoration(VerticalSpaceItemDecoration(15))
            messageList.adapter = messageListAdapter.withLoadStateFooter(loadStateAdapter)
        }
    }

    override fun onStart() {
        super.onStart()
        observePredicateValidators()
        observeCombinedObserversInvalidity()
        observeUIState()
    }

    // COHESION GO YEEEEEEEET
    private fun observeUIState() {
        val activity = requireActivity() as ChatroomActivity

        activityViewModel.authChatroom.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            setToolbarProperties(it, activity)
        })
        activityViewModel.isAuthUserChatroomOwner.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            activity.binding.toolbar
                .navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_admin_panel_settings_24)

            messageListAdapter.setAuthUserChatroomOwner(it)
        })
    }

    private fun setToolbarProperties(chatroom: Chatroom?, activity: ChatroomActivity) {
        activity.title = chatroom?.name
    }

    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        Log.i("ChatroomMListFragment", "Received URIs for images: $uris")
        lifecycleScope.launch {
            viewModel.sendMessageIntent(
                MessageIntent.CreateMessageImages(
                    withContext(Dispatchers.IO) {
                        uris.map {
                            try {
                                ImageUtils.getEncodedImageFromUri(requireActivity(), it)
                            } catch(ex: FileNotFoundException) {
                                Toast.makeText(requireContext(),
                                    "File for sending not found! Please verify it exists!", Toast.LENGTH_LONG)
                                    .show()
                                null
                            }
                        }
                    }.filterNotNull(),
                    activityViewModel.authUser.value!!.id,
                    activityViewModel.authChatroom.value!!.id
                )
            )
        }
    }

    override fun observePredicateValidators() {
        viewModel.message.invalidity.observe(
            viewLifecycleOwner,
            TextInputLayoutFocusValidatorObserver(binding.messageInputField, Constants.messageInputError)
        )
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(
            viewLifecycleOwner, ViewReverseEnablerObserver(binding.sendMessageButton))
    }

    override fun onDestroy() {
        super.onDestroy()
        val activity = requireActivity()

        if(!activity.isTaskRoot) {
            createMessageReceiver?.let {
                localBroadcastManager.unregisterReceiver(it)
            }
            editMessageReceiver?.let {
                localBroadcastManager.unregisterReceiver(it)
            }
            deleteMessageReceiver?.let {
                localBroadcastManager.unregisterReceiver(it)
            }
        }
    }

    override fun onEditMessageSelect(view: View, message: Message) {
        Log.i("ChatroomMListFragment", "onEditMessageSelect triggered in message list fragment for $message!")

        viewModel.message.setValue(message.message) // set to edit current message from bottom sheet
        with(binding) {
            messageInputField.editText?.setSelection(binding.messageInputField.editText!!.text.length)
            sendMessageButton.setOnClickListener(onEditSendMessage(message))
            editMessageOptionsLayout.isVisible = true // ...to the original onClickListener for send button
        }
    }

    override fun onDeleteMessageSelect(view: View, message: Message) {
        Log.i("ChatroomMListFragment", "onDeleteMessageSelect triggered in message list fragment for $message!")
        lifecycleScope.launch {
            viewModel.sendMessageIntent(MessageIntent.DeleteMessage(message.id))
        }
    }

    fun resetMessages() {
        Log.i("ChatroomMListFragment", "resetting messages")
        viewModel.setCurrentMessages(null)
    }
}