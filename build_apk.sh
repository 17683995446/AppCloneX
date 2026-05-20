#!/bin/bash

# AppCloneX APK 构建脚本
# 此脚本需要在配置好Android SDK和Gradle的环境中运行

set -e

echo "=========================================="
echo "AppCloneX APK 构建脚本"
echo "=========================================="

# 检查环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java，请先安装JDK 17"
    exit 1
fi

if [ -z "$ANDROID_HOME" ]; then
    echo "警告: ANDROID_HOME环境变量未设置"
    echo "请设置Android SDK路径: export ANDROID_HOME=/path/to/android-sdk"
fi

# 显示Java版本
echo ""
echo "Java版本:"
java -version 2>&1 | head -3

# 清理
echo ""
echo "[1/4] 清理构建目录..."
rm -rf app/build

# 构建Debug APK
echo ""
echo "[2/4] 构建Debug APK..."
./gradlew assembleDebug --stacktrace

# 检查构建结果
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    echo "[3/4] Debug APK构建成功!"
    ls -lh app/build/outputs/apk/debug/app-debug.apk
    
    # 复制到输出目录
    mkdir -p output
    cp app/build/outputs/apk/debug/app-debug.apk output/AppCloneX-v1.0.0-debug.apk
    echo ""
    echo "APK已复制到: output/AppCloneX-v1.0.0-debug.apk"
else
    echo "错误: APK构建失败"
    exit 1
fi

# 构建Release APK (可选)
echo ""
echo "[4/4] 构建Release APK..."
./gradlew assembleRelease --stacktrace || echo "Release构建失败（可能需要签名配置）"

echo ""
echo "=========================================="
echo "构建完成!"
echo "=========================================="
