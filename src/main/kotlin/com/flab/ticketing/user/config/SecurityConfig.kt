package com.flab.ticketing.user.config

import com.flab.ticketing.user.filter.CustomUsernamePasswordAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(
        http: HttpSecurity,
        usernamePasswordAuthFilter: CustomUsernamePasswordAuthFilter
    ): SecurityFilterChain {
        http
            .csrf { csrfConfig -> csrfConfig.disable() }
            .authorizeHttpRequests { authorizedRequests ->
                authorizedRequests
                    .requestMatchers("/api/user/new/**").permitAll()
                    .requestMatchers("/api/uers/login").permitAll()
                    .anyRequest().authenticated()
            }.formLogin { formLogin -> formLogin.disable() }
            .addFilterAt(usernamePasswordAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

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

}