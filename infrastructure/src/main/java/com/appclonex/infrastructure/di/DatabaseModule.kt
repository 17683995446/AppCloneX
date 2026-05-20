package com.appclonex.infrastructure.di

import android.content.Context
import androidx.room.Room
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration.MIGRATION_1_2
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration.MIGRATION_2_3
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration.MIGRATION_3_4
import com.appclonex.infrastructure.persistence.database.ClonedApplicationDataAccessObject
import com.appclonex.infrastructure.persistence.database.PendingCloneOperationDao
import com.appclonex.infrastructure.persistence.database.ProfileOperationDao
import com.appclonex.infrastructure.persistence.database.UserProfileEntityDataAccessObject
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database Module - 提供 Room 数据库实例
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ApplicationDatabaseConfiguration {
        return Room.databaseBuilder(
            context,
            ApplicationDatabaseConfiguration::class.java,
            "appclonex_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    @Singleton
    fun provideClonedAppDao(database: ApplicationDatabaseConfiguration): ClonedApplicationDataAccessObject {
        return database.clonedAppDao()
    }

    @Provides
    @Singleton
    fun provideUserProfileDao(database: ApplicationDatabaseConfiguration): UserProfileEntityDataAccessObject {
        return database.userProfileDao()
    }

    @Provides
    @Singleton
    fun providePendingCloneOperationDao(database: ApplicationDatabaseConfiguration): PendingCloneOperationDao {
        return database.pendingCloneOperationDao()
    }

    @Provides
    @Singleton
    fun provideProfileOperationDao(database: ApplicationDatabaseConfiguration): ProfileOperationDao {
        return database.profileOperationDao()
    }
}
