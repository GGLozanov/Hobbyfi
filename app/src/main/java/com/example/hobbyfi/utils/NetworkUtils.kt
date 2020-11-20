package com.example.hobbyfi.utils

import android.util.Log
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.TokenResponse
import com.example.hobbyfi.shared.Constants
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException


object NetworkUtils {
    /**
     *
     * @param errorResponse - Retrofit2 response received from server upon API request
     * @param fields - desired fields from which to decode from the JSON body (JSON keys)
     * @return Map<String></String>, Object> result - a fully decoded JSON response if all fields match all keys in the given JSON response
     * @throws JSONException - if there is no matching key in the JSON body for a given field
     * @throws IOException - if the errorBody cannot be converted to a string
     */
    @Throws(JSONException::class, IOException::class)
    fun extractFieldsFromResponseErrorBody(
        errorResponse: HttpException,
        fields: List<String>
    ) : Map<String, Any> {

        val result: MutableMap<String, Any> = HashMap()
        val failedResponseBody =
            JSONObject(errorResponse.response().toString()) // body of request as JSON (used for extraction)
        for (field in fields) {
            result[field] = failedResponseBody[field]
        }
        return result
    }

    /**
     *
     * @param errorResponse - Retrofit2 response received from server upon API request
     * @param responseField - field containing the desired custom API response as a JSON field
     * @return - String. Custom response message from API.
     * @throws JSONException - if there is no matching key in the JSON body for a given field
     * @throws IOException - if the errorBody cannot be converted to a string
     */
    @Throws(JSONException::class, IOException::class)
    fun extractResponseFromResponseErrorBody(
        errorResponse: HttpException,
        responseField: String
    ) : String? {
        Log.w(
            "NetworkError",
            "Extracting response body from response '" + errorResponse.message().toString() + "'"
        )
        return extractFieldsFromResponseErrorBody(
            errorResponse,
            listOf(responseField)
        )[responseField] as String?
    }

    class InvalidTokenException : Exception()

    /**
     *
     * @param errorResponse - Failed response
     * @param refreshToken - Refresh token used for refresh token fetch endpoint
     * @param refreshTokenResponseCallback - Retrofit callback for refresh token endpoint response
     * @param <T> - generic response type
     * @return - void
     * @throws JSONException - if there is no matching key in the JSON body for a given field
     * @throws IOException - if the errorBody cannot be converted to a string
     * @throws NetworkUtils.ResponseSuccessfulException - if the given response is actually successful instead of failed (no errorBody)
     * @throws InvalidTokenException - if the original token is invalid and not expired
    </T> */
    @Throws(
        JSONException::class,
        IOException::class,
        InvalidTokenException::class
    )

    suspend inline fun <T> handleFailedAuthorizedResponse(
        errorResponse: HttpException,
        refreshToken: String?,
        onTokenReceived: (String) -> Unit
    ) {
//        val responseCode: String =
//            NetworkUtils.extractResponseFromResponseErrorBody(errorResponse, Constants.RESPONSE)
//        if (responseCode != "Expired token. Get refresh token.") {
//            Log.e(
//                "Repository",
//                "handleFailedAuthorizedResponse —> Invalid token error message in failed response body or error message is null"
//            )
//            throw InvalidTokenException()
//        }
//        Log.i(
//            "Repository",
//            "handleFailedAuthorizedResponse —> Calling for new token result from refresh token endpoint"
//        )
//        val tokenResult: Call<Token> = apiOperations.refreshToken(
//            refreshToken
//        ) // fetch new token from refresh token
//        tokenResult.enqueue(refreshTokenResponseCallback)

        // TODO: Get new token & use onTokenReceived

        // TODO: Handle failed authorised response with manual callback, etc.
    }

    /**
     *
     * @param response - Token response containing new JWT token
     * @return - New JWT token if response is valid
     */
    fun getTokenFromRefreshResponse(response: TokenResponse?): String? {
//        var token: Token
//        val responseCode: String = response.getResponse()
//        Log.i(
//            "Repository",
//            "getTokenFromRefreshResponse —> Retrieved new token from refresh_token endpoint."
//        )
//        if (responseCode == Constants.SUCCESS_RESPONSE) {
//            Log.i(
//                "Repository",
//                "getTokenFromRefreshResponse —> New token from refresh_token endpoint is valid ('ok' status)."
//            )
//            return token.getJWT()
//        }
//        Log.e(
//            "Repository",
//            "getTokenFromRefreshResponse —> refresh_token endpoint response unsuccessful or body is null. Returning null."
//        )
        return null
    }
}