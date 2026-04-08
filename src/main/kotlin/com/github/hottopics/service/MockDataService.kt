package com.github.hottopics.service

import com.github.hottopics.model.*

/**
 * Mock 数据服务 - 提供模拟的热门话题数据
 */
class MockDataService {
    
    /**
     * 获取指定数据源的话题列表
     */
    fun getTopics(source: SourceType): List<Topic> {
        return when (source) {
            SourceType.V2EX -> getV2EXTopics()
            SourceType.ZHIHU -> getZhihuTopics()
            SourceType.WEIBO -> getWeiboTopics()
            SourceType.CUSTOM -> getCustomTopics()
        }
    }
    
    /**
     * 获取话题详情
     */
    fun getTopicDetail(topicId: String): Topic? {
        return getAllTopics().find { it.id == topicId }
    }
    
    private fun getAllTopics(): List<Topic> {
        return getV2EXTopics() + getZhihuTopics() + getWeiboTopics() + getCustomTopics()
    }
    
    private fun getV2EXTopics(): List<Topic> {
        return listOf(
            Topic(
                id = "v2ex-1",
                title = "2024 年最值得学习的编程语言是什么？",
                author = "programmer_cn",
                authorAvatar = null,
                content = """
                    最近在思考明年的技术学习路线，想请教大家：
                    
                    1. Rust 现在生产环境应用情况如何？
                    2. Go 语言在云原生领域是否还值得投入？
                    3. Python 在 AI 时代的地位如何？
                    4. TypeScript 是否已经取代 JavaScript？
                    
                    背景：我是一名有 5 年经验的 Java 后端开发，想拓展技术栈。希望大家能给一些建议，特别是有实际使用经验的朋友。
                """.trimIndent(),
                replies = listOf(
                    Reply(
                        id = "r1",
                        author = "rust_lover",
                        content = "Rust 的学习曲线确实陡峭，但一旦掌握，你会发现它的内存安全保证太香了。我们团队用 Rust 重写了几个核心服务，性能提升 3 倍，bug 减少 80%。",
                        likeCount = 256,
                        createTime = "2小时前"
                    ),
                    Reply(
                        id = "r2",
                        author = "go_developer",
                        content = "Go 在云原生领域依然是主流，Kubernetes、Docker 都是用 Go 写的。如果你要做云平台开发，Go 是必学的。",
                        likeCount = 189,
                        createTime = "1小时前"
                    ),
                    Reply(
                        id = "r3",
                        author = "pythonista",
                        content = "Python 现在是 AI/ML 领域的绝对王者。PyTorch、TensorFlow、Transformers 这些库让 Python 地位稳固。建议学习 Python + 深度学习框架。",
                        likeCount = 167,
                        createTime = "45分钟前"
                    ),
                    Reply(
                        id = "r4",
                        author = "ts_fan",
                        content = "TypeScript 现在几乎是大型前端项目的标配了。类型安全 + IDE 支持让开发效率大大提升。强烈推荐！",
                        likeCount = 142,
                        createTime = "30分钟前"
                    )
                ),
                viewCount = 15680,
                replyCount = 234,
                likeCount = 567,
                createTime = "3小时前",
                source = SourceType.V2EX,
                tags = listOf("编程语言", "技术选型", "职业发展")
            ),
            Topic(
                id = "v2ex-2",
                title = "分享一下我用 Claude/Cursor 提升开发效率的心得",
                author = "ai_enthusiast",
                content = """
                    作为一个从 GitHub Copilot 迁移到 Cursor + Claude 的用户，分享一些心得：
                    
                    1. Cursor 的多文件上下文理解能力很强
                    2. Claude 的代码解释更清晰
                    3. Composer 模式可以快速生成整个功能模块
                    
                    感兴趣的朋友可以交流一下使用技巧。
                """.trimIndent(),
                replies = listOf(
                    Reply(
                        id = "r5",
                        author = "cursor_user",
                        content = "同感！Cursor 的 Tab 补全和 Cmd+K 快捷键真的太好用了，效率提升至少 50%。",
                        likeCount = 98,
                        createTime = "1小时前"
                    )
                ),
                viewCount = 8900,
                replyCount = 89,
                likeCount = 234,
                createTime = "5小时前",
                source = SourceType.V2EX,
                tags = listOf("AI", "开发工具")
            ),
            Topic(
                id = "v2ex-3",
                title = "独立开发者如何找到第一个付费用户？",
                author = "indie_hacker",
                content = """
                    做了一个小产品，技术实现很满意，但是推广真的很难。
                    
                    尝试过的方法：
                    - Product Hunt 发布（效果一般）
                    - Twitter/X 推广（粉丝太少）
                    - Reddit 相关板块（被删帖）
                    
                    求有经验的独立开发者分享获客经验！
                """.trimIndent(),
                replies = listOf(
                    Reply(
                        id = "r6",
                        author = "successful_indie",
                        content = "建议先从细分社区开始，比如找到真正需要你产品的 100 个用户，服务好他们，口碑传播效果最好。",
                        likeCount = 156,
                        createTime = "4小时前"
                    )
                ),
                viewCount = 12000,
                replyCount = 156,
                likeCount = 345,
                createTime = "6小时前",
                source = SourceType.V2EX,
                tags = listOf("独立开发", "创业", "营销")
            ),
            Topic(
                id = "v2ex-4",
                title = "远程工作三年，我的经验和踩坑分享",
                author = "remote_worker",
                content = """
                    从 2021 年开始全职远程，分享一些真实体会：
                    
                    优点：
                    - 时间灵活，可以兼顾家庭
                    - 省去通勤时间
                    - 可以选择低成本城市生活
                    
                    缺点：
                    - 社交减少，需要主动维护人际关系
                    - 工作生活边界模糊
                    - 职业发展可能受限
                    
                    欢迎交流！
                """.trimIndent(),
                replies = listOf(),
                viewCount = 6700,
                replyCount = 78,
                likeCount = 189,
                createTime = "8小时前",
                source = SourceType.V2EX,
                tags = listOf("远程工作", "生活方式")
            )
        )
    }
    
