package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.*
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isCritical
import com.example.hobbyfi.shared.replace
import com.example.hobbyfi.state.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance
import java.lang.IllegalStateException
import kotlin.properties.Delegates

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(
    application: Application,
    user: User?,
    chatroom: Chatroom?
) : AuthChatroomHolderViewModel(application, user, chatroom) {
    private val eventRepository: EventRepository by instance(tag = "eventRepository")

    private var _currentAdapterUsers: MutableLiveData<List<User>> = MutableLiveData(emptyList())
    val currentAdapterUsers: LiveData<List<User>> get() = _currentAdapterUsers

    private var _authEvents: MutableLiveData<List<Event>> = MutableLiveData()
    val authEvents: LiveData<List<Event>> get() = _authEvents

    private var _authUserGeoPoint: StateFlow<UserGeoPoint?> by Delegates.notNull()
    val authUserGeoPoint: StateFlow<UserGeoPoint?> get() = _authUserGeoPoint

    fun setAuthEvents(events: List<Event>) {
        _authEvents.value = events
    }

    private val eventsStateIntent: StateIntent<EventListState, EventListIntent> = object : StateIntent<EventListState, EventListIntent>() {
        override val _state: MutableStateFlow<EventListState> = MutableStateFlow(EventListState.Idle)
    }
    val eventsState get() = eventsStateIntent.state

    suspend fun sendEventsIntent(intent: EventListIntent) = eventsStateIntent.sendIntent(intent)
    
    private val usersStateIntent: StateIntent<UserListState, UserListIntent> = object : StateIntent<UserListState, UserListIntent>() {
        override val _state: MutableStateFlow<UserListState> = MutableStateFlow(UserListState.Idle)
    }
    val usersState get() = usersStateIntent.state

    suspend fun sendUsersIntent(intent: UserListIntent) = usersStateIntent.sendIntent(intent)

    private val userGeoPointStateIntent: StateIntent<UserGeoPointState, UserGeoPointIntent> = object : StateIntent<UserGeoPointState, UserGeoPointIntent>() {
        override val _state: MutableStateFlow<UserGeoPointState> = MutableStateFlow(UserGeoPointState.Idle)
    }
    val userGeoPointState get() = userGeoPointStateIntent.state

    suspend fun sendUserGeoPointIntent(intent: UserGeoPointIntent) = userGeoPointStateIntent.sendIntent(intent)

    fun resetUserListState() = usersStateIntent.setState(UserListState.Idle)

    fun resetChatroomState() = chatroomStateIntent.setState(ChatroomState.Idle)

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            eventsStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is EventListIntent.AddAnEventCache -> {
                        Log.i("ChatroomActivityVM", "Add Event to List Intent caught")
                        saveEvent(it.event)
                        setAuthEvents((_authEvents.value ?: emptyList()) + it.event)
                        updateAndSaveChatroom(mapOf(Pair(Constants.EVENT_IDS, Constants.tagJsonConverter
                            .toJson(authChatroom.value!!.eventIds?.plus(it.event.id)))))
                    }
                    is EventListIntent.DeleteAnEventCache -> {
                        if(eventRepository.deleteEventCache(it.eventId)) {
                            setAuthEvents(_authEvents.value!!.filter { event -> event.id != it.eventId })
                            updateAndSaveChatroom(mapOf(Pair(Constants.EVENT_IDS, Constants.tagJsonConverter
                                .toJson(authChatroom.value!!.eventIds?.filter { eventId -> eventId != it.eventId }))))
                        }
                    }
                    is EventListIntent.DeleteOldEventsCache -> {
                        // TODO: wire up this with delete_old events in repo and API
                        deleteOldEvents()
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
        viewModelScope.launch {
            userGeoPointStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is UserGeoPointIntent.FetchAuthUserGeoPoint -> {
                        fetchAuthUserGeoPoint()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    init {
        handleIntent()
    }

    private fun fetchAuthUserGeoPoint() {
        userGeoPointStateIntent.setState(UserGeoPointState.Loading)

        userGeoPointStateIntent.setState(try {
            _authUserGeoPoint = eventRepository.getEventUserGeoPoint(authUser.value!!.name)

            UserGeoPointState.OnData.OnAuthUserGeoPointResult(
                _authUserGeoPoint
            ) // TODO: Do something with state (collect it, at least)
        } catch(ex: Exception) {
            ex.printStackTrace()
            UserGeoPointState.Error(
                ex.message, shouldReauth = ex.isCritical
            )
        })
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

        eventRepository.getEvents(_authChatroom.value!!.id).catch { e ->
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

    private suspend fun updateAndSaveEvent(eventFields: Map<String?, String?>) {
        val updatedEvent = _authEvents.value!!.find { it.id == (eventFields[Constants.ID]
                ?: error("Event ID must not be null in UpdateAnEventCache Intent!")).toLong() }!!
            .updateFromFieldMap(eventFields)
        saveEvent(updatedEvent)
        _authEvents.value = _authEvents.value!!.replace(updatedEvent, { it.id == updatedEvent.id })
    }

    private suspend fun saveEvent(event: Event) {
        eventRepository.saveEvent(event)
    }

    private suspend fun deleteOldEvents() {
        eventsStateIntent.setState(EventListState.Loading)

        eventsStateIntent.setState(try {
            val response = eventRepository.deleteOldEvents()
                ?: throw IllegalStateException(Constants.invalidStateError)

            eventRepository.deleteEventsCache(response.modelList)
            updateAndSaveChatroom(mapOf(
                Pair(Constants.EVENT_IDS,
                    Constants.tagJsonConverter.toJson(authChatroom.value!!.eventIds?.filter { !response.modelList.contains(it) }))
            ))

            EventListState.OnData.DeleteOldEventsResult(response.modelList)
        } catch(ex: Exception) {
            ex.printStackTrace()
            EventListState.Error(
                ex.message,
                shouldReauth = ex.isCritical
            )
        })
    }

    private suspend fun deleteUserCache(id: Long, shouldWritePrefTime: Boolean = true) {
        // TODO: Add setState bool?
        userRepository.deleteUserCache(id, shouldWritePrefTime)
    }

    private fun setCurrentUsers(users: List<User>) {
        _currentAdapterUsers.value = users
    }
}