package com.github.hottopics.model

/**
 * 话题数据模型
 */
data class Topic(
    val id: String,
    val title: String,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val replies: List<Reply>,
    val viewCount: Int,
    val replyCount: Int,
    val likeCount: Int,
    val createTime: String,
    val source: SourceType,
    val url: String? = null,
    val tags: List<String> = emptyList()
)

/**
 * 回复数据模型
 */
data class Reply(
    val id: String,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val likeCount: Int,
    val createTime: String,
    val replyTo: String? = null  // 回复的对象
)

/**
 * 数据源类型
 */
enum class SourceType(val displayName: String, val icon: String) {
    V2EX("V2EX", "V"),
    ZHIHU("知乎", "知"),
    WEIBO("微博", "微"),
    CUSTOM("自定义", "自")
}

/**
 * 话题列表项（用于简化列表显示）
 */
data class TopicListItem(
    val id: String,
    val title: String,
    val author: String,
    val replyCount: Int,
    val likeCount: Int,
    val source: SourceType,
    val heat: Int  // 热度值，用于排序
)
