package com.example.hobbyfi.utils

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.hobbyfi.BuildConfig
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
    // TODO: JWT and Facebook access token utils go here
    /**
     * Decodes a given JWT and returns the value of the 'userId' field in the payload
     * @param jwtToken - JWT whose payload is to be decoded and parsed
     * @return
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(ExpiredJwtException::class, MalformedJwtException::class)
    fun getTokenUserIdFromPayload(jwtToken: String?): Long {
        val rsaPublicKey: RSAPublicKey
        rsaPublicKey = try {
            val decodedPublicKey: ByteArray =
                Base64.getDecoder().decode(BuildConfig.JWT_PUBLIC_KEY)
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun getTokenUserIdFromStoredTokens(prefConfig: PrefConfig): Long { // TODO: rename; absolutely horrid name
        return try {
            getTokenUserIdFromPayload(prefConfig.readToken())
        } catch (e: Exception) {
            when(e) {
                is MalformedJwtException, is ExpiredJwtException -> {
                    Log.w(
                        "UserRepository",
                        "getUsers —> User's token is expired & id cannot be retrieved. Attempting to get id from refresh token."
                    )

                    // Catching exceptions & throwing them again in order to provide verbose logging for JWT expiry/invalidity flow
                    try {
                        getTokenUserIdFromPayload(prefConfig.readRefreshToken())
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
                else -> throw Exception("Unrecognised failure in token claims parsing! ${e.message}")
            }
        }
    }
}