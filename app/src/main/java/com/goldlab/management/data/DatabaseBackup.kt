package com.goldlab.management.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object DatabaseBackup {
    private const val FORMAT = "SNJ_GOLD_LAB_BACKUP"
    private const val VERSION = 2

    private val tables = listOf(
        "customers",
        "purchases",
        "payments",
        "audit_log",
        "app_settings",
        "inventory_items",
        "inventory_movements"
    )

    private val deleteOrder = listOf(
        "payments",
        "purchases",
        "inventory_movements",
        "inventory_items",
        "customers",
        "audit_log",
        "app_settings"
    )

    fun exportJson(database: LabDatabase): String {
        val db = database.openHelper.readableDatabase
        val tableData = JSONObject()

        for (table in tables) {
            db.query("SELECT * FROM `$table`").use { cursor ->
                val columns = JSONArray()
                cursor.columnNames.forEach { columns.put(it) }

                val rows = JSONArray()
                while (cursor.moveToNext()) {
                    val row = JSONObject()

                    for (i in cursor.columnNames.indices) {
                        val value: Any = when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                            Cursor.FIELD_TYPE_BLOB ->
                                error("Unexpected binary data in table $table")
                            else -> error("Unsupported database value")
                        }
                        row.put(cursor.getColumnName(i), value)
                    }
                    rows.put(row)
                }

                tableData.put(
                    table,
                    JSONObject().put("columns", columns).put("rows", rows)
                )
            }
        }

        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("tables", tableData)
            .toString(2)
    }

    fun validateBackup(json: String, database: LabDatabase) {
        val root = JSONObject(json)

        require(root.optString("format") == FORMAT) {
            "This is not an SNJ Gold Lab backup."
        }
        val backupVersion = root.optInt("version", -1)
        require(backupVersion in 1..VERSION) {
            "Unsupported backup version."
        }

        val savedTables = root.optJSONObject("tables")
            ?: error("Backup is missing its tables.")

        val db = database.openHelper.readableDatabase

        for (table in tables) {
            val saved = savedTables.optJSONObject(table)
            if (saved == null) {
                val isNewInventoryTable =
                    table == "inventory_items" || table == "inventory_movements"
                if (backupVersion == 1 && isNewInventoryTable) continue
                error("Backup is missing table: $table")
            }

            val savedColumns = saved.optJSONArray("columns")
                ?: error("Missing column information for $table")
            val savedRows = saved.optJSONArray("rows")
                ?: error("Missing rows for $table")

            val currentColumns = db.query("SELECT * FROM `$table` LIMIT 0")
                .use { it.columnNames.toList() }

            val backupColumns = (0 until savedColumns.length())
                .map { savedColumns.getString(it) }

            require(backupColumns == currentColumns) {
                "Backup schema does not match table: $table"
            }

            for (i in 0 until savedRows.length()) {
                val row = savedRows.getJSONObject(i)
                require(row.length() == currentColumns.size) {
                    "Invalid row in table: $table"
                }
                currentColumns.forEach { column ->
                    require(row.has(column)) {
                        "Missing field '$column' in table $table"
                    }
                }
            }
        }
    }

    fun restore(context: Context, database: LabDatabase, json: String): Int {
        validateBackup(json, database)

        // Keep a recoverable copy of the current records before replacing anything.
        val currentBackup = exportJson(database)
        val backupDir = File(context.filesDir, "backups")
        check(backupDir.exists() || backupDir.mkdirs()) {
            "Could not create the safety-backup folder."
        }

        val safetyFile = File(
            backupDir,
            "before_restore_${System.currentTimeMillis()}.json"
        )
        FileOutputStream(safetyFile).use {
            it.write(currentBackup.toByteArray(Charsets.UTF_8))
            it.fd.sync()
        }

        val root = JSONObject(json)
        val savedTables = root.getJSONObject("tables")
        val db = database.openHelper.writableDatabase

        db.beginTransaction()
        try {
            deleteOrder.forEach { table ->
                db.delete(table, null, null)
            }

            for (table in tables) {
                val tableBackup = savedTables.optJSONObject(table) ?: continue
                val rows = tableBackup.getJSONArray("rows")

                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val values = ContentValues()

                    val columns = tableBackup.getJSONArray("columns")

                    for (j in 0 until columns.length()) {
                        val column = columns.getString(j)
                        val value = row.get(column)

                        when (value) {
                            JSONObject.NULL -> values.putNull(column)
                            is String -> values.put(column, value)
                            is Int -> values.put(column, value)
                            is Long -> values.put(column, value)
                            is Double -> values.put(column, value)
                            is Boolean -> values.put(column, if (value) 1 else 0)
                            else -> error("Unsupported value in $table.$column")
                        }
                    }

                    val insertedId = db.insert(
                        table,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT,
                        values
                    )
                    check(insertedId != -1L) {
                        "Could not restore a row in table: $table"
                    }
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        database.invalidationTracker.refreshVersionsAsync()
        return tables.sumOf {
            savedTables.getJSONObject(it).getJSONArray("rows").length()
        }
    }
}
