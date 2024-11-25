package com.flab.ticketing.common.service

import org.springframework.stereotype.Component


@Component
class TempFileService : FileService {
    override fun uploadImage(image: ByteArray, format: String): String {
        return "http://test.com/image/1"
    }
}