package com.example.smartlock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartlock.model.Door
import com.example.smartlock.model.ICCard
import com.example.smartlock.model.Passcode
import com.example.smartlock.model.Record

@Database(
    entities = [Door::class, Passcode::class, ICCard::class, Record::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(com.example.smartlock.data.TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doorDao(): DoorDao
    abstract fun passcodeDao(): PasscodeDao
    abstract fun icCardDao(): ICCardDao
    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartlock_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE doors ADD COLUMN masterPasscode TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE records (
                        id TEXT PRIMARY KEY NOT NULL,
                        doorId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        event TEXT NOT NULL,
                        method TEXT NOT NULL,
                        detail TEXT NOT NULL,
                        state TEXT NOT NULL,
                        sourceMqttMessage TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX index_record_doorId ON records(doorId)")
                database.execSQL("CREATE INDEX index_record_timestamp ON records(timestamp DESC)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE doors ADD COLUMN currentState TEXT NOT NULL DEFAULT 'locked'")
            }
        }
    }
}