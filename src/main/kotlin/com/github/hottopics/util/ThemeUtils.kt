package com.github.hottopics.util

import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.UIManager

/**
 * 主题工具类 - 提供暗色/亮色模式检测和统一的颜色管理
 */
object ThemeUtils {

    /** 当前是否为暗色主题（Darcula / New UI Dark） */
    val isDarkTheme: Boolean
        get() {
            val bg = UIManager.getColor("Panel.background")
            return bg != null && bg.red < 128 && bg.green < 128 && bg.blue < 128
        }

    /** 面板背景色 */
    val panelBackground: Color get() = JBColor.PanelBackground

    /** 文本主色 */
    val textForeground: Color get() = JBColor(0x000000, 0xDDDDDD)

    /** 次要文本色（灰色） */
    val textSecondary: Color get() = JBColor.GRAY

    /** 链接/强调色 */
    val accentColor: Color get() = JBColor(0x448AFF, 0x6D9FFF)

    /** 分隔线颜色 */
    val separatorColor: Color get() = JBColor(0xDDDDDD, 0x444444)

    /** 选中行背景色 */
    val selectionBackground: Color get() = JBColor(0x3D5A80, 0x3D5A80)

    /** 标签背景色（亮色：浅蓝，暗色：深蓝灰） */
    val tagBackgroundColor: Color get() = JBColor(0xE8F4FD, 0x2A3A4A)

    /** 标签文字色 */
    val tagForegroundColor: Color get() = JBColor(0x0066CC, 0x6699CC)

    /** 按钮背景色 */
    val buttonBackground: Color get() = JBColor(0xF0F0F0, 0x3C3F41)

    /** 按钮悬停背景色 */
    val buttonHoverBackground: Color get() = JBColor(0xE0E0E0, 0x4C5052)

    /** 按钮文字色 */
    val buttonForeground: Color get() = JBColor(0x333333, 0xBBBBBB)

    /** HTML 编辑器背景色 */
    val editorBackground: Color get() = JBColor(0xFFFFFF, 0x2B2B2B)

    /** HTML 编辑器文本色 */
    val editorForeground: Color get() = JBColor(0x1A1A1A, 0xCCCCCC)

    /** HTML 编辑器链接色 */
    val editorLinkColor: Color get() = JBColor(0x448AFF, 0x6D9FFF)

    /**
     * 生成暗色模式自适应的 HTML body 样式
     */
    fun htmlBodyStyle(width: Int = 350): String {
        val bg = colorToHex(editorBackground)
        val fg = colorToHex(editorForeground)
        val link = colorToHex(editorLinkColor)
        return "width:${width}px; background-color:$bg; color:$fg; " +
                "font-family:sans-serif; font-size:13px; line-height:1.6;"
    }

    /**
     * 生成暗色模式自适应的 HTML style rules
     */
    fun htmlStyleRules(): String {
        val fg = colorToHex(editorForeground)
        val link = colorToHex(editorLinkColor)
        val bg = colorToHex(editorBackground)
        val codeBg = if (isDarkTheme) "#3C3F41" else "#F5F5F5"
        val codeFg = if (isDarkTheme) "#A9B7C6" else "#333333"

        return """
            body { background-color: $bg; color: $fg; }
            p { margin: 0 0 10px 0; }
            ul, ol { margin: 10px 0; padding-left: 20px; }
            li { margin: 5px 0; }
            code { padding: 2px 6px; border-radius: 3px; font-family: monospace; background-color: $codeBg; color: $codeFg; }
            a { color: $link; }
        """.trimIndent()
    }

    private fun colorToHex(color: Color): String {
        return "#${color.red.toString(16).padStart(2, '0')}" +
               "${color.green.toString(16).padStart(2, '0')}" +
               "${color.blue.toString(16).padStart(2, '0')}"
    }
}
