package com.synergeticsolutions.familyartefacts

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import java.security.SignatureException
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

class JwtAuthorizationFilter(authMgr: AuthenticationManager, private val tokenService: TokenService) :
        BasicAuthenticationFilter(authMgr) {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val auth = getAuthentication(request)
        if (auth == null) {
            chain.doFilter(request, response)
            return
        }
        SecurityContextHolder.getContext().authentication = auth
        chain.doFilter(request, response)
    }

    private fun getAuthentication(request: HttpServletRequest): UsernamePasswordAuthenticationToken? {
        val authorization = request.getHeader("Authorization")

        if (authorization != null && authorization.startsWith("Bearer")) {
            val token = authorization.replace("Bearer", "")
            try {
                val claims = tokenService.validateToken(token)
                val usr = claims.subject
                val authorities = (claims["rol"] as List<*>).map { SimpleGrantedAuthority(it as String) }

                if (usr.isNotBlank()) {
                    return UsernamePasswordAuthenticationToken(usr, null, authorities)
                }
            } catch (e: ExpiredJwtException) {
                log.warn("Request to parse expired JWT: {} failed: {}", token, e.message)
            } catch (e: UnsupportedJwtException) {
                log.warn("Request to parse unsupported JWT: {} failed: {}", token, e.message)
            } catch (e: MalformedJwtException) {
                log.warn("Request to parse invalid JWT: {} failed: {}", token, e.message)
            } catch (e: SignatureException) {
                log.warn("Request to parse JWT with invalid signature: {} failed: {}", token, e.message)
            } catch (e: IllegalArgumentException) {
                log.warn("Request to parse empty or null JWT: {} failed: {}", token, e.message)
            }
        }

        return null
    }
}
