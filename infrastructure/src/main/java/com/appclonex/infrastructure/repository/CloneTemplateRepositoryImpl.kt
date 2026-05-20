package com.appclonex.infrastructure.repository

import android.content.Context
import com.appclonex.domain.model.BuiltInTemplates
import com.appclonex.domain.model.CloneTemplate
import com.appclonex.domain.repository.CloneTemplateRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 克隆模板仓库实现
 * 使用JSON文件存储自定义模板，内置模板在初始化时合并加载
 *
 * 存储路径: context.filesDir/clone_templates.json
 * 仅持久化用户自定义模板，内置模板每次启动时重新加载
 */
@Singleton
class CloneTemplateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CloneTemplateRepository {

    companion object {
        private const val TAG = "CloneTemplateRepo"
        private const val FILE_NAME = "clone_templates.json"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * ID生成器，确保自定义模板ID唯一
     * 初始值设为内置模板最大ID + 1，避免与内置模板ID冲突
     */
    private val idGenerator = AtomicLong(BuiltInTemplates.all.maxOf { it.id } + 1)

    /**
     * 互斥锁，保护 templatesFlow 的读-改-写操作
     */
    private val mutex = Mutex()

    /**
     * 内存中的所有模板列表（内置 + 自定义）
     */
    private val templatesFlow = MutableStateFlow<List<CloneTemplate>>(emptyList())

    /**
     * 自定义模板的持久化文件
     */
    private val storageFile: File
        get() = File(context.filesDir, FILE_NAME)

    init {
        loadTemplates()
    }

    // ==================== 公开接口实现 / Public Interface Implementation ====================

    override fun getAllTemplates(): Flow<List<CloneTemplate>> {
        return templatesFlow.asStateFlow()
    }

    override suspend fun getTemplateById(id: Long): CloneTemplate? {
        return templatesFlow.value.find { it.id == id }
    }

    override suspend fun getBuiltInTemplates(): List<CloneTemplate> {
        return BuiltInTemplates.all
    }

    override suspend fun getCustomTemplates(): List<CloneTemplate> {
        return templatesFlow.value.filter { !it.isBuiltIn }
    }

    override suspend fun insertTemplate(template: CloneTemplate): Long {
        return mutex.withLock {
            try {
                val newId = idGenerator.getAndIncrement()
                val newTemplate = template.copy(
                    id = newId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val currentTemplates = templatesFlow.value.toMutableList()
                currentTemplates.add(newTemplate)
                templatesFlow.value = currentTemplates

                persistCustomTemplates()
                ApplicationLoggingServiceManager.i(TAG, "模板已创建: ${newTemplate.name} (ID: $newId)")
                newId
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "创建模板失败", e)
                throw e
            }
        }
    }

    override suspend fun updateTemplate(template: CloneTemplate) {
        mutex.withLock {
            try {
                val existingTemplate = templatesFlow.value.find { it.id == template.id }
                    ?: throw IllegalArgumentException("模板不存在: ID=${template.id}")

                // 不允许修改内置模板的核心属性
                val updatedTemplate = if (existingTemplate.isBuiltIn) {
                    existingTemplate.copy(
                        options = template.options,
                        appPackages = template.appPackages,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    template.copy(updatedAt = System.currentTimeMillis())
                }

                val currentTemplates = templatesFlow.value.map {
                    if (it.id == updatedTemplate.id) updatedTemplate else it
                }
                templatesFlow.value = currentTemplates

                persistCustomTemplates()
                ApplicationLoggingServiceManager.i(TAG, "模板已更新: ${updatedTemplate.name} (ID: ${updatedTemplate.id})")
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "更新模板失败: ID=${template.id}", e)
                throw e
            }
        }
    }

    override suspend fun deleteTemplate(id: Long) {
        mutex.withLock {
            try {
                val template = templatesFlow.value.find { it.id == id }
                    ?: throw IllegalArgumentException("模板不存在: ID=$id")

                if (template.isBuiltIn) {
                    ApplicationLoggingServiceManager.w(TAG, "内置模板不可删除: ${template.name}")
                    throw IllegalStateException("内置模板不可删除: ${template.name}")
                }

                val currentTemplates = templatesFlow.value.filter { it.id != id }
                templatesFlow.value = currentTemplates

                persistCustomTemplates()
                ApplicationLoggingServiceManager.i(TAG, "模板已删除: ${template.name} (ID: $id)")
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "删除模板失败: ID=$id", e)
                throw e
            }
        }
    }

