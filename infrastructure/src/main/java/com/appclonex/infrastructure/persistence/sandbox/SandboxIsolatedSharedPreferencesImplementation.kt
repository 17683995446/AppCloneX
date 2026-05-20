package com.appclonex.infrastructure.persistence.sandbox

import android.content.SharedPreferences
import android.os.FileObserver
import android.util.JsonReader
import android.util.JsonWriter
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.utility.AtomicFileWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap

/**
 * 沙盒 SharedPreferences 实现

 * 使用 JSON 文件存储，支持沙盒隔离
 * 新增：FileObserver 实现监听器功能
 */
class SandboxIsolatedSharedPreferencesImplementation(
    private val prefsDir: File,
    private val name: String
) : SharedPreferences, Closeable {

    private val prefsFile = File(prefsDir, "$name.json")
    private val data: MutableMap<String, Any?> = ConcurrentHashMap()
    private val listeners = ConcurrentHashMap.newKeySet<SharedPreferences.OnSharedPreferenceChangeListener>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // FileObserver 监听文件变化
    private var fileObserver: FileObserver? = null

    init {
        loadFromFile()
        startFileObserver()
    }

    /**
     * 启动文件监听器
     */
    private fun startFileObserver() {
        if (fileObserver != null) return

        fileObserver = object : FileObserver(prefsFile.absolutePath, MODIFY or CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MODIFY || event == CLOSE_WRITE) {
                    // 文件被外部修改，重新加载
                    scope.launch {
                        val oldData = data.toMap()
                        loadFromFile()
                        // 通知监听器
                        data.keys.forEach { key ->
                            if (oldData[key] != data[key]) {
                                notifyListeners(key)
                            }
                        }
                    }
                }
            }
        }.apply { startWatching() }
    }

    /**
     * 停止文件监听器
     */
    fun stopWatching() {
        fileObserver?.stopWatching()
        fileObserver = null
        scope.cancel()
    }

    /**
     * 实现 Closeable 接口，释放资源
     * 调用方应在使用完毕后调用此方法以防止内存泄漏
     */
    override fun close() {
        fileObserver?.stopWatching()
        fileObserver = null
        scope.cancel()
    }

    /**
     * 通知所有监听器
     */
    private fun notifyListeners(key: String) {
        listeners.forEach { listener ->
            try {
                listener.onSharedPreferenceChanged(this, key)
            } catch (e: Exception) {
                // 忽略监听器异常
            }
        }
    }

    private fun loadFromFile() {
        if (!prefsFile.exists()) return

        try {
            data.clear()
            JsonReader(InputStreamReader(FileInputStream(prefsFile))).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    val key = reader.nextName()
                    when (reader.peek()) {
                        android.util.JsonToken.STRING -> data[key] = reader.nextString()
                        android.util.JsonToken.NUMBER -> data[key] = reader.nextDouble()
                        android.util.JsonToken.BOOLEAN -> data[key] = reader.nextBoolean()
                        android.util.JsonToken.NULL -> {
                            reader.nextNull()
                            data[key] = null
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(
                "SandboxPrefs",
                "JSON 解析失败，文件可能已损坏: ${prefsFile.absolutePath}",
                e
            )
        }
    }

    private fun saveToFile() {
        try {
            prefsFile.parentFile?.mkdirs()
            // 先将数据序列化为 JSON 字符串，再使用 AtomicFileWriter 原子写入
            val stringWriter = StringWriter()
            JsonWriter(OutputStreamWriter(stringWriter)).use { writer ->
                writer.beginObject()
                data.forEach { (key, value) ->
                    writer.name(key)
                    when (value) {
                        is String -> writer.value(value)
                        is Int -> writer.value(value)
                        is Long -> writer.value(value)
                        is Float -> writer.value(value)
                        is Double -> writer.value(value)
                        is Boolean -> writer.value(value)
                        null -> writer.nullValue()
                        else -> writer.value(value.toString())
                    }
                }
                writer.endObject()
            }
            AtomicFileWriter.write(prefsFile, stringWriter.toString())
        } catch (e: Exception) {
            // 忽略写入错误
        }
    }

    override fun getAll(): Map<String, *> = data.toMap()

    override fun getString(key: String, defValue: String?): String? {
        return data[key] as? String ?: defValue
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        @Suppress("UNCHECKED_CAST")
        return data[key] as? Set<String> ?: defValues
    }

    override fun getInt(key: String, defValue: Int): Int {
        return (data[key] as? Number)?.toInt() ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        return (data[key] as? Number)?.toLong() ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return (data[key] as? Number)?.toFloat() ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return data[key] as? Boolean ?: defValue
    }

    override fun contains(key: String): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = SandboxEditor(this)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        listeners.remove(listener)
    }

    private class SandboxEditor(private val prefs: SandboxIsolatedSharedPreferencesImplementation) : SharedPreferences.Editor {
        private val modifications = ConcurrentHashMap<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            modifications[key] = values
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            modifications[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            modifications[key] = REMOVED_MARKER
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            return try {
                applyChanges()
                true
            } catch (e: Exception) {
                false
            }
        }

        override fun apply() {
            try {
                applyChanges()
            } catch (e: Exception) {
                // 忽略
            }
        }

        private fun applyChanges() {
            val changedKeys = mutableListOf<String>()

            if (clearRequested) {
                changedKeys.addAll(prefs.data.keys)
                prefs.data.clear()
            }

            modifications.forEach { (key, value) ->
                if (value === REMOVED_MARKER) {
                    if (prefs.data.remove(key) != null) {
                        changedKeys.add(key)
                    }
                } else {
                    val oldValue = prefs.data.put(key, value)
                    if (oldValue != value) {
                        changedKeys.add(key)
                    }
                }
            }

            prefs.saveToFile()

            // 通知监听器
            changedKeys.forEach { prefs.notifyListeners(it) }
        }

        companion object {
            private object REMOVED_MARKER
        }
    }
}
