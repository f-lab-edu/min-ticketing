package com.flab.ticketing.auth.config

import com.flab.ticketing.auth.filter.CustomUsernamePasswordAuthFilter
import com.flab.ticketing.auth.filter.JwtAuthenticateFilter
import com.flab.ticketing.common.filter.ExceptionHandlerFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(
        http: HttpSecurity,
        usernamePasswordAuthFilter: CustomUsernamePasswordAuthFilter,
        exceptionHandlerFilter: ExceptionHandlerFilter,
        jwtAuthenticateFilter: JwtAuthenticateFilter,
        authenticationEntryPoint: AuthenticationEntryPoint,
        myCorsConfig: CorsConfigurationSource
    ): SecurityFilterChain {
        http
            .csrf { csrfConfig -> csrfConfig.disable() }
            .authorizeHttpRequests { authorizedRequests ->
                authorizedRequests

                    .requestMatchers("/api/user/new/**").permitAll()
                    .requestMatchers("/api/user/login").permitAll()
                    .requestMatchers("/api/performances", "/api/performances/*").permitAll()
                    .requestMatchers("/actuator").permitAll()
                    .requestMatchers(
                        "/api/health-check",
                        "/api/orders/**",
                        "/api/reservations/**",
                        "/api/performances/*/dates/**"
                    )
                    .authenticated()
                    .requestMatchers(HttpMethod.GET).permitAll()
                    .anyRequest().authenticated()
            }.formLogin { formLogin -> formLogin.disable() }
            .exceptionHandling { it.authenticationEntryPoint(authenticationEntryPoint) }
            .addFilterAt(usernamePasswordAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticateFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(exceptionHandlerFilter, JwtAuthenticateFilter::class.java)
            .cors { it.configurationSource(myCorsConfig) }
        return http.build()
    }


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }

    @Bean
    fun authenticationProvider(
        userDetailsService: UserDetailsService
    ): AuthenticationProvider {
        val authenticationProvider = DaoAuthenticationProvider(passwordEncoder())
        authenticationProvider.setUserDetailsService(userDetailsService)

        return authenticationProvider
    }

    @Bean("myCorsConfig")
    @Profile("!test")
    fun corsConfigurationSource(
        @Value("\${cors.origins}") origins: List<String>
    ): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = origins
        configuration.setAllowedMethods(listOf("GET", "POST", "PUT", "DELETE"))
        configuration.allowCredentials = true
        configuration.addAllowedHeader("*")
        configuration.addExposedHeader("authorization")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}