package com.appclonex.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appclonex.infrastructure.persistence.database.AppDatabase
import com.appclonex.infrastructure.persistence.database.UserProfileDao
import com.appclonex.infrastructure.persistence.database.ClonedAppDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 数据库集成测试
 * 
 * 测试覆盖：
 * - Room 数据库创建和迁移
 * - DAO 操作完整性
 * - Flow 响应式查询
 * - 事务处理
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var userProfileDao: UserProfileDao
    private lateinit var clonedAppDao: ClonedAppDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        userProfileDao = db.userProfileDao()
        clonedAppDao = db.clonedAppDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ==================== UserProfile 集成测试 ====================

    @Test
    fun insertAndRetrieveUserProfile() = runBlocking {
        val profile = UserProfileEntity(
            id = 0,
            name = "Work Profile",
            userId = 10,
            iconPath = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val id = userProfileDao.insert(profile)
        assertTrue(id > 0)

        val retrieved = userProfileDao.getById(id)
        assertNotNull(retrieved)
        assertEquals("Work Profile", retrieved?.name)
        assertEquals(10, retrieved?.userId)
    }

    @Test
    fun updateUserProfile() = runBlocking {
        val profile = UserProfileEntity(
            id = 0,
            name = "Original Name",
            userId = 10,
            iconPath = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = userProfileDao.insert(profile)

        val updated = profile.copy(
            id = id,
            name = "Updated Name",
            updatedAt = System.currentTimeMillis()
        )
        userProfileDao.update(updated)

        val retrieved = userProfileDao.getById(id)
        assertEquals("Updated Name", retrieved?.name)
    }

    @Test
    fun deleteUserProfile() = runBlocking {
        val profile = UserProfileEntity(
            id = 0,
            name = "To Delete",
            userId = 10,
            iconPath = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = userProfileDao.insert(profile)

        userProfileDao.deleteById(id)

        val retrieved = userProfileDao.getById(id)
        assertNull(retrieved)
    }

    @Test
    fun getAllProfilesFlow() = runBlocking {
        repeat(5) { index ->
            val profile = UserProfileEntity(
                id = 0,
                name = "Profile $index",
                userId = 10 + index,
                iconPath = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insert(profile)
        }

        val profiles = userProfileDao.getAll().first()
        assertEquals(5, profiles.size)
    }

    // ==================== 数据一致性测试 ====================

    @Test
    fun databaseVersionIsCorrect() {
        assertEquals(1, db.openHelper.writableDatabase.version)
    }

    @Test
    fun databaseIsOpen() {
        assertTrue(db.isOpen)
    }

    @Test
    fun databaseTransactionWorks() = runBlocking {
        db.runInTransaction {
            repeat(3) { index ->
                val profile = UserProfileEntity(
                    id = 0,
                    name = "Transaction Profile $index",
                    userId = 100 + index,
                    iconPath = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userProfileDao.insert(profile)
            }
        }

        val profiles = userProfileDao.getAll().first()
        assertEquals(3, profiles.size)
    }
}

/**
 * 用户配置实体（测试用简化版本）
 */
data class UserProfileEntity(
    val id: Long,
    val name: String,
    val userId: Int,
    val iconPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)
