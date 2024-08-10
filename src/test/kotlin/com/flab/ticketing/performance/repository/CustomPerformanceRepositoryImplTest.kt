package com.flab.ticketing.performance.repository

import com.flab.ticketing.common.RepositoryTest
import com.flab.ticketing.performance.repository.dsl.CustomPerformanceRepositoryImpl
import io.kotest.matchers.shouldNotBe

class CustomPerformanceRepositoryImplTest(
    private val customPerformanceRepositoryImpl: CustomPerformanceRepositoryImpl
) : RepositoryTest() {

    init {

        "CustomPerformanceRepositoryImplTest 객체를 정상적으로 주입받을 수 있다." {
            customPerformanceRepositoryImpl shouldNotBe null
        }
    }

}