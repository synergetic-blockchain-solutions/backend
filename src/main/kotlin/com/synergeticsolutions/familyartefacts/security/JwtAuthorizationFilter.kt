package com.synergeticsolutions.familyartefacts.security

import io.jsonwebtoken.JwtException
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Requests that are not to the authentication endpoionts go through here for authorization. This filter checks that
 * the JWT token is valid.
 */
class JwtAuthorizationFilter(authMgr: AuthenticationManager, private val tokenService: TokenService) :
        BasicAuthenticationFilter(authMgr) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * [doFilterInternal] does the hard work of this filter. It retrieves the Authorization header from [request] and
     * ensures that the bearer token is a valid JWT. It then creates the user context which we use in the controllers
     * to determine who the authenticated user is.
     */
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val authorization = request.getHeader("Authorization")

        if (authorization != null && authorization.startsWith("Bearer")) {
            val token = authorization.replace("Bearer", "")
            try {
                val claims = tokenService.validateToken(token)
                val usr = claims.subject
                val authorities = (claims["rol"] as List<*>).map { SimpleGrantedAuthority(it as String) }

                if (usr.isNotBlank()) {
                    val auth = UsernamePasswordAuthenticationToken(usr, null, authorities)
                    SecurityContextHolder.getContext().authentication = auth
                }
                chain.doFilter(request, response)
            } catch (e: JwtException) {
                log.warn("Request to parse JWT: {} failed: {}", token, e.message)
                response.status = HttpStatus.UNAUTHORIZED.value()
            }
        } else {
            chain.doFilter(request, response)
        }
    }
}

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class TokenException : RuntimeException("")
