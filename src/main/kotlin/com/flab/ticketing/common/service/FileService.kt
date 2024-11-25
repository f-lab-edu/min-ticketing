package com.flab.ticketing.common.service

interface FileService {


    /**
     * format에는 png, jpg, jpeg가 들어갈 수 있습니다.
     * @author minseok kim
     */
    fun uploadImage(image: ByteArray, format: String = "png"): String

}