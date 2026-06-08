package com.example.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 0,
    val username: String = "Jeremy",
    val defaultCity: String = "台北",
    val isSleepSynced: Boolean = false,
    val sleepHours: Float = 0f,
    val sleepSnoringMinutes: Int = 0,
    val sleepCoughCount: Int = 0,
    val lastSyncTime: String = "",
    val dailySteps: Int = 0,
    val avgHeartRate: Int = 0,
    val healthTrendReport: String = "",
    // Weather & Location
    val preciseLocationEnabled: Boolean = true,
    val weatherUnit: String = "C",
    val showFloatingBackButton: Boolean = false,
    // Sleep & Health
    val isHealthSyncEnabled: Boolean = false,
    val isBreathingTrackingEnabled: Boolean = false,
    val isCoughColorAlertEnabled: Boolean = false,
    // Calendar & Tasks
    val selectedCalendarAccount: String = "jeremy1030623@gmail.com",
    val tasksIntegrationEnabled: Boolean = false,
    val aiActivityAnalysisEnabled: Boolean = false,
    // News Feed
    val displayedNewsCount: Int = 3,
    val newsMode: String = "local", // "local" or "international"
    // System & Permissions
    val is24HourFormat: Boolean = true,
    // Gemini Settings
    val geminiModelSelected: String = "gemini-1.5-flash",
    // SystemClock Sync Timing
    val sleepSyncDurationMs: Long = 0,
    // Custom Avatar Settings
    val avatarEmoji: String = "🦊",
    val avatarGradientIndex: Int = 0,
    // Security & Biometrics
    val isBiometricEnabled: Boolean = false,
    // Language & TTS Settings (TTS removed, but used for UI translation)
    val ttsLanguage: String = "system_default"
)

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 0")
    fun getUserSettings(): Flow<UserSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSettings(settings: UserSettings)
}

@Entity(tableName = "task_items")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TaskItemDao {
    @Query("SELECT * FROM task_items ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem)

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM task_items")
    suspend fun clearAllTasks()
}

@Entity(tableName = "sleep_data")
data class SleepData(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val durationHours: Float,
    val sleepQuality: Int // 1-10
)

@Dao
interface SleepDataDao {
    @Query("SELECT * FROM sleep_data ORDER BY date DESC LIMIT 7")
    fun getRecentSleepData(): Flow<List<SleepData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepData(data: SleepData)
}

@Database(entities = [UserSettings::class, TaskItem::class, SleepData::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun taskItemDao(): TaskItemDao
    abstract fun sleepDataDao(): SleepDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "morning_briefing_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
