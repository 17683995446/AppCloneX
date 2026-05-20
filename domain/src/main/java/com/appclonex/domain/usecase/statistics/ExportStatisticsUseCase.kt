package com.appclonex.domain.usecase.statistics

import com.appclonex.domain.repository.UsageStatisticsRepository
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Use-case that exports usage statistics in either JSON or CSV format.
 *
 * The raw JSON is obtained from [UsageStatisticsRepository.exportStatistics]
 * and optionally converted to a CSV string.
 */
class ExportStatisticsUseCase @Inject constructor(
    private val usageStatisticsRepository: UsageStatisticsRepository,
) {

    /**
     * Export statistics in the requested [format].
     *
     * @param format [ExportFormat.JSON] returns the raw JSON string;
     *              [ExportFormat.CSV] converts the JSON to a comma-separated table.
     * @return [Result.success] containing the formatted string, or
     *         [Result.failure] with the encountered exception.
     */
    suspend operator fun invoke(format: ExportFormat = ExportFormat.CSV): Result<String> {
        return try {
            val json = usageStatisticsRepository.exportStatistics()
            when (format) {
                ExportFormat.JSON -> Result.success(json)
                ExportFormat.CSV -> Result.success(convertJsonToCsv(json))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -----------------------------------------------------------------------
    // JSON -> CSV conversion
    // -----------------------------------------------------------------------

    /**
     * Converts a JSON array (or object) of statistics into CSV format.
     *
     * Expected JSON structure -- an array of objects where each object
     * represents one application's statistics:
     * ```json
     * [
     *   {
     *     "packageName": "com.example.app",
     *     "appName": "Example App",
     *     "launchCount": 42,
     *     "usageTimeMs": 3600000,
     *     "lastLaunchTimestamp": 1700000000000
     *   }
     * ]
     * ```
     *
     * If the JSON cannot be parsed as an array, the raw string is returned
     * as a fallback.
     */
    private fun convertJsonToCsv(json: String): String {
        val sb = StringBuilder()

        try {
            val jsonArray = JSONArray(json)

            if (jsonArray.length() == 0) {
                sb.appendLine("Package,App Name,Launch Count,Usage Time (ms),Last Launch")
                return sb.toString()
            }

            // Header row
            sb.appendLine("Package,App Name,Launch Count,Usage Time (ms),Last Launch")

            // Data rows
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue

                val packageName = csvEscape(obj.optString("packageName", ""))
                val appName = csvEscape(obj.optString("appName", ""))
                val launchCount = obj.optLong("launchCount", 0L)
                val usageTimeMs = obj.optLong("usageTimeMs", 0L)
                val lastLaunch = obj.optLong("lastLaunchTimestamp", 0L)

                sb.appendLine("$packageName,$appName,$launchCount,$usageTimeMs,$lastLaunch")
            }
        } catch (_: Exception) {
            // Fallback: return raw JSON wrapped in a minimal CSV structure
            sb.appendLine("Package,App Name,Launch Count,Usage Time (ms),Last Launch")
            sb.appendLine(json)
        }

        return sb.toString()
    }

    /**
     * Escapes a field value for safe CSV output.
     *
     * If the value contains a comma, double-quote, or newline it is
     * wrapped in double-quotes and internal double-quotes are escaped.
     */
    private fun csvEscape(value: String): String {
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}

/** Supported export formats. */
enum class ExportFormat {
    /** Raw JSON string as returned by the repository. */
    JSON,

    /** Comma-separated values with a header row. */
    CSV,
}
