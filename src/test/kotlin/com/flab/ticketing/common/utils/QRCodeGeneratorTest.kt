package com.flab.ticketing.common.utils

import com.flab.ticketing.testutils.UnitTest
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import io.kotest.matchers.equals.shouldBeEqual
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO


class QRCodeGeneratorTest : UnitTest() {


    init {
        "QR코드를 생성해 BufferedImage 형태로 반환할 수 있다." {
            val content = "http://test.com/test/1"
            val qrImage = QRCodeGenerator.gererateQR(content)

            val bufferedImage = ImageIO.read(ByteArrayInputStream(qrImage))
            val luminanceSource = BufferedImageLuminanceSource(bufferedImage)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))

            val decodeResult = MultiFormatReader().decode(binaryBitmap)
            decodeResult.text shouldBeEqual content


        }
    }

}