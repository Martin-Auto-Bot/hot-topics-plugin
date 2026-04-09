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
    private val sourceComboBox: ComboBox<SourceType> = ComboBox(SourceType.values())
    
    init {
        // 创建话题列表面板
        topicListPanel = TopicListPanel(
            onTopicClick = { topic -> showTopicDetail(topic) },
            onOpenInBrowser = { topic -> openInBrowser(topic) }
        )
        
        // 创建话题详情面板
        topicDetailPanel = TopicDetailPanel(
            onBackClick = { showTopicList() },
            onOpenInBrowser = { topic -> openInBrowser(topic) }
        )
        
        // 设置布局
        setupUI()
        
        // 加载初始数据
        loadTopics()
    }
    
    private fun setupUI() {
        // 工具栏
        val toolbar = createToolBar()
        add(toolbar, BorderLayout.NORTH)
        
        // 内容区域
        contentPanel.add(topicListPanel, LIST_VIEW)
        contentPanel.add(topicDetailPanel, DETAIL_VIEW)
        add(contentPanel, BorderLayout.CENTER)
        
        // 初始显示列表
        cardLayout.show(contentPanel, LIST_VIEW)
    }
    
    private fun createToolBar(): JComponent {
        val toolbar = JPanel()
        toolbar.layout = BoxLayout(toolbar, BoxLayout.X_AXIS)
        toolbar.border = JBUI.Borders.empty(5, 8)
        toolbar.background = JBColor.PanelBackground
        
        // 数据源选择
        val sourceLabel = JBLabel("数据源: ")
        toolbar.add(sourceLabel)
        
        sourceComboBox.selectedItem = currentSource
        sourceComboBox.addActionListener { 
            currentSource = sourceComboBox.selectedItem as SourceType
            loadTopics()
        }
        sourceComboBox.preferredSize = Dimension(100, 28)
        toolbar.add(sourceComboBox)
        
        toolbar.add(Box.createHorizontalStrut(10))
        
        // 刷新按钮
        val refreshButton = JButton("刷新").apply {
            toolTipText = "刷新热门话题"
            addActionListener { loadTopics() }
        }
        toolbar.add(refreshButton)
        
        toolbar.add(Box.createHorizontalGlue())
        
        // 状态标签
        val statusLabel = JBLabel("就绪").apply {
            foreground = JBColor.GRAY
        }
        toolbar.add(statusLabel)
        
        return toolbar
    }
    
    private fun loadTopics() {
        SwingUtilities.invokeLater {
            topicListPanel.setLoading(true)
        }
        
        object : Task.Backgroundable(project, "正在加载热门话题...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val topics = dataService.getTopics(currentSource)
                    SwingUtilities.invokeLater {
                        topicListPanel.setTopics(topics)
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
        object : Task.Backgroundable(project, "正在加载话题详情...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val detail = dataService.getTopicDetail(topic.id)
                    SwingUtilities.invokeLater {
                        if (detail != null) {
                            topicDetailPanel.setTopic(detail)
                            cardLayout.show(contentPanel, DETAIL_VIEW)
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        topicListPanel.showError("加载详情失败: ${e.message}")
                    }
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
