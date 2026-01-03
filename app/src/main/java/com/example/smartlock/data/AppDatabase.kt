package com.example.smartlock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smartlock.model.entity.Door
import com.example.smartlock.model.entity.DoorCommand
import com.example.smartlock.model.entity.DoorRecord
import com.example.smartlock.model.entity.DoorShare
import com.example.smartlock.model.entity.ICCard
import com.example.smartlock.model.entity.Passcode
import com.example.smartlock.model.entity.User

@Database(
    entities = [
        User::class,
        Door::class,
        Passcode::class,
        ICCard::class,
        DoorRecord::class,
        DoorCommand::class,
        DoorShare::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(com.example.smartlock.data.TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun doorDao(): DoorDao
    abstract fun passcodeDao(): PasscodeDao
    abstract fun icCardDao(): ICCardDao
    abstract fun doorRecordDao(): DoorRecordDao
    abstract fun doorCommandDao(): DoorCommandDao
    abstract fun doorShareDao(): DoorShareDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartlock_database"
                )
                    .addMigrations()
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}