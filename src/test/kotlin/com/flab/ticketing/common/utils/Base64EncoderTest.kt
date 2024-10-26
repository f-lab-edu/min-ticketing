package com.flab.ticketing.common.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class Base64EncoderTest : StringSpec({
    "특정 문자열을 인코딩할 수 있다" {
        forAll(
            row("abcd", "YWJjZA=="),
            row("123124", "MTIzMTI0"),
            row("test", "dGVzdA=="),
            row("hello", "aGVsbG8=")
        ) { decoded, encoded ->
            Base64Encoder.encode(decoded) shouldBe encoded
        }
    }

    "특정 문자열을 디코딩할 수 있다" {
        forAll(
            row("abcd", "YWJjZA=="),
            row("123124", "MTIzMTI0"),
            row("test", "dGVzdA=="),
            row("hello", "aGVsbG8=")
        ) { decoded, encoded ->
            Base64Encoder.decode(encoded) shouldBe decoded
        }
    }

})