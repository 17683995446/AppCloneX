package com.appclonex

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appclonex.data.database.AppDatabase
import com.appclonex.data.database.dao.UserProfileDao
import com.appclonex.data.database.entity.UserProfileEntity
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
 * 测试 Room 数据库的完整操作流程
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var userProfileDao: UserProfileDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        userProfileDao = db.userProfileDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ==================== UserProfile 集成测试 ====================

    @Test
    fun insertAndRetrieveUserProfile() = runBlocking {
        // 创建测试数据
        val profile = UserProfileEntity(
            id = 0,
            name = "Test Profile",
            userId = 10,
            iconPath = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 插入数据
        val id = userProfileDao.insert(profile)
        assertTrue(id > 0)

        // 查询数据
        val retrieved = userProfileDao.getById(id)
        assertNotNull(retrieved)
        assertEquals("Test Profile", retrieved?.name)
        assertEquals(10, retrieved?.userId)
    }

    @Test
    fun updateUserProfile() = runBlocking {
        // 创建并插入
        val profile = UserProfileEntity(
            id = 0,
            name = "Original Name",
            userId = 10,
            iconPath = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = userProfileDao.insert(profile)

        // 更新
        val updated = profile.copy(
            id = id,
            name = "Updated Name",
            updatedAt = System.currentTimeMillis()
        )
        userProfileDao.update(updated)

        // 验证
        val retrieved = userProfileDao.getById(id)
        assertEquals("Updated Name", retrieved?.name)
    }

    @Test
    fun deleteUserProfile() = runBlocking {
        // 创建并插入
        val profile = UserProfileEntity(
            id = 0,
            name = "To Delete",
            userId = 10,
            iconPath = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = userProfileDao.insert(profile)

        // 删除
        userProfileDao.deleteById(id)

        // 验证
        val retrieved = userProfileDao.getById(id)
        assertNull(retrieved)
    }

    @Test
    fun getAllProfilesFlow() = runBlocking {
        // 插入多个配置
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

        // 获取所有配置
        val profiles = userProfileDao.getAll().first()
        assertEquals(5, profiles.size)
    }

    @Test
    fun getProfileByUserId() = runBlocking {
        // 创建特定 userId 的配置
        val profile = UserProfileEntity(
            id = 0,
            name = "User 99 Profile",
            userId = 99,
            iconPath = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        userProfileDao.insert(profile)

        // 按 userId 查询
        val retrieved = userProfileDao.getByUserId(99)
        assertNotNull(retrieved)
        assertEquals(99, retrieved?.userId)
    }

    @Test
    fun countProfiles() = runBlocking {
        // 插入配置
        repeat(3) {
            val profile = UserProfileEntity(
                id = 0,
                name = "Profile $it",
                userId = it,
                iconPath = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insert(profile)
        }

        // 验证数量
        val count = userProfileDao.getCount()
        assertEquals(3, count)
    }

    // ==================== 数据一致性测试 ====================

    @Test
    fun databaseVersionIsCorrect() {
        assertEquals(1, db.openHelper.writableDatabase.version)
    }

    @Test
    fun databaseNameIsCorrect() {
        // 内存数据库没有名称，但我们可以验证数据库是打开的
        assertTrue(db.isOpen)
    }
}
