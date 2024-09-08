package com.flab.ticketing.order.enums

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.http.HttpStatus


@JsonTypeInfo(
    use= JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = TossPayCancelErrorCode::class, name = TossPayErrorCode.Types.CANCEL),
    JsonSubTypes.Type(value = TossPayConfirmErrorCode::class, name = TossPayErrorCode.Types.CONFIRM),
)
interface TossPayErrorCode {

    val responseStatus: HttpStatus

    class Types{
        companion object{
            const val CONFIRM = "CONFIRM"
            const val CANCEL = "CANCEL"
        }

    }

}