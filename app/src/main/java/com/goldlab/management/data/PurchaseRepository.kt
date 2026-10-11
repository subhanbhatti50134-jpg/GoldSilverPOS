package com.goldlab.management.data

import androidx.room.withTransaction
import com.goldlab.management.domain.MetalCalculator
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PurchaseRepository(
    private val purchases: PurchaseDao,
    private val customers: CustomerDao,
    private val payments: PaymentDao,
    private val database: LabDatabase? = null
) {
    fun observePurchases(): Flow<List<PurchaseEntity>> = purchases.observeAll()
    fun searchPurchases(q: String): Flow<List<PurchaseEntity>> = purchases.search(q)
    fun observeCustomers(): Flow<List<CustomerEntity>> = customers.observeAll()
    fun searchCustomers(q: String): Flow<List<CustomerEntity>> = customers.search(q)

    fun observePaymentsForPurchase(purchaseId: Long): Flow<List<PaymentEntity>> =
        payments.observeForPurchase(purchaseId)

    suspend fun addCustomer(
        name: String,
        phone: String,
        kind: String,
        address: String,
        notes: String
    ) = customers.insert(
        CustomerEntity(
            name = name.trim(),
            phone = phone.trim(),
            kind = kind,
            address = address.trim(),
            notes = notes.trim()
        )
    )

    suspend fun addPurchase(p: PurchaseEntity): Long {
        require(p.partyName.isNotBlank()) { "Customer/supplier name is required" }

        val weight = p.weight.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Enter a valid weight")
        val rate = p.rate.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Enter a valid rate")
        val total = p.totalAmount.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Enter a valid total amount")
        val paid = p.paidAmount.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Enter a valid paid amount")

        require(weight > BigDecimal.ZERO) { "Weight must be greater than zero" }
        require(rate >= BigDecimal.ZERO) { "Rate cannot be negative" }
        require(total >= BigDecimal.ZERO) { "Total cannot be negative" }
        require(paid >= BigDecimal.ZERO) { "Paid amount cannot be negative" }
        require(paid <= total) { "Paid amount cannot exceed total amount" }

        val transactionId = p.transactionId.ifBlank { newTransactionId() }

        suspend fun insert(): Long {
            val id = purchases.insert(p.copy(transactionId = transactionId))
            if (paid > BigDecimal.ZERO) {
                payments.insert(
                    PaymentEntity(
                        purchaseId = id,
                        amount = paid.toPlainString(),
                        method = p.paymentMethod,
                        note = "Initial payment",
                        paidAt = p.createdAt
                    )
                )
            }
            return id
        }

        return if (database != null) {
            database.withTransaction { insert() }
        } else {
            insert()
        }
    }

    suspend fun recordPayment(
        purchaseId: Long,
        amount: String,
        method: String,
        note: String,
        paidAt: Long = System.currentTimeMillis()
    ) {
        val value = amount.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Enter a valid payment amount")

        require(value > BigDecimal.ZERO) { "Payment must be greater than zero" }

        suspend fun save() {
            val purchase = purchases.getById(purchaseId)
                ?: throw IllegalArgumentException("Purchase not found")

            val previouslyPaid = purchase.paidAmount.toBigDecimalOrNull()
                ?: throw IllegalStateException("Existing paid amount is invalid")
            val total = purchase.totalAmount.toBigDecimalOrNull()
                ?: throw IllegalStateException("Existing total amount is invalid")
            val outstanding = total.subtract(previouslyPaid)

            require(outstanding >= BigDecimal.ZERO) {
                "Existing purchase balance is invalid"
            }
            require(value <= outstanding) {
                "Payment exceeds outstanding balance"
            }

            payments.insert(
                PaymentEntity(
                    purchaseId = purchaseId,
                    amount = value.toPlainString(),
                    method = method,
                    note = note.trim(),
                    paidAt = paidAt
                )
            )

            purchases.update(
                purchase.copy(
                    paidAmount = previouslyPaid.add(value).toPlainString(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        if (database != null) {
            database.withTransaction { save() }
        } else {
            save()
        }
    }

    suspend fun deletePurchase(p: PurchaseEntity) {
        suspend fun delete() {
            database?.auditDao()?.insert(
                AuditLogEntity(
                    transactionId = p.transactionId,
                    action = "DELETE",
                    summary = "Deleted ${p.metal} purchase for ${p.partyName}"
                )
            )
            purchases.deleteById(p.id)
        }

        if (database != null) {
            database.withTransaction { delete() }
        } else {
            delete()
        }
    }

    suspend fun updatePurchase(p: PurchaseEntity) {
        purchases.update(p.copy(updatedAt = System.currentTimeMillis()))
    }

    private fun newTransactionId(): String =
        "GL-${SimpleDateFormat("yyMMdd-HHmmss-SSS", Locale.US).format(Date())}"
}
