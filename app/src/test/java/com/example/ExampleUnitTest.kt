package com.example

import com.example.ui.screens.CalculatorState
import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit testing for shop management algorithms and custom calculator engines.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun calculator_operations_and_helpers_areCorrect() {
    val calc = CalculatorState()

    // Test digits input
    calc.onDigit("2")
    calc.onDigit("5")
    calc.onDigit("0")
    assertEquals("250", calc.display)

    // Test Markup/Profit (+20% markup helper)
    calc.onMarkup(0.20)
    assertEquals("300", calc.display) // 250 + 20% (50) = 300

    // Test Discount (-10% off helper)
    calc.onDiscount(0.10)
    assertEquals("270", calc.display) // 300 - 10% (30) = 270

    // Test simple operator chaining
    calc.onOperator("+")
    assertEquals("", calc.display)
    assertEquals("270", calc.memory)

    calc.onDigit("3")
    calc.onDigit("0")
    calc.calculate()
    assertEquals("300", calc.display) // 270 + 30 = 300
  }

  @Test
  fun testProductBargainRateCustomization() {
    val originalProduct = com.example.data.database.Product(
      id = 15,
      name = "Sufi Ghee 1kg",
      costPrice = 480.0,
      sellingPrice = 600.0,
      stock = 25
    )

    // The customer bargains, and shopkeeper modifies the price to 540.0 for this sale
    val bargainedProduct = originalProduct.copy(sellingPrice = 540.0)

    // Verify original remains intact
    assertEquals(600.0, originalProduct.sellingPrice, 0.0)
    assertEquals("Sufi Ghee 1kg", originalProduct.name)
    assertEquals(480.0, originalProduct.costPrice, 0.0)

    // Verify bargained version has new price but retains identical metadata
    assertEquals(540.0, bargainedProduct.sellingPrice, 0.0)
    assertEquals(originalProduct.name, bargainedProduct.name)
    assertEquals(originalProduct.costPrice, bargainedProduct.costPrice, 0.0)
    assertEquals(originalProduct.stock, bargainedProduct.stock)
  }
}
