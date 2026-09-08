package app.piru.android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

class JournalDatabase(context: Context) :
    SQLiteOpenHelper(context, "piru-journal.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE doses (
                id TEXT PRIMARY KEY NOT NULL,
                substance TEXT NOT NULL,
                amount REAL NOT NULL CHECK(amount >= 0),
                unit TEXT NOT NULL,
                route TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                notes TEXT
            )""".trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun addDose(substance: String, amount: Double, unit: String, route: String, notes: String?): DoseEntry {
        require(amount >= 0) { "Dose amount must be non-negative" }
        val entry = DoseEntry(UUID.randomUUID().toString(), substance, amount, unit, route,
            System.currentTimeMillis(), notes)
        val values = ContentValues().apply {
            put("id", entry.id)
            put("substance", entry.substance)
            put("amount", entry.amount)
            put("unit", entry.unit)
            put("route", entry.route)
            put("timestamp", entry.timestamp)
            put("notes", entry.notes)
        }
        writableDatabase.insertOrThrow("doses", null, values)
        return entry
    }

    fun listDoses(): List<DoseEntry> {
        val result = mutableListOf<DoseEntry>()
        readableDatabase.query("doses", null, null, null, null, null, "timestamp DESC").use { cursor ->
            while (cursor.moveToNext()) {
                result += DoseEntry(
                    cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("substance")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    cursor.getString(cursor.getColumnIndexOrThrow("unit")),
                    cursor.getString(cursor.getColumnIndexOrThrow("route")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    cursor.getString(cursor.getColumnIndexOrThrow("notes"))
                )
            }
        }
        return result
    }
}
