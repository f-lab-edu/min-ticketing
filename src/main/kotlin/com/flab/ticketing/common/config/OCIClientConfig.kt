package com.flab.ticketing.common.config

import com.flab.ticketing.common.service.FileService
import com.flab.ticketing.common.service.OciFileService
import com.oracle.bmc.ConfigFileReader
import com.oracle.bmc.Region
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration
import com.oracle.bmc.objectstorage.transfer.UploadManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile


@Configuration
@Profile("!test")
class OCIClientConfig(
    @Value("\${oci.config-file.path}") private val configFilePath: String,
    @Value("\${oci.region-name}") private val regionName: String,
    @Value("\${oci.namespace-name}") private val nameSpaceName: String,
    @Value("\${oci.bucket-name}") private val bucketName: String,
    @Value("\${oci.config-file.profile:DEFAULT}") private val configFileProfile: String
) {


    @Bean
    fun objectStorageUploadManager(): UploadManager {
        val configFile = ConfigFileReader.parse(configFilePath, configFileProfile)
        val provider = ConfigFileAuthenticationDetailsProvider(configFile)


        // REGION 변경 가능성 고려 필요
        val client = ObjectStorageClient.builder()
            .region(Region.valueOf(regionName))
            .build(provider)

        val uploadConfiguration = UploadConfiguration.builder()
            .allowMultipartUploads(true)
            .allowParallelUploads(true)
            .build()

        return UploadManager(client, uploadConfiguration)
    }

    @Bean
    @Primary
    fun ociFileService(
        uploadManager: UploadManager
    ): FileService {
        return OciFileService(
            uploadManager = uploadManager,
            regionName = regionName,
            nameSpaceName = nameSpaceName,
            bucketName = bucketName
        )
    }

}