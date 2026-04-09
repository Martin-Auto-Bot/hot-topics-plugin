package com.github.hottopics.ui

import com.github.hottopics.model.SourceType
import com.github.hottopics.model.Topic
import com.github.hottopics.util.TextUtils
import com.github.hottopics.util.ThemeUtils
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
import javax.swing.border.EmptyBorder
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * 话题列表面板（支持翻页 + 暗色模式）
 */
class TopicListPanel(
    private val onTopicClick: (Topic) -> Unit,
    private val onOpenInBrowser: (Topic) -> Unit,
    private val onPageChange: (Int) -> Unit
) : JPanel(BorderLayout()) {

    private val listModel = DefaultListModel<Topic>()
    private val topicList: JBList<Topic> = JBList(listModel)
    private val loadingPanel = JPanel(BorderLayout())
    private val contentPanel = JPanel(CardLayout())

    private val statusLabel = JBLabel("加载中...")

    private var currentPage = 1
    private var hasMore = true

    init {
        setupUI()
    }

    private fun setupUI() {
        // 设置列表渲染器
        topicList.cellRenderer = TopicListCellRenderer()
        topicList.fixedCellHeight = 80
        topicList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        topicList.emptyText.text = "暂无话题数据"
        topicList.border = JBUI.Borders.empty()

        // 列表点击选中 → 触发详情跳转
        topicList.addListSelectionListener(object : ListSelectionListener {
            override fun valueChanged(e: ListSelectionEvent) {
                if (e.valueIsAdjusting) return
                val index = topicList.selectedIndex
                if (index >= 0 && index < listModel.size) {
                    val topic = listModel.getElementAt(index)
                    onTopicClick(topic)
                }
            }
        })

        // 右键菜单
        topicList.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                showPopupMenu(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                showPopupMenu(e)
            }

            private fun showPopupMenu(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    val index = topicList.locationToIndex(e.point)
                    if (index >= 0 && index < listModel.size) {
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
        scrollPane.background = ThemeUtils.panelBackground

        // 加载面板
        loadingPanel.add(JBLabel("加载中...", AllIcons.Process.Step_1, SwingConstants.CENTER), BorderLayout.CENTER)
        loadingPanel.background = ThemeUtils.panelBackground

        // 空状态面板
        val emptyPanel = JPanel(BorderLayout())
        val emptyLabel = JBLabel("暂无话题数据", AllIcons.General.Warning, SwingConstants.CENTER)
        emptyPanel.add(emptyLabel, BorderLayout.CENTER)
        emptyPanel.background = ThemeUtils.panelBackground

        // 错误状态面板
        val errorPanel = JPanel(BorderLayout())
        val errorLabel = JBLabel("加载失败，请检查网络后重试", AllIcons.General.Error, SwingConstants.CENTER)
        errorPanel.add(errorLabel, BorderLayout.CENTER)
        errorPanel.background = ThemeUtils.panelBackground

        // 内容面板
        contentPanel.add(scrollPane, CONTENT_VIEW)
        contentPanel.add(loadingPanel, LOADING_VIEW)
        contentPanel.add(emptyPanel, EMPTY_VIEW)
        contentPanel.add(errorPanel, ERROR_VIEW)
        contentPanel.background = ThemeUtils.panelBackground

        add(contentPanel, BorderLayout.CENTER)

        // 底部：翻页 + 状态
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.background = ThemeUtils.panelBackground
        bottomPanel.border = EmptyBorder(5, 8, 5, 8)

        // 翻页按钮
        val paginationPanel = JPanel(FlowLayout(FlowLayout.CENTER, 5, 0))
        paginationPanel.background = ThemeUtils.panelBackground

        val prevButton = createStyledButton("← 上一页")
        prevButton.addActionListener {
            if (currentPage > 1) {
                currentPage--
                onPageChange(currentPage)
            }
        }

        val pageLabel = JBLabel("第 1 页")
        pageLabel.foreground = ThemeUtils.textSecondary

        val nextButton = createStyledButton("下一页 →")
        nextButton.addActionListener {
            if (hasMore) {
                currentPage++
                onPageChange(currentPage)
            }
        }

        paginationPanel.add(prevButton)
        paginationPanel.add(Box.createHorizontalStrut(10))
        paginationPanel.add(pageLabel)
        paginationPanel.add(Box.createHorizontalStrut(10))
        paginationPanel.add(nextButton)

        bottomPanel.add(paginationPanel, BorderLayout.CENTER)

        // 状态栏
        val statusBar = JPanel(FlowLayout(FlowLayout.LEFT))
        statusBar.background = ThemeUtils.panelBackground
        statusLabel.foreground = ThemeUtils.textSecondary
        statusBar.add(statusLabel)
        bottomPanel.add(statusBar, BorderLayout.SOUTH)

        add(bottomPanel, BorderLayout.SOUTH)

        showView(CONTENT_VIEW)
    }

    /** 创建暗色模式友好的按钮 */
    private fun createStyledButton(text: String): JButton {
        return JButton(text).apply {
            foreground = ThemeUtils.buttonForeground
            border = JBUI.Borders.customLine(ThemeUtils.separatorColor, 1)
            isContentAreaFilled = false
            isOpaque = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            // 悬停效果
            val rolloverListener = object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    isContentAreaFilled = true
                    background = ThemeUtils.buttonHoverBackground
                }
                override fun mouseExited(e: MouseEvent) {
                    isContentAreaFilled = false
                    background = null
                }
            }
            addMouseListener(rolloverListener)
        }
    }

    /**
     * 设置话题列表并更新翻页状态
     */
    fun setTopics(topics: List<Topic>, page: Int, isLastPage: Boolean) {
        listModel.clear()
        topics.forEach { listModel.addElement(it) }
        currentPage = page
        hasMore = !isLastPage

        statusLabel.text = "第 ${page} 页 · 共 ${topics.size} 条"
        updatePagination()

        if (topics.isEmpty()) {
            showView(EMPTY_VIEW)
        } else {
            showView(CONTENT_VIEW)
        }
    }

    /** 兼容旧调用（非 V2EX 数据源无翻页） */
    fun setTopics(topics: List<Topic>) {
        setTopics(topics, 1, true)
    }

    fun setLoading(loading: Boolean) {
        if (loading) {
            showView(LOADING_VIEW)
            statusLabel.text = "加载中..."
        }
    }

    fun showError(message: String) {
        statusLabel.text = message
        topicList.emptyText.text = message
        showView(ERROR_VIEW)
    }

    fun resetPage() {
        currentPage = 1
        updatePagination()
    }

    private fun updatePagination() {
        val bottomPanel = getComponent(1) as JPanel
        val paginationPanel = bottomPanel.getComponent(0) as JPanel
        val prevButton = paginationPanel.getComponent(0) as JButton
        val pageLabel = paginationPanel.getComponent(2) as JBLabel
        val nextButton = paginationPanel.getComponent(4) as JButton

        prevButton.isEnabled = currentPage > 1
        prevButton.foreground = if (currentPage > 1) ThemeUtils.buttonForeground else ThemeUtils.textSecondary
        pageLabel.text = "第 $currentPage 页"
        nextButton.isEnabled = hasMore
        nextButton.foreground = if (hasMore) ThemeUtils.buttonForeground else ThemeUtils.textSecondary
    }

    private fun showView(viewName: String) {
        (contentPanel.layout as CardLayout).show(contentPanel, viewName)
    }

    private fun showContextMenu(e: MouseEvent, topic: Topic) {
        val popup = JPopupMenu()
        popup.background = ThemeUtils.panelBackground

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
 * 话题列表单元格渲染器（暗色模式完整适配）
 */
class TopicListCellRenderer : JPanel(BorderLayout()), ListCellRenderer<Topic> {

    private val titleLabel = JBLabel().apply {
        font = font.deriveFont(Font.BOLD, 13f)
        foreground = ThemeUtils.textForeground
    }
    private val metaLabel = JBLabel().apply {
        foreground = ThemeUtils.textSecondary
        font = font.deriveFont(11f)
    }
    private val statsLabel = JBLabel().apply {
        foreground = ThemeUtils.accentColor
        font = font.deriveFont(11f)
    }
    private val sourceBadge = JBLabel().apply {
        font = font.deriveFont(10f)
        isOpaque = true
        border = JBUI.Borders.empty(2, 6)
    }

    init {
        border = JBUI.Borders.empty(8, 10)
        isOpaque = true

        val topPanel = JPanel(BorderLayout())
        topPanel.isOpaque = false
        topPanel.add(titleLabel, BorderLayout.CENTER)
        topPanel.add(sourceBadge, BorderLayout.EAST)

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
        titleLabel.text = TextUtils.truncateText(topic.title, 50)

        sourceBadge.text = " ${topic.source.displayName} "
        sourceBadge.background = getSourceColor(topic.source)
        sourceBadge.foreground = Color.WHITE

        metaLabel.text = "${topic.author} · ${topic.createTime}"

        statsLabel.text = "💬 ${topic.replyCount}  👁 ${TextUtils.formatCount(topic.viewCount)}  ❤ ${topic.likeCount}"

        // 暗色模式适配
        if (isSelected) {
            background = ThemeUtils.selectionBackground
        } else {
            background = ThemeUtils.panelBackground
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
}
