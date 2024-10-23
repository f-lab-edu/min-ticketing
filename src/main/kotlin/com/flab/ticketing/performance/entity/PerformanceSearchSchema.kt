package com.flab.ticketing.performance.entity

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jakarta.persistence.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.WriteTypeHint
import java.time.ZonedDateTime


@Document(indexName = "performance_index", writeTypeHint = WriteTypeHint.FALSE)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class PerformanceSearchSchema(

    @Id
    private val id: String,

    @Field(type = FieldType.Text)
    private val title: String,

    @Field(type = FieldType.Date)
    private val showTimes: List<ZonedDateTime>,

    @Field(type = FieldType.Integer)
    private val price: Int,

    @Field(type = FieldType.Keyword)
    private val region: String,

    @Field(type = FieldType.Keyword, index = false)
    private val image: String


) {

    companion object {
        fun of(performance: Performance): PerformanceSearchSchema {
            return PerformanceSearchSchema(
                performance.uid,
                performance.name,
                performance.performanceDateTime.map { it.showTime },
                performance.price,
                performance.regionName,
                performance.image
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PerformanceSearchSchema

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}