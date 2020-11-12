package com.example.hobbyfi.responses

data class TokenResponse(
    override val response: String?,
    val jwt: String?,
    val refreshJwt: String?
) : Response(response)