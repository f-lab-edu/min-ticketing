package com.flab.ticketing.common.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream


object QRCodeGenerator {

    private val qrCodeWriter = QRCodeWriter()
    fun gererateQR(content: String, width: Int = 300, height: Int = 300, format: String = "PNG"): ByteArray {
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height)
        
        return ByteArrayOutputStream().use { outputStream ->
            MatrixToImageWriter.writeToStream(bitMatrix, format, outputStream)
            outputStream.toByteArray()
        }
    }
}