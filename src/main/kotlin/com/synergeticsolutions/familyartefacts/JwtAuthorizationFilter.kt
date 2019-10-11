package com.synergeticsolutions.familyartefacts

import io.jsonwebtoken.JwtException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.bind.annotation.ResponseStatus
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthorizationFilter(authMgr: AuthenticationManager, private val tokenService: TokenService) :
        BasicAuthenticationFilter(authMgr) {
    private val log = LoggerFactory.getLogger(this::class.java)

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
        }
    }
}

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class TokenException : RuntimeException("")
