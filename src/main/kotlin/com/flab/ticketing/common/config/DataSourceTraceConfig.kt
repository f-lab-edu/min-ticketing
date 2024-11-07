package com.flab.ticketing.common.config

import com.zaxxer.hikari.HikariDataSource
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import net.ttddyy.dsproxy.ExecutionInfo
import net.ttddyy.dsproxy.QueryInfo
import net.ttddyy.dsproxy.listener.QueryExecutionListener
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

//@Configuration
//@Profile("prod")
class DataSourceTraceConfig(
    private val properties: DataSourceProperties,
    private val observationRegistry: ObservationRegistry
) {

    @Bean
    fun dataSource(): DataSource {
        val dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
        return ProxyDataSourceBuilder.create(dataSource)
            .name("dataSource")
            .listener(DataSourceQueryExecutionListener(observationRegistry))
            .build()
    }

}

class DataSourceQueryExecutionListener(private val observationRegistry: ObservationRegistry) : QueryExecutionListener {
    override fun beforeQuery(execInfo: ExecutionInfo?, queryInfoList: MutableList<QueryInfo>?) {
        // No operation needed before query execution
    }

    override fun afterQuery(execInfo: ExecutionInfo?, queryInfoList: MutableList<QueryInfo>?) {
        execInfo?.let { info ->
            val elapsedTime = info.elapsedTime
            val queryType = queryInfoList?.firstOrNull()?.let { getQueryType(it.query) } ?: "UNKNOWN"

            Observation.createNotStarted("database.query", observationRegistry)
                .lowCardinalityKeyValue("db.name", "myDatabase")
                .lowCardinalityKeyValue("db.operation", queryType)
                .observe {
                    elapsedTime
                }
        }
    }

    private fun getQueryType(query: String): String {
        return when {
            query.trim().startsWith("SELECT", ignoreCase = true) -> "SELECT"
            query.trim().startsWith("INSERT", ignoreCase = true) -> "INSERT"
            query.trim().startsWith("UPDATE", ignoreCase = true) -> "UPDATE"
            query.trim().startsWith("DELETE", ignoreCase = true) -> "DELETE"
            else -> "OTHER"
        }
    }
}