package com.goldlab.management.data

import androidx.room.*

@Entity(tableName = "customers", indices = [Index(value = ["name"]), Index(value = ["phone"])])
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val kind: String = "Supplier",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchases", indices = [Index(value = ["transactionId"], unique = true), Index(value = ["customerId"]), Index(value = ["metal"]), Index(value = ["createdAt"])], foreignKeys = [ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.SET_NULL)])
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val customerId: Long? = null,
    val partyName: String,
    val phone: String = "",
    val metal: String = "Gold",
    val productType: String = "Loose Gold",
    val company: String = "",
    val serialNumber: String = "",
    val purity: String = "24K",
    val weight: String,
    val weightUnit: String = "grams",
    val rate: String,
    val rateUnit: String = "per gram",
    val totalAmount: String,
    val paidAmount: String = "0",
    val paymentMethod: String = "Cash",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val manualTotalOverride: Boolean = false
)

@Entity(tableName = "payments", indices = [Index(value = ["purchaseId"])], foreignKeys = [ForeignKey(entity = PurchaseEntity::class, parentColumns = ["id"], childColumns = ["purchaseId"], onDelete = ForeignKey.CASCADE)])
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val amount: String,
    val method: String,
    val note: String = "",
    val paidAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_log", indices = [Index(value = ["transactionId"]), Index(value = ["timestamp"])])
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val action: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val laboratoryName: String = "Gold & Silver Laboratory",
    val address: String = "",
    val phone: String = "",
    val receiptFooter: String = "Thank you",
    val currency: String = "PKR",
    val defaultWeightUnit: String = "grams",
    val gramsPerTola: String = "11.6638",
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val darkTheme: Boolean = true
)

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["name"]),
        Index(value = ["metal"]),
        Index(value = ["productType"])
    ]
)
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val metal: String,
    val productType: String,
    val purity: String,
    // Loose stock: total grams. Biscuit stock: grams per biscuit.
    val quantity: Int = 0,
    val unitWeightGrams: String,
    val ratePerGram: String = "0",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_movements",
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["createdAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = InventoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val movementType: String,
    val quantityDelta: Int = 0,
    // Positive for stock-in, negative for stock-out.
    val weightGramsDelta: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
