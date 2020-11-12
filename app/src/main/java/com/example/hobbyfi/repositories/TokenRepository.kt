package com.example.hobbyfi.repositories

import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.responses.TokenResponse
import com.example.hobbyfi.shared.PrefConfig
import retrofit2.HttpException

class TokenRepository(prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI) : Repository(prefConfig, hobbyfiAPI) {
    suspend fun getRegisterToken(email: String, password: String, username: String, description: String, tags: List<Tag>?) : TokenResponse? {
        return try {
            hobbyfiAPI.fetchRegistrationToken(
                email,
                password,
                username,
                description,
                tags ?: emptyList()
            )
        } catch(ex: Exception) {
            when(ex) {
                is HobbyfiAPI.NoConnectivityException -> throw Exception("Couldn't register! Please check your connection!")
                is HttpException -> throw Exception(ex.code().toString())
                else -> throw Exception("Unknown error! Please check your connection")
            }
        }
    }

    suspend fun getLoginToken(email: String, password: String) {

    }
}