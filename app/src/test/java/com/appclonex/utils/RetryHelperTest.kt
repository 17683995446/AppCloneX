package com.appclonex.utils

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * RetryHelper 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RetryHelperTest {

    @Test
    fun `withRetry returns result on first successful attempt`() = runTest {
        var callCount = 0
        val result = RetryHelper.withRetry(times = 3, initialDelay = 0) {
            callCount++
            "success"
        }

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("success")
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `withRetry retries specified times before succeeding`() = runTest {
        var callCount = 0
        val result = RetryHelper.withRetry(times = 3, initialDelay = 0) {
            callCount++
            if (callCount < 3) {
                throw IOException("Temporary failure")
            }
            "success"
        }

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("success")
        assertThat(callCount).isEqualTo(3)
    }

    @Test
    fun `withRetry returns failure after exhausting all retries`() = runTest {
        var callCount = 0
        val result = RetryHelper.withRetry(times = 3, initialDelay = 0) {
            callCount++
            throw IOException("Persistent failure")
        }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(callCount).isEqualTo(3)
    }

    @Test
    fun `withRetryIf does not retry when condition is not met`() = runTest {
        var callCount = 0
        val result = RetryHelper.withRetryIf(
            times = 3,
            initialDelay = 0,
            retryIf = { e -> e is IllegalStateException } // Only retry on IllegalStateException
        ) {
            callCount++
            throw IOException("Not retryable")
        }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        // Should only be called once because IOException doesn't meet the retry condition
        assertThat(callCount).isEqualTo(1)
    }
}
