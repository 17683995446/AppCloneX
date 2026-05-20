/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appclonex.infrastructure.network

import com.appclonex.domain.model.WebDavConfig
import com.appclonex.domain.model.WebDavResource
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

/**
 * WebDAV API 服务
 * 封装 WebDAV 协议（RFC 4918）的核心 HTTP 操作，使用 HttpURLConnection 实现。
 *
 * 支持的操作：
 * - PROPFIND: 列出目录内容、获取文件属性
 * - PUT: 上传文件（支持进度回调）
 * - GET: 下载文件（支持进度回调）
 * - DELETE: 删除文件或目录
 * - MKCOL: 创建目录
 * - HEAD: 检查文件是否存在
 *
 * 认证方式：HTTP Basic Auth
 * XML 解析：javax.xml.parsers.DocumentBuilderFactory
 */
@Singleton
class WebDavApiService @Inject constructor() {

    companion object {
        private const val TAG = "WebDavApiService"

        /** WebDAV XML 命名空间 */
        private const val DAV_NAMESPACE = "DAV:"

        /** PROPFIND 请求体模板（深度1：列出目录内容） */
        private const val PROPFIND_LIST_BODY = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:displayname/>
                    <d:getcontentlength/>
                    <d:getlastmodified/>
                    <d:getcontenttype/>
                    <d:getetag/>
                    <d:resourcetype/>
                </d:prop>
            </d:propfind>
        """.trimIndent()

        /** PROPFIND 请求体模板（深度0：获取单个文件属性） */
        private const val PROPFIND_SINGLE_BODY = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:displayname/>
                    <d:getcontentlength/>
                    <d:getlastmodified/>
                    <d:getcontenttype/>
                    <d:getetag/>
                    <d:resourcetype/>
                </d:prop>
            </d:propfind>
        """.trimIndent()

        /** WebDAV 请求缓冲区大小 */
        private const val BUFFER_SIZE = 8192
    }

    // ==================== PROPFIND 操作 ====================

