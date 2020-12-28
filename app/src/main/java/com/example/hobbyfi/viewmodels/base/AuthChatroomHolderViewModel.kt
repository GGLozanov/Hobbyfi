package com.example.hobbyfi.viewmodels.base

import android.app.Application
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthChatroomHolderViewModel(application: Application, user: User?, chatroom: Chatroom?)
    : AuthUserHolderViewModel(application, user) {

    protected var _authChatroom: MutableLiveData<Chatroom?> = MutableLiveData(chatroom)
    val authChatroom: LiveData<Chatroom?> get() = _authChatroom

    protected val chatroomRepository: ChatroomRepository by instance(tag = "chatroomRepository")

    private val chatroomStateIntent: StateIntent<ChatroomState, ChatroomIntent> = object : StateIntent<ChatroomState, ChatroomIntent>() {
        override val _state: MutableStateFlow<ChatroomState> = MutableStateFlow(ChatroomState.Idle)
    }

    val chatroomState get() = chatroomStateIntent.state

    private var _isAuthUserChatroomOwner = MutableLiveData(authUser.value?.id ==
            authChatroom.value?.ownerId) // initial check; updated every time auth user or auth chatroom changes
    val isAuthUserChatroomOwner get() = _isAuthUserChatroomOwner

    fun setChatroom(chatroom: Chatroom?) {
        _authChatroom.value = chatroom
        _isAuthUserChatroomOwner.value = isAuthUserAuthChatroomOwner()
    }

    suspend fun sendChatroomIntent(i: ChatroomIntent) {
        chatroomStateIntent.sendIntent(i)
    }

    override fun setUser(user: User) {
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
                        updateChatroom(it.chatroomUpdateFields)
                    }
                    is ChatroomIntent.DeleteChatroomCache -> {
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
        // TODO: If this doesn't work or seems too coupled, make a separate fetch chatroom method and add it to ChatroomState/ChatroomIntent
        chatroomStateIntent.setState(ChatroomState.Loading)

        chatroomRepository.getChatroom().catch { e ->
            e.printStackTrace()
            chatroomStateIntent.setState(
                ChatroomState.Error(Constants.reauthError, shouldExit = isExceptionCritical(e as Exception)))
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
                chatroomRepository.deleteChatroom()
            )

            deleteChatroomCache()

            response
        } catch(ex: Exception) {
            ChatroomState.Error(
                ex.message,
                shouldExit = isExceptionCritical(ex)
            )
        })
    }

    private suspend fun deleteChatroomCache(setState: Boolean = false) {
        val success = chatroomRepository.deleteChatroomCache(_authChatroom.value!!)
        if(!success) {
            throw Exception(Constants.cacheDeletionError)
        }

        updateAndSaveUser(mapOf(
            Pair(Constants.CHATROOM_ID, "0")
        )) // nullify chatroom for cache user after deletion

        if(setState) {
            chatroomStateIntent.setState(if(success) ChatroomState.OnData.DeleteChatroomCacheResult
                else ChatroomState.Error(Constants.cacheDeletionError))
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
                shouldExit = isExceptionCritical(ex)
            )
        })
    }

    suspend fun updateAndSaveChatroom(chatroomFields: Map<String?, String?>) {
        val updatedChatroom = _authChatroom.value?.updateFromFieldMap(chatroomFields)

        chatroomRepository.saveChatroom(updatedChatroom!!)
        _authChatroom.value = updatedChatroom
    }

    // evaluates current auth room and auth user ownership
    // (for when user and chatroom aren't passed and need to be fetched async - i.e. deeplink)
    private fun isAuthUserAuthChatroomOwner(): Boolean {
        return authUser.value?.id ==
                authChatroom.value?.ownerId
    }

}