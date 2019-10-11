package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

class JwtAuthenticationFilter(val authManager: AuthenticationManager, loginUrl: String, val tokenService: TokenService) : UsernamePasswordAuthenticationFilter() {
    init {
        setFilterProcessesUrl(loginUrl)
    }

    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse?): Authentication {
        val body = request.inputStream
        val credentials = ObjectMapper().registerKotlinModule().readValue(body, LoginRequest::class.java)
        val token = UsernamePasswordAuthenticationToken(credentials.email, credentials.password)
        return authManager.authenticate(token)
    }

    override fun successfulAuthentication(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain, authResult: Authentication) {
        val user = authResult.principal as User
        val roles = user.authorities.map { it.authority }
        val token = tokenService.createToken(user.username, roles)
        val loginResponse = LoginResponse(token)
        response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
        response.writer.write(ObjectMapper().registerKotlinModule().writeValueAsString(loginResponse))
    }
}
