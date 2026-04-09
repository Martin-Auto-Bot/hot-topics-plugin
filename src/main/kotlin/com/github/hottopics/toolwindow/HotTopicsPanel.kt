package com.github.hottopics.toolwindow

import com.github.hottopics.model.SourceType
import com.github.hottopics.model.Topic
import com.github.hottopics.service.TopicService
import com.github.hottopics.ui.TopicDetailPanel
import com.github.hottopics.ui.TopicListPanel
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import javax.swing.*

/**
 * Hot Topics 主面板
 * 包含工具栏、话题列表和详情页面
 */
class HotTopicsPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val dataService = TopicService()
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)

    private val topicListPanel: TopicListPanel
    private val topicDetailPanel: TopicDetailPanel

    private var currentSource = SourceType.V2EX
    private var currentV2exTab = TopicService.V2exTab.ALL
    private var currentPage = 1

    private val sourceComboBox: ComboBox<SourceType> = ComboBox(SourceType.values())

    /** V2EX 分类切换（仅在选择 V2EX 时显示） */
    private val v2exTabComboBox = ComboBox(TopicService.V2exTab.values())

    init {
        topicListPanel = TopicListPanel(
            onTopicClick = { topic -> showTopicDetail(topic) },
            onOpenInBrowser = { topic -> openInBrowser(topic) },
            onPageChange = { page -> currentPage = page; loadTopics() }
        )

        topicDetailPanel = TopicDetailPanel(
            onBackClick = { showTopicList() },
            onOpenInBrowser = { topic -> openInBrowser(topic) }
        )

        setupUI()
        loadTopics()
    }

    private fun setupUI() {
        val toolbar = createToolBar()
        add(toolbar, BorderLayout.NORTH)

        contentPanel.add(topicListPanel, LIST_VIEW)
        contentPanel.add(topicDetailPanel, DETAIL_VIEW)
        add(contentPanel, BorderLayout.CENTER)

        cardLayout.show(contentPanel, LIST_VIEW)
    }

    private fun createToolBar(): JComponent {
        val toolbar = JPanel()
        toolbar.layout = BoxLayout(toolbar, BoxLayout.X_AXIS)
        toolbar.border = JBUI.Borders.empty(5, 8)
        toolbar.background = JBColor.PanelBackground

        // 数据源选择
        val sourceLabel = JBLabel("数据源:")
        toolbar.add(sourceLabel)

        sourceComboBox.selectedItem = currentSource
        sourceComboBox.preferredSize = Dimension(80, 28)
        sourceComboBox.addActionListener { e ->
            currentSource = sourceComboBox.selectedItem as SourceType
            currentPage = 1
            topicListPanel.resetPage()
            updateV2exTabVisibility()
            loadTopics()
        }
        toolbar.add(sourceComboBox)

        toolbar.add(Box.createHorizontalStrut(10))

        // V2EX 分类选择（仅 V2EX 时显示）
        v2exTabComboBox.selectedItem = currentV2exTab
        v2exTabComboBox.preferredSize = Dimension(70, 28)
        v2exTabComboBox.addActionListener { e ->
            if (e.source === v2exTabComboBox) {
                currentV2exTab = v2exTabComboBox.selectedItem as TopicService.V2exTab
                currentPage = 1
                topicListPanel.resetPage()
                loadTopics()
            }
        }
        toolbar.add(v2exTabComboBox)

        toolbar.add(Box.createHorizontalStrut(10))

        // 刷新按钮
        val refreshButton = JButton("刷新").apply {
            toolTipText = "刷新热门话题"
            addActionListener {
                currentPage = 1
                topicListPanel.resetPage()
                loadTopics()
            }
        }
        toolbar.add(refreshButton)

        toolbar.add(Box.createHorizontalGlue())

        val statusLabel = JBLabel("就绪").apply {
            foreground = JBColor.GRAY
        }
        toolbar.add(statusLabel)

        // 初始状态：V2EX 默认显示分类
        updateV2exTabVisibility()

        return toolbar
    }

    /** 根据当前数据源控制 V2EX 分类选择器可见性 */
    private fun updateV2exTabVisibility() {
        v2exTabComboBox.isVisible = (currentSource == SourceType.V2EX)
    }

    private fun loadTopics() {
        SwingUtilities.invokeLater {
            topicListPanel.setLoading(true)
        }

        object : Task.Backgroundable(project, "正在加载热门话题...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val topics = dataService.getTopics(currentSource, currentV2exTab, currentPage)
                    SwingUtilities.invokeLater {
                        // V2EX latest.json 不支持分页，始终为最后一页；其他数据源根据数据量判断
                        val isLastPage = currentSource == SourceType.V2EX || topics.size < 25
                        topicListPanel.setTopics(topics, currentPage, isLastPage)
                        topicListPanel.setLoading(false)
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    SwingUtilities.invokeLater {
                        topicListPanel.showError("V2EX 请求超时，请检查网络连接或尝试刷新")
                        topicListPanel.setLoading(false)
                    }
                } catch (e: java.net.UnknownHostException) {
                    SwingUtilities.invokeLater {
                        topicListPanel.showError("无法连接 V2EX (DNS 解析失败)，请检查网络")
                        topicListPanel.setLoading(false)
                    }
                } catch (e: java.net.ConnectException) {
                    SwingUtilities.invokeLater {
                        topicListPanel.showError("无法连接 V2EX，网络可能不可达或被防火墙拦截")
                        topicListPanel.setLoading(false)
                    }
                } catch (e: javax.net.ssl.SSLException) {
                    SwingUtilities.invokeLater {
                        topicListPanel.showError("V2EX SSL 连接异常，请检查网络环境")
                        topicListPanel.setLoading(false)
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        topicListPanel.showError("加载失败: ${e.message}")
                        topicListPanel.setLoading(false)
                    }
                }
            }
        }.queue()
    }

    private fun showTopicDetail(topic: Topic) {
        // 立即用列表中的数据展示详情，确保用户点击后能立刻看到
        topicDetailPanel.setTopic(topic)
        cardLayout.show(contentPanel, DETAIL_VIEW)

        // 异步加载回复（不阻塞 UI，失败也不影响已显示的详情）
        object : Task.Backgroundable(project, "正在加载回复...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val detail = dataService.getTopicDetail(topic.id)
                    if (detail != null && detail.replies.isNotEmpty()) {
                        SwingUtilities.invokeLater {
                            topicDetailPanel.setTopic(detail)
                        }
                    }
                } catch (_: Exception) {
                    // 回复加载失败不影响已展示的基本详情
                }
            }
        }.queue()
    }

    private fun showTopicList() {
        cardLayout.show(contentPanel, LIST_VIEW)
    }

    private fun openInBrowser(topic: Topic) {
        topic.url?.let { url ->
            try {
                val desktop = java.awt.Desktop.getDesktop()
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(java.net.URI(url))
                }
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this,
                    "无法打开链接: ${e.message}",
                    "错误",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        } ?: run {
            JOptionPane.showMessageDialog(
                this,
                "该话题没有可用的链接",
                "提示",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    companion object {
        private const val LIST_VIEW = "LIST"
        private const val DETAIL_VIEW = "DETAIL"
    }
}
