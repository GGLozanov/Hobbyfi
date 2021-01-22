package com.example.hobbyfi.api

import android.net.ConnectivityManager
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.adapters.chatroom.ChatroomResponseDeserializer
import com.example.hobbyfi.adapters.message.MessageResponseDeserializer
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.adapters.user.UserResponseDeserializer
import com.example.hobbyfi.models.*
import com.example.hobbyfi.models.Tag
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


interface HobbyfiAPI {

    /**
     * FormUrlEncoded POST request to create a new User resource with given credentials
     * @param email - auth user's email address
     * @param username - auth user's username
     * @param password - auth user's password (hashed server-side)
     * @param description - auth user's description
     * @return - a Token model containing a JWT and refresh JWT on success and a failed response from the server on failure
     */
    @POST("api/v1.0/user/create")
    @FormUrlEncoded
    suspend fun fetchRegistrationToken(
        @Header(Constants.AUTH_HEADER) facebookToken: String?, // potential fb token sent to validate fb create request
        @Field(Constants.EMAIL) email: String?,
        @Field(Constants.USERNAME) username: String,
        @Field(Constants.PASSWORD) password: String?,
        @Field(Constants.DESCRIPTION) description: String?,
        @Field(Constants.IMAGE) image: String?,
        @Field(Constants.TAGS + "[]") tags: String?
    ): TokenResponse?

    /**
     * GET request to check if a user already exists on the back-end
     * @param username - a given username to check by (usernames are ALWAYS unique)
     */
    @GET("api/v1.0/user/exists")
    suspend fun fetchUserExists(
        @Query(Constants.ID) id: Long
    ): Boolean

    /**
     * GET request to retrieve a new JWT upon requested login
     * @param email - auth user's email
     * @param password - auth user's password
     * @return - a Token model containing a JWT and refresh JWT on success and a failed response from the server on failure
     */
    @GET("api/v1.0/user/authenticate")
    suspend fun fetchLoginToken(
        @Query(Constants.EMAIL) email: String,
        @Query(Constants.PASSWORD) password: String
    ): TokenResponse?

    /**
     * GET request to retrieve a new token using the refresh token
     * @param refreshJWT - refresh JWT with a long expiry date used to retrieve new user JWTs
     * @return - a Token model containing a new JWT on success and a failed response from the server on failure
     */
    @GET("api/v1.0/user/refresh_token")
    suspend fun fetchNewTokenWithRefresh(
        @Header(Constants.AUTH_HEADER) refreshJWT: String
    ): TokenResponse?

    /**
     * GET request to retrieve a user's/users' info from the server
     * @param token - JWT for the given auth user used to validate requests to secure endpoints
     * @return - a User model containing all the necessary information and the appropriate response from server
     */
    @GET("api/v1.0/user/read")
    suspend fun fetchUser(
        @Header(Constants.AUTH_HEADER) token: String // id inside token for DB query; token inside auth header
    ): CacheResponse<User>?

    /**
     *
     */
    @GET("api/v1.0/users/read")
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
    @POST("api/v1.0/user/edit") // should semantically be PATCH but w/e (for now)
    @FormUrlEncoded
    suspend fun editUser(
        @Header(Constants.AUTH_HEADER) token: String,
        @FieldMap body: Map<String?, String?> // variable body parameters (which is why a map is used)
    ): Response?

    /**
     * DELETE request to, well, delete a user
     * @param token - JWT for the given auth user used to validate requests to secure endpoints (contains auth user's id)
     */
    @DELETE("api/v1.0/user/delete")
    suspend fun deleteUser(
        @Header(Constants.AUTH_HEADER) token: String
    ): Response?

    /**
     *
     */
    @POST("api/v1.0/chatroom/create")
    @FormUrlEncoded
    suspend fun createChatroom(
        @Header(Constants.AUTH_HEADER) token: String,
        @Field(Constants.NAME) name: String,
        @Field(Constants.DESCRIPTION) description: String?,
        @Field(Constants.IMAGE) image: String?,
        @Field(Constants.TAGS + "[]") tags: List<Tag>?
    ): IdResponse?

