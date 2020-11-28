package com.example.hobbyfi.utils

import android.os.Build
import android.util.Base64.DEFAULT
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.*


object TokenUtils {
    class InvalidStoredTokenException : Exception()

    /**
     * Decodes a given JWT and returns the value of the 'userId' field in the payload
     * @param jwtToken - JWT whose payload is to be decoded and parsed
     * @return
     */
    @Throws(ExpiredJwtException::class, MalformedJwtException::class, InvalidStoredTokenException::class)
    fun getTokenUserIdFromPayload(jwtToken: String?): Long {
        if(jwtToken == Constants.INVALID_TOKEN) {
            throw InvalidStoredTokenException()
        }

        val rsaPublicKey: RSAPublicKey
        rsaPublicKey = try {
            val decodedPublicKey: ByteArray = android.util.Base64.decode(BuildConfig.JWT_PUBLIC_KEY, DEFAULT)
            val keySpecX509 = X509EncodedKeySpec(decodedPublicKey) // ASN.1 encoding of public key
            val keyFactory: KeyFactory =
                KeyFactory.getInstance("RSA") // key factory for generating RSA keys
            keyFactory.generatePublic(keySpecX509) as RSAPublicKey
        } catch (e: NoSuchAlgorithmException) {
            throw Exception("Bad key parsing! No such algorithm!")
        } catch (e: InvalidKeySpecException) {
            throw Exception("Invalid key parsing!")
        }

        val parser: JwtParser = Jwts.parserBuilder()
            .setSigningKey(rsaPublicKey)
            .build()
        return parser.parseClaimsJws(jwtToken)
            .body
            .get("userId", Date::class.java) // has to be Date object because the ID is too big for JWT...
            .time
    }

    @Throws(MalformedJwtException::class, ExpiredJwtException::class, InvalidStoredTokenException::class)
    fun getTokenUserIdFromStoredTokens(prefConfig: PrefConfig): Long {
        return try {
            prefConfig.readToken().let {
                if(it != Constants.INVALID_TOKEN) getTokenUserIdFromPayload(it)
                    else throw InvalidStoredTokenException()
            }
        } catch (e: Exception) {
            when(e) {
                is MalformedJwtException, is ExpiredJwtException -> {
                    Log.w(
                        "UserRepository",
                        "getUsers —> User's token is expired & id cannot be retrieved. Attempting to get id from refresh token."
                    )

                    // Catching exceptions & throwing them again in order to provide verbose logging for JWT expiry/invalidity flow
                    return try {
                        prefConfig.readRefreshToken().let {
                            if(it != Constants.INVALID_TOKEN) getTokenUserIdFromPayload(it)
                                else throw InvalidStoredTokenException()
                        }
                    } catch (x: ExpiredJwtException) {
                        Log.w(
                            "UserRepository", "getUsers —> User needs to reauth. " +
                                    "This method will suspend and the user will be logged out after the response has been handled from the network entity."
                        )
                        throw x
                    } catch (x: MalformedJwtException) {
                        Log.w(
                            "UserRepository", "getUsers —> User needs to reauth. " +
                                    "This method will suspend and the user will be logged out after the response has been handled from the network entity."
                        )
                        throw x
                    }
                }
                else -> throw e
            }
        }
    }
}