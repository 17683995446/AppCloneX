package com.appclonex.infrastructure.caching

import org.junit.Assert.*
import org.junit.Test

class ApplicationIconModelTest {

    @Test
    fun `ApplicationIconModel stores package name correctly`() {
        val model = ApplicationIconModel("com.example.app")
        assertEquals("com.example.app", model.packageName)
    }

    @Test
    fun `ApplicationIconModel equality works correctly`() {
        val model1 = ApplicationIconModel("com.example.app")
        val model2 = ApplicationIconModel("com.example.app")
        val model3 = ApplicationIconModel("com.other.app")
        
        assertEquals(model1, model2)
        assertNotEquals(model1, model3)
    }

    @Test
    fun `ApplicationIconModel hashCode is consistent`() {
        val model1 = ApplicationIconModel("com.example.app")
        val model2 = ApplicationIconModel("com.example.app")
        
        assertEquals(model1.hashCode(), model2.hashCode())
    }

    @Test
    fun `ApplicationIconModel handles empty package name`() {
        val model = ApplicationIconModel("")
        assertEquals("", model.packageName)
    }
}
