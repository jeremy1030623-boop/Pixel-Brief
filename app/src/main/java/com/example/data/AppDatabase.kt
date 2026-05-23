package com.example.data

import androidx.room.*
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
    // System & Permissions
    val is24HourFormat: Boolean = true
)

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 0")
    fun getUserSettings(): Flow<UserSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSettings(settings: UserSettings)
}

@Database(entities = [UserSettings::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSettingsDao(): UserSettingsDao

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
