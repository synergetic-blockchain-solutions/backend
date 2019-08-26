package com.synergeticsolutions.familyartefacts

import io.jsonwebtoken.Claims

interface TokenService {
    fun createToken(name: String, roles: List<String>): String
    fun validateToken(token: String): Claims
}
