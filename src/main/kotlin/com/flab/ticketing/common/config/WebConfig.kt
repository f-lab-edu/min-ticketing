package com.flab.ticketing.common.config

import com.flab.ticketing.auth.resolver.LoginedUserArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val loginedUserArgumentResolver: LoginedUserArgumentResolver
) : WebMvcConfigurer {


    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(loginedUserArgumentResolver)
    }
}