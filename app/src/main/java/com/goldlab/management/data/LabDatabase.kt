package com.goldlab.management.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CustomerEntity::class,
        PurchaseEntity::class,
        PaymentEntity::class,
        AuditLogEntity::class,
        AppSettingsEntity::class,
        InventoryEntity::class,
        InventoryMovementEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class LabDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun paymentDao(): PaymentDao
    abstract fun auditDao(): AuditDao
    abstract fun settingsDao(): SettingsDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inventory_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `metal` TEXT NOT NULL,
                        `productType` TEXT NOT NULL,
                        `purity` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitWeightGrams` TEXT NOT NULL,
                        `ratePerGram` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_inventory_items_name` " +
                        "ON `inventory_items` (`name`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_inventory_items_metal` " +
                        "ON `inventory_items` (`metal`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_inventory_items_productType` " +
                        "ON `inventory_items` (`productType`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inventory_movements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `movementType` TEXT NOT NULL,
                        `quantityDelta` INTEGER NOT NULL,
                        `weightGramsDelta` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`)
                            REFERENCES `inventory_items`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_inventory_movements_itemId` " +
                        "ON `inventory_movements` (`itemId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_inventory_movements_createdAt` " +
                        "ON `inventory_movements` (`createdAt`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: LabDatabase? = null

        fun getInstance(context: Context): LabDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LabDatabase::class.java,
                    "gold_silver_lab.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
