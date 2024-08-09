package com.flab.ticketing.auth.resolver

import com.flab.ticketing.auth.dto.CustomUserDetails
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.auth.resolver.annotation.LoginUser
import com.flab.ticketing.common.exception.UnAuthorizedException
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer


@Component
class LoginedUserUidArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        // 파라미터의 타입이 이 Resolver이 처리할 수 있는 타입인지 확인
        val isParameterTypeSupports = parameter.getParameterType().isAssignableFrom(String::class.java)

        // 이 ArgumentResolver가 적용되는 Annotation 지정
        val hasAnnotation = parameter.hasParameterAnnotation(LoginUser::class.java)
        return isParameterTypeSupports && hasAnnotation
    }

    @Throws(Exception::class)
    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {

        return runCatching {
            val userDetails = SecurityContextHolder.getContext().authentication!!.principal as? CustomUserDetails

            return userDetails?.username ?: throw UnAuthorizedException(AuthErrorInfos.AUTH_INFO_INVALID)
        }.getOrElse {
            throw UnAuthorizedException(AuthErrorInfos.AUTH_INFO_INVALID)
        }
    }
}
