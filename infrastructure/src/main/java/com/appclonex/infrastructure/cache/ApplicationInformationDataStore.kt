package com.appclonex.infrastructure.cache

import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.infrastructure.network.ApplicationNetworkResponseModel
import com.appclonex.infrastructure.network.ApplicationNetworkServiceInterface
import org.mobilenativefoundation.store.store5.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用信息数据存储
 * 使用 Store 5 实现智能缓存
 */
@Singleton
class ApplicationInformationDataStore @Inject constructor(
    private val networkService: ApplicationNetworkServiceInterface
) {

    private val store: Store<String, ClonedApplicationInformationModel> = StoreBuilder
        .from(
            fetcher = Fetcher.of { packageName: String ->
                networkService.getApplicationInformation(packageName).toDomainModel()
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { packageName ->
                    // 从本地缓存读取（这里简化实现）
                    null
                },
                writer = { packageName, data ->
                    // 写入本地缓存（这里简化实现）
                },
                delete = { packageName ->
                    // 删除缓存
                },
                deleteAll = {
                    // 清空所有缓存
                }
            )
        )
        .cachePolicy(
            MemoryPolicy.builder()
                .setMaxSize(100)
                .setExpireAfterWrite(60 * 60 * 1000) // 1小时过期
                .build()
        )
        .build()

    /**
     * 获取应用信息
     */
    suspend fun getApplicationInformation(
        packageName: String,
        refresh: Boolean = false
    ): ClonedApplicationInformationModel {
        return if (refresh) {
            store.fresh(packageName)
        } else {
            store.get(packageName)
        }
    }

    /**
     * 清除缓存
     */
    suspend fun clearCache(packageName: String) {
        store.clear(packageName)
    }

    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache() {
        store.clear()
    }
}

/**
 * 网络模型转换为领域模型
 */
private fun ApplicationNetworkResponseModel.toDomainModel(): ClonedApplicationInformationModel {
    return ClonedApplicationInformationModel(
        packageName = this.packageName,
        appName = this.appName,
        userId = 0
    )
}
