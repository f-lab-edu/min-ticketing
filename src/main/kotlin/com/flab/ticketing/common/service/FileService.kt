package com.flab.ticketing.common.service

import java.awt.image.BufferedImage

interface FileService {

    fun uploadImage(image: BufferedImage): String
    
}