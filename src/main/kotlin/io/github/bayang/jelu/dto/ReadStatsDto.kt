package io.github.bayang.jelu.dto

import io.github.bayang.jelu.utils.centsToDouble

data class YearStatsDto(
    val dropped: Int = 0,
    val finished: Int = 0,
    val year: Int,
    val pageCount: Int = 0,
    val price: Double = 0.0,
)

data class MonthStatsDto(
    val dropped: Int = 0,
    val finished: Int = 0,
    val year: Int,
    val month: Int,
    val pageCount: Int = 0,
    val price: Double = 0.0,
)

data class TotalsStatsDto(
    val read: Long = 0,
    val unread: Long = 0,
    val dropped: Long = 0,
    val total: Long = 0,
    val price: Double = 0.0,
)

data class YearStatsCentsDto(
    val dropped: Int = 0,
    val finished: Int = 0,
    val year: Int,
    val pageCount: Int = 0,
    val priceInCents: Long = 0,
) {
    fun toYearStatsDto(): YearStatsDto =
        YearStatsDto(
            dropped = dropped,
            finished = finished,
            year = year,
            pageCount = pageCount,
            price = centsToDouble(priceInCents) ?: 0.0,
        )
}

data class MonthStatsCentsDto(
    val dropped: Int = 0,
    val finished: Int = 0,
    val year: Int,
    val month: Int,
    val pageCount: Int = 0,
    val priceInCents: Long = 0,
) {
    fun toMonthStatsDto(): MonthStatsDto =
        MonthStatsDto(
            dropped = dropped,
            finished = finished,
            year = year,
            month = month,
            pageCount = pageCount,
            price = centsToDouble(priceInCents) ?: 0.0,
        )
}

data class TotalsStatsCentsDto(
    val read: Long = 0,
    val unread: Long = 0,
    val dropped: Long = 0,
    val total: Long = 0,
    val priceInCents: Long = 0,
) {
    fun toTotalsStatsDto(): TotalsStatsDto =
        TotalsStatsDto(
            read = read,
            unread = unread,
            dropped = dropped,
            total = total,
            price = centsToDouble(priceInCents) ?: 0.0,
        )
}
