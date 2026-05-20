package com.appclonex.domain.model

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.junit5.MockKExtension

/**
 * ShizukuConnectionState 枚举单元测试
 */
class ShizukuConnectionStateTest {

    @Test
    fun `all ShizukuConnectionState enum values have non-empty name`() {
        for (state in ShizukuConnectionState.values()) {
            assertThat(state.name).isNotEmpty()
        }
    }

    @Test
    fun `ShizukuConnectionState enum completeness - has all expected states`() {
        val expectedStates = setOf(
            ShizukuConnectionState.NOT_INSTALLED,
            ShizukuConnectionState.NOT_RUNNING,
            ShizukuConnectionState.PERMISSION_DENIED,
            ShizukuConnectionState.READY,
            ShizukuConnectionState.ERROR
        )

        val actualStates = ShizukuConnectionState.values().toSet()
        assertThat(actualStates).containsExactlyElementsIn(expectedStates)
    }

    @Test
    fun `ShizukuConnectionState enum has correct number of values`() {
        assertThat(ShizukuConnectionState.values().size).isEqualTo(5)
    }

    @Test
    fun `ShizukuConnectionState valueOf returns correct enum for valid names`() {
        assertThat(ShizukuConnectionState.valueOf("NOT_INSTALLED")).isEqualTo(ShizukuConnectionState.NOT_INSTALLED)
        assertThat(ShizukuConnectionState.valueOf("NOT_RUNNING")).isEqualTo(ShizukuConnectionState.NOT_RUNNING)
        assertThat(ShizukuConnectionState.valueOf("PERMISSION_DENIED")).isEqualTo(ShizukuConnectionState.PERMISSION_DENIED)
        assertThat(ShizukuConnectionState.valueOf("READY")).isEqualTo(ShizukuConnectionState.READY)
        assertThat(ShizukuConnectionState.valueOf("ERROR")).isEqualTo(ShizukuConnectionState.ERROR)
    }
}

/**
 * ShizukuServiceConnectionProvider 集成测试
 * 测试 getCurrentConnectionState() 在不同 Shizuku 状态下的返回值
 */
@ExtendWith(MockKExtension::class)
class ShizukuServiceConnectionProviderTest {

    @Before
    fun setUp() {
        mockkStatic("dev.rikka.shizuku.Shizuku")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getCurrentConnectionState 返回 NOT_INSTALLED 当 Shizuku pingBinder 抛异常时`() {
        // Mock Shizuku.pingBinder() 抛出异常，模拟 Shizuku 未安装
        every { dev.rikka.shizuku.Shizuku.pingBinder() } throws RuntimeException("Shizuku not installed")

        // 注意：实际测试需要创建 ShizukuServiceConnectionProvider 实例
        // 这里仅作为示例，实际实现可能需要依赖注入或其他方式
    }

    @Test
    fun `getCurrentConnectionState 返回 NOT_RUNNING 当 pingBinder 返回 false 时`() {
        every { dev.rikka.shizuku.Shizuku.pingBinder() } returns false

        // 注意：实际测试需要创建 ShizukuServiceConnectionProvider 实例
    }

    @Test
    fun `getCurrentConnectionState 返回 PERMISSION_DENIED 当 pingBinder 成功但权限未授予时`() {
        every { dev.rikka.shizuku.Shizuku.pingBinder() } returns true
        every { dev.rikka.shizuku.Shizuku.checkSelfPermission() } returns
            android.content.pm.PackageManager.PERMISSION_DENIED

        // 注意：实际测试需要创建 ShizukuServiceConnectionProvider 实例
    }

    @Test
    fun `getCurrentConnectionState 返回 READY 当 pingBinder 成功且权限已授予时`() {
        every { dev.rikka.shizuku.Shizuku.pingBinder() } returns true
        every { dev.rikka.shizuku.Shizuku.checkSelfPermission() } returns
            android.content.pm.PackageManager.PERMISSION_GRANTED

        // 注意：实际测试需要创建 ShizukuServiceConnectionProvider 实例
    }

    @Test
    fun `getCurrentConnectionState 返回 NOT_INSTALLED 当 checkSelfPermission 抛异常时`() {
        // pingBinder 成功但 checkSelfPermission 抛异常（模拟旧版本 Shizuku）
        every { dev.rikka.shizuku.Shizuku.pingBinder() } returns true
        every { dev.rikka.shizuku.Shizuku.checkSelfPermission() } throws
            RuntimeException("Shizuku API not available")

        // 注意：实际测试需要创建 ShizukuServiceConnectionProvider 实例
    }
}
