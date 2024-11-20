package com.flab.ticketing.testutils

import com.flab.ticketing.performance.repository.dsl.CustomPerformanceRepositoryImpl
import com.flab.ticketing.testutils.config.TestUtilConfig
import com.flab.ticketing.testutils.persistence.UserTestUtils
import com.linecorp.kotlinjdsl.support.spring.data.jpa.autoconfigure.KotlinJdslAutoConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(
    KotlinJdslAutoConfiguration::class,
    CustomPerformanceRepositoryImpl::class,
    TestUtilConfig::class
)
abstract class RepositoryTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    protected lateinit var userTestUtils: UserTestUtils

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

    }

}