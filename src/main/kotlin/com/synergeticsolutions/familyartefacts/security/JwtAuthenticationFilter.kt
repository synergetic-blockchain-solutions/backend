package com.synergeticsolutions.familyartefacts.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.synergeticsolutions.familyartefacts.dtos.LoginRequest
import com.synergeticsolutions.familyartefacts.dtos.LoginResponse
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Security filter to exchange the user's email and password for a JWT token.
 *
 * This filter is only user on [loginUrl].
 */
class JwtAuthenticationFilter(val authManager: AuthenticationManager, loginUrl: String, val tokenService: TokenService) : UsernamePasswordAuthenticationFilter() {
    init {
        setFilterProcessesUrl(loginUrl)
    }

    /**
     * This is the entry point for the filter. The user's email and password are retrieved from the [request] here
     * and then checked using the [AuthenticationManager].
     */
    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse?): Authentication {
        val body = request.inputStream
        val credentials = ObjectMapper().registerKotlinModule().readValue(body, LoginRequest::class.java)
        val token = UsernamePasswordAuthenticationToken(credentials.email, credentials.password)
        return authManager.authenticate(token)
    }

    /**
     * [successfulAuthentication] is only called with the user's email and password as correct. It creates a JWT token
     * with the required fields and returns it to the user in a JSON document with the key "token".
     */
    override fun successfulAuthentication(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain, authResult: Authentication) {
        val user = authResult.principal as User
        val roles = user.authorities.map { it.authority }
        val token = tokenService.createToken(user.username, roles)
        val loginResponse = LoginResponse(token)
        response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
        response.writer.write(ObjectMapper().registerKotlinModule().writeValueAsString(loginResponse))
    }
}
