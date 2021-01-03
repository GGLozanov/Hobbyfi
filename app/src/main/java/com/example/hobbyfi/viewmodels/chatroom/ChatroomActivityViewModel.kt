package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import com.example.hobbyfi.models.User
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.UserListState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(application: Application, user: User?, chatroom: Chatroom?)
    : AuthChatroomHolderViewModel(application, user, chatroom) {

    private val eventRepository: EventRepository by instance(tag = "eventRepository")

    private var _currentAdapterUsers: MutableLiveData<List<User>> = MutableLiveData(emptyList())
    val currentAdapterUsers: LiveData<List<User>> get() = _currentAdapterUsers

    private var _authEvent: MutableLiveData<Event> = MutableLiveData()
    val authEvent: LiveData<Event> get() = _authEvent

    fun setAuthEvent(event: Event) {
        _authEvent.value = event
    }

    private val eventStateIntent: StateIntent<EventState, EventIntent> = object : StateIntent<EventState, EventIntent>() {
        override val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState.Idle)
    }
    val eventState get() = eventStateIntent.state

    suspend fun sendEventIntent(intent: EventIntent) = eventStateIntent.sendIntent(intent)

    private val usersStateIntent: StateIntent<UserListState, UserListIntent> = object : StateIntent<UserListState, UserListIntent>() {
        override val _state: MutableStateFlow<UserListState> = MutableStateFlow(UserListState.Idle)
    }
    val usersState get() = usersStateIntent.state

    suspend fun sendUsersIntent(intent: UserListIntent) = usersStateIntent.sendIntent(intent)
    fun resetUserListState() = usersStateIntent.setState(UserListState.Idle)

    fun resetChatroomState() = chatroomStateIntent.setState(ChatroomState.Idle)

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            eventStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is EventIntent.DeleteEvent -> {
                        deleteEvent()
                    }
                    is EventIntent.UpdateEvent -> {
                        updateEvent(it.eventUpdateFields)
                    }
                    is EventIntent.UpdateEventCache -> {
                        updateAndSaveEvent(it.eventUpdateFields)
                    }
                    is EventIntent.CreateEventCache -> {
                        saveEvent(it.event)
                    }
                    is EventIntent.FetchEvent -> {
                        fetchEvent()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
        viewModelScope.launch {
            usersStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is UserListIntent.AddAUserCache -> {
                        saveUser(it.user, false)
                    }
                    is UserListIntent.DeleteAUserCache -> {
                        deleteUserCache(it.userId, false)
                    }
                    is UserListIntent.UpdateAUserCache -> {
                        // TODO: Update lastUsersFetchTime or something similar for Glide signature caching
                        saveUser(currentAdapterUsers.value!!.find { user -> user.id ==
                                (it.userUpdateFields[Constants.ID] ?: error("User ID must not be null in saveUser call!")).toLong() }!!
                            .updateFromFieldMap(it.userUpdateFields), false)
                    }
                    is UserListIntent.FetchUsers -> {
                        fetchUsers()
                    }
                }
            }
        }
    }

    init {
        handleIntent()
    }

    private suspend fun fetchUsers() {
        usersStateIntent.setState(UserListState.Loading)

        userRepository.getChatroomUsers(
            authChatroom.value!!.id
        ).distinctUntilChanged().catch { e ->
            usersStateIntent.setState(
                UserListState.Error(
                    e.message,
                    (e as Exception).isCritical
                )
            )
        }.collect {
            if(it != null) {
                Log.i("ChatroomActivityVM", "Collecting new users from SSOT cache!!! $it")
                setCurrentUsers(it)
                usersStateIntent.setState(
                    UserListState.OnData.UsersResult(
                        it
                    )
                )
            }
        }
    }

    private suspend fun fetchEvent() {
        eventStateIntent.setState(EventState.Loading)

        eventRepository.getEvent(_authChatroom.value!!.id).catch { e ->
            eventStateIntent.setState(
                EventState.Error(
                    e.message,
                    (e as Exception).isCritical
                )
            )
        }.collect {
            if(it != null) {
                _authEvent.value = it
                eventStateIntent.setState(
                    EventState.OnData.EventResult(
                        it
                    )
                )
            }
        }
    }

    private suspend fun updateEvent(updateFields: Map<String?, String?>) {
        eventStateIntent.setState(EventState.Loading)

        eventStateIntent.setState(try {
            val state = EventState.OnData.EventEditResult(eventRepository.editEvent(
                updateFields
            ))

            updateAndSaveEvent(updateFields)

            state
        } catch(ex: Exception) {
            ex.printStackTrace()

            EventState.Error(
                ex.message,
                ex.isCritical
            )
        })
    }

    private suspend fun updateAndSaveEvent(eventFields: Map<String?, String?>) {
        val updatedEvent = _authEvent.value!!.updateFromFieldMap(eventFields)
        eventRepository.saveEvent(updatedEvent)
        _authEvent.value = updatedEvent
    }

    private suspend fun saveEvent(updatedEvent: Event) {
        eventRepository.saveEvent(updatedEvent)
        _authEvent.value = updatedEvent
    }

    private suspend fun deleteEvent() {
        eventStateIntent.setState(EventState.Loading)

        eventStateIntent.setState(try {
            val state = EventState.OnData.EventDeleteResult(
                eventRepository.deleteEvent()
            )

            deleteEventCache()

            state
        } catch(ex: Exception) {
            ex.printStackTrace()

            EventState.Error(
                ex.message
            )
        })
    }

    // FIXME: Ge. Ne. RIIIIICS. Well, not really but still code dup with other deleteCache methods. Mitigate that
    private suspend fun deleteEventCache(setState: Boolean = false) {
        val success = eventRepository.deleteEventCache(_authEvent.value!!.id)

        updateAndSaveChatroom(mapOf(
            Pair(Constants.LAST_EVENT_ID, "0")
        ))

        if(setState) {
            eventStateIntent.setState(if(success) EventState.OnData.DeleteEventCacheResult
                else EventState.Error(Constants.cacheDeletionError))
        } else if(!success) {
            throw Exception(Constants.cacheDeletionError)
        }
    }

    private suspend fun deleteUserCache(id: Long, shouldWritePrefTime: Boolean = true) {
        // TODO: Add setState bool?
        userRepository.deleteUserCache(id, shouldWritePrefTime)
    }

    // TODO: Hide behind intent? Also, bruh conversions
    private fun setCurrentUsers(users: List<User>) {
        _currentAdapterUsers.value = users
    }
}