package com.appclonex.infrastructure.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.infrastructure.network.ApplicationNetworkResponseModel
import com.appclonex.infrastructure.network.ApplicationSearchResponseModel
import com.appclonex.infrastructure.network.ApplicationNetworkServiceInterface
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

/**
 * 克隆应用分页数据源单元测试
 * 
 * 测试覆盖：
 * - 分页加载
 * - 错误处理
 * - 刷新逻辑
 */
class ClonedApplicationPagingDataSourceTest {

    private lateinit var networkService: ApplicationNetworkServiceInterface
    private lateinit var pagingSource: ClonedApplicationPagingDataSource

    @Before
    fun setup() {
        networkService = mockk()
    }

    // ==================== 分页加载测试 ====================

    @Test
    fun `load should return first page successfully`() = runBlocking {
        val mockResponse = ApplicationSearchResponseModel(
            applications = listOf(
                createMockApp("com.app1", "App 1"),
                createMockApp("com.app2", "App 2")
            ),
            totalCount = 10,
            currentPage = 1,
            hasMore = true
        )

        coEvery { networkService.searchApplications("test", 1, 20) } returns mockResponse

        pagingSource = ClonedApplicationPagingDataSource(networkService, "test")
        val result = pagingSource.load(PagingSource.LoadParams.Refresh(1, 20, false))

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(2, page.data.size)
        assertEquals("com.app1", page.data[0].packageName)
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun `load should return correct next key when hasMore is false`() = runBlocking {
        val mockResponse = ApplicationSearchResponseModel(
            applications = listOf(createMockApp("com.app1", "App 1")),
            totalCount = 1,
            currentPage = 1,
            hasMore = false
        )

        coEvery { networkService.searchApplications("test", 1, 20) } returns mockResponse

        pagingSource = ClonedApplicationPagingDataSource(networkService, "test")
        val result = pagingSource.load(PagingSource.LoadParams.Refresh(1, 20, false))

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertNull(page.nextKey)
    }

    @Test
    fun `load should return empty list when no search query`() = runBlocking {
        pagingSource = ClonedApplicationPagingDataSource(networkService, "")
        val result = pagingSource.load(PagingSource.LoadParams.Refresh(1, 20, false))

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertTrue(page.data.isEmpty())
        assertNull(page.prevKey)
        assertNull(page.nextKey)
    }

    // ==================== 错误处理测试 ====================

    @Test
    fun `load should return error on IOException`() = runBlocking {
        coEvery { networkService.searchApplications("test", 1, 20) } throws IOException("Network error")

        pagingSource = ClonedApplicationPagingDataSource(networkService, "test")
        val result = pagingSource.load(PagingSource.LoadParams.Refresh(1, 20, false))

        assertTrue(result is PagingSource.LoadResult.Error)
        val error = result as PagingSource.LoadResult.Error
        assertTrue(error.throwable is IOException)
    }

    @Test
    fun `load should return error on HttpException`() = runBlocking {
        val httpException = mockk<HttpException>()
        coEvery { networkService.searchApplications("test", 1, 20) } throws httpException

        pagingSource = ClonedApplicationPagingDataSource(networkService, "test")
        val result = pagingSource.load(PagingSource.LoadParams.Refresh(1, 20, false))

        assertTrue(result is PagingSource.LoadResult.Error)
        val error = result as PagingSource.LoadResult.Error
        assertTrue(error.throwable is HttpException)
    }

    // ==================== 刷新逻辑测试 ====================

    @Test
    fun `getRefreshKey should return correct key`() = runBlocking {
        val state = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(createMockDomainApp("com.app1")),
                    prevKey = null,
                    nextKey = 2
                )
            ),
            anchorPosition = 1,
            config = androidx.paging.PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )

        pagingSource = ClonedApplicationPagingDataSource(networkService, "test")
        val refreshKey = pagingSource.getRefreshKey(state)

        assertEquals(1, refreshKey)
    }

    @Test
    fun `getRefreshKey should return null when no anchor position`() = runBlocking {
        val state = PagingState<Int, ClonedApplicationInformationModel>(
            pages = emptyList(),
            anchorPosition = null,
            config = androidx.paging.PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )

        pagingSource = ClonedApplicationPagingDataSource(networkService, "test")
        val refreshKey = pagingSource.getRefreshKey(state)

        assertNull(refreshKey)
    }

    // ==================== 辅助方法 ====================

    private fun createMockApp(packageName: String, appName: String): ApplicationNetworkResponseModel {
        return ApplicationNetworkResponseModel(
            packageName = packageName,
            appName = appName,
            versionName = "1.0.0",
            versionCode = 1,
            iconUrl = null,
            category = null,
            description = null,
            downloadCount = 1000,
            rating = 4.5f
        )
    }

    private fun createMockDomainApp(packageName: String): ClonedApplicationInformationModel {
        return ClonedApplicationInformationModel(
            packageName = packageName,
            appName = "Test App",
            userId = 0
        )
    }
}
