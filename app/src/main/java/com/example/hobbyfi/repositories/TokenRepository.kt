package com.example.hobbyfi.repositories

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
import org.json.JSONException
import retrofit2.HttpException

class TokenRepository(prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI) : Repository(prefConfig, hobbyfiAPI) {
    suspend fun getRegisterToken(
        facebookToken: String?,
            email: String?, password: String?, username: String, description: String?,
            base64Image: String?, tags: List<Tag>?) : TokenResponse? {

        try {
            val tokenResponse = hobbyfiAPI.fetchRegistrationToken(
                facebookToken,
                email,
                username,
                password,
                description,
                base64Image,
                tags
            )

            if(tokenResponse?.response == Constants.EXISTS_RESPONSE) {
                throw Exception("The account you're trying to register already exists! Please try again!")
            }

            return tokenResponse
        } catch(ex: Exception) {
            Callbacks.dissectRepositoryExceptionAndThrow(ex)
            return null
        }
    }

    suspend fun getLoginToken(email: String, password: String) : TokenResponse? {
        return try {
            hobbyfiAPI.fetchLoginToken(
                email,
                password
            )
        } catch(ex: Exception) {
            Callbacks.dissectRepositoryExceptionAndThrow(ex)
            null
        }
    }

    suspend fun getFacebookUserEmail() : String? {
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