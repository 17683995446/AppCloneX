package com.appclonex.infrastructure.persistence.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appclonex.domain.model.ClonedApplicationInformationModel
import kotlinx.coroutines.flow.Flow

/**
 * 克隆应用DAO
 */
@Dao
interface ClonedApplicationDataAccessObject {

    @Query("SELECT * FROM cloned_apps WHERE profileUserId = :profileUserId ORDER BY appName ASC")
    fun getAppsByProfile(profileUserId: Int): Flow<List<ClonedApplicationInformationModel>>

    @Query("SELECT * FROM cloned_apps ORDER BY appName ASC")
    fun getAllClonedApps(): Flow<List<ClonedApplicationInformationModel>>

    @Query("SELECT * FROM cloned_apps WHERE id = :id")
    suspend fun getAppById(id: Long): ClonedApplicationInformationModel?

    @Query("SELECT * FROM cloned_apps WHERE packageName = :packageName AND profileUserId = :profileUserId")
    suspend fun getAppByPackageAndUser(packageName: String, profileUserId: Int): ClonedApplicationInformationModel?

    @Query("SELECT * FROM cloned_apps WHERE packageName = :packageName AND profileUserId = :profileUserId AND cloneIndex = :cloneIndex")
    suspend fun getAppByPackageAndProfileAndIndex(packageName: String, profileUserId: Int, cloneIndex: Int): ClonedApplicationInformationModel?

    @Query("SELECT COUNT(*) FROM cloned_apps WHERE packageName = :packageName")
    suspend fun getCloneCount(packageName: String): Int

    /**
     * 获取指定配置文件中某包名的克隆数量（用于分配cloneIndex）
     */
    @Query("SELECT COUNT(*) FROM cloned_apps WHERE packageName = :packageName AND profileUserId = :profileUserId")
    suspend fun getCloneCountByPackageAndProfile(packageName: String, profileUserId: Int): Int

    @Query("SELECT * FROM cloned_apps WHERE packageName = :packageName ORDER BY profileUserId ASC")
    suspend fun getAppsByPackageName(packageName: String): List<ClonedApplicationInformationModel>

    @Query("SELECT COUNT(*) FROM cloned_apps WHERE profileUserId = :profileUserId")
    suspend fun getAppCountByProfile(profileUserId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: ClonedApplicationInformationModel): Long

    @Update
    suspend fun updateApp(app: ClonedApplicationInformationModel)

    @Delete
    suspend fun deleteApp(app: ClonedApplicationInformationModel)

    @Query("DELETE FROM cloned_apps WHERE id = :id")
    suspend fun deleteAppById(id: Long)

    @Query("DELETE FROM cloned_apps WHERE profileUserId = :profileUserId")
    suspend fun deleteAppsByProfile(profileUserId: Int)

    @Query("DELETE FROM cloned_apps WHERE packageName = :packageName AND profileUserId = :profileUserId")
    suspend fun deleteAppByPackageAndUser(packageName: String, profileUserId: Int)

    @Query("UPDATE cloned_apps SET lastLaunchTime = :time, launchCount = launchCount + 1 WHERE id = :id")
    suspend fun updateLaunchStats(id: Long, time: Long)

    @Query("SELECT COALESCE(SUM(size), 0) FROM cloned_apps WHERE profileUserId = :profileUserId")
    suspend fun getTotalSizeByProfile(profileUserId: Int): Long

    /**
     * 分页查询克隆应用
     * 
     * @param limit 每页数量，必须大于0
     * @param offset 偏移量，必须大于等于0
     */
    @Query("SELECT * FROM cloned_apps ORDER BY appName ASC LIMIT :limit OFFSET :offset")
    suspend fun getClonedAppsPaged(limit: Int, offset: Int): List<ClonedApplicationInformationModel>

    /**
     * 分页查询指定配置文件的克隆应用
     * 
     * @param profileUserId 配置文件用户ID
     * @param limit 每页数量，必须大于0
     * @param offset 偏移量，必须大于等于0
     */
    @Query("SELECT * FROM cloned_apps WHERE profileUserId = :profileUserId ORDER BY appName ASC LIMIT :limit OFFSET :offset")
    suspend fun getAppsByProfilePaged(profileUserId: Int, limit: Int, offset: Int): List<ClonedApplicationInformationModel>

    /**
     * 获取总记录数
     */
    @Query("SELECT COUNT(*) FROM cloned_apps")
    suspend fun getTotalCount(): Int

    /**
     * 获取指定配置文件的应用数量
     */
    @Query("SELECT COUNT(*) FROM cloned_apps WHERE profileUserId = :profileUserId")
    suspend fun getCountByProfile(profileUserId: Int): Int

    /**
     * 获取指定配置文件的所有克隆应用列表（非Flow）
     * 用于配置导入导出功能
     */
    @Query("SELECT * FROM cloned_apps WHERE profileUserId = :profileUserId ORDER BY appName ASC")
    suspend fun getAppsByProfileList(profileUserId: Int): List<ClonedApplicationInformationModel>
}
