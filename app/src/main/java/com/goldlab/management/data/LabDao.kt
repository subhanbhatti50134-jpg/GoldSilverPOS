package com.goldlab.management.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<CustomerEntity>>
    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name") fun search(query: String): Flow<List<CustomerEntity>>
    @Insert suspend fun insert(customer: CustomerEntity): Long
    @Update suspend fun update(customer: CustomerEntity)
    @Delete suspend fun delete(customer: CustomerEntity)
    @Query("SELECT * FROM customers WHERE id = :id") suspend fun getById(id: Long): CustomerEntity?
}

@Dao interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY createdAt DESC") fun observeAll(): Flow<List<PurchaseEntity>>
    @Query("SELECT * FROM purchases WHERE transactionId LIKE '%' || :q || '%' OR partyName LIKE '%' || :q || '%' OR company LIKE '%' || :q || '%' OR serialNumber LIKE '%' || :q || '%' ORDER BY createdAt DESC") fun search(q: String): Flow<List<PurchaseEntity>>
    @Query("SELECT * FROM purchases WHERE id = :id") suspend fun getById(id: Long): PurchaseEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(purchase: PurchaseEntity): Long
    @Update suspend fun update(purchase: PurchaseEntity)
    @Query("DELETE FROM purchases WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("SELECT COUNT(*) FROM purchases") fun countFlow(): Flow<Int>
    @Query("SELECT * FROM purchases WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC") fun byDateRange(start: Long, end: Long): Flow<List<PurchaseEntity>>
}

@Dao interface PaymentDao {
    @Query("SELECT * FROM payments WHERE purchaseId = :purchaseId ORDER BY paidAt DESC") fun observeForPurchase(purchaseId: Long): Flow<List<PaymentEntity>>
    @Query("SELECT * FROM payments ORDER BY paidAt DESC") fun observeAll(): Flow<List<PaymentEntity>>
    @Insert suspend fun insert(payment: PaymentEntity): Long
    @Query("DELETE FROM payments WHERE purchaseId = :purchaseId") suspend fun deleteForPurchase(purchaseId: Long)
}

@Dao interface AuditDao {
    @Insert suspend fun insert(log: AuditLogEntity)
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 500") fun observeRecent(): Flow<List<AuditLogEntity>>
}

@Dao interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1") fun observe(): Flow<AppSettingsEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(settings: AppSettingsEntity)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY metal, productType, name")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: Long): InventoryEntity?

    @Insert
    suspend fun insert(item: InventoryEntity): Long

    @Update
    suspend fun update(item: InventoryEntity)

    @Insert
    suspend fun insertMovement(movement: InventoryMovementEntity): Long

    @Query(
        "SELECT * FROM inventory_movements " +
        "WHERE itemId = :itemId ORDER BY createdAt DESC, id DESC"
    )
    fun observeMovements(itemId: Long): Flow<List<InventoryMovementEntity>>
}
