package com.synergeticsolutions.familyartefacts.security

import com.synergeticsolutions.familyartefacts.repositories.UserRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.util.Date
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service used by other components to create and validate JWT tokens.
 */
@Service
class TokenServiceImpl(
    @Value("\${auth.jwtSecret}")
    val jwtSecret: String,
    @Value("\${auth.jwtLifetime}")
    val jwtLifetime: Long,
    @Value("\${auth.audience}")
    val audience: String,
    @Value("\${auth.issuer}")
    val issuer: String,
    @Autowired
    val userRepository: UserRepository
) : TokenService {

    /**
     * Create a token for a user [name] with roles [roles].
     */
    override fun createToken(name: String, roles: List<String>): String {
        val signatureAlgorithm = SignatureAlgorithm.HS512
        val apiKeySecretBytes = DatatypeConverter.parseBase64Binary(jwtSecret)
        val signingKey = SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.jcaName)

        val now = Date(System.currentTimeMillis())
        val expiration = Date(now.time + jwtLifetime!!)

        val user = userRepository.findByEmail(name)!!
        return Jwts.builder()
                .setAudience(audience)
                .setIssuer(issuer)
                .setSubject(name)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("rol", roles)
            .claim("nam", user.name)
                .signWith(signingKey, signatureAlgorithm)
                .compact()
    }

    /**
     * Check that [token] is valid.
     *
     * Valid means that it is well formed, is signed with the correct key, etc.
     */
    override fun validateToken(token: String): Claims {
        return Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(jwtSecret)).parseClaimsJws(token).body
    }
}