    /**
     * 列出目录内容（PROPFIND 深度1）
     * 向指定路径发送 PROPFIND 请求，解析 XML 响应获取目录下的所有资源
     *
     * @param config WebDAV 连接配置
     * @param path 远程目录路径
     * @return 资源列表，失败时返回 null
     */
    fun propfindList(config: WebDavConfig, path: String): List<WebDavResource>? {
        val connection: HttpURLConnection
        return try {
            val fullUrl = config.buildFullUrl(path)
            ApplicationLoggingServiceManager.d(TAG, "PROPFIND 列表: $fullUrl")

            connection = createConnection(fullUrl, "PROPFIND", config)
            connection.setRequestProperty("Depth", "1")
            connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(PROPFIND_LIST_BODY.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            when (responseCode) {
                207 -> {
                    // Multi-Status 响应，解析 XML
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    parseMultiStatusResponse(responseBody, fullUrl)
                }
                else -> {
                    handleError(TAG, "PROPFIND 列表", responseCode, connection)
                    null
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "PROPFIND 列表异常: $path", e)
            null
        } finally {
            // R9-EDGE-031: 确保连接被正确关闭，防止资源泄漏
            try { connection.disconnect() } catch (_: Exception) {}
        }
    }

    /**
     * 获取单个文件属性（PROPFIND 深度0）
     *
     * @param config WebDAV 连接配置
     * @param path 远程文件路径
     * @return 资源信息，失败时返回 null
     */
    fun propfindSingle(config: WebDavConfig, path: String): WebDavResource? {
        val connection: HttpURLConnection
        return try {
            val fullUrl = config.buildFullUrl(path)
            ApplicationLoggingServiceManager.d(TAG, "PROPFIND 单文件: $fullUrl")

            connection = createConnection(fullUrl, "PROPFIND", config)
            connection.setRequestProperty("Depth", "0")
            connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(PROPFIND_SINGLE_BODY.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            when (responseCode) {
                207 -> {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    val resources = parseMultiStatusResponse(responseBody, fullUrl)
                    // 深度0请求只返回一个资源（排除根目录自身，取第一个非根资源）
                    resources.firstOrNull { it.href != fullUrl && it.href != "$fullUrl/" }
                }
                else -> {
                    handleError(TAG, "PROPFIND 单文件", responseCode, connection)
                    null
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "PROPFIND 单文件异常: $path", e)
            null
        } finally {
            // R9-EDGE-031: 确保连接被正确关闭，防止资源泄漏
            try { connection.disconnect() } catch (_: Exception) {}
        }
    }

    // ==================== PUT 操作（上传文件） ====================

    /**
     * 上传文件到 WebDAV 服务器
     * 使用 PUT 请求将本地文件内容写入远程路径
     *
     * @param config WebDAV 连接配置
     * @param localFile 本地文件
     * @param remotePath 远程目标路径
     * @param progress 上传进度回调 (已传输字节数, 总字节数)
     * @return 上传成功后的资源信息，失败时返回 null
     */
    fun putFile(
        config: WebDavConfig,
        localFile: File,
        remotePath: String,
        progress: ((Long, Long) -> Unit)? = null
    ): WebDavResource? {
        val connection: HttpURLConnection
        return try {
            if (!localFile.exists() || !localFile.isFile) {
                ApplicationLoggingServiceManager.e(TAG, "上传文件不存在: ${localFile.absolutePath}")
                return null
            }

            val fullUrl = config.buildFullUrl(remotePath)
            val fileSize = localFile.length()
            ApplicationLoggingServiceManager.i(TAG, "PUT 上传: ${localFile.name} ($fileSize bytes) -> $fullUrl")

            connection = createConnection(fullUrl, "PUT", config)
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setRequestProperty("Content-Length", fileSize.toString())
            connection.doOutput = true

            // 分块写入文件内容，支持进度回调
            var bytesTransferred = 0L
            val buffer = ByteArray(BUFFER_SIZE)

            FileInputStream(localFile).use { fis ->
                connection.outputStream.use { os ->
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        os.write(buffer, 0, bytesRead)
                        bytesTransferred += bytesRead
                        progress?.invoke(bytesTransferred, fileSize)
                    }
                    os.flush()
                }
            }

            val responseCode = connection.responseCode
            when (responseCode) {
                200, 201, 204 -> {
                    val etag = connection.getHeaderField("ETag") ?: ""
                    ApplicationLoggingServiceManager.i(TAG, "PUT 上传成功: ${localFile.name}")
                    WebDavResource(
                        href = fullUrl,
                        displayName = localFile.name,
                        contentLength = fileSize,
                        lastModified = System.currentTimeMillis(),
                        mimeType = "application/octet-stream",
                        isCollection = false,
                        etag = etag
                    )
                }
                else -> {
                    handleError(TAG, "PUT 上传", responseCode, connection)
                    null
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "PUT 上传异常: ${localFile.name}", e)
            null
        } finally {
            // R9-EDGE-031: 确保连接被正确关闭，防止资源泄漏
            try { connection.disconnect() } catch (_: Exception) {}
        }
    }

    // ==================== GET 操作（下载文件） ====================

    /**
     * 从 WebDAV 服务器下载文件
     * 使用 GET 请求下载远程文件内容到本地
     *
     * @param config WebDAV 连接配置
     * @param remotePath 远程文件路径
     * @param localFile 本地目标文件
     * @param progress 下载进度回调 (已传输字节数, 总字节数)
     * @return 下载成功后的本地文件，失败时返回 null
     */
    fun getFile(
        config: WebDavConfig,
        remotePath: String,
        localFile: File,
        progress: ((Long, Long) -> Unit)? = null
    ): File? {
        val connection: HttpURLConnection
        return try {
            val fullUrl = config.buildFullUrl(remotePath)
            ApplicationLoggingServiceManager.i(TAG, "GET 下载: $fullUrl -> ${localFile.absolutePath}")

            // 确保目标文件父目录存在
            localFile.parentFile?.mkdirs()

            connection = createConnection(fullUrl, "GET", config)

            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    val contentLength = connection.contentLengthLong
                    var bytesTransferred = 0L
                    val buffer = ByteArray(BUFFER_SIZE)

                    FileOutputStream(localFile).use { fos ->
                        connection.inputStream.use { inputStream ->
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                fos.write(buffer, 0, bytesRead)
                                bytesTransferred += bytesRead
                                progress?.invoke(bytesTransferred, if (contentLength > 0) contentLength else -1)
                            }
                            fos.flush()
                        }
                    }

                    ApplicationLoggingServiceManager.i(TAG, "GET 下载成功: ${localFile.name} ($bytesTransferred bytes)")
                    localFile
                }
                else -> {
                    handleError(TAG, "GET 下载", responseCode, connection)
                    null
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "GET 下载异常: $remotePath", e)
            null
        } finally {
            // R9-EDGE-031: 确保连接被正确关闭，防止资源泄漏
            try { connection.disconnect() } catch (_: Exception) {}
        }
    }

    // ==================== DELETE 操作 ====================

    /**
     * 删除远程文件或目录
     * 使用 DELETE 请求删除指定路径的资源
     *
     * @param config WebDAV 连接配置
     * @param remotePath 远程路径
     * @return true 表示删除成功
     */
    fun deleteResource(config: WebDavConfig, remotePath: String): Boolean {
        return try {
            val fullUrl = config.buildFullUrl(remotePath)
            ApplicationLoggingServiceManager.i(TAG, "DELETE: $fullUrl")

            val connection = createConnection(fullUrl, "DELETE", config)

            val responseCode = connection.responseCode
            when (responseCode) {
                200, 204 -> {
                    ApplicationLoggingServiceManager.i(TAG, "DELETE 成功: $remotePath")
                    true
                }
                else -> {
                    handleError(TAG, "DELETE", responseCode, connection)
                    false
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "DELETE 异常: $remotePath", e)
            false
        }
    }

    // ==================== MKCOL 操作（创建目录） ====================

    /**
     * 创建远程目录
     * 使用 MKCOL 请求创建指定路径的目录
     *
     * @param config WebDAV 连接配置
     * @param path 目录路径
     * @return true 表示创建成功
     */
    fun makeCollection(config: WebDavConfig, path: String): Boolean {
        return try {
            val fullUrl = config.buildFullUrl(path)
            ApplicationLoggingServiceManager.i(TAG, "MKCOL: $fullUrl")

            val connection = createConnection(fullUrl, "MKCOL", config)

            val responseCode = connection.responseCode
            when (responseCode) {
                200, 201 -> {
                    ApplicationLoggingServiceManager.i(TAG, "MKCOL 成功: $path")
                    true
                }
                // 405 表示目录已存在，视为成功
                405 -> {
                    ApplicationLoggingServiceManager.d(TAG, "MKCOL 目录已存在: $path")
                    true
                }
                else -> {
                    handleError(TAG, "MKCOL", responseCode, connection)
                    false
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "MKCOL 异常: $path", e)
            false
        }
    }

    // ==================== HEAD 操作（检查文件存在） ====================

    /**
     * 检查远程文件是否存在
     * 使用 HEAD 请求检查指定路径的资源是否存在
     *
     * @param config WebDAV 连接配置
     * @param remotePath 远程文件路径
     * @return true 表示文件存在
     */
    fun headResource(config: WebDavConfig, remotePath: String): Boolean {
        return try {
            val fullUrl = config.buildFullUrl(remotePath)
            ApplicationLoggingServiceManager.d(TAG, "HEAD: $fullUrl")

            val connection = createConnection(fullUrl, "HEAD", config)

            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> true
                404 -> false
                else -> {
                    handleError(TAG, "HEAD", responseCode, connection)
                    false
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "HEAD 异常: $remotePath", e)
            false
        }
    }

    // ==================== 连接创建与认证 ====================

    /**
     * 创建带认证的 HttpURLConnection
     * 设置 Basic Auth 认证头和超时参数
     *
     * @param url 完整的请求 URL
     * @param method HTTP 请求方法
     * @param config WebDAV 连接配置
     * @return 配置好的 HttpURLConnection 实例
     */
    private fun createConnection(url: String, method: String, config: WebDavConfig): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = config.connectTimeout
        connection.readTimeout = config.readTimeout
        connection.instanceFollowRedirects = true

        // 设置 Basic Auth 认证
        if (config.username.isNotEmpty()) {
            val credentials = "${config.username}:${config.password}"
            val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
            connection.setRequestProperty("Authorization", "Basic $encoded")
        }

        return connection
    }

    // ==================== XML 响应解析 ====================

    /**
     * 解析 WebDAV Multi-Status XML 响应
     * 使用 DocumentBuilderFactory 解析 XML，提取每个 response 节点中的属性
     *
     * @param xmlBody XML 响应体
     * @param requestUrl 请求的 URL（用于过滤根目录自身）
     * @return 解析后的资源列表
     */
    private fun parseMultiStatusResponse(xmlBody: String, requestUrl: String): List<WebDavResource> {
        return try {
            val resources = mutableListOf<WebDavResource>()
            val factory = DocumentBuilderFactory.newInstance()

            // 安全配置：禁用外部实体解析防止 XXE 攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)

            val builder = factory.newDocumentBuilder()
            val document = builder.parse(xmlBody.byteInputStream(Charsets.UTF_8))

            // 获取所有 <d:response> 节点
            val responseNodes = document.getElementsByTagNameNS(DAV_NAMESPACE, "response")
            // 兼容不带命名空间前缀的情况
            if (responseNodes.length == 0) {
                parseResponseNodes(document.getElementsByTagName("response"), requestUrl, resources)
            } else {
                parseResponseNodes(responseNodes, requestUrl, resources)
            }

            ApplicationLoggingServiceManager.d(TAG, "解析到 ${resources.size} 个 WebDAV 资源")
            resources
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "解析 Multi-Status XML 失败", e)
            emptyList()
        }
    }

    /**
     * 解析 response 节点列表
     *
     * @param responseNodes response 节点列表
     * @param requestUrl 请求 URL
     * @param resources 输出的资源列表
     */
    private fun parseResponseNodes(
        responseNodes: org.w3c.dom.NodeList,
        requestUrl: String,
        resources: MutableList<WebDavResource>
    ) {
        for (i in 0 until responseNodes.length) {
            val responseNode = responseNodes.item(i)
            try {
                val resource = parseResponseNode(responseNode, requestUrl)
                if (resource != null) {
                    resources.add(resource)
                }
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.w(TAG, "解析 response 节点 #$i 失败", e)
            }
        }
    }

    /**
     * 解析单个 response 节点为 WebDavResource
     *
     * @param responseNode XML response 节点
     * @param requestUrl 请求 URL
     * @return 解析后的资源对象
     */
    private fun parseResponseNode(responseNode: org.w3c.dom.Node, requestUrl: String): WebDavResource? {
        val children = responseNode.childNodes
        var href = ""
        var displayName = ""
        var contentLength = 0L
        var lastModified = 0L
        var contentType = ""
        var etag = ""
        var isCollection = false

        for (j in 0 until children.length) {
            val child = children.item(j)
            val localName = child.localName

            when (localName) {
                "href" -> {
                    href = child.textContent.trim()
                    // URL 解码
                    try {
                        href = java.net.URLDecoder.decode(href, "UTF-8")
                    } catch (_: Exception) {
                        // 解码失败则使用原始值
                    }
                }
                "propstat" -> {
                    // 解析 propstat 节点中的属性
                    val propResult = parsePropstatNode(child)
                    displayName = propResult.displayName
                    contentLength = propResult.contentLength
                    lastModified = propResult.lastModified
                    contentType = propResult.contentType
                    etag = propResult.etag
                    isCollection = propResult.isCollection
                }
            }
        }

        if (href.isEmpty()) return null

        // 过滤掉请求的根目录自身
        val normalizedHref = href.trimEnd('/')
        val normalizedUrl = requestUrl.trimEnd('/')
        if (normalizedHref == normalizedUrl) return null

        return WebDavResource(
            href = href,
            displayName = displayName,
            contentLength = contentLength,
            lastModified = lastModified,
            mimeType = contentType,
            isCollection = isCollection,
            etag = etag
        )
    }

    /**
     * 解析 propstat 节点中的属性值
     *
     * @param propstatNode propstat XML 节点
     * @return 包含解析结果的 PropData
     */
    private fun parsePropstatNode(propstatNode: org.w3c.dom.Node): PropData {
        val data = PropData()
        val children = propstatNode.childNodes

        for (k in 0 until children.length) {
            val child = children.item(k)
            if (child.localName == "prop") {
                val propChildren = child.childNodes
                for (l in 0 until propChildren.length) {
                    val propChild = propChildren.item(l)
                    when (propChild.localName) {
                        "displayname" -> data.displayName = propChild.textContent.trim()
                        "getcontentlength" -> {
                            val text = propChild.textContent.trim()
                            if (text.isNotEmpty()) data.contentLength = text.toLongOrNull() ?: 0L
                        }
                        "getlastmodified" -> {
                            data.lastModified = parseHttpDate(propChild.textContent.trim())
                        }
                        "getcontenttype" -> data.contentType = propChild.textContent.trim()
                        "getetag" -> data.etag = propChild.textContent.trim()
                        "resourcetype" -> {
                            // 检查是否包含 <d:collection/> 子节点
                            data.isCollection = hasCollectionChild(propChild)
                        }
                    }
                }
            }
        }

        return data
    }

    /**
     * 检查 resourcetype 节点是否包含 collection 子节点
     *
     * @param resourcetypeNode resourcetype 节点
     * @return true 表示该资源是目录
     */
    private fun hasCollectionChild(resourcetypeNode: org.w3c.dom.Node): Boolean {
        val children = resourcetypeNode.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.localName == "collection") return true
        }
        return false
    }

