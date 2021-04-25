
package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.intents.*
import com.example.hobbyfi.models.data.*
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.repositories.MessageRepository
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kodein.di.generic.instance
import java.lang.IllegalStateException

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(
    application: Application,
    user: User?,
    chatroom: Chatroom?
) : AuthChatroomHolderViewModel(application, user, chatroom) {
    private val eventRepository: EventRepository by instance(tag = "eventRepository")
    private val prefConfig: PrefConfig by instance(tag = "prefConfig")

    private var _lastSentJoinChatroomSocketEventId: Long = 0
    val lastSentJoinChatroomSocketEventId get() = _lastSentJoinChatroomSocketEventId
    fun setLastSentJoinChatroomSocketEventId(lastId: Long) {
        _lastSentJoinChatroomSocketEventId = lastId
    }

    private var _shownSocketError: Boolean = false
    val shownSocketError: Boolean get() = _shownSocketError
    fun setShownSocketError(shown: Boolean) {
        _shownSocketError = shown
    }

    private var _chatroomUsers: MutableLiveData<List<User>> = MutableLiveData(arrayListOf())
    val chatroomUsers: LiveData<List<User>> get() = _chatroomUsers

    private var _authEvents: MutableLiveData<List<Event>> = MutableLiveData()
    val authEvents: LiveData<List<Event>> get() = _authEvents

    private var _authUserGeoPoint: StateFlow<UserGeoPoint?> = MutableStateFlow(null)
    val authUserGeoPoint: StateFlow<UserGeoPoint?> get() = _authUserGeoPoint

    fun setAuthEvents(events: List<Event>?) {
        _authEvents.value = events ?: arrayListOf()
    }

    fun setChatroomUsers(users: List<User>?) {
        _chatroomUsers.value = users ?: arrayListOf()
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

    private val eventStateIntent: StateIntent<EventState, EventIntent> = object : StateIntent<EventState, EventIntent>() {
        override val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState.Idle)
    }
    
    val eventState get() = eventStateIntent.state

    suspend fun sendEventIntent(intent: EventIntent) = eventStateIntent.sendIntent(intent)

    suspend fun sendUserGeoPointIntent(intent: UserGeoPointIntent) = userGeoPointStateIntent.sendIntent(intent)

    fun resetUserListState() = usersStateIntent.setState(UserListState.Idle)

    fun resetChatroomState() = chatroomStateIntent.setState(ChatroomState.Idle)

    fun resetEventListState() = eventsStateIntent.setState(EventListState.Idle)

    fun resetEventState() = eventStateIntent.setState(EventState.Idle)

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            eventsStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is EventListIntent.AddAnEventCache -> {
                        Log.i("ChatroomActivityVM", "Add Event to List Intent caught")
                        saveEvent(it.event)
                        setAuthEvents((_authEvents.value ?: arrayListOf()) + it.event)
                        updateAndSaveChatroom(mapOf(Pair(Constants.EVENT_IDS, Constants.jsonConverter
                            .toJson(authChatroom.value!!.eventIds?.plus(it.event.id)))))
                    }
                    is EventListIntent.DeleteAnEventCache -> {
                        eventsStateIntent.setState(EventListState.Idle)
                        if(eventRepository.deleteEventCache(it.eventId)) {
                            setAuthEvents(_authEvents.value!!.filter { event -> event.id != it.eventId })
                            updateAndSaveChatroom(mapOf(Pair(Constants.EVENT_IDS, Constants.jsonConverter
                                .toJson(authChatroom.value!!.eventIds?.filter { eventId -> eventId != it.eventId }))))
                            eventsStateIntent.setState(EventListState.OnData.DeleteAnEventCacheResult(it.eventId))
                        }
                    }
                    is EventListIntent.DeleteOldEvents -> {
                        deleteOldEvents()
                    }
                    is EventListIntent.DeleteEventsCache -> {
                        Log.i("ChatroomActivityVM", "Delete events cache intent received!")
                        deleteOldEventsCache(it.eventIds, true)
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
            eventStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is EventIntent.DeleteEvent -> {
                        viewModelScope.launch {
                            deleteEvent(it.eventId)
                        }
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
                        setCurrentUsers(chatroomUsers.value!! + it.user)
                    }
                    is UserListIntent.DeleteAUserCache -> {
                        deleteUserCache(it.userId, false)
                        setCurrentUsers(chatroomUsers.value!!.filter { user -> user.id != it.userId })
                    }
                    is UserListIntent.UpdateAUserCache -> {
                        // TODO: Update lastUsersFetchTime or something similar for Glide signature caching
                        // TODO: Check if user has image updated? That'd retrigger every image to be refetched on next VH onBind call
                        saveUser(chatroomUsers.value!!.find { user -> user.id ==
                                (it.userUpdateFields[Constants.ID] ?: error("User ID must not be null in saveUser call!")).toLong() }!!
                            .updateFromFieldMap(it.userUpdateFields), false)
                        setCurrentUsers(chatroomUsers.value!!)
                    }
                    is UserListIntent.FetchUsers -> {
                        viewModelScope.launch {
                            fetchUsers()
                        }
                    }
                    is UserListIntent.KickUser -> {
                        kickUser(it.userId)
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

        try {
            userRepository.getChatroomUsers(
                authChatroom.value!!.id,
                prefConfig.getAuthUserIdFromToken()
            ).catch { e ->
                usersStateIntent.setState(
                    UserListState.Error(
                        e.message,
                        e.isCritical
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
        } catch(e: Exception) {
            usersStateIntent.setState(
                UserListState.Error(
                    e.message,
                    e.isCritical
                )
            )
        }
    }

    private suspend fun fetchEvents() {
        eventsStateIntent.setState(EventListState.Loading)

        // run collection on another coroutine to avoid conflicts with intent collection
        // that NEEDS observation but will OVERSHADOW it in the case of an incoming intent
        // this SHOULD be the practice for ALL flow observations in VM, so TODO to avoid future mishaps
        // (there haven't been any as of now but it's good to keep it in mind)
        viewModelScope.launch {
            eventRepository.getEvents(_authChatroom.value!!.id).catch { e ->
                eventsStateIntent.setState(
                    EventListState.Error(
                        e.message,
                        e.isCritical
                    )
                )
            }.distinctUntilChanged().collectLatest {
                Log.i("ChatroomActivityVM", "Events collected: ${it}")
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
    }

    private suspend fun updateAndSaveEvent(eventFields: Map<String, String?>) {
        val updatedEvent = _authEvents.value!!.find { it.id == (eventFields[Constants.ID]
                ?: error("Event ID must not be null in UpdateAnEventCache Intent!")).toLong() }!!
            .updateFromFieldMap(eventFields)
        saveEvent(updatedEvent)
        _authEvents.value = _authEvents.value!!.replaceOrAdd(updatedEvent, { it.id == updatedEvent.id })
    }

    private suspend fun saveEvent(event: Event) {
        eventRepository.saveEvent(event)
    }

    private suspend fun deleteOldEvents() {
        eventsStateIntent.setState(EventListState.Loading)

        eventsStateIntent.setState(try {
            val response = eventRepository.deleteOldEvents(authChatroom.value!!.id)
                ?: throw IllegalStateException(Constants.invalidStateError)

            deleteOldEventsCache(response.modelList, false)

            EventListState.OnData.DeleteOldEventsResult(response.modelList)
        } catch(ex: Exception) {
            ex.printStackTrace()
            EventListState.Error(
                ex.message,
                shouldReauth = ex.isCritical
            )
        })
    }

    private suspend fun deleteEvent(eventId: Long) {
        eventStateIntent.setState(EventState.Loading)

        eventStateIntent.setState(try {
            val state = EventState.OnData.EventDeleteResult(
                eventRepository.deleteEvent(eventId), eventId
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

    private suspend fun deleteUserCache(id: Long, shouldWritePrefTime: Boolean = true) {
        // TODO: Add setState bool?
        userRepository.deleteUserCache(id, shouldWritePrefTime)
    }

    private suspend fun deleteOldEventsCache(eventIds: List<Long>, setState: Boolean) {
        val success = eventRepository.deleteEventsCache(eventIds)
        if(success) {
            updateAndSaveChatroom(mapOf(
                Constants.EVENT_IDS to
                    Constants.jsonConverter.toJson(authChatroom.value!!.eventIds?.filter { !eventIds.contains(it) })
            ))
            setAuthEvents(_authEvents.value!!.filter { event -> !eventIds.contains(event.id) })
        }

        if(setState) {
            eventsStateIntent.setState(if(success)
                EventListState.OnData.DeleteEventsCacheResult(eventIds)
            else EventListState.Error(getApplication<MainApplication>().applicationContext.getString(R.string.cache_deletion_error)))
        }
    }

    // FIXME: Ge. Ne. RIIIIICS. Well, not really but still code dup with other deleteCache methods. Mitigate that.
    // TODO: Also, delete this if not actually needed because fcm sync
    private suspend fun deleteEventCache(eventId: Long, setState: Boolean = false): Boolean {
        val success = eventRepository.deleteEventCache(eventId)

        if(setState) {
            eventStateIntent.setState(if(success) EventState.OnData.DeleteEventCacheResult
                else EventState.Error(getApplication<MainApplication>().applicationContext.getString(
                R.string.cache_deletion_error)))
        } else if(!success) {
            throw Exception(getApplication<MainApplication>().applicationContext.getString(R.string.cache_deletion_error))
        }

        return true
    }

    private suspend fun kickUser(userId: Long) {
        // no loading here (no fetch)
        viewModelScope.launch {
            usersStateIntent.setState(try {
                chatroomRepository.kickUser(userId, authChatroom.value!!.id)
                UserListState.OnUserKick(userId)
            } catch(e: Exception) {
                UserListState.Error(
                    getApplication<MainApplication>().applicationContext.getString(R.string.kick_user_fail),
                    shouldReauth = e.isCritical
                )
            })
        }
    }

    private fun setCurrentUsers(users: List<User>) {
        _chatroomUsers.value = users
    }

    private var _consumedEventDeepLink: Boolean = false
    val consumedEventDeepLink get() = _consumedEventDeepLink

    fun setConsumedEventDeepLink(consumed: Boolean) {
        _consumedEventDeepLink = consumed
    }

    private var _currentLinkProperties: JSONObject? = null
    val currentLinkProperties get() = _currentLinkProperties
    fun setCurrentLinkProperties(linkProps: JSONObject?) {
        _currentLinkProperties = linkProps
    }

    fun forceIsAuthUserOwnerObservation() {
        _isAuthUserChatroomOwner.forceObserve()
    }
}