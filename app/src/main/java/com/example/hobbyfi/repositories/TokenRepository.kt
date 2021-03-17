package com.example.hobbyfi.repositories

import android.util.Log
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.TokenResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.ColourUtils
import com.facebook.AccessToken
import com.facebook.GraphRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

class TokenRepository(prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI) : Repository(prefConfig, hobbyfiAPI) {
    suspend fun getRegisterToken(
        facebookToken: String?,
            email: String?, password: String?, username: String, description: String?,
            base64Image: String?, tags: List<Tag>): TokenResponse? {
        Log.i("TokenRepository", "getRegisterToken -> getting user w/ email:"
                + email + "; username:" + username + "; description: " + description + "; image: " + base64Image + "; tags: " + tags + "\n register token")

        return try {
            hobbyfiAPI.fetchRegistrationToken(
                facebookToken,
                email,
                username,
                password,
                description,
                base64Image,
                if(tags.isEmpty()) null else Constants.jsonConverter.toJson(tags)
            )
        } catch(ex: Exception) {
            Callbacks.dissectRepositoryExceptionAndThrow(ex)
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
            Callbacks.dissectRepositoryExceptionAndThrow(ex)
        }
    }

    suspend fun getUserExistence(id: Long): Boolean = hobbyfiAPI.fetchUserExists(id)

    suspend fun getFacebookUserEmail(): String? {
        Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user email")

        return withContext(Dispatchers.IO) { // need context due to FB's network calls having something like retrofit's coroutine threading support
            val response = GraphRequest.executeAndWait(
                GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "me?fields=email"
                )
            )

            try {
                response.jsonObject.getString(Constants.EMAIL)
            } catch(ex: JSONException) {
                throw Exception(Constants.FACEBOOK_EMAIL_FAILED_EXCEPTION)
            }
        }
    }

    suspend fun getFacebookUserPageTitlesAsTags(): List<Tag> {
        Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user page titles as tags")

        return withContext(Dispatchers.IO) {
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
                throw Exception(Constants.FACEBOOK_TAGS_FAILED_EXCEPTION)
            }
        }
    }

    suspend fun resetPassword(email: String): Response? {
        Log.i("TokenRepository", "resetPassword -> resetting user password w/ email:"
                + email + "\n login token")
        return try {
            hobbyfiAPI.resetPassword(email)
        } catch(ex: Exception) {
            Callbacks.dissectRepositoryExceptionAndThrow(ex)
        }
    }
}