package com.flab.ticketing.common.service

import com.flab.ticketing.common.utils.NanoIdGenerator
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.oracle.bmc.objectstorage.transfer.UploadManager
import com.oracle.bmc.objectstorage.transfer.UploadManager.UploadRequest
import java.io.ByteArrayInputStream


class OciFileService(
    private val uploadManager: UploadManager,
    private val regionName: String,
    private val nameSpaceName: String,
    private val bucketName: String
) : FileService {

    override fun uploadImage(image: ByteArray, format: String): String {

        val objectName = "qr-image/${NanoIdGenerator.createNanoId(nanoIdSize = 15)}.$format"
        val request = PutObjectRequest.builder()
            .namespaceName(nameSpaceName)
            .bucketName(bucketName)
            .objectName(objectName)
            .contentType("image/$format")
            .build()

        val uploadDetails =
            UploadRequest.builder(ByteArrayInputStream(image), image.size.toLong()).allowOverwrite(false).build(request)

        uploadManager.upload(uploadDetails)

        return "https://objectstorage.${regionName}.oraclecloud.com/n/$nameSpaceName/b/$bucketName/o/$objectName"
    }
}