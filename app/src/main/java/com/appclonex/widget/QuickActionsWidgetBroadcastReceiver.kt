package com.appclonex.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 快捷操作小组件接收器
 * 继承 GlanceAppWidgetReceiver，关联 QuickActionsDesktopAppWidget
 */
class QuickActionsWidgetBroadcastReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = QuickActionsDesktopAppWidget()
}
