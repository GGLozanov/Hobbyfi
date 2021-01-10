package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.EventListIntent
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
import com.example.hobbyfi.shared.replace
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.UserListState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(
    application: Application,
    user: User?,
    chatroom: Chatroom?
) : AuthChatroomHolderViewModel(application, user, chatroom) {
    private val eventRepository: EventRepository by instance(tag = "eventRepository")

    private var _currentAdapterUsers: MutableLiveData<List<User>> = MutableLiveData(emptyList())
    val currentAdapterUsers: LiveData<List<User>> get() = _currentAdapterUsers

    // TODO: Change from authEvent one to authEvent List and pass in event selected by user for deletion
    private var _authEvents: MutableLiveData<List<Event>> = MutableLiveData()
    val authEvents: LiveData<List<Event>> get() = _authEvents

    fun setAuthEvents(events: List<Event>) {
        _authEvents.value = events
    }

    private val eventsStateIntent: StateIntent<EventListState, EventListIntent> = object : StateIntent<EventListState, EventListIntent>() {
        override val _state: MutableStateFlow<EventListState> = MutableStateFlow(EventListState.Idle)
    }
    val eventsState get() = eventsStateIntent.state

    suspend fun sendEventsIntent(intent: EventListIntent) = eventsStateIntent.sendIntent(intent)

    private val eventStateIntent: StateIntent<EventState, EventIntent> = object : StateIntent<EventState, EventIntent>() {
        override val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState.Idle)
    }
    
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
                        deleteEvent(it.eventId)
                    }
                    is EventIntent.UpdateEvent -> {
                        updateEvent(it.eventUpdateFields)
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
        viewModelScope.launch {
            eventsStateIntent.intentAsFlow().collect {
                when(it) {
                    is EventListIntent.DeleteAnEventCache -> {
                        if(deleteEventCache(it.eventId)) {
                            setAuthEvents(_authEvents.value!!.filter { event -> event.id != it.eventId })
                        }
                    }
                    is EventListIntent.UpdateAnEventCache -> {
                        updateAndSaveEvent(it.eventUpdateFields)
                    }
                    is EventListIntent.FetchEvents -> {
                        fetchEvents()
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
                        setCurrentUsers(currentAdapterUsers.value!! + it.user)
                    }
                    is UserListIntent.DeleteAUserCache -> {
                        deleteUserCache(it.userId, false)
                        setCurrentUsers(currentAdapterUsers.value!!.filter { user -> user.id != it.userId })
                    }
                    is UserListIntent.UpdateAUserCache -> {
                        // TODO: Update lastUsersFetchTime or something similar for Glide signature caching
                        // TODO: Check if user has image updated? That'd retrigger every image to be refetched on next VH onBind call
                        saveUser(currentAdapterUsers.value!!.find { user -> user.id ==
                                (it.userUpdateFields[Constants.ID] ?: error("User ID must not be null in saveUser call!")).toLong() }!!
                            .updateFromFieldMap(it.userUpdateFields), false)
                        setCurrentUsers(currentAdapterUsers.value!!)
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
        ).catch { e ->
            usersStateIntent.setState(
                UserListState.Error(
                    e.message,
                    (e as Exception).isCritical
                )
            )
        }.collectLatest {
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

    private suspend fun fetchEvents() {
        eventsStateIntent.setState(EventListState.Loading)

        eventRepository.getEvent(_authChatroom.value!!.id).catch { e ->
            eventsStateIntent.setState(
                EventListState.Error(
                    e.message,
                    (e as Exception).isCritical
                )
            )
        }.collect {
            if(it != null) {
                _authEvents.value = it
                eventsStateIntent.setState(
                    EventListState.OnData.EventsResult(
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
        val updatedEvent = _authEvents.value!!.find { it.id == (eventFields[Constants.ID]
                ?: error("Event ID must not be null in UpdateAnEventCache Intent!")).toLong() }!!
            .updateFromFieldMap(eventFields)
        saveEvent(updatedEvent)
    }

    private suspend fun saveEvent(event: Event) {
        eventRepository.saveEvent(event)
        _authEvents.value = _authEvents.value!!.replace(event, { it.id == event.id })
    }

    private suspend fun deleteEvent(eventId: Long) {
        eventStateIntent.setState(EventState.Loading)

        eventStateIntent.setState(try {
            val state = EventState.OnData.EventDeleteResult(
                eventRepository.deleteEvent(eventId)
            )

            deleteEventCache(eventId)

            state
        } catch(ex: Exception) {
            ex.printStackTrace()

            EventState.Error(
                ex.message
            )
        })
    }

    // FIXME: Ge. Ne. RIIIIICS. Well, not really but still code dup with other deleteCache methods. Mitigate that
    private suspend fun deleteEventCache(eventId: Long, setState: Boolean = false): Boolean {
        val success = eventRepository.deleteEventCache(eventId)

        if(setState) {
            eventStateIntent.setState(if(success) EventState.OnData.DeleteEventCacheResult
                else EventState.Error(Constants.cacheDeletionError))
        } else if(!success) {
            throw Exception(Constants.cacheDeletionError)
        }

        return true
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