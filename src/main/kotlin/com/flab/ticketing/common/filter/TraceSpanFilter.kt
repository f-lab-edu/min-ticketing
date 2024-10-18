package com.flab.ticketing.common.filter

import io.micrometer.tracing.Tracer
import io.micrometer.tracing.annotation.NewSpan
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class NginxSpanFilter(private val tracer: Tracer) : OncePerRequestFilter() {

    @NewSpan("nginx")
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val nginxSpan = tracer.nextSpan().name("nginx")

        nginxSpan.tag("service.name", "nginx")
        nginxSpan.tag("span.kind", "SERVER")

        tracer.withSpan(nginxSpan.start()).use { _ ->
            try {
                filterChain.doFilter(request, response)
            } finally {
                nginxSpan.end()
            }
        }
    }
}