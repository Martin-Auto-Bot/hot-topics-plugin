#!/bin/bash

# Hot Topics Plugin 构建脚本
# 使用方法: ./build.sh

set -e

echo "=========================================="
echo "  Hot Topics Plugin 构建脚本"
echo "=========================================="
echo ""

# 检查 Java 版本
echo ">>> 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "错误: 未找到 Java，请先安装 JDK 17+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "Java 版本: $JAVA_VERSION"

if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "错误: 需要 JDK 17 或更高版本"
    exit 1
fi

# 检查是否有 gradlew
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
else
    echo ">>> 未找到 Gradle，正在下载 Gradle Wrapper..."
    
    # 下载 gradle wrapper
    mkdir -p gradle/wrapper
    
    if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
        curl -sL "https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar" -o gradle/wrapper/gradle-wrapper.jar
    fi
    
    # 创建 gradlew 脚本
    cat > gradlew << 'GRADLEW_EOF'
#!/bin/sh
APP_BASE_NAME=${0##*/}
APP_HOME=$( cd "${APP_HOME:-./}" > /dev/null && pwd -P )
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
exec java $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
GRADLEW_EOF
    chmod +x gradlew
    GRADLE_CMD="./gradlew"
fi

echo "使用 Gradle: $GRADLE_CMD"
echo ""

# 构建插件
echo ">>> 开始构建插件..."
echo "    (首次构建需要下载依赖，请耐心等待...)"
echo ""

$GRADLE_CMD buildPlugin --no-daemon

# 检查构建结果
if [ -f "build/distributions/hot-topics-plugin-1.0.0.zip" ]; then
    echo ""
    echo "=========================================="
    echo "  ✅ 构建成功！"
    echo "=========================================="
    echo ""
    echo "插件文件位置:"
    echo "  build/distributions/hot-topics-plugin-1.0.0.zip"
    echo ""
    echo "安装方法:"
    echo "  1. 打开 IntelliJ IDEA"
    echo "  2. 进入 Settings → Plugins → ⚙️ → Install Plugin from Disk"
    echo "  3.选择上面的 zip 文件"
    echo "  4. 重启 IDEA"
    echo ""
else
    echo ""
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi
