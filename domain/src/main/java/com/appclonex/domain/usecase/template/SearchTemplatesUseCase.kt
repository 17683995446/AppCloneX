/*
 * Copyright (c) 2024 AppCloneX
 *
 * 搜索模板用例
 * 封装模板市场搜索的业务逻辑，支持关键词搜索、分类筛选和排序
 */

package com.appclonex.domain.usecase.template

import com.appclonex.domain.model.CloneTemplate
import com.appclonex.domain.model.TemplateSearchFilter
import com.appclonex.domain.repository.TemplateMarketplaceRepository
import javax.inject.Inject

/**
 * 搜索模板用例
 * 封装模板市场搜索的业务逻辑
 *
 * 支持以下搜索能力：
 * - 按名称和描述进行关键词模糊搜索
 * - 按分类进行筛选
 * - 按最低评分进行过滤
 * - 按多种字段进行排序
 *
 * @property repository 模板市场仓库接口
 */
class SearchTemplatesUseCase @Inject constructor(
    private val repository: TemplateMarketplaceRepository
) {

    /**
     * 执行模板搜索
     * 根据提供的过滤条件在模板市场中搜索匹配的模板
     *
     * @param filter 搜索过滤条件，包含关键词、分类、评分阈值和排序参数
     * @return 搜索结果列表，失败时返回异常
     */
    suspend operator fun invoke(filter: TemplateSearchFilter): Result<List<CloneTemplate>> {
        // 参数校验：评分值必须在合理范围内
        require(filter.minRating in 0f..5f) {
            "最低评分必须在 0.0 到 5.0 之间，当前值: ${filter.minRating}"
        }

        return repository.searchTemplates(filter)
    }
}
