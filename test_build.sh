#!/bin/bash

# 测试构建脚本 - 验证新的文件名配置

echo "🚀 开始测试构建..."

# 清理之前的构建
echo "📁 清理之前的构建..."
./gradlew clean

# 构建 Debug 版本
echo "🔨 构建 Debug 版本..."
./gradlew assembleDebug

# 检查 Debug APK 文件
echo "🔍 检查 Debug APK 文件..."
DEBUG_APK=$(find app/build/outputs/apk/debug/ -name "smsforward-v*-debug.apk" | head -1)
if [ -f "$DEBUG_APK" ]; then
    echo "✅ Debug APK 构建成功: $(basename "$DEBUG_APK")"
    ls -la "$DEBUG_APK"
else
    echo "❌ Debug APK 构建失败"
    echo "📂 Debug 目录内容:"
    ls -la app/build/outputs/apk/debug/
fi

# 构建 Release 版本（如果有密钥库）
if [ -f "keystore.properties" ]; then
    echo "🔨 构建 Release 版本..."
    ./gradlew assembleRelease
    
    # 检查 Release APK 文件
    echo "🔍 检查 Release APK 文件..."
    RELEASE_APK=$(find app/build/outputs/apk/release/ -name "smsforward-v*.apk" | head -1)
    if [ -f "$RELEASE_APK" ]; then
        echo "✅ Release APK 构建成功: $(basename "$RELEASE_APK")"
        ls -la "$RELEASE_APK"
    else
        echo "❌ Release APK 构建失败"
        echo "📂 Release 目录内容:"
        ls -la app/build/outputs/apk/release/
    fi
    
    # 构建 AAB
    echo "🔨 构建 Release AAB..."
    ./gradlew bundleRelease
    
    # 检查 AAB 文件
    echo "🔍 检查 AAB 文件..."
    AAB_FILE=$(find app/build/outputs/bundle/release/ -name "*.aab" | head -1)
    if [ -f "$AAB_FILE" ]; then
        echo "✅ AAB 构建成功: $(basename "$AAB_FILE")"
        ls -la "$AAB_FILE"
    else
        echo "❌ AAB 构建失败"
        echo "📂 Bundle 目录内容:"
        ls -la app/build/outputs/bundle/release/
    fi
else
    echo "⚠️  未找到 keystore.properties，跳过 Release 构建"
    echo "💡 如需测试 Release 构建，请先运行: ./create_keystore.sh"
fi

echo "🎯 构建测试完成！"
