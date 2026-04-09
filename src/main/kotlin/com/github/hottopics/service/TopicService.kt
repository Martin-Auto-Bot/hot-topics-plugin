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
 * - V2EX: 通过真实 API 获取话题，支持分类切换和分页
 * - 知乎 / 微博: 回退到 MockDataService（暂无公开 API）
 */
class TopicService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val mockDataService = MockDataService()

    /** V2EX 当前已加载的话题缓存，用于详情查看 */
    private val v2exTopicCache = mutableMapOf<String, Topic>()

    companion object {
        private const val V2EX_LATEST_URL = "https://www.v2ex.com/api/topics/latest.json"
        private const val V2EX_REPLIES_URL = "https://www.v2ex.com/api/replies/show.json?topic_id="

        /** 技术相关的 V2EX 节点名称 */
        private val TECH_NODE_NAMES = setOf(
            "programmer", "programming", "python", "java", "go", "rust",
            "javascript", "nodejs", "php", "ruby", "swift", "kotlin",
            "ios", "android", "macos", "linux", "windows", "chrome",
            "firefox", "gcc", "mysql", "postgresql", "redis", "mongodb",
            "docker", "kubernetes", "aws", "devops", "qianduan",
            "html", "css", "react", "vue", "angular", "share", "create",
            "design", "ui", "ux", "apple", "google", "microsoft",
            "ai", "machinelearning", "deep learning", "blockchain",
            "web", "security", "network", "database", "algorithms",
            "emacs", "vim", "vscode", "github", "git",
            "云计算", "服务器", "运维", "前端", "后端", "开源项目",
            "程序员", "分享创造", "问与答", "酷工作", "二级市场"
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    //  V2EX API 响应 DTO
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

    private data class V2EXReplyResponse(
        val id: Long,
        @SerializedName("topic_id")
        val topicId: Long,
        val content: String? = null,
        @SerializedName("content_rendered")
        val contentRendered: String? = null,
        val created: Long,
        val member: V2EXMember? = null,
        @SerializedName("reply_to")
        val replyTo: Int? = null
    )

    // ──────────────────────────────────────────────────────────────────────
    //  公开 API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * V2EX 分类标签
     */
    enum class V2exTab(val displayName: String) {
        ALL("全部"),
        TECH("技术")
    }

    /**
     * 获取指定数据源的话题列表（统一入口）。
     *
     * @param source 数据源类型
     * @param tab V2EX 分类标签（仅 V2EX 有效）
     * @param page 页码（从 1 开始，仅 V2EX 有效）
     * @return 话题列表
     */
    fun getTopics(source: SourceType, tab: V2exTab = V2exTab.ALL, page: Int = 1): List<Topic> {
        return when (source) {
            SourceType.V2EX -> fetchV2EXTopics(tab, page)
            SourceType.ZHIHU -> mockDataService.getTopics(SourceType.ZHIHU)
            SourceType.WEIBO -> mockDataService.getTopics(SourceType.WEIBO)
            SourceType.CUSTOM -> mockDataService.getTopics(SourceType.CUSTOM)
        }
    }

    /**
     * 获取话题详情。
     *
     * - V2EX: 优先从缓存获取，并尝试加载回复
     * - 其他: 从 Mock 数据查找
     */
    fun getTopicDetail(topicId: String): Topic? {
        // 优先从 V2EX 缓存中查找
        val cached = v2exTopicCache[topicId]
        if (cached != null) {
            // 尝试获取 V2EX 回复
            val replies = fetchV2EXReplies(topicId)
            return if (replies.isNotEmpty()) {
                cached.copy(replies = replies)
            } else {
                cached
            }
        }
        // 回退到 Mock 数据
        return mockDataService.getTopicDetail(topicId)
    }

    // ──────────────────────────────────────────────────────────────────────
    //  V2EX API 请求
    // ──────────────────────────────────────────────────────────────────────

    private fun fetchV2EXTopics(tab: V2exTab, page: Int): List<Topic> {
        val url = "$V2EX_LATEST_URL?p=$page"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "HotTopics-IntelliJ-Plugin/1.0")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("V2EX API 请求失败: HTTP ${response.code}")
            }

            val body = response.body?.string()
                ?: throw RuntimeException("V2EX API 返回数据为空")

            val topicArray = gson.fromJson(body, Array<V2EXTopicResponse>::class.java)
                ?: throw RuntimeException("V2EX API 数据解析失败")

            return topicArray
                .map { it.toTopic() }
                .filter { topic ->
                    when (tab) {
                        V2exTab.ALL -> true
                        V2exTab.TECH -> topic.tags.any { tag ->
                            TECH_NODE_NAMES.contains(tag.lowercase())
                        }
                    }
                }
                .also { topics ->
                    topics.forEach { v2exTopicCache[it.id] = it }
                }
        }
    }

    /**
     * 获取 V2EX 话题的回复列表
     */
    private fun fetchV2EXReplies(topicId: String): List<Reply> {
        // 从 topicId 中提取 V2EX 原始 ID（格式: "v2ex-12345"）
        val numericId = topicId.removePrefix("v2ex-").toLongOrNull() ?: return emptyList()

        return try {
            val request = Request.Builder()
                .url("$V2EX_REPLIES_URL$numericId")
                .header("User-Agent", "HotTopics-IntelliJ-Plugin/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()

                val body = response.body?.string() ?: return emptyList()

                val replyArray = gson.fromJson(body, Array<V2EXReplyResponse>::class.java)
                    ?: return emptyList()

                replyArray.map { it.toReply() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  数据映射
    // ──────────────────────────────────────────────────────────────────────

    private fun V2EXTopicResponse.toTopic(): Topic {
        val plainContent = buildString {
            val html = contentRendered ?: content ?: ""
            append(Jsoup.parse(html).text())
            if (isEmpty()) append(title)
        }

        val tagName = node?.name
        val tags = if (tagName.isNullOrBlank()) emptyList() else listOf(tagName)

        return Topic(
            id = "v2ex-$id",
            title = title,
            author = member?.username ?: "匿名用户",
            authorAvatar = null,
            content = plainContent,
            replies = emptyList(),
            viewCount = viewsCount,
            replyCount = repliesCount,
            likeCount = 0,
            createTime = formatRelativeTime(created),
            source = SourceType.V2EX,
            url = url,
            tags = tags
        )
    }

    private fun V2EXReplyResponse.toReply(): Reply {
        val plainContent = buildString {
            val html = contentRendered ?: content ?: ""
            append(Jsoup.parse(html).text())
        }

        return Reply(
            id = "v2ex-reply-$id",
            author = member?.username ?: "匿名用户",
            authorAvatar = null,
            content = plainContent,
            likeCount = 0,
            createTime = formatRelativeTime(created),
            replyTo = replyTo?.toString()
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    //  时间格式化
    // ──────────────────────────────────────────────────────────────────────

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
