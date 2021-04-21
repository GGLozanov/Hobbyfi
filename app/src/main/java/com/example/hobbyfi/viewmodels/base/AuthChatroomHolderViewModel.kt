package com.example.hobbyfi.viewmodels.base

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.StateIntent
import com.example.hobbyfi.repositories.ChatroomRepository
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthChatroomHolderViewModel(
    application: Application,
    user: User?,
    chatroom: Chatroom?
) : AuthUserHolderViewModel(application, user) {

    protected var _authChatroom: MutableLiveData<Chatroom?> = MutableLiveData(chatroom)
    val authChatroom: LiveData<Chatroom?> get() = _authChatroom

    protected val chatroomRepository: ChatroomRepository by instance(tag = "chatroomRepository")

    protected val chatroomStateIntent: StateIntent<ChatroomState, ChatroomIntent> = object : StateIntent<ChatroomState, ChatroomIntent>() {
        override val _state: MutableStateFlow<ChatroomState> = MutableStateFlow(ChatroomState.Idle)
    }

    val chatroomState get() = chatroomStateIntent.state

    protected var _isAuthUserChatroomOwner = MutableLiveData((authUser.value?.id ?: false) ==
            authChatroom.value?.ownerId) // initial check; updated every time auth user or auth chatroom changes
    val isAuthUserChatroomOwner: LiveData<Boolean> get() = _isAuthUserChatroomOwner

    fun setChatroom(chatroom: Chatroom?) {
        _authChatroom.value = chatroom
        _isAuthUserChatroomOwner.value = isAuthUserAuthChatroomOwner()
    }

    suspend fun sendChatroomIntent(i: ChatroomIntent) {
        chatroomStateIntent.sendIntent(i)
    }

    override fun setUser(user: User?) {
        super.setUser(user)
        _isAuthUserChatroomOwner.value = isAuthUserAuthChatroomOwner()
    }

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            chatroomStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is ChatroomIntent.FetchChatroom -> {
                        fetchChatroom(it.currentDestinationId)
                    }
                    is ChatroomIntent.DeleteChatroom -> {
                        deleteChatroom()
                    }
                    is ChatroomIntent.UpdateChatroom -> {
                        viewModelScope.launch { // handling potential image upload heaviness from request
                            updateChatroom(it.chatroomUpdateFields)
                        }
                    }
                    is ChatroomIntent.DeleteChatroomCache -> {
                        Log.i("AuthChatromHVM", "Deleting chatroom auth chatroom cache intent sent!")
                        deleteChatroomCache(true, it.kicked)
                    }
                    is ChatroomIntent.UpdateChatroomCache -> {
                        updateAndSaveChatroom(it.chatroomUpdateFields)
                    }
                    is ChatroomIntent.TogglePushNotificationForChatroomAuthUser -> {
                        togglePushNotificationAllow(it.send)
                    }
                }
            }
        }
    }

    private suspend fun fetchChatroom(currentDestinationId: Int?) {
        // ehhhhhhh, this kinda shouldn't be here
        if(currentDestinationId != null && currentDestinationId == R.id.chatroomMessageListFragment) {
            chatroomStateIntent.setState(ChatroomState.Loading)
        }

        chatroomRepository.getChatroom().catch { e ->
            chatroomStateIntent.setState(
                ChatroomState.Error(
                    e.message,
                    shouldExit = e.isCritical || e is HobbyfiAPI.NoConnectivityException // always needs to be connected for these calls (due to CONTEXT)
                )
            )
        }.collect {
            if(it != null) {
                Log.i("AuthChatroomHolderVM", "Collecting new non-null auth chatroom: ${it}")
                setChatroom(it)
                chatroomStateIntent.setState(ChatroomState.OnData.ChatroomResult(it))
            }
        }
    }

    private suspend fun deleteChatroom() {
        chatroomStateIntent.setState(ChatroomState.Loading)

        chatroomStateIntent.setState(try {
            val response = ChatroomState.OnData.ChatroomDeleteResult(
                chatroomRepository.deleteChatroom(_authChatroom.value!!.id)
            )

            deleteChatroomCache(kicked = false)

            response
        } catch(ex: Exception) {
            ChatroomState.Error(
                ex.message,
                shouldExit = ex.isCritical
            )
        })
    }

    private suspend fun deleteChatroomCache(setState: Boolean = false, kicked: Boolean) {
        val success = if(!kicked) {
            chatroomRepository.deleteChatroomCache(_authChatroom.value!!) &&
                    userRepository.deleteUsersCache(_authUser.value!!.id)
        } else userRepository.deleteUsersCache(_authUser.value!!.id)
        // just delete users if kick (hacky solution and fucks up semantics but w/e)

        if(!kicked) {
            updateAndSaveUser(mapOf(
                Constants.CHATROOM_IDS to
                    Constants.jsonConverter.toJson(_authUser.value!!.chatroomIds?.filter { chIds -> chIds != _authChatroom.value!!.id }),
                Constants.ALLOWED_PUSH_CHATROOM_IDS to
                        Constants.jsonConverter.toJson(_authUser.value!!.allowedPushChatroomIds?.filter { chIds -> chIds != _authChatroom.value!!.id }))
            ) // nullify chatroom for cache user after deletion
        }

        if(setState) {
            chatroomStateIntent.setState(if(success) ChatroomState.OnData.DeleteChatroomCacheResult(kicked)
                else ChatroomState.Error(Constants.cacheDeletionError))
        } else if(!success) {
            throw Exception(Constants.cacheDeletionError)
        }
    }

    private suspend fun updateChatroom(updateFields: Map<String, String?>) {
        chatroomStateIntent.setState(ChatroomState.Loading)

        chatroomStateIntent.setState(try {
            val response = ChatroomState.OnData.ChatroomUpdateResult(
                chatroomRepository.editChatroom(updateFields),
                updateFields
            )

            updateAndSaveChatroom(updateFields)

            response
        } catch(ex: Exception) {
            ChatroomState.Error(
                ex.message,
                shouldExit = ex.isCritical
            )
        })
    }

    protected suspend fun updateAndSaveChatroom(chatroomFields: Map<String, String?>) {
        val updatedChatroom = _authChatroom.value?.updateFromFieldMap(chatroomFields)

        saveChatroom(updatedChatroom!!)
        _authChatroom.value = updatedChatroom
    }

    private suspend fun togglePushNotificationAllow(allow: Boolean) {
        try {
            userRepository.togglePushNotificationAllowForChatroomUser(
                authChatroom.value!!.id,
                allow
            )

            updateAndSaveUser(mapOf(
                Constants.ALLOWED_PUSH_CHATROOM_IDS to
                    (if(!allow) Constants.jsonConverter.toJson(
                        _authUser.value!!.allowedPushChatroomIds?.filter { chId -> chId != _authChatroom.value!!.id }
                    ) else Constants.jsonConverter.toJson(_authUser.value!!.allowedPushChatroomIds?.plus(
                        _authChatroom.value!!.id) ?: arrayOf(_authChatroom.value!!.id))
            )))
        } catch(ex: Exception) {
            chatroomStateIntent.setState(
                ChatroomState.Error(
                    ex.message,
                    ex.isCritical
                )
            )
        }
    }

    private suspend fun saveChatroom(chatroom: Chatroom) = chatroomRepository.saveChatroom(chatroom)

    // evaluates current auth room and auth user ownership
    // (for when user and chatroom aren't passed and need to be fetched async - i.e. deeplink)
    private fun isAuthUserAuthChatroomOwner(): Boolean {
        return authUser.value?.id ==
                authChatroom.value?.ownerId
    }

}