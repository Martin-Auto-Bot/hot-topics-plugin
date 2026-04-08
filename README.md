# Hot Topics Browser - IntelliJ IDEA 插件

一个用于浏览热门话题的 IntelliJ IDEA / GoLand 插件。

## 🚀 快速获取插件（无需本地环境）

### 方法：GitHub Actions 自动构建

**您不需要安装 JDK 或 Gradle！** 只需将代码推送到 GitHub，云端自动构建。

#### 步骤 1：创建 GitHub 仓库

1. 登录 GitHub
2. 点击右上角 `+` → `New repository`
3. 填写仓库名称，如 `hot-topics-plugin`
4. 选择 `Public` 或 `Private`
5. 点击 `Create repository`

#### 步骤 2：推送代码

```bash
# 解压下载的 hot-topics-plugin.zip
cd hot-topics-plugin

# 初始化 Git（如果还没有）
git init
git add .
git commit -m "Initial commit"

# 推送到 GitHub（替换为你的用户名）
git remote add origin https://github.com/你的用户名/hot-topics-plugin.git
git branch -M main
git push -u origin main
```

#### 步骤 3：等待构建完成

1. 访问你的 GitHub 仓库页面
2. 点击 `Actions` 标签
3. 等待构建完成（约 3-5 分钟）

#### 步骤 4：下载插件

1. 构建完成后，点击该 workflow
2. 在页面底部 `Artifacts` 区域找到 `hot-topics-plugin`
3. 点击下载，得到 `hot-topics-plugin.zip`

#### 步骤 5：安装到 GoLand

1. 打开 GoLand
2. `Settings` → `Plugins` → ⚙️ → `Install Plugin from Disk...`
3. 选择下载的 zip 文件
4. 重启 GoLand
5. 在右侧边栏找到 `Hot Topics` 面板

---

## 🎮 手动触发构建

如果自动构建没有触发：

1. 进入 GitHub 仓库 → `Actions` 标签
2. 点击左侧 `Build Plugin`
3. 点击右侧 `Run workflow` → `Run workflow`
4. 等待构建完成后下载

---

## 📋 功能

- ✅ 多数据源：V2EX、知乎、微博
- ✅ 话题列表浏览
- ✅ 点击查看详情和回复
- ✅ 切换数据源
- ✅ 刷新功能

---

## ❓ 常见问题

### Q: 我没有 GitHub 账号怎么办？

A: GitHub 注册免费且简单，访问 [github.com](https://github.com) 即可注册。

### Q: 可以不用 GitHub 吗？

A: 如果您有其他 CI/CD 平台（如 GitLab CI、Jenkins 等），也可以配置类似的自动构建流程。

### Q: 构建失败怎么办？

A: 查看 Actions 标签中的错误日志，通常是依赖下载失败，可以重新运行 workflow。

---

## 📁 项目结构

```
hot-topics-plugin/
├── .github/workflows/build.yml  # GitHub Actions 构建配置
├── build.gradle.kts             # Gradle 构建脚本
├── src/main/
│   ├── kotlin/                  # Kotlin 源代码
│   └── resources/META-INF/
│       └── plugin.xml           # 插件配置
└── README.md
```

## 许可证

MIT License
