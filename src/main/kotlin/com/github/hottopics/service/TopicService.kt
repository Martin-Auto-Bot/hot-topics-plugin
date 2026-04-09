package com.github.hottopics.service

import com.github.hottopics.model.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * 话题数据获取服务 - 统一的话题数据获取接口
 *
 * - V2EX: 通过真实 API 获取热门话题数据
 * - 知乎 / 微博: 回退到 MockDataService（暂无公开 API）
 */
class TopicService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val mockDataService = MockDataService()

    companion object {
        private const val V2EX_HOT_TOPICS_URL = "https://www.v2ex.com/api/topics/hot.json"
    }

    // ──────────────────────────────────────────────────────────────────────
    //  V2EX API 响应 DTO（内部使用，不暴露给外部）
    // ──────────────────────────────────────────────────────────────────────

    private data class V2EXTopicResponse(
        val id: Long,
        val title: String,
        val content: String? = null,
        @SerializedName("content_rendered")
        val contentRendered: String? = null,
        val url: String? = null,
        val created: Long,
        @SerializedName("last_modified")
        val lastModified: Long? = null,
        val node: V2EXNode? = null,
        val member: V2EXMember? = null,
        @SerializedName("replies_count")
        val repliesCount: Int = 0,
        @SerializedName("views_count")
        val viewsCount: Int = 0
    )

    private data class V2EXNode(
        val name: String? = null,
        val title: String? = null
    )

    private data class V2EXMember(
        val id: Long? = null,
        val username: String? = null
    )

    // ──────────────────────────────────────────────────────────────────────
    //  公开 API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 获取指定数据源的话题列表（统一入口）。
     *
     * - V2EX: 请求真实 API
     * - 知乎 / 微博 / 自定义: 回退到 MockDataService
     *
     * @param source 数据源类型
     * @return 话题列表；网络异常时 V2EX 返回空列表，其他数据源返回 Mock 数据
     */
    fun getTopics(source: SourceType): List<Topic> {
        return when (source) {
            SourceType.V2EX -> fetchV2EXHotTopics()
            SourceType.ZHIHU -> mockDataService.getTopics(SourceType.ZHIHU)
            SourceType.WEIBO -> mockDataService.getTopics(SourceType.WEIBO)
            SourceType.CUSTOM -> mockDataService.getTopics(SourceType.CUSTOM)
        }
    }

    /**
     * 获取话题详情。
     *
     * 优先从 Mock 数据中查找；V2EX 话题也可以通过 API 查询
     * （当前暂不实现单个 V2EX 话题的详情 API）。
     *
     * @param topicId 话题 ID
     * @return 话题详情，未找到时返回 null
     */
    fun getTopicDetail(topicId: String): Topic? {
        return mockDataService.getTopicDetail(topicId)
    }

    // ──────────────────────────────────────────────────────────────────────
    //  V2EX API 请求
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 从 V2EX 热门话题 API 获取并映射话题列表。
     *
     * 任何网络 / 解析异常均被捕获，返回空列表以确保插件稳定运行。
     */
    private fun fetchV2EXHotTopics(): List<Topic> {
        return try {
            val request = Request.Builder()
                .url(V2EX_HOT_TOPICS_URL)
                .header("User-Agent", "HotTopics-IntelliJ-Plugin/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return emptyList()
                }

                val body = response.body?.string() ?: return emptyList()

                val topicArray = gson.fromJson(body, Array<V2EXTopicResponse>::class.java)
                    ?: return emptyList()

                topicArray.map { it.toTopic() }
            }
        } catch (_: Exception) {
            // 捕获所有异常（网络错误、JSON 解析错误等），返回空列表
            emptyList()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  数据映射
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 将 V2EX API 响应映射到统一的 [Topic] 模型。
     */
    private fun V2EXTopicResponse.toTopic(): Topic {
        // 使用 Jsoup 去除 HTML 标签，获取纯文本内容
        val plainContent = buildString {
            // 优先使用 content_rendered（已渲染的 HTML），否则使用 content
            val html = contentRendered ?: content ?: ""
            append(Jsoup.parse(html).text())
            if (isEmpty()) {
                append(title) // 兜底：如果没有正文则使用标题
            }
        }

        // 节点名称作为标签
        val tagName = node?.name
        val tags = if (tagName.isNullOrBlank()) emptyList() else listOf(tagName)

        return Topic(
            id = "v2ex-$id",
            title = title,
            author = member?.username ?: "匿名用户",
            authorAvatar = null,
            content = plainContent,
            replies = emptyList(),  // 热门话题列表 API 不包含回复详情
            viewCount = viewsCount,
            replyCount = repliesCount,
            likeCount = 0,         // V2EX 热门话题 API 不提供点赞数
            createTime = formatRelativeTime(created),
            source = SourceType.V2EX,
            url = url,
            tags = tags
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    //  时间格式化
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 将 Unix 时间戳（秒）转换为相对时间字符串，如 "3小时前"、"2天前"。
     */
    private fun formatRelativeTime(epochSeconds: Long): String {
        val now = System.currentTimeMillis() / 1000
        val diff = now - epochSeconds

        return when {
            diff < 0 -> "刚刚"
            diff < 60 -> "${diff}秒前"
            diff < 3600 -> "${diff / 60}分钟前"
            diff < 86400 -> "${diff / 3600}小时前"
            diff < 2592000 -> "${diff / 86400}天前"
            diff < 31536000 -> "${diff / 2592000}个月前"
            else -> "${diff / 31536000}年前"
        }
    }
}
