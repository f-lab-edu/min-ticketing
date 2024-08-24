package com.flab.ticketing.performance.dto.service

data class PerformanceDetailSearchResult(
    val uid: String,
    val image: String,
    val title: String,
    val regionName: String,
    val placeName: String,
    val price: Int,
    val description: String
)