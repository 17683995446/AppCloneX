package com.appclonex.ui.viewmodels

import com.appclonex.data.model.ClonedApp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * FreezerUiState 单元测试
 */
class FreezerUiStateTest {

    // ==================== 默认值验证 ====================

    @Test
    fun `FreezerUiState 默认值验证`() {
        val state = FreezerUiState()

        assertThat(state.clonedApps).isEmpty()
        assertThat(state.frozenApps).isEmpty()
        assertThat(state.frozenCount).isEqualTo(0)
        assertThat(state.totalCount).isEqualTo(0)
        assertThat(state.currentUserId).isEqualTo(0)
        assertThat(state.isProcessing).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.message).isNull()
        assertThat(state.error).isNull()
    }

    // ==================== frozenCount 计算 ====================

    @Test
    fun `frozenCount 为 0 当 frozenApps 为空时`() {
        val state = FreezerUiState(frozenApps = emptyMap())
        assertThat(state.frozenCount).isEqualTo(0)
    }

    @Test
    fun `frozenCount 正确计算全部冻结的应用数量`() {
        val frozenApps = mapOf(
            "com.app1" to true,
            "com.app2" to true,
            "com.app3" to true
        )
        val state = FreezerUiState(frozenApps = frozenApps, frozenCount = 3)
        assertThat(state.frozenCount).isEqualTo(3)
    }

    @Test
    fun `frozenCount 正确计算部分冻结的应用数量`() {
        val frozenApps = mapOf(
            "com.app1" to true,
            "com.app2" to false,
            "com.app3" to true,
            "com.app4" to false,
            "com.app5" to true
        )
        val expectedCount = frozenApps.count { it.value }
        val state = FreezerUiState(frozenApps = frozenApps, frozenCount = expectedCount)
        assertThat(state.frozenCount).isEqualTo(3)
    }

    @Test
    fun `frozenCount 为 0 当所有应用都未冻结时`() {
        val frozenApps = mapOf(
            "com.app1" to false,
            "com.app2" to false,
            "com.app3" to false
        )
        val state = FreezerUiState(frozenApps = frozenApps, frozenCount = 0)
        assertThat(state.frozenCount).isEqualTo(0)
    }

    @Test
    fun `frozenCount 与 frozenApps 中 value 为 true 的数量一致`() {
        // 随机混合 true/false
        val frozenApps = mapOf(
            "com.a" to true,
            "com.b" to false,
            "com.c" to true,
            "com.d" to true,
            "com.e" to false,
            "com.f" to false,
            "com.g" to true
        )
        val expectedCount = frozenApps.values.count { it }
        val state = FreezerUiState(frozenApps = frozenApps, frozenCount = expectedCount)
        assertThat(state.frozenCount).isEqualTo(expectedCount)
        assertThat(state.frozenCount).isEqualTo(4)
    }

    // ==================== copy() 保持不变值 ====================

    @Test
    fun `copy 保持 clonedApps 不变`() {
        val apps = listOf(
            ClonedApp(packageName = "com.app1", appName = "App1", profileUserId = 1),
            ClonedApp(packageName = "com.app2", appName = "App2", profileUserId = 1)
        )
        val state = FreezerUiState(clonedApps = apps, totalCount = 2)
        val copied = state.copy(isProcessing = true)

        assertThat(copied.clonedApps).isEqualTo(apps)
        assertThat(copied.clonedApps).hasSize(2)
        assertThat(copied.totalCount).isEqualTo(2)
        assertThat(copied.isProcessing).isTrue()
    }

    @Test
    fun `copy 保持 frozenApps 和 frozenCount 不变`() {
        val frozenApps = mapOf("com.app1" to true, "com.app2" to false)
        val state = FreezerUiState(frozenApps = frozenApps, frozenCount = 1)
        val copied = state.copy(message = "已冻结")

        assertThat(copied.frozenApps).isEqualTo(frozenApps)
        assertThat(copied.frozenCount).isEqualTo(1)
        assertThat(copied.message).isEqualTo("已冻结")
    }

    @Test
    fun `copy 保持 isLoading 和 error 不变`() {
        val state = FreezerUiState(isLoading = true, error = "加载失败")
        val copied = state.copy(isLoading = false)

        assertThat(copied.isLoading).isFalse()
        assertThat(copied.error).isEqualTo("加载失败")
    }

    @Test
    fun `copy 多次修改保持其他字段不变`() {
        val state = FreezerUiState(
            clonedApps = listOf(
                ClonedApp(packageName = "com.app", appName = "App", profileUserId = 1)
            ),
            frozenApps = mapOf("com.app" to true),
            frozenCount = 1,
            totalCount = 1,
            currentUserId = 1,
            isProcessing = false,
            isLoading = false,
            message = null,
            error = null
        )

        val copied = state.copy(
            isProcessing = true,
            message = "处理中"
        )

        assertThat(copied.clonedApps).hasSize(1)
        assertThat(copied.frozenApps).containsEntry("com.app", true)
        assertThat(copied.frozenCount).isEqualTo(1)
        assertThat(copied.totalCount).isEqualTo(1)
        assertThat(copied.currentUserId).isEqualTo(1)
        assertThat(copied.isProcessing).isTrue()
        assertThat(copied.isLoading).isFalse()
        assertThat(copied.message).isEqualTo("处理中")
        assertThat(copied.error).isNull()
    }

    // ==================== data class 行为验证 ====================

    @Test
    fun `FreezerUiState equals 正确比较`() {
        val state1 = FreezerUiState(
            frozenApps = mapOf("com.app" to true),
            frozenCount = 1
        )
        val state2 = FreezerUiState(
            frozenApps = mapOf("com.app" to true),
            frozenCount = 1
        )
        val state3 = FreezerUiState(
            frozenApps = mapOf("com.app" to false),
            frozenCount = 0
        )

        assertThat(state1).isEqualTo(state2)
        assertThat(state1).isNotEqualTo(state3)
    }

    @Test
    fun `FreezerUiState 设置 message 和 error`() {
        val state = FreezerUiState(message = "操作成功", error = null)
        assertThat(state.message).isEqualTo("操作成功")
        assertThat(state.error).isNull()

        val errorState = FreezerUiState(error = "操作失败", message = null)
        assertThat(errorState.error).isEqualTo("操作失败")
        assertThat(errorState.message).isNull()
    }
}
