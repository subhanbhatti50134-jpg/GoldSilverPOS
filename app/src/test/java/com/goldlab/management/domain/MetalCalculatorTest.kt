package com.goldlab.management.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MetalCalculatorTest {
    @Test fun gramsAtPerGram() { assertEquals(BigDecimal("20000.00"), MetalCalculator.calculate(BigDecimal("10"), "grams", BigDecimal("2000"), "per gram")) }
    @Test fun gramsAtPerTola() { assertEquals(BigDecimal("17147.07"), MetalCalculator.calculate(BigDecimal("10"), "grams", BigDecimal("20000"), "per tola")) }
    @Test fun kilogramConversion() { assertEquals(BigDecimal("100000.00"), MetalCalculator.calculate(BigDecimal("1"), "kilograms", BigDecimal("100"), "per gram")) }
}