    private fun getZhihuTopics(): List<Topic> {
        return listOf(
            Topic(
                id = "zhihu-1",
                title = "为什么很多程序员都觉得 35 岁是职业生涯的分水岭？",
                author = "职场观察者",
                authorAvatar = null,
                content = """
                    这个问题困扰着很多技术人。从我的观察来看：
                    
                    1. 体力精力下降 - 长期加班带来的健康问题
                    2. 学习速度变慢 - 技术更新换代太快
                    3. 薪资期望高 - 企业成本考量
                    4. 家庭责任 - 不能像年轻人那样投入
                    
                    但其实，35 岁也可以是新的起点，关键是如何转型...
                """.trimIndent(),
                replies = listOf(
                    Reply(
                        id = "zr1",
                        author = "35岁程序员",
                        content = "32 岁转管理，现在带 8 人团队。技术深度够了，做架构和技术决策更有底气。建议有管理能力的同学可以考虑转型。",
                        likeCount = 1892,
                        createTime = "3小时前"
                    ),
                    Reply(
                        id = "zr2",
                        author = "技术老兵",
                        content = "45 岁了，依然在一线写代码。关键是保持学习热情，我每年都会学一门新语言或框架。当然，身体也很重要，现在我每天运动 1 小时。",
                        likeCount = 2341,
                        createTime = "2小时前"
                    ),
                    Reply(
                        id = "zr3",
                        author = "HR视角",
                        content = "作为大厂 HR，说实话我们确实会关注年龄，但更重要的是能力和性价比。如果你在某个领域有深度，年龄反而不是问题。",
                        likeCount = 1567,
                        createTime = "1小时前"
                    )
                ),
                viewCount = 89000,
                replyCount = 1256,
                likeCount = 4567,
                createTime = "今天",
                source = SourceType.ZHIHU,
                tags = listOf("程序员", "职业发展", "35岁危机")
            ),
            Topic(
                id = "zhihu-2",
                title = "ChatGPT 发布一周年，它对你的工作和生活产生了什么影响？",
                author = "科技观察",
                content = """
                    回顾 ChatGPT 发布这一年：
                    
                    工作方面：
                    - 编程效率提升约 30%
                    - 文档写作更快了
                    - 学习新技术的速度加快
                    
                    生活方面：
                    - 日常问题咨询更方便
                    - 语言学习有了好帮手
                    
                    想知道大家的使用体验如何？
                """.trimIndent(),
                replies = listOf(
                    Reply(
                        id = "zr4",
                        author = "重度用户",
                        content = "我直接付费了 GPT-4，每个月 20 美元绝对值。光是在编程辅助方面节省的时间就超过这个价值了。",
                        likeCount = 890,
                        createTime = "5小时前"
                    )
                ),
                viewCount = 56000,
                replyCount = 678,
                likeCount = 2345,
                createTime = "昨天",
                source = SourceType.ZHIHU,
                tags = listOf("ChatGPT", "AI", "工作效率")
            ),
            Topic(
                id = "zhihu-3",
                title = "如何评价 2024 年互联网大厂的裁员潮？",
                author = "互联网评论员",
                content = """
                    2024 年，各大厂继续裁员，涉及的业务线越来越广：
                    
                    - 某大厂游戏业务裁员 20%
                    - 某电商平台优化中后台
                    - 某短视频公司收缩边缘业务
                    
                    这是周期性调整还是长期趋势？大家怎么看？
                """.trimIndent(),
                replies = listOf(),
                viewCount = 120000,
                replyCount = 2345,
                likeCount = 5678,
                createTime = "今天",
                source = SourceType.ZHIHU,
                tags = listOf("互联网", "裁员", "经济")
            )
        )
    }
    
