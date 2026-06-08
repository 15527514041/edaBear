#!/bin/sh

# 易达熊 Gradle 构建脚本
# 使用本地已解压的 Gradle（绕过 macOS 锁文件权限问题）

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export JAVA_HOME

# 使用项目内缓存目录（绕过 ~/.gradle 的锁文件权限问题）
PROJECT_GRADLE_CACHE="$APP_HOME/.gradle-cache"
mkdir -p "$PROJECT_GRADLE_CACHE/caches"
export GRADLE_USER_HOME="$PROJECT_GRADLE_CACHE"

# 使用本地已解压的 Gradle 二进制
GRADLE_BIN="$HOME/.gradle/wrapper/dists/gradle-8.11.1-bin/bpt9gzteqjrbo1mjrsomdt32c/gradle-8.11.1/bin/gradle"

if [ -x "$GRADLE_BIN" ]; then
    cd "$APP_HOME" || exit 1
    exec "$GRADLE_BIN" "$@"
fi

# 兜底：用原始 wrapper
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec "$JAVA_HOME/bin/java" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
