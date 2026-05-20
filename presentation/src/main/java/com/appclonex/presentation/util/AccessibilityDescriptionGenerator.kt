/**
 * Copyright (c) 2025 AppCloneX Contributors
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

package com.appclonex.presentation.util

/**
 * Utility object for generating accessibility content descriptions.
 *
 * Provides concise, screen-reader-friendly descriptions for common UI
 * components such as charts, metric cards, notifications, plugins,
 * progress indicators, and navigation items. All descriptions follow
 * accessibility best practices: they are short, contextual, and
 * convey the essential state information that visually impaired users need.
 */
object AccessibilityDescriptionGenerator {

    /**
     * Generates a content description for a chart element.
     *
     * @param type  The chart type (e.g. "bar", "line", "pie").
     * @param title The chart title describing what data is shown.
     * @param dataPoints The number of data points rendered in the chart.
     * @return A human-readable description suitable for TalkBack / VoiceOver.
     */
    fun describeChart(
        type: String,
        title: String,
        dataPoints: Int,
    ): String {
        return "$title $type chart with $dataPoints data points"
    }

    /**
     * Generates a content description for a metric / statistic card.
     *
     * @param label  The metric label (e.g. "Storage Used").
     * @param value  The current metric value (e.g. "2.4 GB").
     * @param change The change indicator (e.g. "+12% from last week").
     * @return A description combining label, value, and trend information.
     */
    fun describeMetricCard(
        label: String,
        value: String,
        change: String,
    ): String {
        return "$label: $value, $change"
    }

    /**
     * Generates a content description for a notification item.
     *
     * @param title    The notification title.
     * @param priority The priority level (e.g. "high", "normal", "low").
     * @param isRead   Whether the notification has already been read.
     * @return A description including priority and read state.
     */
    fun describeNotification(
        title: String,
        priority: String,
        isRead: Boolean,
    ): String {
        val readState = if (isRead) "read" else "unread"
        return "$title notification, $priority priority, $readState"
    }

    /**
     * Generates a content description for a plugin entry.
     *
     * @param name    The plugin display name.
     * @param version The plugin version string (e.g. "1.3.0").
     * @param state   The current plugin state (e.g. "enabled", "disabled", "error").
     * @return A description combining name, version, and state.
     */
    fun describePlugin(
        name: String,
        version: String,
        state: String,
    ): String {
        return "$name plugin, version $version, $state"
    }

    /**
     * Generates a content description for a determinate progress indicator.
     *
     * @param current The current progress value.
     * @param total   The maximum progress value.
     * @return A percentage-based progress description.
     */
    fun describeProgress(
        current: Int,
        total: Int,
    ): String {
        val percent = if (total > 0) (current * 100) / total else 0
        return "Loading, $percent percent complete"
    }

    /**
     * Generates a content description for a navigation item (tab / drawer entry).
     *
     * @param title     The navigation item label.
     * @param isSelected Whether the item is currently selected / active.
     * @return A description indicating the item label and selection state.
     */
    fun describeNavigationItem(
        title: String,
        isSelected: Boolean,
    ): String {
        val state = if (isSelected) "selected" else "not selected"
        return "$title, $state"
    }
}
