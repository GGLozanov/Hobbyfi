package com.example.hobbyfi.api

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.adapters.chatroom.ChatroomResponseTypeAdapter
import com.example.hobbyfi.adapters.message.MessageResponseTypeAdapter
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.adapters.user.UserResponseTypeAdapter
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.responses.*
import com.example.hobbyfi.shared.Constants
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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
        @Field(Constants.TAGS + "[]") tags: List<Tag>?
    ): TokenResponse?

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
    suspend fun fetchRefreshToken(
        @Header(Constants.AUTH_HEADER) refreshJWT: String
    ): TokenResponse?

    /**
     * GET request to retrieve a single user and their info from the server
     * @param token - JWT for the given auth user used to validate requests to secure endpoints
     * @return - a User model containing all the necessary information and the appropriate response from server
     */
    @GET("api/v1.0/user/read")
    suspend fun fetchUser(
        @Header(Constants.AUTH_HEADER) token: String // id inside token for DB query; token inside auth header
    ): UserResponse?


    /**
     * GET request to fetch all other users apart from the authenticated (auth'd) one
     * @param token - JWT for the given auth user used to validate requests to secure endpoints (contains auth user's id)
     * @return - a map of "user" string (+ their id - i.e. "user1") and user models (no string response; if something goes awry, blame it on the user's connection)
     */
    @GET("api/v1.0/users/read")
    fun fetchChatroomUsers(
        @Header(Constants.AUTH_HEADER) token: String
    ): Map<String?, UserResponse?>?

    /**
     * POST request to update a given user with the params specified in the body
     * @param token - JWT for the given auth user used to validate requests to secure endpoints (contains auth user's id)
     * @param body - POST body fields containing key-value pairs on fields to be updated in the backend
     * @return - a Model containing a response from the server based on success or failure
     */
    @POST("api/v1.0/user/edit") // should semantically be PATCH but w/e (for now) FIXME potentially in backend
    @FormUrlEncoded
    fun editUser(
        @Header(Constants.AUTH_HEADER) token: String,
        @FieldMap body: Map<String?, String?> // variable body parameters (which is why a map is used)
    ): Response?

    @DELETE("api/v1.0/user/delete")
    fun deleteUser(
        @Header(Constants.AUTH_HEADER) token: String
    ): Response?

    companion object {
        @RequiresApi(Build.VERSION_CODES.N)
        operator fun invoke(connectivityManager: ConnectivityManager): HobbyfiAPI {
            val requestInterceptor = Interceptor {
                val activeNetworkInfo = connectivityManager.activeNetwork

                val networkCapabilities = connectivityManager.getNetworkCapabilities(
                    activeNetworkInfo
                )

                if(!(networkCapabilities != null &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))) {
                    throw NoConnectivityException()
                }

                val headers = it.request().headers()
                    .newBuilder()
                    .add("content-type", "application/json")
                    .build()

                val request = it.request()
                    .newBuilder()
                    .headers(headers)
                    .build()

                return@Interceptor it.proceed(request)
            }

            val client: OkHttpClient = OkHttpClient()
                .newBuilder()
                .addInterceptor(requestInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            .serializeNulls()
                            .registerTypeAdapter(
                                UserResponse::class.java,
                                UserResponseTypeAdapter()
                            )
                            .registerTypeAdapter(
                                ChatroomResponse::class.java,
                                ChatroomResponseTypeAdapter()
                            )
                            .registerTypeAdapter(
                                MessageResponse::class.java,
                                MessageResponseTypeAdapter()
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