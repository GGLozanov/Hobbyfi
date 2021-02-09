package com.example.hobbyfi.viewmodels.base

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.repositories.ChatroomRepository
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.models.User
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
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

    private var _isAuthUserChatroomOwner = MutableLiveData((authUser.value?.id ?: false) ==
            authChatroom.value?.ownerId) // initial check; updated every time auth user or auth chatroom changes
    val isAuthUserChatroomOwner get() = _isAuthUserChatroomOwner

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
                        fetchChatroom()
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
                        deleteChatroomCache(true)
                    }
                    is ChatroomIntent.UpdateChatroomCache -> {
                        updateAndSaveChatroom(it.chatroomUpdateFields)
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    private suspend fun fetchChatroom() {
        chatroomStateIntent.setState(ChatroomState.Loading)

        chatroomRepository.getChatroom().catch { e ->
            chatroomStateIntent.setState(
                ChatroomState.Error(
                    e.message,
                    shouldExit = e.isCritical
                )
            )
        }.collect {
            if(it != null) {
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

            deleteChatroomCache()

            response
        } catch(ex: Exception) {
            ChatroomState.Error(
                ex.message,
                shouldExit = ex.isCritical
            )
        })
    }

    private suspend fun deleteChatroomCache(setState: Boolean = false) {
        val success = chatroomRepository.deleteChatroomCache(_authChatroom.value!!) &&
                userRepository.deleteUsersCache(_authUser.value!!.id)

        updateAndSaveUser(mapOf(
            Pair(Constants.CHATROOM_IDS,
                Constants.tagJsonConverter.toJson(_authUser.value!!.chatroomIds?.filter { chIds -> chIds != _authChatroom.value!!.id }))
        )) // nullify chatroom for cache user after deletion

        if(setState) {
            chatroomStateIntent.setState(if(success) ChatroomState.OnData.DeleteChatroomCacheResult
                else ChatroomState.Error(Constants.cacheDeletionError))
        } else if(!success) {
            throw Exception(Constants.cacheDeletionError)
        }
    }

    private suspend fun updateChatroom(updateFields: Map<String?, String?>) {
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

    protected suspend fun updateAndSaveChatroom(chatroomFields: Map<String?, String?>) {
        val updatedChatroom = _authChatroom.value?.updateFromFieldMap(chatroomFields)

        saveChatroom(updatedChatroom!!)
        _authChatroom.value = updatedChatroom
    }

    private suspend fun saveChatroom(chatroom: Chatroom) = chatroomRepository.saveChatroom(chatroom)

    // evaluates current auth room and auth user ownership
    // (for when user and chatroom aren't passed and need to be fetched async - i.e. deeplink)
    private fun isAuthUserAuthChatroomOwner(): Boolean {
        return authUser.value?.id ==
                authChatroom.value?.ownerId
    }

}