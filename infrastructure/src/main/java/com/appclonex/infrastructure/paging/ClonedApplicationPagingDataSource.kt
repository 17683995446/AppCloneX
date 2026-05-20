package com.appclonex.infrastructure.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.infrastructure.network.ApplicationNetworkServiceInterface
import com.appclonex.infrastructure.network.ApplicationNetworkResponseModel
import retrofit2.HttpException
import java.io.IOException

/**
 * 克隆应用分页数据源
 * 使用 Paging 3 实现分页加载
 */
class ClonedApplicationPagingDataSource(
    private val networkService: ApplicationNetworkServiceInterface,
    private val userId: Int = 0,
    private val searchQuery: String = ""
) : PagingSource<Int, ClonedApplicationInformationModel>() {

    companion object {
        const val DEFAULT_PAGE_INDEX = 1
        const val DEFAULT_PAGE_SIZE = 20
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ClonedApplicationInformationModel> {
        val page = params.key ?: DEFAULT_PAGE_INDEX

        return try {
            val response = if (searchQuery.isNotEmpty()) {
                networkService.searchApplications(
                    query = searchQuery,
                    page = page,
                    limit = params.loadSize
                )
            } else {
                // 返回空列表，因为无搜索时不需要网络请求
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (page == DEFAULT_PAGE_INDEX) null else page - 1,
                    nextKey = null
                )
            }

            LoadResult.Page(
                data = response.applications.map { it.toDomainModel(userId) },
                prevKey = if (page == DEFAULT_PAGE_INDEX) null else page - 1,
                nextKey = if (response.hasMore) page + 1 else null
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ClonedApplicationInformationModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

/**
 * 网络模型转换为领域模型
 */
private fun ApplicationNetworkResponseModel.toDomainModel(userId: Int): ClonedApplicationInformationModel {
    return ClonedApplicationInformationModel(
        packageName = this.packageName,
        appName = this.appName,
        profileUserId = userId
    )
}
