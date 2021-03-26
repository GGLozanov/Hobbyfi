package com.example.hobbyfi.ui.chatroom

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.message.ChatroomMessageListAdapter
import com.example.hobbyfi.databinding.FragmentChatroomMessageListBinding
import com.example.hobbyfi.intents.MessageIntent
import com.example.hobbyfi.intents.MessageListIntent
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.ui.UIMessage
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.MessageState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.RefreshConnectionAware
import com.example.hobbyfi.ui.base.ServerSocketAccessor
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.chatroom.ChatroomMessageListFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.kroegerama.imgpicker.BottomSheetImagePicker
import com.kroegerama.imgpicker.ButtonType
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject
import java.io.FileNotFoundException
import java.util.*


@ExperimentalCoroutinesApi
@ExperimentalPagingApi
class ChatroomMessageListFragment : ChatroomMessageFragment(), TextFieldInputValidationOnus,
        BottomSheetImagePicker.OnImagesSelectedListener,
        ChatroomMessageBottomSheetDialogFragment.OnMessageOptionSelected, ServerSocketAccessor,
        SharedPreferences.OnSharedPreferenceChangeListener, RefreshConnectionAware {

    public override val viewModel: ChatroomMessageListFragmentViewModel by viewModels()

    private lateinit var binding: FragmentChatroomMessageListBinding
    private var typingTimer: CountDownTimer? = null

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
            navController.navigate(
                ChatroomMessageListFragmentDirections
                    .actionChatroomMessageListFragmentToImageViewFragment(messageB.userMessage.text.toString())
            )
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
            val messageMap = mutableMapOf<String, String?>()

            if(editedMessage.message != viewModel.message.value) { // kinda bruh for the two-way databinding but I'm dumb
                messageMap[Constants.MESSAGE] = viewModel.message.value
            } else {
                Toast.makeText(
                    requireContext(),
                    "You can't edit a message with the same message!",
                    Toast.LENGTH_LONG
                ).show()
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

    override val emitterListenerFactory: EmitterListenerFactory by lazy {
        EmitterListenerFactory(requireActivity())
    }

    override val serverSocket: Socket? by lazy {
        (requireActivity() as ChatroomActivity).serverSocket
    }

    private val socketEmissionErrorFallback = { e: Exception ->
        (requireActivity() as ChatroomActivity).handleAuthActionableError(
            e.message,
            e.isCritical,
            context = requireContext()
        )
    }

    val createMessageEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForCreate(
            ::Message,
            { message ->
                if (prefConfig.readReachedBottomMessagesAfterSearch()) {
                    if (activityViewModel.authUser.value?.chatroomIds?.contains(message.chatroomSentId) == true) {
                        // assert user still in chatroom (kick race condition)
                        lifecycleScope.launchWhenCreated {
                            viewModel.messageStateIntent.sendIntent(
                                MessageIntent.CreateMessageCache(
                                    message
                                )
                            )
                        }
                    } else {
                        Log.wtf(
                            "ChatroomActivity",
                            "Received message for INVALID CHATROOM. THIS SHOULDN'T HAPPEN!"
                        )
                    }
                } else {
                    Log.i(
                        "ChatroomActivity",
                        "RECEIVED MESSAGE WHILE BOTTOM GENERATION IS STILL NOT RENDERED"
                    )
                    viewModel.addSearchDeferredMessage(message)
                }
            },
            socketEmissionErrorFallback
        )
    }

    val editMessageEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForEdit(
            { editFields ->
                if (messageListAdapter.findItemFromCurrentPagingData { msg ->
                        msg is UIMessage.MessageItem && msg.message.id ==
                                editFields[Constants.ID]?.toLong()
                    } != null &&
                    activityViewModel.authUser.value?.chatroomIds?.contains(
                        editFields[Constants.CHATROOM_SENT_ID]?.toLong()) == true) {
                    // only update if item is currently visible in pages
                    lifecycleScope.launchWhenCreated {
                        viewModel.messageStateIntent.sendIntent(
                            MessageIntent.UpdateMessageCache(
                                editFields
                            )
                        )
                    }
                }
            },
            socketEmissionErrorFallback
        )
    }

    val deleteMessageEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForDelete(
            { id ->
                if (messageListAdapter.findItemFromCurrentPagingData { msg -> msg is UIMessage.MessageItem && msg.message.id == id } != null &&
                    activityViewModel.authUser.value?.chatroomIds?.contains(
                        activityViewModel.authChatroom.value?.id
                    ) == true) {
                    // only delete if message currently visible
                    lifecycleScope.launchWhenCreated {
                        viewModel.messageStateIntent.sendIntent(
                            MessageIntent.DeleteMessageCache(
                                id
                            )
                        )
                    }
                }
            },
            socketEmissionErrorFallback
        )
    }

    private val userTypingEmitterListener: Emitter.Listener = Emitter.Listener {
        requireActivity().runOnUiThread {
            messageListAdapter.addTypingUser((it[0] as JSONObject).getLong(Constants.ID))
        }
    }

    private val userStopTyingEmitterListener: Emitter.Listener = Emitter.Listener {
        requireActivity().runOnUiThread {
            messageListAdapter.removeTypingUser((it[0] as JSONObject).getLong(Constants.ID))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        connectServerSocketListeners()
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
            cancelHeader.setOnClickListener { sendMessageButton.setOnClickListener(
                onNormalSendMessage
            )
                viewModel!!.message.setValue(null)
                editMessageOptionsLayout.isVisible = false
            }

            initTypingListener()
            initMessageListAdapter()
            observeUsers()
            observeMessageState()
            observeMessagesState()
            observeConnectionRefresh(
                savedInstanceState, (requireActivity() as BaseActivity)
                    .refreshConnectivityMonitor
            )

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSearchMessage()
    }

    override fun onConnectedServerSocketFail() {
        // handled by ChatroomActivity. . . BAD implementation
    }

    override fun connectServerSocketListeners() {
        serverSocket?.on(
            Constants.CREATE_MESSAGE_TYPE,
            createMessageEmitterListener
        )
        serverSocket?.on(
            Constants.EDIT_MESSAGE_TYPE,
            editMessageEmitterListener
        )
        serverSocket?.on(
            Constants.DELETE_MESSAGE_TYPE,
            deleteMessageEmitterListener
        )
        serverSocket?.on(
            Constants.USER_TYPING,
            userTypingEmitterListener
        )
        serverSocket?.on(
            Constants.USER_CEASE_TYPING,
            userStopTyingEmitterListener
        )
    }

    @ExperimentalPagingApi
    @ExperimentalCoroutinesApi
    override fun observeUsers() {
        activityViewModel.chatroomUsers.observe(viewLifecycleOwner, Observer {
            Log.i("ChatroomMListFragment", "Adapter users: $it")
            if (viewModel.areCurrentMessagesNull && it.isNotEmpty()) {
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
                        // TODO: stop button? Or do nothing? Or just show the queued up messages as invisible and sending WIP?
                    }
                    is MessageState.OnData.MessageCreateResult -> {
                        viewModel.message.setValue(null) // reset msg
                        // FIXME: Inefficient?
                        typingTimer = null
                        serverSocket?.emit(Constants.USER_CEASE_TYPING, JSONObject(mapOf(
                            Constants.ID to activityViewModel.authUser.value?.id,
                            Constants.CHATROOM_ID to activityViewModel.authChatroom.value?.id
                        ))) // stop typing
                    }
                    is MessageState.OnData.MessageUpdateResult -> {
                        binding.cancelHeader.callOnClick()
                    }
                    is MessageState.OnData.MessageDeleteResult -> {
                        Toast.makeText(
                            requireContext(),
                            "Successfully deleted message!",
                            Toast.LENGTH_LONG
                        ).show()
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
                    messageListAdapter.findItemPositionFromCurrentPagingData(
                        UIMessage.MessageItem(
                            it
                        )
                    ).run {
                        Log.i(
                            "ChatroomMListFragment",
                            "POSITION received from navcontroller handle: ${this}"
                        )

                        when {
                            this != null -> {
                                binding.messageList.scrollToPosition(this)
                                Handler(
                                    Looper.myLooper() ?: binding.messageList.handler.looper
                                ).postDelayed(
                                    {
                                        binding.messageList.smoothScrollToPosition(
                                            this
                                        )
                                    },
                                    50
                                )

                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    Constants.searchMessage,
                                    null
                                )
                            }
                            connectivityManager.isConnected() -> { // assert whether there's any point to search through the messages
                                // delete messages cached so that new generation can be propagated (like Discord)
                                viewModel.setSentMessageIdFetchRequestPrior(true)
                                lifecycleScope.launch {
                                    viewModel.sendIntent(
                                        MessageListIntent.FetchMessages(
                                            activityViewModel.authChatroom.value!!.id,
                                            messageId = navController.currentBackStackEntry
                                                ?.savedStateHandle?.get<Message?>(Constants.searchMessage)!!.id
                                            // always set at this point
                                        )
                                    )
                                }
                            }
                            else -> { // do nothing otherwise (this should some kind of a banner in the where it says there's no connection)
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    Constants.searchMessage,
                                    null
                                )
                            }
                        }
                    }
                }
            })
    }

    override fun onPostMessageListCollect(currentMessages: PagingData<UIMessage>, qMessageId: Long?) {
        if(qMessageId != null) {
            if(viewModel.sentMessageIdFetchRequestPrior) {
                viewModel.setSentMessageIdFetchRequestPrior(false)
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    Constants.searchMessage,
                    null
                )
            }
        }

        // FIXME: Remove for better option because of Bundle size limits
        navController.currentBackStackEntry?.savedStateHandle?.set<List<Message>?>(
            Constants.currentMessages, messageListAdapter.extractListFromCurrentPagingData()
                .filterIsInstance<UIMessage.MessageItem>()
                .map { it.message }
        )
    }

    override fun observeConnectionRefresh(
        savedState: Bundle?,
        refreshConnectivityMonitor: RefreshConnectivityMonitor
    ) {
        super.observeConnectionRefresh(savedState, refreshConnectivityMonitor)
        refreshConnectivityMonitor.observe(viewLifecycleOwner, Observer { connectionRefreshed ->
            //  connectivityManager.isConnected() IMPORTANT TODO: Fix in order to refetch if user
            //  enter without internet (currently refetches old chatrooms in itiial joins)
            if (connectionRefreshed) {
                Log.i("ChatroomMListFragment", "ChatroomMessageListFragment CONNECTED")
                refreshDataOnConnectionRefresh()
            } else {
                Log.i("ChatroomMListFragment", "ChatroomMessageListFragment DIS-CONNECTED")
            }
        })
    }

    override fun refreshDataOnConnectionRefresh() {
        messageListAdapter.refresh()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        if(key == getString(R.string.pref_reached_bottom_messages_after_search) && prefs.getBoolean(
                key,
                true
            )) {
            viewModel.createSearchDeferredMessages()
        }
    }

    private fun initTypingListener() {
        binding.messageInputField.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if(typingTimer == null) {
                    serverSocket?.emit(Constants.USER_TYPING, JSONObject(mapOf(
                        Constants.ID to activityViewModel.authUser.value?.id,
                        Constants.CHATROOM_ID to activityViewModel.authChatroom.value?.id
                    )))
                    typingTimer = object : CountDownTimer(5000, 5000) {
                        override fun onFinish() {
                            typingTimer = null
                            serverSocket?.emit(Constants.USER_CEASE_TYPING, JSONObject(mapOf(
                                Constants.ID to activityViewModel.authUser.value?.id,
                                Constants.CHATROOM_ID to activityViewModel.authChatroom.value?.id
                            )))
                        }

                        override fun onTick(p0: Long) {

                        }
                    }.apply {
                        start()
                    }
                } else {
                    typingTimer?.cancel()
                    typingTimer?.start()
                }
            }
        })

        binding.messageInputField.editText?.onFocusChangeListener =
            OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    serverSocket?.emit(Constants.USER_CEASE_TYPING, JSONObject(mapOf(
                        Constants.ID to activityViewModel.authUser.value?.id,
                        Constants.CHATROOM_ID to activityViewModel.authChatroom.value?.id
                    )))
                }
            }
    }
    
    override fun initMessageListAdapter() {
        with(binding) {
            messageList.addItemDecoration(VerticalSpaceItemDecoration(15))
            messageListAdapter.registerAdapterDataObserver(object :
                RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    val firstVisiblePosition: Int =
                        (binding.messageList.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()

                    Log.i(
                        "ChatroomMListFragment",
                        "first visible position: $firstVisiblePosition. Position start: $positionStart"
                    )
                    if (firstVisiblePosition == -1 || (firstVisiblePosition == 0 && positionStart == 0)) {
                        binding.messageList.scrollToPosition(positionStart)
                    }
                }
            })
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
        activityViewModel.isAuthUserChatroomOwner.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer {
                activity.binding.toolbar
                    .navigationIcon = if (it)
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_baseline_admin_panel_settings_24
                    )
                else null

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
                                ImageUtils.getEncodedImageFromUri(
                                    requireActivity().contentResolver,
                                    it
                                )
                            } catch (ex: FileNotFoundException) {
                                Toast.makeText(
                                    requireContext(),
                                    "File for sending not found! Please verify it exists!",
                                    Toast.LENGTH_LONG
                                ).show()
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
            TextInputLayoutFocusValidatorObserver(
                binding.messageInputField,
                Constants.messageInputError
            )
        )
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(
            viewLifecycleOwner, ViewReverseEnablerObserver(binding.sendMessageButton)
        )
    }

    override fun onEditMessageSelect(view: View, message: Message) {
        Log.i(
            "ChatroomMListFragment",
            "onEditMessageSelect triggered in message list fragment for $message!"
        )

        viewModel.message.setValue(message.message) // set to edit current message from bottom sheet
        with(binding) {
            messageInputField.editText?.setSelection(binding.messageInputField.editText!!.text.length)
            sendMessageButton.setOnClickListener(onEditSendMessage(message))
            editMessageOptionsLayout.isVisible = true // ...to the original onClickListener for send button
        }
    }

    override fun onDeleteMessageSelect(view: View, message: Message) {
        Log.i(
            "ChatroomMListFragment",
            "onDeleteMessageSelect triggered in message list fragment for $message!"
        )
        lifecycleScope.launch {
            viewModel.sendMessageIntent(MessageIntent.DeleteMessage(message.id))
        }
    }

    fun resetMessages() {
        Log.i("ChatroomMListFragment", "resetting messages")
        viewModel.setCurrentMessages(null)
    }
}