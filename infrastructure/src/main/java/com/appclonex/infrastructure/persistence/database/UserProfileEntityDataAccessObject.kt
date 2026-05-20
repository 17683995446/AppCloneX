package com.appclonex.infrastructure.persistence.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appclonex.domain.model.UserProfileConfigurationModel
import kotlinx.coroutines.flow.Flow

/**
 * 用户配置文件DAO
 */
@Dao
interface UserProfileEntityDataAccessObject {

    @Query("SELECT * FROM user_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<UserProfileConfigurationModel>>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getProfileById(userId: Int): UserProfileConfigurationModel?

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): UserProfileConfigurationModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileConfigurationModel)

    @Update
    suspend fun updateProfile(profile: UserProfileConfigurationModel)

    @Delete
    suspend fun deleteProfile(profile: UserProfileConfigurationModel)

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteProfileById(userId: Int)

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getProfileCount(): Int

    @Query("UPDATE user_profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE user_profiles SET clonedAppCount = :count WHERE userId = :userId")
    suspend fun updateClonedAppCount(userId: Int, count: Int)
}