    /**
     *
     */
    @POST("api/v1.0/chatroom/edit")
    @FormUrlEncoded
    suspend fun editChatroom(
        @Header(Constants.AUTH_HEADER) token: String,
        @FieldMap body: Map<String?, String?>
    ): Response?

    /**
     *
     */
    @DELETE("api/v1.0/chatroom/delete")
    suspend fun deleteChatroom(
        @Header(Constants.AUTH_HEADER) token: String
    ): Response?

    /**
     *
     */
    @GET("api/v1.0/chatroom/read")
    suspend fun fetchChatroom(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.ID) chatroomId: Long
    ): CacheResponse<Chatroom>?

    /**
     *
     */
    @GET("api/v1.0/chatrooms/read")
    suspend fun fetchChatrooms(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.PAGE) page: Int,
    ): CacheListResponse<Chatroom>


    /**
     *
     * This is a separate request because pagination in all chatrooms read request doesn't guarantee return of joined chatrooms on initial page
     */
    @GET("api/v1.0/chatrooms/read_own")
    suspend fun fetchAuthChatrooms(
        @Header(Constants.AUTH_HEADER) token: String?,
        @Query(Constants.PAGE) page: Int
    ): CacheListResponse<Chatroom>

    /**
     *
     */
    @POST("api/v1.0/message/create")
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
    @GET("api/v1.0/messages/read")
    suspend fun fetchMessages(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.CHATROOM_ID) chatroomId: Long,
        @Query(Constants.PAGE) page: Int
    ): CacheListResponse<Message>

    /**
     *
     */
    @POST("api/v1.0/message/edit")
    @FormUrlEncoded
    suspend fun editMessage(
        @Header(Constants.AUTH_HEADER) token: String,
        @FieldMap body: Map<String?, String?> // ALWAYS takes Id
    ): Response?

    @DELETE("api/v1.0/message/delete")
    suspend fun deleteMessage(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.ID) id: Long
    ): Response?

    // TODO: Modify to CacheResponse<List<Event>> when backend supports one-to-many chatroom and event connection
    @GET("api/v1.0/events/read")
    suspend fun fetchEvent(
        @Header(Constants.AUTH_HEADER) token: String
    ): CacheListResponse<Event>

    /**
     *
     */
    @POST("api/v1.0/event/create")
    @FormUrlEncoded
    suspend fun createEvent(
        @Header(Constants.AUTH_HEADER) token: String,
        @Field(Constants.NAME) name: String,
        @Field(Constants.DESCRIPTION) description: String?,
        @Field(Constants.DATE) date: String,
        @Field(Constants.IMAGE) image: String?,
        @Field(Constants.LATITUDE) lat: Double,
        @Field(Constants.LONGITUDE) long: Double
    ): StartDateIdResponse?

    @POST("api/v1.0/event/edit")
    @FormUrlEncoded
    suspend fun editEvent(
        @Header(Constants.AUTH_HEADER) token: String,
        @FieldMap body: Map<String?, String?>
    ): Response?

    /**
     *
     */
    @DELETE("api/v1.0/event/delete")
    suspend fun deleteEvent(
        @Header(Constants.AUTH_HEADER) token: String,
        @Query(Constants.ID) eventId: Long
    ): Response?

    /**
     *
     */
    @DELETE("api/v1.0/event/delete_old")
    suspend fun deleteOldEvents(
        @Header(Constants.AUTH_HEADER) token: String
    ): CacheListResponse<Long>?

    companion object {
        operator fun invoke(connectivityManager: ConnectivityManager): HobbyfiAPI {
            val requestInterceptor = Interceptor {
                if(!connectivityManager.isConnected()) {
                    throw NoConnectivityException()
                }

                val headers = it.request().headers
                    .newBuilder()
                    .add("Content-Type", if(it.request().method == "POST") "application/x-www-form-urlencoded"
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
            }

            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client: OkHttpClient = OkHttpClient()
                .newBuilder()
                .addInterceptor(requestInterceptor)
                .addInterceptor(loggingInterceptor)
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