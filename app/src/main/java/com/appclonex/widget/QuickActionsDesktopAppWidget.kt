package com.appclonex.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import com.appclonex.presentation.activity.ApplicationMainLauncherActivity as MainActivity
import com.appclonex.worker.ApplicationBackgroundTaskWorker

/**
 * 快捷操作小组件
 * 使用 Jetpack Glance 框架实现
 * 尺寸：2x2
 *
 * 改进点：
 * - 使用 WorkManager 处理后台操作（如冻结全部）
 * - 前台操作（如打开应用）使用 Intent 启动 Activity
 */
class QuickActionsDesktopAppWidget : GlanceAppWidget() {

    override val minResizeWidth: DpSize = DpSize(110.dp, 110.dp)
    override val minResizeHeight: DpSize = DpSize(110.dp, 110.dp)
    override val defaultMode = ResizeMode.Precise

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceAppWidgetId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val context = LocalContext.current

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "AppCloneX",
                style = androidx.glance.text.TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(bottom = 12.dp)
            )

            // 快捷操作按钮网格
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 第一列：新建配置
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QuickActionButton(
                        iconResId = android.R.drawable.ic_input_add,
                        label = "新建配置",
                        action = createForegroundAction(context, WidgetAction.ACTION_NEW_PROFILE)
                    )
                }

                // 第二列：快速克隆
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QuickActionButton(
                        iconResId = android.R.drawable.ic_menu_add,
                        label = "快速克隆",
                        action = createForegroundAction(context, WidgetAction.ACTION_QUICK_CLONE)
                    )
                }
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 第三列：冻结全部（后台操作）
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QuickActionButton(
                        iconResId = android.R.drawable.ic_menu_close_clear_cancel,
                        label = "冻结全部",
                        action = createBackgroundAction(context, WidgetAction.ACTION_FREEZE_ALL)
                    )
                }

                // 第四列：打开应用
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QuickActionButton(
                        iconResId = android.R.drawable.ic_menu_send,
                        label = "打开应用",
                        action = createForegroundAction(context, WidgetAction.ACTION_OPEN_APP)
                    )
                }
            }
        }
    }

    @Composable
    private fun QuickActionButton(
        iconResId: Int,
        label: String,
        action: androidx.glance.action.Action
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(action)
                .background(GlanceTheme.colors.primary)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(iconResId),
                contentDescription = label,
                modifier = GlanceModifier.size(24.dp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = label,
                style = androidx.glance.text.TextStyle(
                    color = GlanceTheme.colors.onPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }

    /**
     * 创建前台操作 Action（启动 Activity）
     */
    private fun createForegroundAction(context: Context, action: String): androidx.glance.action.Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetAction.EXTRA_ACTION, action)
        }
        return actionStartActivity(intent)
    }

    /**
     * 创建后台操作 Action（使用 WorkManager）
     */
    private fun createBackgroundAction(context: Context, action: String): androidx.glance.action.Action {
        return actionRunCallback<WidgetActionCallback>(
            actionParametersOf(WidgetActionCallback.KEY_ACTION to action)
        )
    }
}

/**
 * Widget Action 回调处理类
 */
class WidgetActionCallback : ActionCallback {
    companion object {
        val KEY_ACTION = androidx.glance.action.ActionParameters.Key<String>("action")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceAppWidgetId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        val action = parameters[KEY_ACTION] ?: return

        // 使用 WorkManager 调度后台任务
        when (action) {
            WidgetAction.ACTION_FREEZE_ALL -> {
                ApplicationBackgroundTaskWorker.schedule(context, action)
            }
            else -> {
                // 其他操作启动前台 Activity
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(WidgetAction.EXTRA_ACTION, action)
                }
                context.startActivity(intent)
            }
        }
    }
}

/**
 * 小组件操作常量
 */
object WidgetAction {
    const val EXTRA_ACTION = "widget_action"

    const val ACTION_NEW_PROFILE = "action_new_profile"
    const val ACTION_QUICK_CLONE = "action_quick_clone"
    const val ACTION_FREEZE_ALL = "action_freeze_all"
    const val ACTION_OPEN_APP = "action_open_app"
}
