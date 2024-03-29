package com.example.hobbyfi.api

import android.net.ConnectivityManager
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.adapters.chatroom.ChatroomResponseDeserializer
import com.example.hobbyfi.adapters.message.MessageResponseDeserializer
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.adapters.user.UserResponseDeserializer
import com.example.hobbyfi.models.*
import com.example.hobbyfi.models.data.*
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.requests.FetchRegisterTokenRequest
import com.example.hobbyfi.responses.*
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.isConnected
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.TimeUnit


const val API_VERSION = "1.0"

interface HobbyfiAPI {

    /**
     * POST request to create a new User resource with given credentials
     * @param email - auth user's email address
     * @param username - auth user's username
     * @param password - auth user's password (hashed server-side)
     * @param description - auth user's description
     * @return - a Token model containing a JWT and refresh JWT on success and a failed response from the server on failure
     */
    @POST("v${API_VERSION}/user/create.php")
    suspend fun fetchRegistrationToken(
        @Header(Constants.AUTH_HEADER) facebookToken: String?, // potential fb token sent to validate fb create request
        @Body request: FetchRegisterTokenRequest
    ): TokenResponse?

    /**
     * GET request to check if a user already exists on the back-end
     * @param username - a given username to check by (usernames are ALWAYS unique)
     */
    @GET("v${API_VERSION}/user/exists.php")
    suspend fun fetchUserExists(
        @Query(Constants.ID) id: Long
    ): Boolean

    /**
     * GET request to retrieve a new JWT upon requested login
     * @param email - auth user's email
     * @param password - auth user's password
     * @return - a Token model containing a JWT and refresh JWT on success and a failed response from the server on failure
     */
    @GET("v${API_VERSION}/user/authenticate.php")
    suspend fun fetchLoginToken(
        @Query(Constants.EMAIL) email: String,
        @Query(Constants.PASSWORD) password: String
    ): TokenResponse?

    /**
     * GET request to retrieve a new token using the refresh token
     * @param refreshJWT - refresh JWT with a long expiry date used to retrieve new user JWTs
     * @return - a Token model containing a new JWT on success and a failed response from the server on failure
     */
    @GET("v${API_VERSION}/user/refresh_token.php")
    suspend fun fetchNewTokenWithRefresh(
        @Header(Constants.AUTH_HEADER) refreshJWT: String
    ): TokenResponse?

    /**
     *
     */
    @GET("v${API_VERSION}/user/reset_password.php")
    suspend fun resetPassword(
        @Query(Constants.EMAIL) email: String
    ): Response?

    /**
     * GET request to retrieve a user's/users' info from the server
     * @param token - JWT for the given auth user used to validate requests to secure endpoints
     * @return - a User model containing all the necessary information and the appropriate response from server
     */
    @GET("v${API_VERSION}/user/read.php")
    suspend fun fetchUser(
        @Header(Constants.AUTH_HEADER) token: String // id inside token for DB query; token inside auth header
    ): CacheResponse<User>?

