package io.github.bayang.jelu.dto

data class YearStatsDto(
    val dropped: Int = 0,
    val finished: Int = 0,
    val year: Int,
    val pageCount: Int = 0,
    val priceInCents: Long = 0,
)

data class MonthStatsDto(
    val dropped: Int = 0,
    val finished: Int = 0,
    val year: Int,
    val month: Int,
    val pageCount: Int = 0,
    val priceInCents: Long = 0,
)

data class TotalsStatsDto(
    val read: Long = 0,
    val unread: Long = 0,
    val dropped: Long = 0,
    val total: Long = 0,
    val priceInCents: Long = 0,
)
