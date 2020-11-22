package com.example.hobbyfi.repositories

import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.responses.TokenResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.PrefConfig
import java.lang.IllegalStateException

abstract class Repository(protected val prefConfig: PrefConfig, protected val hobbyfiAPI: HobbyfiAPI) {
    class AuthorisedRequestException : Exception()
    class ReauthenticationException : Exception()

    protected suspend fun getNewTokenWithRefresh(): TokenResponse? {
        return try {
            // TODO: Constants
            if (prefConfig.readToken() != "invalid" && prefConfig.readRefreshToken() != "invalid") {
                val token = hobbyfiAPI.fetchNewTokenWithRefresh(prefConfig.readRefreshToken()!!)
                prefConfig.writeToken(token?.jwt!!)
                token
            } else throw IllegalStateException()
        } catch(ex: Exception) {
            Callbacks.dissectRepositoryExceptionAndThrow(ex)
            null
        }
    }
}