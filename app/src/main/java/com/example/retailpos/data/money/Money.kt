package com.example.retailpos.data.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Exact monetary value represented as Indian paise.
 * Currency arithmetic should use Long paise rather than Double.
 */
@JvmInline
value class Money(val paise: Long) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(paise + other.paise)
    operator fun minus(other: Money): Money = Money(paise - other.paise)
    operator fun times(multiplier: Long): Money = Money(paise * multiplier)

    override operator fun compareTo(other: Money): Int = paise.compareTo(other.paise)

    fun toMajor(): BigDecimal = BigDecimal.valueOf(paise, 2)

    override fun toString(): String = toMajor().setScale(2, RoundingMode.UNNECESSARY).toPlainString()

    companion object {
        val ZERO = Money(0L)

        fun fromMajor(value: BigDecimal): Money = Money(
            value.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
        )

        fun fromMajor(value: String): Money = fromMajor(BigDecimal(value.trim()))
    }
}