    private fun getWeiboTopics(): List<Topic> {
        return listOf(
            Topic(
                id = "weibo-1",
                title = "#春节假期延长至9天# 网友热议：终于可以多陪陪家人了",
                author = "热搜观察",
                content = """
                    今日，关于春节假期延长的话题登上热搜。
                    
                    网友评论：
                    - "太好了，终于不用那么赶了"
                    - "建议全国推广"
                    - "希望能落实到位"
                    
                    你支持春节假期延长吗？
                """.trimIndent(),
                replies = listOf(
                    Reply(
                        id = "wr1",
                        author = "打工人",
                        content = "支持！每年春节来回跑太累了，多几天假可以好好休息。",
                        likeCount = 2345,
                        createTime = "1小时前"
                    )
                ),
                viewCount = 450000,
                replyCount = 5678,
                likeCount = 12345,
                createTime = "今天",
                source = SourceType.WEIBO,
                tags = listOf("春节", "假期", "民生")
            ),
            Topic(
                id = "weibo-2",
                title = "#年轻人为什么不愿意结婚了# 专家分析引发争议",
                author = "社会观察",
                content = """
                    近日，关于年轻人婚恋观念的话题持续发酵。
                    
                    主要原因分析：
                    1. 经济压力大
                    2. 个人主义兴起
                    3. 对婚姻质量要求更高
                    
                    你怎么看这个现象？
                """.trimIndent(),
                replies = listOf(),
                viewCount = 380000,
                replyCount = 4567,
                likeCount = 8901,
                createTime = "今天",
                source = SourceType.WEIBO,
                tags = listOf("婚姻", "社会现象")
            )
        )
    }
    
    private fun getCustomTopics(): List<Topic> {
        return listOf(
            Topic(
                id = "custom-1",
                title = "欢迎使用 Hot Topics 插件！",
                author = "插件作者",
                content = """
                    感谢安装 Hot Topics Browser 插件！
                    
                    功能介绍：
                    ✅ 支持多数据源（V2EX、知乎、微博）
                    ✅ 实时刷新热门话题
                    ✅ 查看话题详情和回复
                    ✅ 点击话题标题跳转原网页
                    
                    使用方法：
                    1. 点击工具栏的刷新按钮获取最新数据
                    2. 使用下拉菜单切换数据源
                    3. 点击话题标题查看详情
                    4. 点击「在浏览器打开」跳转原网页
                    
                    如果觉得有用，欢迎 star 支持！
                """.trimIndent(),
                replies = listOf(
                    Reply(
                        id = "cr1",
                        author = "测试用户",
                        content = "很好用的插件，终于不用切换浏览器看热榜了！",
                        likeCount = 100,
                        createTime = "刚刚"
                    )
                ),
                viewCount = 1000,
                replyCount = 10,
                likeCount = 99,
                createTime = "刚刚",
                source = SourceType.CUSTOM,
                tags = listOf("插件", "使用指南")
            )
        )
    }
}
