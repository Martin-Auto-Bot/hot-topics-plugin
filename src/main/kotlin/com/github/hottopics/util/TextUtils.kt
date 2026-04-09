package com.github.hottopics.util

/**
 * 公共文本工具类 - 提取自 TopicListPanel 和 TopicDetailPanel 的重复代码
 */
object TextUtils {

    /**
     * 格式化大数字为易读字符串
     * >= 10000 显示为 "x.xw"
     * >= 1000 显示为 "x.xk"
     */
    fun formatCount(count: Int): String {
        return when {
            count >= 10000 -> String.format("%.1fw", count / 10000.0)
            count >= 1000 -> String.format("%.1fk", count / 1000.0)
            else -> count.toString()
        }
    }

    /**
     * 截断文本，超出最大长度时追加 "..."
     */
    fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength) + "..."
        } else {
            text
        }
    }

    /**
     * HTML 特殊字符转义
     */
    fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * 简单的纯文本转 HTML 格式
     * - 转义 HTML 特殊字符
     * - 双换行转为段落分隔
     * - 单换行转为 <br>
     * - 自动识别 URL 并转为超链接
     */
    fun convertToHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n\n", "</p><p>")
            .replace("\n", "<br>")
            .let { "<p>$it</p>" }
            .replace(Regex("(https?://\\S+)")) { matchResult ->
                "<a href='${matchResult.value}'>${matchResult.value}</a>"
            }
    }
}
