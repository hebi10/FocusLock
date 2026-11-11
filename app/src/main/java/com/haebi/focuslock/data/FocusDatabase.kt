package com.haebi.focuslock.data

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
import androidx.room.Update

object FocusSessionStatus {
    const val RUNNING = "RUNNING"
    const val PAUSED = "PAUSED"
    const val COMPLETED = "COMPLETED"
    const val STOPPED = "STOPPED"
}

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val createdAtMillis: Long,
    val finishedAtMillis: Long? = null,
    val durationMinutes: Int,
    val elapsedSeconds: Int = 0,
    val checkpointElapsedRealtime: Long,
    val bootCount: Int,
    val status: String = FocusSessionStatus.RUNNING
)

@Entity(tableName = "focus_rewards")
data class FocusRewardEntity(
    @PrimaryKey val sessionId: String,
    val earnedAtMillis: Long,
    val experience: Int,
    val points: Int
)

@Dao
interface FocusDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: FocusSessionEntity)

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReward(reward: FocusRewardEntity): Long

    @Query("SELECT * FROM focus_sessions WHERE status IN ('RUNNING','PAUSED') ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun activeSession(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun sessionById(id: String): FocusSessionEntity?

    @Query("SELECT * FROM focus_rewards WHERE sessionId = :id LIMIT 1")
    suspend fun rewardBySession(id: String): FocusRewardEntity?

    @Query("SELECT COALESCE(SUM(experience), 0) FROM focus_rewards")
    suspend fun totalExperience(): Int

    @Query("SELECT COALESCE(SUM(experience), 0) FROM focus_rewards WHERE earnedAtMillis >= :fromMillis AND earnedAtMillis < :untilMillis")
    suspend fun experienceBetween(fromMillis: Long, untilMillis: Long): Int

    @Query("SELECT * FROM focus_sessions WHERE finishedAtMillis >= :sinceMillis ORDER BY finishedAtMillis DESC")
    suspend fun finishedSince(sinceMillis: Long): List<FocusSessionEntity>
}

@Database(entities = [FocusSessionEntity::class, FocusRewardEntity::class], version = 1, exportSchema = false)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun focusDao(): FocusDao

    companion object {
        @Volatile private var instance: FocusDatabase? = null

        fun get(context: Context): FocusDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, FocusDatabase::class.java, "focuslock_sessions.db")
                    .build().also { instance = it }
            }
    }
}
