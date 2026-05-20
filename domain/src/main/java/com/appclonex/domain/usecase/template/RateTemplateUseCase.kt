/*
 * Copyright (c) 2024 AppCloneX
 *
 * 评分模板用例
 * 封装用户对模板进行评分和评论的业务逻辑
 */

package com.appclonex.domain.usecase.template

import com.appclonex.domain.model.TemplateRating
import com.appclonex.domain.repository.TemplateMarketplaceRepository
import javax.inject.Inject

/**
 * 评分模板用例
 * 封装用户对模板进行评分和评论的业务逻辑
 *
 * 业务规则：
 * - 评分值必须在 1.0 到 5.0 之间
 * - 评论内容不能超过 500 个字符
 * - 模板ID必须大于 0
 *
 * @property repository 模板市场仓库接口
 */
class RateTemplateUseCase @Inject constructor(
    private val repository: TemplateMarketplaceRepository
) {

    companion object {
        /** 评论内容最大长度 */
        private const val MAX_COMMENT_LENGTH = 500
    }

    /**
     * 执行模板评分操作
     * 提交用户对指定模板的评分和评论
     *
     * @param templateId 模板ID，必须大于 0
     * @param rating 评分值，范围 1.0 - 5.0
     * @param comment 评论内容，不能超过 [MAX_COMMENT_LENGTH] 个字符
     * @return 已保存的评分数据，失败时返回异常
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    suspend operator fun invoke(
        templateId: Long,
        rating: Float,
        comment: String
    ): Result<TemplateRating> {
        // 参数校验
        require(templateId > 0) { "模板ID必须大于0，当前值: $templateId" }
        require(rating in 1f..5f) { "评分必须在 1.0 到 5.0 之间，当前值: $rating" }
        require(comment.length <= MAX_COMMENT_LENGTH) {
            "评论内容不能超过 $MAX_COMMENT_LENGTH 个字符，当前长度: ${comment.length}"
        }

        return repository.rateTemplate(templateId, rating, comment)
    }
}
