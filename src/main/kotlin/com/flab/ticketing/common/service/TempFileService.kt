package com.flab.ticketing.common.service

import org.springframework.stereotype.Component
import java.awt.image.BufferedImage


@Component
class TempFileService : FileService {
    override fun uploadImage(image: BufferedImage): String {
        return "http://test.com/image/1"
    }


}