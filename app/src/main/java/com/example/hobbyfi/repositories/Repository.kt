package com.example.hobbyfi.repositories

import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.responses.TokenResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.TokenUtils
import com.facebook.AccessToken
import com.google.firebase.FirebaseException
import io.jsonwebtoken.ExpiredJwtException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.lang.IllegalStateException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

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
            dissectRepositoryExceptionAndThrow(ex)
        }
    }

    protected suspend fun<T> performAuthorisedRequest(request: suspend () -> T, refreshTokenFallback: suspend () -> T): T = try {
        prefConfig.getAuthUserIdFromToken() // validate token expiry by attempting to get id from token
        request()
    } catch(ex: Exception) {
        try {
            dissectRepositoryExceptionAndThrow(ex, isAuthorisedRequest = true)
        } catch(authEx: AuthorisedRequestException) {
            getNewTokenWithRefresh()
                // if this ^ throws exception => user reauth; invalid refresh token & can't fetch response
            refreshTokenFallback()
        }
    }

    companion object {
        // always throws an exception
        fun dissectRepositoryExceptionAndThrow(ex: Exception, isAuthorisedRequest: Boolean = false): Nothing {
            ex.printStackTrace()
            when(ex) {
                is HobbyfiAPI.NoConnectivityException, is FirebaseException -> throw Exception(Constants.noConnectionError)
                is HttpException -> {
                    when (ex.code()) {
                        400 -> { // bad request (missing data)
                            throw Exception(Constants.missingDataError)
                        }
                        401 -> { // unauthorized
                            throw if (!isAuthorisedRequest) Exception(Constants.invalidTokenError)
                            else if (AccessToken.getCurrentAccessToken() != null) ReauthenticationException(Constants.reauthError)
                            else AuthorisedRequestException(Constants.unauthorisedAccessError)
                        }
                        404 -> { // not found
                            throw ReauthenticationException(Constants.resourceNotFoundError)
                        }
                        406 -> { // not acceptable
                            throw Exception(Constants.invalidDataError)
                        }
                        409 -> { // conflict
                            throw Exception(Constants.resourceExistsError) // FIXME: Generify response for future endpoints with "exist" as response, idfk
                        }
                        429 -> { // too many
                            throw Exception(Constants.limitReachedError)
                        }
                        500 -> { // server error
                            throw ReauthenticationException(Constants.internalServerError)
                        }
                    }

                    throw NetworkException(ex.message().toString() + "; code: " + ex.code())
                }
                is ExpiredJwtException -> {
                    throw if (isAuthorisedRequest) AuthorisedRequestException()
                    else ReauthenticationException(Constants.expiredTokenError)
                }
                is ReauthenticationException,
                TokenUtils.InvalidStoredTokenException, is InstantiationException, is CancellationException -> throw ex
                else -> throw if(ex is SocketTimeoutException)
                    ReauthenticationException(Constants.serverConnectionError)
                else UnknownErrorException(Constants.unknownError(ex.message))
            }
        }
    }
}