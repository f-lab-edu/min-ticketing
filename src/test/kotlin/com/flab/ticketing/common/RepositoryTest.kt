package com.flab.ticketing.common

import com.flab.ticketing.performance.repository.dsl.CustomPerformanceRepositoryImpl
import com.linecorp.kotlinjdsl.support.spring.data.jpa.autoconfigure.KotlinJdslAutoConfiguration
import io.kotest.core.spec.style.StringSpec
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(
    KotlinJdslAutoConfiguration::class,
    CustomPerformanceRepositoryImpl::class
)
abstract class RepositoryTest : StringSpec()