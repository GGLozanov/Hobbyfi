package com.example.hobbyfi.repositories

import android.content.res.Resources
import android.util.Log
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.TokenResponse
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.addFetchRegisterTokenRequest
import com.example.hobbyfi.utils.ColourUtils
import com.facebook.AccessToken
import com.facebook.GraphRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MultipartBody
import org.json.JSONException

class TokenRepository(
    prefConfig: PrefConfig,
    hobbyfiAPI: HobbyfiAPI,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : Repository(prefConfig, hobbyfiAPI, coroutineDispatcher) {
    suspend fun getRegisterToken(
        facebookToken: String?,
            email: String?, password: String?, username: String, description: String?,
            tags: List<Tag>): TokenResponse? {
        Log.i("TokenRepository", "getRegisterToken -> getting user w/ email:"
                + email + "; username:" + username + "; description: " + description + "; tags: " + tags + "\n register token")

        return try {
            hobbyfiAPI.fetchRegistrationToken(
                facebookToken,
                FormBody.Builder()
                    .addFetchRegisterTokenRequest(
                        email, password, username,
                        description, tags
                    )
                    .build()
            )
        } catch(ex: Exception) {
            dissectExceptionAndThrow(ex)
        }
    }

    suspend fun getLoginToken(email: String, password: String): TokenResponse? {
        Log.i("TokenRepository", "getLoginToken -> getting user w/ email:"
                + email + "\n login token")

        return try {
            hobbyfiAPI.fetchLoginToken(
                email,
                password
            )
        } catch(ex: Exception) {
            dissectExceptionAndThrow(ex)
        }
    }

    suspend fun getUserExistence(id: Long): Boolean = hobbyfiAPI.fetchUserExists(id)

    suspend fun getFacebookUserEmail(): String? {
        Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user email")

        return withContext(coroutineDispatcher) { // need context due to FB's network calls having something like retrofit's coroutine threading support
            val response = GraphRequest.executeAndWait(
                GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "me?fields=email"
                )
            )

            try {
                response.jsonObject.getString(Constants.EMAIL)
            } catch(ex: JSONException) {
                throw Exception(MainApplication.applicationContext.resources.getString(R.string.facebook_email_fail_error))
            }
        }
    }

    suspend fun getFacebookUserPageTitlesAsTags(): List<Tag> {
        Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user page titles as tags")

        return withContext(coroutineDispatcher) {
            val response = GraphRequest.executeAndWait(
                GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "me/likes"
                )
            )

            try {
                val pages = response.jsonObject.getJSONArray("data")
                val tags = mutableListOf<Tag>()

                for(i in 0..3) {
                    if(!pages.isNull(i)) {
                        tags.add(i, Tag(
                            pages.getJSONObject(i)
                                .getString("name"),
                            ColourUtils.getRandomHex(),
                            true
                        )
                        )
                    }
                }

                tags
            } catch(ex: JSONException) {
                throw Exception(MainApplication.applicationContext.resources.getString(R.string.facebook_tag_fetch_error))
            }
        }
    }

    suspend fun resetPassword(email: String): Response? {
        Log.i("TokenRepository", "resetPassword -> resetting user password w/ email:"
                + email + "\n login token")
        return try {
            hobbyfiAPI.resetPassword(email)
        } catch(ex: Exception) {
            dissectExceptionAndThrow(ex)
        }
    }
}