    override suspend fun incrementUsageCount(id: Long) {
        mutex.withLock {
            try {
                val currentTemplates = templatesFlow.value.map { template ->
                    if (template.id == id) {
                        template.copy(
                            usageCount = template.usageCount + 1,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        template
                    }
                }
                templatesFlow.value = currentTemplates

                // 仅持久化自定义模板的使用计数变更
                persistCustomTemplates()
                ApplicationLoggingServiceManager.d(TAG, "模板使用次数已增加: ID=$id")
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "增加使用次数失败: ID=$id", e)
            }
        }
    }

    // ==================== 私有辅助方法 / Private Helper Methods ====================

    /**
     * 加载模板：合并内置模板与持久化的自定义模板
     * 初始化时调用，从JSON文件读取自定义模板并与内置模板合并
     */
    private fun loadTemplates() {
        try {
            val customTemplates = loadCustomTemplatesFromFile()

            // 确保ID生成器不会与已有自定义模板ID冲突
            val maxCustomId = customTemplates.maxOfOrNull { it.id } ?: 0
            val maxBuiltInId = BuiltInTemplates.all.maxOf { it.id }
            val nextId = maxOf(maxCustomId, maxBuiltInId) + 1
            idGenerator.set(nextId)

            // 合并内置模板和自定义模板
            val allTemplates = BuiltInTemplates.all + customTemplates
            templatesFlow.value = allTemplates

            ApplicationLoggingServiceManager.i(
                TAG,
                "模板加载完成: ${BuiltInTemplates.all.size} 个内置模板, ${customTemplates.size} 个自定义模板"
            )
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "加载模板失败，使用内置模板", e)
            templatesFlow.value = BuiltInTemplates.all
        }
    }

    /**
     * 从JSON文件加载自定义模板
     *
     * @return 自定义模板列表，文件不存在或解析失败时返回空列表
     */
    private fun loadCustomTemplatesFromFile(): List<CloneTemplate> {
        val file = storageFile
        if (!file.exists()) {
            return emptyList()
        }

        return try {
            val jsonString = file.readText(Charsets.UTF_8)
            if (jsonString.isBlank()) {
                return emptyList()
            }
            val templates = json.decodeFromString<List<CloneTemplate>>(jsonString)
            // 确保加载的模板标记为非内置
            templates.map { it.copy(isBuiltIn = false) }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "解析模板文件失败", e)
            emptyList()
        }
    }

    /**
     * 将自定义模板持久化到JSON文件
     * 仅保存非内置模板，内置模板每次启动时重新加载
     */
    private fun persistCustomTemplates() {
        try {
            val customTemplates = templatesFlow.value.filter { !it.isBuiltIn }
            val jsonString = json.encodeToString(customTemplates)

            // 原子写入：先写入临时文件，再重命名
            val tempFile = File(storageFile.parent, "$FILE_NAME.tmp")
            tempFile.writeText(jsonString, Charsets.UTF_8)

            val renamed = tempFile.renameTo(storageFile)
            if (!renamed) {
                // 重命名失败时直接写入目标文件
                storageFile.writeText(jsonString, Charsets.UTF_8)
                tempFile.delete()
            }

            ApplicationLoggingServiceManager.d(TAG, "自定义模板已持久化: ${customTemplates.size} 个")
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "持久化模板失败", e)
        }
    }
}
