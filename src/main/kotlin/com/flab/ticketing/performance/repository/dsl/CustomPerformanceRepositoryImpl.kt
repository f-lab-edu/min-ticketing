package com.flab.ticketing.performance.repository.dsl

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.stereotype.Repository


@Repository
class CustomPerformanceRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor
) : CustomPerformanceRepository