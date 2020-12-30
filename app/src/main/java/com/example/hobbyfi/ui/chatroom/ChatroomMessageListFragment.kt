package com.example.hobbyfi.ui.chatroom

import android.content.BroadcastReceiver
import android.content.Intent
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
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.DefaultLoadStateAdapter
import com.example.hobbyfi.adapters.message.ChatroomMessageListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomMessageListBinding
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.shared.ChatroomMessageBroadacastReceiverFactory
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.MessageListState
import com.example.hobbyfi.state.MessageState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.main.MainActivity
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.kroegerama.imgpicker.BottomSheetImagePicker
import com.kroegerama.imgpicker.ButtonType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomMessageListFragment : ChatroomFragment(),
        BottomSheetImagePicker.OnImagesSelectedListener, ChatroomMessageBottomSheetDialogFragment.OnMessageOptionSelected {
    private val viewModel: ChatroomMessageListFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChatroomMessageListBinding

    private lateinit var messageListAdapter: ChatroomMessageListAdapter

    private val onNormalSendMessage = View.OnClickListener {
        if(assertTextFieldsInvalidity()) {
            return@OnClickListener
        }

        lifecycleScope.launch {
            createMessage()
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

    private lateinit var chatroomMessageBroadacastReceiverFactory: ChatroomMessageBroadacastReceiverFactory

    private lateinit var createMessageReceiver: BroadcastReceiver
    private lateinit var editMessageReceiver: BroadcastReceiver
    private lateinit var deleteMessageReceiver: BroadcastReceiver

    private val loadStateAdapter: DefaultLoadStateAdapter = DefaultLoadStateAdapter(
        { messageListAdapter.retry() },
        null,
        userHasChatroom = true
    )

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = requireActivity() as ChatroomActivity

        // TODO: Move receiver registration in after chatroom messages fetch!!!
        chatroomMessageBroadacastReceiverFactory = ChatroomMessageBroadacastReceiverFactory
            .getInstance(viewModel, activityViewModel, activity)
        createMessageReceiver = chatroomMessageBroadacastReceiverFactory.createActionatedReceiver(Constants.CREATE_MESSAGE_TYPE)
        editMessageReceiver = chatroomMessageBroadacastReceiverFactory.createActionatedReceiver(Constants.EDIT_MESSAGE_TYPE)
        deleteMessageReceiver = chatroomMessageBroadacastReceiverFactory.createActionatedReceiver(Constants.DELETE_MESSAGE_TYPE)

        activity.registerReceiver(createMessageReceiver, IntentFilter(Constants.CREATE_MESSAGE_TYPE))
        activity.registerReceiver(editMessageReceiver, IntentFilter(Constants.EDIT_MESSAGE_TYPE))
        activity.registerReceiver(deleteMessageReceiver, IntentFilter(Constants.DELETE_MESSAGE_TYPE))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_chatroom_message_list, container, false)
        binding.viewModel = viewModel

        messageListAdapter = ChatroomMessageListAdapter(
            activityViewModel.currentAdapterUsers.value ?: emptyList()
        ) { _, message ->
            val bottomSheet = ChatroomMessageBottomSheetDialogFragment.newInstance(message)
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            return@ChatroomMessageListAdapter true
        }

        // TODO: BroadcastReceiver here triggered => insert message and remote keys (calculate them based on adapter dataset itemCount divided by page size)
        // TODO: They will be used later in the RemoteMediator
        // TODO: If notification for new message is received => insert message into database => trigger APPEND
        // TODO: In RemoteMediator, check if REFRESH loadstate => has new message in database with its remote key =>
        // TODO: return Mediator Success and load new list with new message?
        // TODO: Append message to room db so that it can be pushed to the paging data automatically;

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
                    .show(parentFragmentManager)
            }

            sendMessageButton.setOnClickListener(onNormalSendMessage)
            cancelHeader.setOnClickListener { sendMessageButton.setOnClickListener(onNormalSendMessage)
                cancelHeader.isVisible = false
            }

            initMessageListAdapter()
            observeUsers()
            observeMessageState()
            observeMessagesState()
            observeConnectionRefresh()

            return@onCreateView root
        }
    }

    private fun observeUsers() {
        activityViewModel.currentAdapterUsers.observe(viewLifecycleOwner, Observer {
            if(viewModel.areCurrentMessagesNull) {
                // send message fetch intent
                lifecycleScope.launch {
                    viewModel.sendIntent(MessageListIntent.FetchMessages)
                }
            } else {
                // update chatroomlistadapter users
                messageListAdapter.setCurrentUsers(it)
            }
        })
    }

    private fun observeMessagesState() {
        lifecycleScope.launch {
            viewModel.mainState.collectLatest {
                when(it) {
                    is MessageListState.Idle -> {

                    }
                    is MessageListState.Loading -> {

                    }
                    is MessageListState.OnData.MessagesResult -> {
                        binding.swipeRefresh.isRefreshing = false
                        it.messages.catch { e ->
                            e.printStackTrace()
                            if((e as Exception).isCritical) {
                                Toast.makeText(requireContext(), Constants.reauthError, Toast.LENGTH_LONG)
                                    .show()
                                // TODO: Switch to `startActivityForResult` calls cuz process death
                                (requireActivity()).sendBroadcast(Intent(Constants.LOGOUT))
                            } else if(e !is CancellationException) {
                                Log.i("ChatroomMListFragment", "it.messages collect() received a normal exception: $e")
                                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }.collectLatest { data ->
                            messageListAdapter.submitData(data)
                        }
                    }
                    is MessageListState.Error -> {

                    }
                }
            }
        }
    }

    private fun observeMessageState() {
        lifecycleScope.launch {
            viewModel.messageState.collectLatest {
                when(it) {
                    is MessageState.Idle -> {

                    }
                    is MessageState.Loading -> {

                    }
                    is MessageState.OnData.MessageCreateResult -> {

                        viewModel.message.value = null // reset msg
                    }
                    is MessageState.OnData.MessageUpdateResult -> {

                        viewModel.message.value = null
                    }
                    is MessageState.OnData.MessageDeleteResult -> {
                        Toast.makeText(requireContext(), "Successfully deleted message!", Toast.LENGTH_LONG)
                            .show()
                    }
                    is MessageState.OnData.DeleteMessageCacheResult -> {
                        // prolly don't do much, if anything here
                    }
                    is MessageState.Error -> {

                    }
                }
            }
        }
    }


    private fun observeConnectionRefresh() {
        (requireActivity() as BaseActivity).refreshConnectivityMonitor.observe(viewLifecycleOwner, Observer { connectionRefreshed ->
            if(connectionRefreshed) {
                Log.i("ChatroomMListFragment", "ChatroomMessageListFragment CONNECTED")
                messageListAdapter.refresh()
            } else {
                Log.i("ChatroomMListFragment", "ChatroomMessageListFragment DIS-CONNECTED")
            }
        })
    }

    private fun initMessageListAdapter() {
        with(binding) {
            messageList.addItemDecoration(VerticalSpaceItemDecoration(10))
            messageList.adapter = messageListAdapter.withLoadStateFooter(loadStateAdapter)

            swipeRefresh.setOnRefreshListener {
                messageListAdapter.refresh()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        observeUIState()
    }

    // COHESION GO YEEEEEEEET
    private fun observeUIState() {
        val activity = requireActivity() as ChatroomActivity

        activityViewModel.authChatroom.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            setToolbarProperties(it, activity)
        })
        activityViewModel.isAuthUserChatroomOwner.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if(it) {
                activity.binding.toolbar
                    .navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_admin_panel_settings_24)
            }
        })
    }

    private fun setToolbarProperties(chatroom: Chatroom?, activity: ChatroomActivity) {
        activity.title = chatroom?.name
    }

    // FIXME: might, like, totally not work. Maybe just send one intent and parse the URIs there and make them seperate requests
    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        lifecycleScope.launch {
            uris.forEach {
                createMessage(ImageUtils.getEncodedImageFromUri(requireActivity(), it))
            }
        }
    }

    override fun initTextFieldValidators() {
        binding.messageInputField.addTextChangedListener(
            Constants.messageInputError,
            Constants.messagePredicate
        )
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        return FieldUtils.isTextFieldInvalid(binding.messageInputField, Constants.messageInputError)
    }

    override fun onDestroy() {
        super.onDestroy()
        val activity = requireActivity()

        activity.unregisterReceiver(createMessageReceiver)
        activity.unregisterReceiver(editMessageReceiver)
        activity.unregisterReceiver(deleteMessageReceiver)
    }

    override fun onEditMessageSelect(view: View, message: Message) {
        viewModel.message.value = message.message // set to edit current message from bottom sheet
        binding.sendMessageButton.setOnClickListener(onEditSendMessage(message))
        binding.cancelHeader.isVisible = true
        // to the original onClickListener for send button
    }

    override fun onDeleteMessageSelect(view: View, message: Message) {
        lifecycleScope.launch {
            viewModel.sendMessageIntent(MessageIntent.DeleteMessage(message.id))
        }
    }

    private suspend fun createMessage(message: String? = null) {
        viewModel.sendMessageIntent(
            MessageIntent.CreateMessage(
                message,
                activityViewModel.authUser.value!!.id,
                activityViewModel.authChatroom.value!!.id
            )
        )
    }
}