package com.goldlab.management.domain

import java.math.BigDecimal
import java.math.RoundingMode

object MetalCalculator {
    val DEFAULT_GRAMS_PER_TOLA = BigDecimal("11.6638")
    fun grams(weight: BigDecimal, unit: String, gramsPerTola: BigDecimal = DEFAULT_GRAMS_PER_TOLA): BigDecimal = when (unit.lowercase()) {
        "grams", "gram", "g" -> weight
        "tola", "tolas" -> weight.multiply(gramsPerTola)
        "kilograms", "kilogram", "kg" -> weight.multiply(BigDecimal("1000"))
        else -> throw IllegalArgumentException("Unsupported weight unit: $unit")
    }
    fun calculate(weight: BigDecimal, weightUnit: String, rate: BigDecimal, rateUnit: String, gramsPerTola: BigDecimal = DEFAULT_GRAMS_PER_TOLA): BigDecimal {
        require(weight > BigDecimal.ZERO) { "Weight must be greater than zero" }
        require(rate >= BigDecimal.ZERO) { "Rate cannot be negative" }
        val amount = when (rateUnit.lowercase()) {
            "per gram", "gram", "per g" -> grams(weight, weightUnit, gramsPerTola).multiply(rate)
            "per tola", "tola" -> grams(weight, weightUnit, gramsPerTola).divide(gramsPerTola, 12, RoundingMode.HALF_UP).multiply(rate)
            else -> throw IllegalArgumentException("Unsupported rate unit: $rateUnit")
        }
        return amount.setScale(2, RoundingMode.HALF_UP)
    }
    fun outstanding(total: BigDecimal, paid: BigDecimal): BigDecimal = total.subtract(paid).setScale(2, RoundingMode.HALF_UP)
}
