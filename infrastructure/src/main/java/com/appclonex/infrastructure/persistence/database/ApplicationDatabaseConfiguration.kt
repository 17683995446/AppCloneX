package com.appclonex.infrastructure.persistence.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.model.PendingCloneOperation
import com.appclonex.domain.model.ProfileOperation
import com.appclonex.domain.model.UserProfileConfigurationModel

/**
 * AppCloneX Room数据库
 * 数据库实例由 Hilt DI 框架通过 DatabaseModule 提供
 */
@Database(
    entities = [
        UserProfileConfigurationModel::class,
        ClonedApplicationInformationModel::class,
        PendingCloneOperation::class,
        ProfileOperation::class
    ],
    version = DATABASE_VERSION,
    exportSchema = true
)
abstract class ApplicationDatabaseConfiguration : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileEntityDataAccessObject
    abstract fun clonedAppDao(): ClonedApplicationDataAccessObject
    abstract fun pendingCloneOperationDao(): PendingCloneOperationDao
    abstract fun profileOperationDao(): ProfileOperationDao

    companion object {
        const val DATABASE_VERSION = 4

        /**
         * 数据库迁移: v1 → v2
         *
         * 变更内容:
         * - 预留迁移（当前为空迁移）
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes required for v1 → v2
            }
        }

        /**
         * 数据库迁移: v2 → v3
         *
         * 变更内容:
         * - 添加 pending_clone_operations 表，支持幂等性克隆操作
         * - 实现分布式事务的事务日志模式
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建待处理克隆操作表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_clone_operations` (
                        `operationId` TEXT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `profileUserId` INTEGER NOT NULL,
                        `cloneIndex` INTEGER NOT NULL DEFAULT 1,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `errorMessage` TEXT,
                        `retryCount` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`operationId`)
                    )
                """.trimIndent())

                // 创建索引
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_clone_operations_packageName_profileUserId`
                    ON `pending_clone_operations` (`packageName`, `profileUserId`)
                """.trimIndent())

                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_pending_clone_operations_status`
                    ON `pending_clone_operations` (`status`)
                """.trimIndent())
            }
        }

        /**
         * 数据库迁移: v3 → v4
         *
         * 变更内容:
         * - 添加 profile_operations 表，支持Saga模式
         * - 实现配置文件创建/删除的分布式事务
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建配置文件操作表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `profile_operations` (
                        `operationId` TEXT NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `userId` INTEGER NOT NULL DEFAULT -1,
                        `profileName` TEXT,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `errorMessage` TEXT,
                        `retryCount` INTEGER NOT NULL DEFAULT 0,
                        `compensationAttempts` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`operationId`)
                    )
                """.trimIndent())

                // 创建索引
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_profile_operations_userId`
                    ON `profile_operations` (`userId`)
                """.trimIndent())

                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_profile_operations_status`
                    ON `profile_operations` (`status`)
                """.trimIndent())

                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_profile_operations_operationType`
                    ON `profile_operations` (`operationType`)
                """.trimIndent())
            }
        }
    }
}
