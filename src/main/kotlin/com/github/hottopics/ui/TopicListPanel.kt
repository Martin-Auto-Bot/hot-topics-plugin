package com.github.hottopics.ui

import com.github.hottopics.model.SourceType
import com.github.hottopics.model.Topic
import com.github.hottopics.util.TextUtils
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * 话题列表面板
 */
class TopicListPanel(
    private val onTopicClick: (Topic) -> Unit,
    private val onOpenInBrowser: (Topic) -> Unit
) : JPanel(BorderLayout()) {
    
    private val listModel = DefaultListModel<Topic>()
    private val topicList: JBList<Topic> = JBList(listModel)
    private val loadingPanel = JPanel(BorderLayout())
    private val contentPanel = JPanel(CardLayout())
    
    private val statusLabel = JBLabel("加载中...")
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        // 设置列表渲染器
        topicList.cellRenderer = TopicListCellRenderer()
        topicList.fixedCellHeight = 80
        topicList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        
        // 添加鼠标监听器
        topicList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    val index = topicList.locationToIndex(e.point)
                    if (index >= 0) {
                        val topic = listModel.getElementAt(index)
                        onTopicClick(topic)
                    }
                }
            }
            
            override fun mousePressed(e: MouseEvent) {
                showPopupMenu(e)
            }
            
            override fun mouseReleased(e: MouseEvent) {
                showPopupMenu(e)
            }
            
            private fun showPopupMenu(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    val index = topicList.locationToIndex(e.point)
                    if (index >= 0) {
                        topicList.selectedIndex = index
                        val topic = listModel.getElementAt(index)
                        showContextMenu(e, topic)
                    }
                }
            }
        })
        
        // 滚动面板
        val scrollPane = JBScrollPane(topicList)
        scrollPane.border = JBUI.Borders.empty()
        
        // 加载面板
        loadingPanel.add(JBLabel("加载中...", AllIcons.Process.Step_1, SwingConstants.CENTER), BorderLayout.CENTER)
        loadingPanel.background = JBColor.PanelBackground
        
        // 内容面板
        contentPanel.add(scrollPane, CONTENT_VIEW)
        contentPanel.add(loadingPanel, LOADING_VIEW)
        contentPanel.add(createEmptyPanel(), EMPTY_VIEW)
        contentPanel.add(createErrorPanel(), ERROR_VIEW)
        
        add(contentPanel, BorderLayout.CENTER)
        
        // 底部状态栏
        val statusBar = JPanel(FlowLayout(FlowLayout.LEFT))
        statusBar.border = JBUI.Borders.emptyTop(5)
        statusBar.background = JBColor.PanelBackground
        statusBar.add(statusLabel)
        add(statusBar, BorderLayout.SOUTH)
        
        // 默认显示内容
        showView(CONTENT_VIEW)
    }
    
    private fun createEmptyPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = JBColor.PanelBackground
        
        val emptyLabel = JBLabel("暂无话题数据", AllIcons.General.Information, SwingConstants.CENTER)
        emptyLabel.foreground = JBColor.GRAY
        panel.add(emptyLabel, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createErrorPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = JBColor.PanelBackground
        
        val errorLabel = JBLabel("加载失败，请重试", AllIcons.General.Error, SwingConstants.CENTER)
        errorLabel.foreground = JBColor.RED
        panel.add(errorLabel, BorderLayout.CENTER)
        
        return panel
    }
    
    fun setTopics(topics: List<Topic>) {
        listModel.clear()
        topics.forEach { listModel.addElement(it) }
        statusLabel.text = "共 ${topics.size} 个话题"
        showView(CONTENT_VIEW)
    }
    
    fun setLoading(loading: Boolean) {
        if (loading) {
            showView(LOADING_VIEW)
            statusLabel.text = "加载中..."
        }
    }
    
    fun showError(message: String) {
        statusLabel.text = message
        showView(ERROR_VIEW)
    }
    
    private fun showView(viewName: String) {
        (contentPanel.layout as CardLayout).show(contentPanel, viewName)
    }
    
    private fun showContextMenu(e: MouseEvent, topic: Topic) {
        val popup = JPopupMenu()
        
        val openItem = JMenuItem("查看详情", AllIcons.Actions.Edit)
        openItem.addActionListener { onTopicClick(topic) }
        popup.add(openItem)
        
        val browserItem = JMenuItem("在浏览器打开", AllIcons.General.Web)
        browserItem.addActionListener { onOpenInBrowser(topic) }
        popup.add(browserItem)
        
        popup.addSeparator()
        
        val copyItem = JMenuItem("复制标题", AllIcons.Actions.Copy)
        copyItem.addActionListener {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val stringSelection = StringSelection(topic.title)
            clipboard.setContents(stringSelection, null)
        }
        popup.add(copyItem)
        
        popup.show(e.component, e.x, e.y)
    }
    
    companion object {
        private const val CONTENT_VIEW = "CONTENT"
        private const val LOADING_VIEW = "LOADING"
        private const val EMPTY_VIEW = "EMPTY"
        private const val ERROR_VIEW = "ERROR"
    }
}

