#!/bin/bash

# AppCloneX 手动构建脚本
# 使用Android SDK工具直接构建APK

set -e

# 配置
ANDROID_SDK=/opt/android-sdk
BUILD_TOOLS=$ANDROID_SDK/build-tools/33.0.2
PLATFORM=$ANDROID_SDK/platforms/android-33
PROJECT_DIR=/workspace/AppCloneX
APP_DIR=$PROJECT_DIR/app
BUILD_DIR=$APP_DIR/build
GEN_DIR=$BUILD_DIR/gen
OBJ_DIR=$BUILD_DIR/intermediates
OUT_DIR=$BUILD_DIR/outputs/apk/debug
MANIFEST=$APP_DIR/src/main/AndroidManifest.xml

# 清理
rm -rf $BUILD_DIR
mkdir -p $GEN_DIR $OBJ_DIR $OUT_DIR

echo "=== AppCloneX APK 手动构建 ==="
echo ""

# 1. 生成R.java
echo "[1/7] 生成资源文件..."
$BUILD_TOOLS/aapt package -f -m -J $GEN_DIR -S $APP_DIR/src/main/res -M $MANIFEST -I $PLATFORM/android.jar

# 2. 编译Java源码
echo "[2/7] 编译Java源码..."
find $GEN_DIR -name "*.java" > /tmp/sources.txt 2>/dev/null || true
find $APP_DIR/src/main/java -name "*.java" >> /tmp/sources.txt

javac -source 11 -target 11 \
    -d $OBJ_DIR \
    -classpath $PLATFORM/android.jar \
    -sourcepath $GEN_DIR:$APP_DIR/src/main/java \
    @/tmp/sources.txt

# 3. 打包DEX
echo "[3/7] 打包DEX文件..."
$BUILD_TOOLS/d8 --output $OBJ_DIR \
    $(find $OBJ_DIR -name "*.class")

# 4. 打包资源（包含AndroidManifest）
echo "[4/7] 打包资源文件..."
$BUILD_TOOLS/aapt package -f \
    -F $OBJ_DIR/app-unsigned.apk \
    -S $APP_DIR/src/main/res \
    -M $MANIFEST \
    -I $PLATFORM/android.jar

# 5. 添加DEX到APK
echo "[5/7] 添加DEX文件..."
cd $OBJ_DIR
$BUILD_TOOLS/aapt add app-unsigned.apk classes.dex
cd $PROJECT_DIR

# 6. 对齐
echo "[6/7] 对齐APK..."
$BUILD_TOOLS/zipalign -f 4 $OBJ_DIR/app-unsigned.apk $OUT_DIR/app-debug.apk

# 创建调试密钥（如果不存在）
if [ ! -f ~/.android/debug.keystore ]; then
    echo "创建调试密钥..."
    mkdir -p ~/.android
    keytool -genkey -v -keystore ~/.android/debug.keystore \
        -alias androiddebugkey \
        -storepass android \
        -keypass android \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null
fi

# 7. 签名
echo "[7/7] 签名APK..."
$BUILD_TOOLS/apksigner sign \
    --ks ~/.android/debug.keystore \
    --ks-pass pass:android \
    --key-pass pass:android \
    --min-sdk-version 24 \
    $OUT_DIR/app-debug.apk

echo ""
echo "=== 构建完成 ==="
ls -lh $OUT_DIR/app-debug.apk
echo ""
echo "APK路径: $OUT_DIR/app-debug.apk"
