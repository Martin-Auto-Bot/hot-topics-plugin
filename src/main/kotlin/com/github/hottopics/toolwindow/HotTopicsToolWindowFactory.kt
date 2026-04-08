package com.github.hottopics.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Hot Topics 工具窗口工厂
 */
class HotTopicsToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val panel = HotTopicsPanel(project)
        
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
    
    override fun init(toolWindow: ToolWindow) {
        // 设置工具窗口图标和显示名称
        toolWindow.stripeTitle = "Hot Topics"
    }
}
