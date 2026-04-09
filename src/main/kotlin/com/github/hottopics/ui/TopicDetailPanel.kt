package com.github.hottopics.ui

import com.github.hottopics.model.Reply
import com.github.hottopics.model.Topic
import com.github.hottopics.util.TextUtils
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

/**
 * 话题详情面板（暗色模式适配）
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
        val toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)

        val mainContent = JPanel()
        mainContent.layout = BoxLayout(mainContent, BoxLayout.Y_AXIS)
        mainContent.background = JBColor.PanelBackground

        headerPanel.layout = BorderLayout()
        headerPanel.border = JBUI.Borders.empty(10)
        headerPanel.background = JBColor.PanelBackground

        contentPanel.layout = BorderLayout()
        contentPanel.border = JBUI.Borders.empty(10, 15, 10, 15)
        contentPanel.background = JBColor.PanelBackground

        repliesPanel.layout = BoxLayout(repliesPanel, BoxLayout.Y_AXIS)
        repliesPanel.background = JBColor.PanelBackground

        mainContent.add(headerPanel)
        mainContent.add(createSeparator())
        mainContent.add(contentPanel)
        mainContent.add(createSeparator())
        mainContent.add(repliesPanel)

        val scrollPane = JBScrollPane(mainContent)
        scrollPane.border = JBUI.Borders.empty()
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER

        add(scrollPane, BorderLayout.CENTER)
    }

    /** 创建适配暗色模式的分隔线 */
    private fun createSeparator(): JSeparator {
        return JSeparator(JSeparator.HORIZONTAL).apply {
            foreground = JBColor(0xDDDDDD, 0x444444)
        }
    }

    private fun createToolbar(): JComponent {
        val toolbar = JPanel(BorderLayout())
        toolbar.border = JBUI.Borders.empty(5, 10)
        toolbar.background = JBColor.PanelBackground

        val backButton = JButton("← 返回列表").apply {
            addActionListener { onBackClick() }
        }
        toolbar.add(backButton, BorderLayout.WEST)

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

        val titleLabel = JBLabel("<html><body style='width: 350px; font-size: 16px; font-weight: bold;'>${TextUtils.escapeHtml(topic.title)}</body></html>")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
        titleLabel.border = JBUI.Borders.emptyBottom(10)
        headerPanel.add(titleLabel, BorderLayout.NORTH)

        val metaPanel = JPanel(FlowLayout(FlowLayout.LEFT, 15, 0))
        metaPanel.background = JBColor.PanelBackground

        val authorLabel = JBLabel("👤 ${topic.author}", AllIcons.General.User, SwingConstants.LEFT)
        metaPanel.add(authorLabel)

        val timeLabel = JBLabel("📅 ${topic.createTime}")
        metaPanel.add(timeLabel)

        val sourceLabel = JBLabel("📍 ${topic.source.displayName}")
        sourceLabel.foreground = JBColor.GRAY
        metaPanel.add(sourceLabel)

        val statsLabel = JBLabel("👁 ${TextUtils.formatCount(topic.viewCount)}  💬 ${topic.replyCount}  ❤ ${topic.likeCount}")
        statsLabel.foreground = JBColor(0x448AFF, 0x6D9FFF)
        metaPanel.add(statsLabel)

        headerPanel.add(metaPanel, BorderLayout.CENTER)

        // 标签（暗色模式适配）
        if (topic.tags.isNotEmpty()) {
            val tagsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
            tagsPanel.background = JBColor.PanelBackground
            tagsPanel.border = JBUI.Borders.emptyTop(8)

            topic.tags.forEach { tag ->
                val tagLabel = JBLabel(tag)
                tagLabel.isOpaque = true
                tagLabel.background = JBColor(0xE8F4FD, 0x2A3A4A)
                tagLabel.foreground = JBColor(0x0066CC, 0x6699CC)
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

        val contentPane = JEditorPane()
        contentPane.contentType = "text/html"
        contentPane.isEditable = false
        contentPane.background = JBColor.PanelBackground

        val kit = HTMLEditorKit()
        val styleSheet = StyleSheet()
        styleSheet.addRule("body { font-family: sans-serif; font-size: 13px; line-height: 1.6; }")
        styleSheet.addRule("p { margin: 0 0 10px 0; }")
        styleSheet.addRule("ul, ol { margin: 10px 0; }")
        styleSheet.addRule("li { margin: 5px 0; }")
        styleSheet.addRule("code { font-family: monospace; }")
        styleSheet.addRule("a { color: #448AFF; }")
        kit.styleSheet = styleSheet
        contentPane.editorKit = kit

        val htmlContent = TextUtils.convertToHtml(topic.content)
        contentPane.text = "<html><body style='width: 350px;'>$htmlContent</body></html>"

        contentPanel.add(contentPane, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun updateReplies(replies: List<Reply>) {
        repliesPanel.removeAll()

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
                    val separator = createSeparator()
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
        styleSheet.addRule("body { font-family: sans-serif; font-size: 12px; line-height: 1.5; }")
        kit.styleSheet = styleSheet
        contentPane.editorKit = kit

        val htmlContent = TextUtils.convertToHtml(reply.content)
        contentPane.text = "<html><body style='width: 330px;'>$htmlContent</body></html>"
        contentPane.border = JBUI.Borders.emptyTop(8)

        panel.add(contentPane, BorderLayout.CENTER)

        // 回复对象
        reply.replyTo?.let { replyTo ->
            val replyToLabel = JBLabel("回复 @${replyTo}")
            replyToLabel.foreground = JBColor(0x448AFF, 0x6D9FFF)
            replyToLabel.border = JBUI.Borders.emptyTop(5)
            panel.add(replyToLabel, BorderLayout.SOUTH)
        }

        return panel
    }
}