    /**
     *
     */
    @GET("v${API_VERSION}/users/read.php")
    suspend fun fetchUsers(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.CHATROOM_ID) chatroomId: Long
    ): CacheListResponse<User>?

    /**
     * POST request to update a given user with the params specified in the body
     * @param token - JWT for the given auth user used to validate requests to secure endpoints (contains auth user's id)
     * @param body - POST body fields containing key-value pairs on fields to be updated in the backend
     * @return - a Model containing a response from the server based on success or failure
     */
    @POST("v${API_VERSION}/user/edit.php") // should semantically be PATCH but w/e (for now)
    @FormUrlEncoded
    suspend fun editUser(
        @Header(Constants.AUTH_HEADER) token: String,
        @FieldMap body: Map<String, String?> // variable body parameters (which is why a map is used)
    ): Response?

    /**
     * DELETE request to, well, delete a user
     * @param token - JWT for the given auth user used to validate requests to secure endpoints (contains auth user's id)
     */
    @DELETE("v${API_VERSION}/user/delete.php")
    suspend fun deleteUser(
        @Header(Constants.AUTH_HEADER) token: String
    ): Response?

    /**
     *
     */
    @POST("v${API_VERSION}/chatroom/create.php")
    @FormUrlEncoded
    suspend fun createChatroom(
        @Header(Constants.AUTH_HEADER) token: String,
        @Field(Constants.NAME) name: String,
        @Field(Constants.DESCRIPTION) description: String?,
        @Field(Constants.TAGS + "[]") tags: String?
    ): IdResponse?

    /**
     *
     */
    @POST("v${API_VERSION}/chatroom/edit.php")
    @FormUrlEncoded
    suspend fun editChatroom(
        @Header(Constants.AUTH_HEADER) token: String,
        @FieldMap body: Map<String, String?>
    ): Response?

    /**
     *
     */
    @DELETE("v${API_VERSION}/chatroom/delete.php")
    suspend fun deleteChatroom(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.ID) chatroomId: Long
    ): Response?

    /**
     *
     */
    @GET("v${API_VERSION}/chatroom/read.php")
    suspend fun fetchChatroom(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.ID) chatroomId: Long
    ): CacheResponse<Chatroom>?

    /**
     *
     */
    @GET("v${API_VERSION}/chatrooms/read.php")
    suspend fun fetchChatrooms(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.PAGE) page: Int,
    ): CacheListResponse<Chatroom>


    /**
     *
     * This is a separate request because pagination in all chatrooms read request doesn't guarantee return of joined chatrooms on initial page
     */
    @GET("v${API_VERSION}/chatrooms/read_own.php")
    suspend fun fetchAuthChatrooms(
        @Header(Constants.AUTH_HEADER) token: String?,
        @Query(Constants.PAGE) page: Int
    ): CacheListResponse<Chatroom>

    @POST("v${API_VERSION}/chatroom/kick.php")
    @FormUrlEncoded
    suspend fun kickUser(
        @Header(Constants.AUTH_HEADER) token: String?,
        @Field(Constants.USER_ID) userId: Long,
        @Field(Constants.ID) chatroomId: Long
    ): Response?

    /**
     *
     */
    @POST("v${API_VERSION}/message/create.php")
    @FormUrlEncoded
    suspend fun createMessage(
        @Header(Constants.AUTH_HEADER) token: String,
        @Field(Constants.CHATROOM_ID) chatroomId: Long,
        @Field(Constants.MESSAGE) message: String?,
        @Field(Constants.IMAGE) imageMessage: String?
    ): CreateTimeIdResponse?

    /**
     *
     */
    @GET("v${API_VERSION}/messages/read.php")
    suspend fun fetchMessages(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.CHATROOM_ID) chatroomId: Long,
        @Query(Constants.PAGE) page: Int,
        @Query(Constants.QUERY) query: String? = null
    ): CacheListResponse<Message>

    /**
     *
     */
    @GET("v${API_VERSION}/messages/page_find.php")
    suspend fun fetchMessagesId(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.CHATROOM_ID) chatroomId: Long,
        @Query(Constants.MESSAGE_ID) messageId: Long
    ): CacheListPageResponse<Message>

    /**
     *
     */
    @POST("v${API_VERSION}/message/edit.php")
    @FormUrlEncoded
    suspend fun editMessage(
        @Header(Constants.AUTH_HEADER) token: String,
        @FieldMap body: Map<String, String?> // ALWAYS takes Id
    ): Response?


    @DELETE("v${API_VERSION}/message/delete.php")
    suspend fun deleteMessage(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.ID) id: Long
    ): Response?

    @GET("v${API_VERSION}/event/read.php")
    suspend fun fetchEvent(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.ID) id: Long
    ): CacheResponse<Event>

    @GET("v${API_VERSION}/events/read.php")
    suspend fun fetchEvents(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.CHATROOM_ID) chatroomId: Long
    ): CacheListResponse<Event>

    /**
     *
     */
    @POST("v${API_VERSION}/event/create.php")
    @FormUrlEncoded
    suspend fun createEvent(
        @Header(Constants.AUTH_HEADER) token: String,
        @Field(Constants.CHATROOM_ID) chatroomId: Long,
        @Field(Constants.NAME) name: String,
        @Field(Constants.DESCRIPTION) description: String?,
        @Field(Constants.DATE) date: String,
        @Field(Constants.LATITUDE) lat: Double,
        @Field(Constants.LONGITUDE) long: Double
    ): StartDateIdResponse?

    @POST("v${API_VERSION}/event/edit.php")
    @FormUrlEncoded
    suspend fun editEvent(
        @Header(Constants.AUTH_HEADER) token: String,
        @FieldMap body: Map<String, String?>
    ): Response?

    /**
     *
     */
    @DELETE("v${API_VERSION}/event/delete.php")
    suspend fun deleteEvent(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.ID) eventId: Long
    ): Response?

    /**
     *
     */
    @DELETE("v${API_VERSION}/event/delete_old.php")
    suspend fun deleteOldEvents(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.CHATROOM_ID) chatroomId: Long,
    ): CacheListResponse<Long>?

    /**
     *
     */
    @POST("v${API_VERSION}/token/fcm.php")
    @FormUrlEncoded
    suspend fun sendDeviceToken(
        @Header(Constants.AUTH_HEADER) token: String,
        @Field(Constants.TOKEN) deviceToken: String
    ): Response?

    /**
     *
     */
    @DELETE("v${API_VERSION}/token/fcm.php")
    suspend fun deleteDeviceToken(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.TOKEN) deviceToken: String
    ): Response?

    @POST("v${API_VERSION}/user/toggle_push.php")
    @FormUrlEncoded
    suspend fun togglePushNotificationAllowForChatroom(
        @Header(Constants.AUTH_HEADER) token: String,
        @Field(Constants.CHATROOM_ID) chatroomId: Long,
        @Field(Constants.TOGGLE) toggle: Int
    ): Response?

    @POST("v${API_VERSION}/image/upload.php")
    @FormUrlEncoded
    suspend fun uploadImage(
        @Header(Constants.AUTH_HEADER) token: String,
        @Field(Constants.ID) modelId: Long,
        @Field(Constants.IMAGE) image: String,
        @Field(Constants.TYPE) type: String,
        @Field(Constants.CHATROOM_ID) chatroomId: Long?,
    ): Response?

    companion object {
        operator fun invoke(connectivityManager: ConnectivityManager): HobbyfiAPI {
            val requestInterceptor = Interceptor {
                try {
                    if(!connectivityManager.isConnected()) {
                        throw NoConnectivityException()
                    }

                    val headers = it.request().headers
                        .newBuilder()
                        .add("Content-Type", if(
                            it.request().method == "POST" &&
                            !it.request().url.toString().contains("/user/create.php") // exclusion because endpoint is retarded
                        ) "application/x-www-form-urlencoded"
                        else
                            "application/json")
                        .add("Accept", "*/*")
                        .add("Connection", "keep-alive")
                        .build()

                    val request = it.request()
                        .newBuilder()
                        .headers(headers)
                        .build()

                    return@Interceptor it.proceed(request)
                } catch(ex: ConnectException) {
                    throw NoConnectivityException()
                }
            }

            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client: OkHttpClient = OkHttpClient()
                .newBuilder()
                .addInterceptor(requestInterceptor)
                .addInterceptor(loggingInterceptor)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            .serializeNulls()
                            .setLenient()
                            .registerTypeAdapter(
                                TypeToken.getParameterized(
                                    CacheResponse::class.java,
                                    User::class.java
                                ).type,
                                UserResponseDeserializer()
                            )
                            .registerTypeAdapter(
                                TypeToken.getParameterized(
                                    CacheResponse::class.java,
                                    Chatroom::class.java
                                ).type,
                                ChatroomResponseDeserializer()
                            )
                            .registerTypeAdapter(
                                TypeToken.getParameterized(
                                    CacheResponse::class.java,
                                    Message::class.java
                                ).type,
                                MessageResponseDeserializer()
                            )
                            .registerTypeAdapter(
                                Tag::class.java,
                                TagTypeAdapter()
                            )
                            .create()
                    )
                )
                .build()
                .create(HobbyfiAPI::class.java)
        }
    }

    class NoConnectivityException : IOException()
}