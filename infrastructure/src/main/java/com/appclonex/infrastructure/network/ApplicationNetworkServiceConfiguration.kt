package com.appclonex.infrastructure.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 应用网络服务接口
 * 使用 Retrofit 实现网络请求
 */
interface ApplicationNetworkServiceInterface {

    /**
     * 获取应用信息
     */
    @GET("api/v1/apps/{packageName}")
    suspend fun getApplicationInformation(
        @Path("packageName") packageName: String
    ): ApplicationNetworkResponseModel

    /**
     * 搜索应用
     */
    @GET("api/v1/apps/search")
    suspend fun searchApplications(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApplicationSearchResponseModel

    /**
     * 获取应用更新信息
     */
    @GET("api/v1/apps/{packageName}/update")
    suspend fun checkApplicationUpdate(
        @Path("packageName") packageName: String,
        @Query("versionCode") currentVersionCode: Long
    ): ApplicationUpdateResponseModel
}

/**
 * 应用网络响应模型
 */
@JsonClass(generateAdapter = true)
data class ApplicationNetworkResponseModel(
    @Json(name = "package_name") val packageName: String,
    @Json(name = "app_name") val appName: String,
    @Json(name = "version_name") val versionName: String,
    @Json(name = "version_code") val versionCode: Long,
    @Json(name = "icon_url") val iconUrl: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "download_count") val downloadCount: Long,
    @Json(name = "rating") val rating: Float
)

/**
 * 应用搜索响应模型
 */
@JsonClass(generateAdapter = true)
data class ApplicationSearchResponseModel(
    @Json(name = "apps") val applications: List<ApplicationNetworkResponseModel>,
    @Json(name = "total") val totalCount: Int,
    @Json(name = "page") val currentPage: Int,
    @Json(name = "has_more") val hasMore: Boolean
)

/**
 * 应用更新响应模型
 */
@JsonClass(generateAdapter = true)
data class ApplicationUpdateResponseModel(
    @Json(name = "has_update") val hasUpdate: Boolean,
    @Json(name = "latest_version") val latestVersion: String?,
    @Json(name = "latest_version_code") val latestVersionCode: Long?,
    @Json(name = "update_log") val updateLog: String?,
    @Json(name = "update_url") val updateUrl: String?
)
