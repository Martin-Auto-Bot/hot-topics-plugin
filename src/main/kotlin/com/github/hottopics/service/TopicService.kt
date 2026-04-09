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
     * 策略：V2EX 优先 API 获取回复 → 失败则 Jsoup 抓取网页解析 → 都失败则只展示基本信息
     * 其他数据源直接从 Mock 数据查找。
     */
    fun getTopicDetail(topicId: String): Topic? {
        // 优先从 V2EX 缓存中查找
        val cached = v2exTopicCache[topicId]
        if (cached != null) {
            // 方式一：API 获取回复
            try {
                val replies = fetchV2EXReplies(topicId)
                if (replies.isNotEmpty()) return cached.copy(replies = replies)
            } catch (_: Exception) {
                // API 失败，继续尝试抓取
            }

            // 方式二：Jsoup 抓取网页解析回复 + 完整正文
            val topicUrl = cached.url
            if (!topicUrl.isNullOrBlank()) {
                try {
                    val scraped = scrapeV2exTopicPage(topicUrl)
                    return cached.copy(
                        content = if (scraped.first.isNotBlank()) scraped.first else cached.content,
                        replies = scraped.second
                    )
                } catch (_: Exception) {
                    // 抓取也失败，返回缓存的基本信息
                }
            }

            return cached
        }
        // 回退到 Mock 数据
        return mockDataService.getTopicDetail(topicId)
    }

    // ──────────────────────────────────────────────────────────────────────
    //  V2EX API 请求
    // ──────────────────────────────────────────────────────────────────────

    private fun fetchV2EXTopics(tab: V2exTab, page: Int): List<Topic> {
        // V2EX latest.json 不支持分页，page 参数无效，始终返回最新 25 条
        val url = V2EX_LATEST_URL
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
    //  Jsoup 网页抓取（API 失败时的备选方案）
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 通过 Jsoup 抓取 V2EX 话题详情页，解析正文和回复。
     *
     * @param topicUrl 话题页面 URL（如 https://www.v2ex.com/t/1204632）
     * @return Pair(正文纯文本, 回复列表)
     */
    private fun scrapeV2exTopicPage(topicUrl: String): Pair<String, List<Reply>> {
        val doc = Jsoup.connect(topicUrl)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(8000)
            .followRedirects(true)
            .get()

        // 解析正文
        val contentText = doc.selectFirst("div.markdown_body")?.text() ?: ""

        // 解析回复
        val replies = mutableListOf<Reply>()
        val replyCells = doc.select("div[id^=r_]")

        for (cell in replyCells) {
            val replyId = cell.id().removePrefix("r_")

            // 作者
            val authorEl = cell.selectFirst("strong > a.dark, a.dark")
            val author = authorEl?.text() ?: "匿名用户"

            // 回复内容
            val contentEl = cell.selectFirst("div.reply_content")
            val replyContent = contentEl?.text() ?: ""

            if (replyContent.isBlank()) continue  // 跳过空回复

            // 发布时间
            val timeEl = cell.selectFirst("span.ago")
            val time = timeEl?.attr("title") ?: timeEl?.text() ?: ""

            // 感谢数
            var thankCount = 0
            val thankEl = cell.selectFirst("span.smallfade")
            if (thankEl != null) {
                val match = Regex("\\d+").findAll(thankEl.text()).toList()
                if (match.size >= 2) thankCount = match[1].value.toIntOrNull() ?: 0
            }

            // 回复对象
            val replyToEl = cell.selectFirst("a.reply2")
            val replyTo = replyToEl?.text()?.removePrefix("@")

            replies.add(Reply(
                id = "v2ex-reply-$replyId",
                author = author,
                authorAvatar = null,
                content = replyContent,
                likeCount = thankCount,
                createTime = time,
                replyTo = replyTo
            ))
        }

        return Pair(contentText, replies)
    }

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
