package com.flab.ticketing.performance.dto.response

import com.flab.ticketing.common.entity.Region

data class RegionInfoResponse(
    val uid : String,
    val name : String
){
    companion object{

        fun of(region : Region): RegionInfoResponse{
            return RegionInfoResponse(
                region.uid,
                region.name
            )
        }
    }

}