package com.example.smartlock.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase{
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "smart_lock_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideDoorDao(database: AppDatabase) = database.doorDao()

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase) = database.userDao()

    @Provides
    @Singleton
    fun provideDoorRecordDao(database: AppDatabase) = database.doorRecordDao()

    @Provides
    @Singleton
    fun provideDoorShareDao(database: AppDatabase) = database.doorShareDao()

    @Provides
    @Singleton
    fun providePasscodeDao(database: AppDatabase) = database.passcodeDao()

    @Provides
    @Singleton
    fun provideICCardDao(database: AppDatabase) = database.icCardDao()

    @Provides
    @Singleton
    fun provideDoorCommandDao(database: AppDatabase) = database.doorCommandDao()
}