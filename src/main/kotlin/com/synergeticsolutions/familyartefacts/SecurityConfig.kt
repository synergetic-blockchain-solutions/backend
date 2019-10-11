package com.synergeticsolutions.familyartefacts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@EnableWebSecurity
class SecurityConfig : WebSecurityConfigurerAdapter() {
    @Autowired
    private lateinit var userDetailsService: UserDetailsService
    @Autowired
    private lateinit var tokenService: TokenService

    private val publicEndpoints = OrRequestMatcher(
        AntPathRequestMatcher("/user", "POST"),
        AntPathRequestMatcher("/register", "POST")
    )

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder())
    }

    override fun configure(http: HttpSecurity) {
        http
            .cors()
            .and().csrf().disable()
            .authorizeRequests()
            .requestMatchers(publicEndpoints).permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilter(JwtAuthenticationFilter(authenticationManager(), "/login", tokenService))
            .addFilter(JwtAuthorizationFilter(authenticationManager(), tokenService))
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    }

    /**
     * Provide a [PasswordEncoder] to be used by services.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    /**
     * Cross-Origin Resource Sharing config. This is a REST API so just allow everything.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", CorsConfiguration().applyPermitDefaultValues())
        return source
    }
}
