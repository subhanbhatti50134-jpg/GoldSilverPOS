package com.goldlab.management.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

class InventoryRepository(private val database: LabDatabase) {
    private val dao = database.inventoryDao()

    fun observeItems(): Flow<List<InventoryEntity>> = dao.observeAll()

    fun observeMovements(itemId: Long): Flow<List<InventoryMovementEntity>> =
        dao.observeMovements(itemId)

    suspend fun addItem(
        name: String,
        metal: String,
        productType: String,
        purity: String,
        quantity: Int,
        weightGrams: String,
        ratePerGram: String,
        notes: String
    ) {
        require(name.isNotBlank()) { "Enter an item name." }
        require(metal in listOf("Gold", "Silver")) { "Select Gold or Silver." }
        require(productType in listOf("Loose", "Biscuit")) {
            "Select Loose or Biscuit."
        }
        require(purity.isNotBlank()) { "Enter purity, for example 24K or 999." }

        val weight = weightGrams.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Enter a valid weight.")
        val rate = ratePerGram.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Enter a valid rate.")

        require(weight > BigDecimal.ZERO) { "Weight must be greater than zero." }
        require(rate >= BigDecimal.ZERO) { "Rate cannot be negative." }
        if (productType == "Biscuit") {
            require(quantity > 0) { "Enter at least one biscuit." }
        }

        val storedQuantity = if (productType == "Biscuit") quantity else 0
        val totalWeight = if (productType == "Biscuit") {
            weight.multiply(BigDecimal(quantity))
        } else {
            weight
        }
        val now = System.currentTimeMillis()

        database.withTransaction {
            val id = dao.insert(
                InventoryEntity(
                    name = name.trim(),
                    metal = metal,
                    productType = productType,
                    purity = purity.trim(),
                    quantity = storedQuantity,
                    unitWeightGrams = weight.toPlainString(),
                    ratePerGram = rate.toPlainString(),
                    notes = notes.trim(),
                    createdAt = now,
                    updatedAt = now
                )
            )

            dao.insertMovement(
                InventoryMovementEntity(
                    itemId = id,
                    movementType = "OPENING",
                    quantityDelta = storedQuantity,
                    weightGramsDelta = totalWeight.toPlainString(),
                    note = notes.trim().ifBlank { "Opening stock" },
                    createdAt = now
                )
            )
        }
    }

    suspend fun changeStock(
        itemId: Long,
        direction: String,
        amountText: String,
        note: String
    ) {
        require(direction == "IN" || direction == "OUT") {
            "Invalid stock movement."
        }

        database.withTransaction {
            val item = dao.getById(itemId)
                ?: throw IllegalArgumentException("Inventory item not found.")

            val now = System.currentTimeMillis()
            val sign = if (direction == "IN") BigDecimal.ONE
                else BigDecimal("-1")

            val weightDelta: BigDecimal
            val quantityDelta: Int
            val updated: InventoryEntity

            if (item.productType == "Loose") {
                val amount = amountText.toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("Enter weight in grams.")
                require(amount > BigDecimal.ZERO) {
                    "Weight must be greater than zero."
                }

                val current = item.unitWeightGrams.toBigDecimal()
                if (direction == "OUT") {
                    require(amount <= current) {
                        "Insufficient stock. Available: ${current.toPlainString()} g"
                    }
                }

                weightDelta = amount.multiply(sign)
                quantityDelta = 0
                updated = item.copy(
                    unitWeightGrams = current.add(weightDelta).toPlainString(),
                    updatedAt = now
                )
            } else {
                val units = amountText.toIntOrNull()
                    ?: throw IllegalArgumentException("Enter biscuit quantity.")
                require(units > 0) { "Quantity must be greater than zero." }

                val delta = if (direction == "IN") units else -units
                val newQuantity = item.quantity + delta
                require(newQuantity >= 0) {
                    "Insufficient stock. Available: ${item.quantity} biscuits"
                }

                quantityDelta = delta
                weightDelta = item.unitWeightGrams.toBigDecimal()
                    .multiply(BigDecimal(units))
                    .multiply(sign)

                updated = item.copy(
                    quantity = newQuantity,
                    updatedAt = now
                )
            }

            dao.update(updated)
            dao.insertMovement(
                InventoryMovementEntity(
                    itemId = item.id,
                    movementType = direction,
                    quantityDelta = quantityDelta,
                    weightGramsDelta = weightDelta.toPlainString(),
                    note = note.trim(),
                    createdAt = now
                )
            )
        }
    }
}