/**
 * 话题列表单元格渲染器
 */
class TopicListCellRenderer : JPanel(BorderLayout()), ListCellRenderer<Topic> {
    
    private val titleLabel = JBLabel().apply {
        font = font.deriveFont(Font.BOLD, 13f)
    }
    private val metaLabel = JBLabel().apply {
        foreground = JBColor.GRAY
        font = font.deriveFont(11f)
    }
    private val statsLabel = JBLabel().apply {
        foreground = JBColor.BLUE
        font = font.deriveFont(11f)
    }
    private val sourceBadge = JBLabel().apply {
        font = font.deriveFont(10f)
        isOpaque = true
        border = JBUI.Borders.empty(2, 6)
    }
    
    init {
        border = JBUI.Borders.empty(8, 10)
        background = JBColor.PanelBackground
        
        // 顶部：标题 + 来源标签
        val topPanel = JPanel(BorderLayout())
        topPanel.isOpaque = false
        topPanel.add(titleLabel, BorderLayout.CENTER)
        topPanel.add(sourceBadge, BorderLayout.EAST)
        
        // 底部：作者 + 统计信息
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.isOpaque = false
        bottomPanel.add(metaLabel, BorderLayout.WEST)
        bottomPanel.add(statsLabel, BorderLayout.EAST)
        
        add(topPanel, BorderLayout.NORTH)
        add(bottomPanel, BorderLayout.SOUTH)
    }
    
    override fun getListCellRendererComponent(
        list: JList<out Topic>?,
        topic: Topic,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): JComponent {
        // 设置标题
        titleLabel.text = TextUtils.truncateText(topic.title, 50)
        
        // 设置来源标签
        sourceBadge.text = topic.source.displayName
        sourceBadge.background = getSourceColor(topic.source)
        sourceBadge.foreground = Color.WHITE
        
        // 设置元信息
        metaLabel.text = "${topic.author} · ${topic.createTime}"
        
        // 设置统计信息
        statsLabel.text = "💬 ${topic.replyCount}  👁 ${TextUtils.formatCount(topic.viewCount)}  ❤ ${topic.likeCount}"
        
        // 设置选中状态
        if (isSelected) {
            background = JBColor(ListCellRendererSelectedBackground, ListCellRendererSelectedBackground)
        } else {
            background = if (index % 2 == 0) JBColor.PanelBackground else JBColor(ListCellRendererAlternateBackground, ListCellRendererAlternateBackground)
        }
        
        return this
    }
    
    private fun getSourceColor(source: SourceType): Color {
        return when (source) {
            SourceType.V2EX -> Color(0x33, 0x33, 0x33)
            SourceType.ZHIHU -> Color(0x00, 0x86, 0xEB)
            SourceType.WEIBO -> Color(0xE6, 0x16, 0x2D)
            SourceType.CUSTOM -> Color(0x66, 0x66, 0x66)
        }
    }
    
    companion object {
        private val ListCellRendererSelectedBackground = Color(0x3D, 0x5A, 0x80)
        private val ListCellRendererAlternateBackground = Color(0xF5, 0xF5, 0xF5)
    }
}
