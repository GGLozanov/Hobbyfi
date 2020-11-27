package com.example.hobbyfi.repositories

import android.util.Log
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.responses.TokenResponse
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.ColourUtils
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import retrofit2.HttpException
import java.io.BufferedWriter
import java.io.StringWriter

class TokenRepository(prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI) : Repository(prefConfig, hobbyfiAPI) {
    suspend fun getRegisterToken(
        facebookToken: String?,
            email: String?, password: String?, username: String, description: String?,
            base64Image: String?, tags: List<Tag>) : TokenResponse? {

        return withContext(Dispatchers.IO) {
            Log.i("TokenRepository", "getRegisterToken -> getting user w/ email:"
                    + email + "; username:" + username + "; description: " + description + "; image: " + base64Image + "; tags: " + tags + "\n register token")

            return@withContext try {
                hobbyfiAPI.fetchRegistrationToken(
                    facebookToken,
                    email,
                    username,
                    password,
                    description,
                    base64Image,
                    if(tags.isEmpty()) null else tags
                )
            } catch(ex: Exception) {
                Callbacks.dissectRepositoryExceptionAndThrow(ex)
            }
        }
    }

    suspend fun getLoginToken(email: String, password: String) : TokenResponse? {
        return withContext(Dispatchers.IO) {
            Log.i("TokenRepository", "getLoginToken -> getting user w/ email:"
                    + email + "\n login token")

            return@withContext try {
                hobbyfiAPI.fetchLoginToken(
                    email,
                    password
                )
            } catch(ex: Exception) {
                Callbacks.dissectRepositoryExceptionAndThrow(ex)
            }
        }
    }

    suspend fun getFacebookUserEmail() : String? {
        Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user email")

        val response = GraphRequest.executeAndWait(
            GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "me?fields=email"
            )
        )

        try {
            return response.jsonObject.getString(Constants.EMAIL)
        } catch(ex: JSONException) {
            throw Exception(Constants.FACEBOOK_EMAIL_FAILED_EXCEPTION)
        }
    }

    suspend fun getFacebookUserPageTitlesAsTags() : List<Tag> {
        Log.i("TokenRepository", "getFacebookUserEmail -> getting current facebook user page titles as tags")

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
                    ))
                }
            }

            return tags
        } catch(ex: JSONException) {
            throw Exception(Constants.FACEBOOK_TAGS_FAILED_EXCEPTION)
        }
    }
}