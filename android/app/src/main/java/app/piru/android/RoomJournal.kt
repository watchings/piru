package app.piru.android

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase

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

@Dao
interface RoomJournalDao {
    @Query("SELECT * FROM dose_entries ORDER BY timestamp DESC")
    suspend fun list(): List<RoomDoseEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: RoomDoseEntry)

    @Query("DELETE FROM dose_entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [RoomDoseEntry::class], version = 1, exportSchema = true)
abstract class PiruRoomDatabase : RoomDatabase() {
    abstract fun journal(): RoomJournalDao
}
