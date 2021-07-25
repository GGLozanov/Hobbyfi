package com.example.hobbyfi.repositories

import android.content.res.Resources
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.responses.TokenResponse
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.TokenUtils
import com.facebook.AccessToken
import com.google.firebase.FirebaseException
import io.jsonwebtoken.ExpiredJwtException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.lang.IllegalStateException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

abstract class Repository(
    protected val prefConfig: PrefConfig,
    protected val hobbyfiAPI: HobbyfiAPI,
    protected val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    class AuthorisedRequestException(message: String? = null) : Exception(message)
    class ReauthenticationException(message: String? = null) : Exception(message)
    class NetworkException(message: String? = null) : Exception(message)
    class UnknownErrorException(message: String? = null) : Exception(message)

    protected suspend fun getNewTokenWithRefresh(): TokenResponse? {
        return try {
            if (prefConfig.readRefreshToken() != "invalid") {
                withContext(coroutineDispatcher) {
                    val token = hobbyfiAPI.fetchNewTokenWithRefresh(prefConfig.readRefreshToken()!!)
                    prefConfig.writeToken(token?.jwt!!)
                    token
                }
            } else throw IllegalStateException()
        } catch(ex: Exception) {
            dissectExceptionAndThrow(ex)
        }
    }

    protected suspend fun<T> performAuthorisedRequest(request: suspend () -> T, refreshTokenFallback: suspend () -> T): T = try {
        prefConfig.getAuthUserIdFromToken() // validate token expiry by attempting to get id from token
        request()
    } catch(ex: Exception) {
        try {
            dissectExceptionAndThrow(ex, isAuthorisedRequest = true)
        } catch(authEx: AuthorisedRequestException) {
            getNewTokenWithRefresh()
                // if this ^ throws exception => user reauth; invalid refresh token & can't fetch response
            refreshTokenFallback()
        }
    }

    companion object {
        // always throws an exception
        fun dissectExceptionAndThrow(ex: Exception, isAuthorisedRequest: Boolean = false): Nothing {
            ex.printStackTrace()
            val res = MainApplication.applicationContext.resources
            when(ex) {
                is HobbyfiAPI.NoConnectivityException, is FirebaseException ->
                    throw Exception(res.getString(R.string.no_connection_error))
                is HttpException -> {
                    when (ex.code()) {
                        400 -> { // bad request (missing data)
                            throw Exception(res.getString(R.string.missing_data_error))
                        }
                        401 -> { // unauthorized
                            throw if (!isAuthorisedRequest) Exception(res.getString(R.string.invalid_token))
                            else if (AccessToken.getCurrentAccessToken() != null)
                                ReauthenticationException(res.getString(R.string.reauth_error))
                            else AuthorisedRequestException(res.getString(R.string.unathorised_access))
                        }
                        404 -> { // not found
                            throw ReauthenticationException(res.getString(R.string.resource_not_found_error))
                        }
                        406 -> { // not acceptable
                            throw Exception(res.getString(R.string.invalid_data))
                        }
                        409 -> { // conflict
                            throw Exception(res.getString(R.string.resource_exists_error)) // FIXME: Generify response for future endpoints with "exist" as response, idfk
                        }
                        429 -> { // too many
                            throw Exception(res.getString(R.string.limit_reacher_error))
                        }
                        500 -> { // server error
                            throw ReauthenticationException(res.getString(R.string.internal_server_error))
                        }
                    }

                    throw NetworkException(ex.message().toString() + "; code: " + ex.code())
                }
                is ExpiredJwtException -> {
                    throw if (isAuthorisedRequest) AuthorisedRequestException()
                    else ReauthenticationException(res.getString(R.string.expired_token_error))
                }
                is ReauthenticationException,
                TokenUtils.InvalidStoredTokenException(), is InstantiationException, is CancellationException -> throw ex
                else -> throw if(ex is SocketTimeoutException)
                    ReauthenticationException(res.getString(R.string.server_connection_error))
                else UnknownErrorException(res.getString(R.string.something_wrong) + " $ex.message")
            }
        }
    }
}