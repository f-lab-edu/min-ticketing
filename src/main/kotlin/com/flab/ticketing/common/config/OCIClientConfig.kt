package com.flab.ticketing.common.config

import com.oracle.bmc.ConfigFileReader
import com.oracle.bmc.Region
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration
import com.oracle.bmc.objectstorage.transfer.UploadManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class OCIClientConfig(
    @Value("\${oci.config-file.path}") private val configFilePath: String,
    @Value("\${oci.config-file.profile:DEFAULT}") private val configFileProfile: String
) {


    @Bean
    fun objectStorageUploadManager(): UploadManager {
        val configFile = ConfigFileReader.parse(configFilePath, configFileProfile)
        val provider = ConfigFileAuthenticationDetailsProvider(configFile)


        // REGION 변경 가능성 고려 필요
        val client = ObjectStorageClient.builder()
            .region(Region.AP_CHUNCHEON_1)
            .build(provider)

        val uploadConfiguration = UploadConfiguration.builder()
            .allowMultipartUploads(true)
            .allowParallelUploads(true)
            .build()

        return UploadManager(client, uploadConfiguration)
    }

}