    /**
     * 解析 HTTP 日期格式为毫秒时间戳
     * 支持格式：RFC 1123 (EEE, dd MMM yyyy HH:mm:ss zzz)
     *
     * @param dateStr HTTP 日期字符串
     * @return 毫秒时间戳，解析失败返回 0
     */
    private fun parseHttpDate(dateStr: String): Long {
        if (dateStr.isEmpty()) return 0L
        return try {
            val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
            format.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            // 尝试 ISO 8601 格式
            try {
                val cleanedTime = dateStr.replace("Z", "+00:00")
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                format.parse(cleanedTime)?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    // ==================== 错误处理 ====================

    /**
     * 处理 HTTP 错误响应
     * 根据状态码记录相应的错误日志
     *
     * @param tag 日志标签
     * @param operation 操作名称
     * @param responseCode HTTP 状态码
     * @param connection HTTP 连接
     */
    private fun handleError(tag: String, operation: String, responseCode: Int, connection: HttpURLConnection) {
        val errorBody = try {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "无错误详情"
        } catch (e: Exception) {
            "无法读取错误详情"
        }

        val errorMsg = when (responseCode) {
            401 -> "认证失败：用户名或密码错误"
            403 -> "权限不足：无权访问该资源"
            404 -> "资源不存在"
            405 -> "方法不允许"
            409 -> "冲突：资源已存在或路径冲突"
            423 -> "资源被锁定"
            507 -> "存储空间不足"
            else -> "HTTP $responseCode"
        }

        ApplicationLoggingServiceManager.e(tag, "$operation 失败: $errorMsg, 详情: $errorBody")
    }

    /**
     * 内部数据类，用于暂存 propstat 解析结果
     */
    private data class PropData(
        var displayName: String = "",
        var contentLength: Long = 0L,
        var lastModified: Long = 0L,
        var contentType: String = "",
        var etag: String = "",
        var isCollection: Boolean = false
    )
}
