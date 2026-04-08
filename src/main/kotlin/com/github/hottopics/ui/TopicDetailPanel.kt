package com.github.hottopics.ui

import com.github.hottopics.model.Reply
import com.github.hottopics.model.Topic
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

/**
 * 话题详情面板
 */
class TopicDetailPanel(
    private val onBackClick: () -> Unit,
    private val onOpenInBrowser: (Topic) -> Unit
) : JPanel(BorderLayout()) {
    
    private var currentTopic: Topic? = null
    
    private val headerPanel = JPanel(BorderLayout())
    private val contentPanel = JPanel()
    private val repliesPanel = JPanel()
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        // 顶部工具栏
        val toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)
        
        // 主内容区域（可滚动）
        val mainContent = JPanel()
        mainContent.layout = BoxLayout(mainContent, BoxLayout.Y_AXIS)
        mainContent.background = JBColor.PanelBackground
        
        // 话题头部信息
        headerPanel.layout = BorderLayout()
        headerPanel.border = JBUI.Borders.empty(10)
        headerPanel.background = JBColor.PanelBackground
        
        // 话题内容
        contentPanel.layout = BorderLayout()
        contentPanel.border = JBUI.Borders.empty(10, 15, 10, 15)
        contentPanel.background = JBColor.PanelBackground
        
        // 回复列表
        repliesPanel.layout = BoxLayout(repliesPanel, BoxLayout.Y_AXIS)
        repliesPanel.background = JBColor.PanelBackground
        
        mainContent.add(headerPanel)
        mainContent.add(JSeparator(JSeparator.HORIZONTAL))
        mainContent.add(contentPanel)
        mainContent.add(JSeparator(JSeparator.HORIZONTAL))
        mainContent.add(repliesPanel)
        
        val scrollPane = JBScrollPane(mainContent)
        scrollPane.border = JBUI.Borders.empty()
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        
        add(scrollPane, BorderLayout.CENTER)
    }
    
    private fun createToolbar(): JComponent {
        val toolbar = JPanel(BorderLayout())
        toolbar.border = JBUI.Borders.empty(5, 10)
        toolbar.background = JBColor.PanelBackground
        
        // 返回按钮
        val backButton = JButton("← 返回列表").apply {
            addActionListener { onBackClick() }
        }
        toolbar.add(backButton, BorderLayout.WEST)
        
        // 操作按钮
        val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))
        actionsPanel.background = JBColor.PanelBackground
        
        val browserButton = JButton("在浏览器打开", AllIcons.General.Web).apply {
            addActionListener { currentTopic?.let { onOpenInBrowser(it) } }
        }
        actionsPanel.add(browserButton)
        
        toolbar.add(actionsPanel, BorderLayout.EAST)
        
        return toolbar
    }
    
    fun setTopic(topic: Topic) {
        currentTopic = topic
        updateHeader(topic)
        updateContent(topic)
        updateReplies(topic.replies)
    }
    
    private fun updateHeader(topic: Topic) {
        headerPanel.removeAll()
        
        // 标题
        val titleLabel = JBLabel("<html><body style='width: 350px; font-size: 16px; font-weight: bold;'>${escapeHtml(topic.title)}</body></html>")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
        titleLabel.border = JBUI.Borders.emptyBottom(10)
        headerPanel.add(titleLabel, BorderLayout.NORTH)
        
        // 元信息
        val metaPanel = JPanel(FlowLayout(FlowLayout.LEFT, 15, 0))
        metaPanel.background = JBColor.PanelBackground
        
        val authorLabel = JBLabel("👤 ${topic.author}", AllIcons.General.User, SwingConstants.LEFT)
        metaPanel.add(authorLabel)
        
        val timeLabel = JBLabel("📅 ${topic.createTime}")
        metaPanel.add(timeLabel)
        
        val sourceLabel = JBLabel("📍 ${topic.source.displayName}")
        sourceLabel.foreground = JBColor.GRAY
        metaPanel.add(sourceLabel)
        
        // 统计信息
        val statsLabel = JBLabel("👁 ${formatCount(topic.viewCount)}  💬 ${topic.replyCount}  ❤ ${topic.likeCount}")
        statsLabel.foreground = JBColor.BLUE
        metaPanel.add(statsLabel)
        
        headerPanel.add(metaPanel, BorderLayout.CENTER)
        
        // 标签
        if (topic.tags.isNotEmpty()) {
            val tagsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
            tagsPanel.background = JBColor.PanelBackground
            tagsPanel.border = JBUI.Borders.emptyTop(8)
            
            topic.tags.forEach { tag ->
                val tagLabel = JBLabel(tag)
                tagLabel.isOpaque = true
                tagLabel.background = Color(0xE8, 0xF4, 0xFD)
                tagLabel.foreground = Color(0x00, 0x66, 0xCC)
                tagLabel.border = JBUI.Borders.empty(2, 8)
                tagsPanel.add(tagLabel)
            }
            
            headerPanel.add(tagsPanel, BorderLayout.SOUTH)
        }
        
        headerPanel.revalidate()
        headerPanel.repaint()
    }
    
    private fun updateContent(topic: Topic) {
        contentPanel.removeAll()
        
        // 内容区域
        val contentPane = JEditorPane()
        contentPane.contentType = "text/html"
        contentPane.isEditable = false
        contentPane.background = JBColor.PanelBackground
        
        // 设置 HTML 样式
        val kit = HTMLEditorKit()
        val styleSheet = StyleSheet()
        styleSheet.addRule("body { font-family: sans-serif; font-size: 13px; line-height: 1.6; color: #333; }")
        styleSheet.addRule("p { margin: 0 0 10px 0; }")
        styleSheet.addRule("ul, ol { margin: 10px 0; padding-left: 20px; }")
        styleSheet.addRule("li { margin: 5px 0; }")
        styleSheet.addRule("code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-family: monospace; }")
        kit.styleSheet = styleSheet
        contentPane.editorKit = kit
        
        // 将内容转换为 HTML 格式
        val htmlContent = convertToHtml(topic.content)
        contentPane.text = "<html><body style='width: 350px;'>$htmlContent</body></html>"
        
        // 设置固定宽度
        contentPane.preferredSize = Dimension(380, Int.MAX_VALUE)
        
        contentPanel.add(contentPane, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()
    }
    
    private fun updateReplies(replies: List<Reply>) {
        repliesPanel.removeAll()
        
        // 回复标题
        val titleLabel = JBLabel("💬 回复 (${replies.size})")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 14f)
        titleLabel.border = JBUI.Borders.empty(15, 15, 10, 15)
        repliesPanel.add(titleLabel)
        
        if (replies.isEmpty()) {
            val emptyLabel = JBLabel("暂无回复")
            emptyLabel.foreground = JBColor.GRAY
            emptyLabel.border = JBUI.Borders.empty(10, 15)
            repliesPanel.add(emptyLabel)
        } else {
            replies.forEachIndexed { index, reply ->
                val replyPanel = createReplyPanel(reply, index + 1)
                repliesPanel.add(replyPanel)
                
                if (index < replies.size - 1) {
                    val separator = JSeparator(JSeparator.HORIZONTAL)
                    separator.border = JBUI.Borders.empty(0, 15)
                    repliesPanel.add(separator)
                }
            }
        }
        
        repliesPanel.revalidate()
        repliesPanel.repaint()
    }
    
    private fun createReplyPanel(reply: Reply, index: Int): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10, 15)
        panel.background = JBColor.PanelBackground
        panel.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        
        // 头部：作者 + 时间 + 点赞
        val headerPanel = JPanel(BorderLayout())
        headerPanel.background = JBColor.PanelBackground
        
        val authorLabel = JBLabel("#$index ${reply.author}")
        authorLabel.font = authorLabel.font.deriveFont(Font.BOLD, 12f)
        headerPanel.add(authorLabel, BorderLayout.WEST)
        
        val metaPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0))
        metaPanel.background = JBColor.PanelBackground
        
        val timeLabel = JBLabel(reply.createTime)
        timeLabel.foreground = JBColor.GRAY
        metaPanel.add(timeLabel)
        
        val likeLabel = JBLabel("❤ ${reply.likeCount}")
        likeLabel.foreground = JBColor.RED
        metaPanel.add(likeLabel)
        
        headerPanel.add(metaPanel, BorderLayout.EAST)
        panel.add(headerPanel, BorderLayout.NORTH)
        
        // 回复内容
        val contentPane = JEditorPane()
        contentPane.contentType = "text/html"
        contentPane.isEditable = false
        contentPane.background = JBColor.PanelBackground
        
        val kit = HTMLEditorKit()
        val styleSheet = StyleSheet()
        styleSheet.addRule("body { font-family: sans-serif; font-size: 12px; line-height: 1.5; color: #555; }")
        kit.styleSheet = styleSheet
        contentPane.editorKit = kit
        
        val htmlContent = convertToHtml(reply.content)
        contentPane.text = "<html><body style='width: 330px;'>$htmlContent</body></html>"
        contentPane.border = JBUI.Borders.emptyTop(8)
        
        panel.add(contentPane, BorderLayout.CENTER)
        
        // 回复对象（如果有）
        reply.replyTo?.let { replyTo ->
            val replyToLabel = JBLabel("回复 @${replyTo}")
            replyToLabel.foreground = JBColor.BLUE
            replyToLabel.border = JBUI.Borders.emptyTop(5)
            panel.add(replyToLabel, BorderLayout.SOUTH)
        }
        
        return panel
    }
    
    private fun convertToHtml(text: String): String {
        // 简单的文本转 HTML
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
    
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
    
    private fun formatCount(count: Int): String {
        return when {
            count >= 10000 -> String.format("%.1fw", count / 10000.0)
            count >= 1000 -> String.format("%.1fk", count / 1000.0)
            else -> count.toString()
        }
    }
}
