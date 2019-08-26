package com.synergeticsolutions.familyartefacts

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.util.Date
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TokenServiceImpl : TokenService {
    @Value("\${auth.jwtSecret}")
    lateinit var jwtSecret: String
    @Value("\${auth.jwtLifetime}")
    val jwtLifetime: Long? = null
    @Value("\${auth.audience}")
    lateinit var audience: String
    @Value("\${auth.issuer}")
    lateinit var issuer: String

    override fun createToken(name: String, roles: List<String>): String {
        val signatureAlgorithm = SignatureAlgorithm.HS512
        val apiKeySecretBytes = DatatypeConverter.parseBase64Binary(jwtSecret)
        val signingKey = SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.jcaName)

        val now = Date(System.currentTimeMillis())
        val expiration = Date(now.time + jwtLifetime!!)

        return Jwts.builder()
                .setAudience(audience)
                .setIssuer(issuer)
                .setSubject(name)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("rol", roles)
                .signWith(signingKey, signatureAlgorithm)
                .compact()
    }

    override fun validateToken(token: String): Claims {
        return Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(jwtSecret)).parseClaimsJws(token).body
    }
}