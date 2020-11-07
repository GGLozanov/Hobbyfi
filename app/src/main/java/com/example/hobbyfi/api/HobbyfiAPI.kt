package com.example.hobbyfi.api

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.adapters.chatroom.ChatroomResponseTypeAdapter
import com.example.hobbyfi.adapters.message.MessageResponseTypeAdapter
import com.example.hobbyfi.adapters.user.UserResponseTypeAdapter
import com.example.hobbyfi.responses.ChatroomResponse
import com.example.hobbyfi.responses.UserResponse
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

interface HobbyfiAPI {

    companion object {
        @RequiresApi(Build.VERSION_CODES.N)
        operator fun invoke(connectivityManager: ConnectivityManager): HobbyfiAPI {
            val requestInterceptor = Interceptor {
                val activeNetworkInfo = connectivityManager.activeNetwork

                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetworkInfo)

                if(!(networkCapabilities != null &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))) {
                    throw NoConnectivityException()
                }

                val headers = it.request().headers()
                    .newBuilder()
                    .add("accept", "application/json")
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
                .addConverterFactory(GsonConverterFactory.create(
                    GsonBuilder()
                        .serializeNulls()
                        .registerTypeAdapter(UserResponse::class.java, UserResponseTypeAdapter())
                        .registerTypeAdapter(ChatroomResponse::class.java, ChatroomResponseTypeAdapter())
                        .registerTypeAdapter(MessageResponseTypeAdapter::class.java, MessageResponseTypeAdapter())
                        .create()
                ))
                .build()
                .create(HobbyfiAPI::class.java)
        }

        class NoConnectivityException : IOException()
    }
}