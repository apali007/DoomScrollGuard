package com.doomscrollguard.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Active feed time per app per day. [day] is an ISO date, e.g. "2026-11-01". */
@Entity(tableName = "usage", primaryKeys = ["day", "pkg"])
data class UsageEntity(
    val day: String,
    val pkg: String,
    val activeMs: Long,
)

/** One intervention shown to the user and what they did about it. */
@Entity(tableName = "interventions")
data class InterventionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: String,
    val shownAtEpochMs: Long,
    val pkg: String,
    val level: String,
    val outcome: String,
    val reason: String? = null,
)

/** What happened after an intervention was shown. */
enum class Outcome {
    /** Still on screen / waiting to see what the user does. */
    PENDING,
    /** User left the feed (button or on their own shortly after). */
    LEFT,
    /** User chose to keep scrolling. */
    CONTINUED,
    /** Nudge was shown but the user kept going without leaving. */
    IGNORED,
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM usage WHERE day = :day AND pkg = :pkg")
    suspend fun usageFor(day: String, pkg: String): UsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putUsage(usage: UsageEntity)

    /** Adds [ms] to the running total (SQLite on older Androids lacks UPSERT). */
    @Transaction
    suspend fun addUsage(day: String, pkg: String, ms: Long) {
        val current = usageFor(day, pkg)?.activeMs ?: 0L
        putUsage(UsageEntity(day, pkg, current + ms))
    }

    @Query("SELECT * FROM usage WHERE day >= :fromDay")
    fun usageSince(fromDay: String): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage WHERE day >= :fromDay")
    suspend fun usageSinceOnce(fromDay: String): List<UsageEntity>

    @Insert
    suspend fun insertIntervention(intervention: InterventionEntity): Long

    @Query("UPDATE interventions SET outcome = :outcome, reason = :reason WHERE id = :id")
    suspend fun setOutcome(id: Long, outcome: String, reason: String?)

    @Query("SELECT * FROM interventions WHERE day >= :fromDay")
    fun interventionsSince(fromDay: String): Flow<List<InterventionEntity>>

    @Query("SELECT * FROM interventions WHERE day >= :fromDay")
    suspend fun interventionsSinceOnce(fromDay: String): List<InterventionEntity>
}

@Database(entities = [UsageEntity::class, InterventionEntity::class], version = 1, exportSchema = false)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        fun create(context: Context): StatsDatabase =
            Room.databaseBuilder(context, StatsDatabase::class.java, "stats.db").build()
    }
}
