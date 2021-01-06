package com.example.hobbyfi.repositories

import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.responses.TokenResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.PrefConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException

abstract class Repository(protected val prefConfig: PrefConfig, protected val hobbyfiAPI: HobbyfiAPI) {
    class AuthorisedRequestException(message: String? = null) : Exception(message)
    class ReauthenticationException(message: String? = null) : Exception(message)
    class NetworkException(message: String? = null) : Exception(message)
    class UnknownErrorException(message: String? = null) : Exception(message)

    protected suspend fun getNewTokenWithRefresh(): TokenResponse? {
        return try {
            if (prefConfig.readRefreshToken() != "invalid") {
                withContext(Dispatchers.IO) {
                    val token = hobbyfiAPI.fetchNewTokenWithRefresh(prefConfig.readRefreshToken()!!)
                    prefConfig.writeToken(token?.jwt!!)
                    token
                }
            } else throw IllegalStateException()
        } catch(ex: Exception) {
            Callbacks.dissectRepositoryExceptionAndThrow(ex)
        }
    }

    protected suspend fun<T> performAuthorisedRequest(request: suspend () -> T, refreshTokenFallback: suspend () -> T): T = try {
        prefConfig.getAuthUserIdFromToken() // validate token expiry by attempting to get id from token
        request()
    } catch(ex: Exception) {
        try {
            Callbacks.dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
        } catch(authEx: AuthorisedRequestException) {
            getNewTokenWithRefresh()
                // if this ^ throws exception => user reauth; invalid refresh token & can't fetch response
            refreshTokenFallback()
        }
    }
}