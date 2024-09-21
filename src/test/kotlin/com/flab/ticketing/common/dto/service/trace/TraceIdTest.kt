package com.flab.ticketing.common.dto.service.trace

import com.flab.ticketing.common.UnitTest
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

class TraceIdTest : UnitTest() {


    init {

        "TraceId를 생성할 시 8글자의 Unique한 ID를 생성할 수 있다." {
            val traceId = TraceId()

            traceId.id.length shouldBe 8
        }

        "TraceId의 level을 증가시켜 start prefix를 생성할 수 있다." {
            forAll(
                table(
                    headers("input", "expected"),
                    row(0, "|"),
                    row(1, "|->"),
                    row(2, "|-->"),
                    row(3, "|--->")
                )
            ) { input, expected ->
                val traceId = TraceId()

                for (i in 0..<input) {
                    traceId.addLevel()
                }

                traceId.getStartPrefix() shouldBeEqual expected
            }
        }
        "TraceId의 level을 증가시켜 end prefix를 생성할 수 있다." {
            forAll(
                table(
                    headers("input", "expected"),
                    row(0, "|"),
                    row(1, "|<-"),
                    row(2, "|<--"),
                    row(3, "|<---")
                )
            ) { input, expected ->
                val traceId = TraceId()

                for (i in 0..<input) {
                    traceId.addLevel()
                }

                traceId.getEndPrefix() shouldBeEqual expected
            }
        }
        "TraceId의 level을 증가시켜 exception prefix를 생성할 수 있다." {
            forAll(
                table(
                    headers("input", "expected"),
                    row(0, "|"),
                    row(1, "|<X-"),
                    row(2, "|<X--"),
                    row(3, "|<X---")
                )
            ) { input, expected ->
                val traceId = TraceId()

                for (i in 0..<input) {
                    traceId.addLevel()
                }

                traceId.getExceptionPrefix() shouldBeEqual expected
            }
        }

    }

}