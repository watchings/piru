package app.piru.android

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "dose_entries")
data class RoomDoseEntry(
    @androidx.room.PrimaryKey val id: String,
    val substance: String,
    val amount: Double,
    val unit: String,
    val route: String,
    val timestamp: Long,
    val notes: String?
)

@Entity(tableName = "sessions")
data class RoomSession(
    @androidx.room.PrimaryKey val id: String,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val notes: String?
)

@Entity(tableName = "daily_dose_items")
data class RoomDailyDoseItem(
    @androidx.room.PrimaryKey val id: String,
    val substance: String,
    val amount: Double,
    val unit: String,
    val timestamp: Long,
    val completed: Boolean
)

@Entity(tableName = "inventory_items")
data class RoomInventoryItem(
    @androidx.room.PrimaryKey val id: String,
    val substance: String,
    val amount: Double,
    val unit: String,
    val notes: String?
)

@Entity(tableName = "tolerance_states")
data class RoomToleranceState(
    @androidx.room.PrimaryKey val substance: String,
    val acute: Double,
    val adaptive: Double,
    val deep: Double,
    val synthesis: Double,
    val updatedTimestamp: Long
)

@Entity(tableName = "favorites")
data class RoomFavorite(@androidx.room.PrimaryKey val substance: String)

@Entity(tableName = "substance_colors")
data class RoomSubstanceColor(
    @androidx.room.PrimaryKey val substance: String,
    val color: String
)

@Dao
interface RoomJournalDao {
    @Query("SELECT * FROM dose_entries ORDER BY timestamp DESC")
    suspend fun list(): List<RoomDoseEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: RoomDoseEntry)

    @Query("DELETE FROM dose_entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(
    entities = [
        RoomDoseEntry::class, RoomSession::class, RoomDailyDoseItem::class,
        RoomInventoryItem::class, RoomToleranceState::class, RoomFavorite::class,
        RoomSubstanceColor::class
    ],
    version = 2,
    exportSchema = true
)
abstract class PiruRoomDatabase : RoomDatabase() {
    abstract fun journal(): RoomJournalDao
}

val ROOM_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS sessions (" +
                "id TEXT NOT NULL PRIMARY KEY, startTimestamp INTEGER NOT NULL, " +
                "endTimestamp INTEGER, notes TEXT)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS daily_dose_items (" +
                "id TEXT NOT NULL PRIMARY KEY, substance TEXT NOT NULL, amount REAL NOT NULL, " +
                "unit TEXT NOT NULL, timestamp INTEGER NOT NULL, completed INTEGER NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS inventory_items (" +
                "id TEXT NOT NULL PRIMARY KEY, substance TEXT NOT NULL, amount REAL NOT NULL, " +
                "unit TEXT NOT NULL, notes TEXT)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS tolerance_states (" +
                "substance TEXT NOT NULL PRIMARY KEY, acute REAL NOT NULL, adaptive REAL NOT NULL, " +
                "deep REAL NOT NULL, synthesis REAL NOT NULL, updatedTimestamp INTEGER NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS favorites (substance TEXT NOT NULL PRIMARY KEY)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS substance_colors (" +
                "substance TEXT NOT NULL PRIMARY KEY, color TEXT NOT NULL)"
        )
    }
}
