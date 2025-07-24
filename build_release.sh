#!/bin/bash

# SMS Forward App Release Build Script
# Author: hentiflo

echo "🚀 Starting SMS Forward App Release Build..."

# Set up Java environment
ANDROID_STUDIO_JDK="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [ -d "$ANDROID_STUDIO_JDK" ]; then
    export JAVA_HOME="$ANDROID_STUDIO_JDK"
    export PATH="$JAVA_HOME/bin:$PATH"
    print_status "Java environment set up using Android Studio JDK"
else
    print_error "Android Studio JDK not found. Please run ./setup_java_env.sh first"
    exit 1
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "app/build.gradle" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Check if keystore.properties exists
if [ ! -f "keystore.properties" ]; then
    print_error "keystore.properties not found!"
    print_status "Please run ./create_keystore.sh first to create a signing keystore"
    exit 1
fi

# Verify keystore file exists
KEYSTORE_FILE=$(grep "storeFile=" keystore.properties | cut -d'=' -f2)
if [ ! -f "$KEYSTORE_FILE" ]; then
    print_error "Keystore file not found: $KEYSTORE_FILE"
    print_status "Please run ./create_keystore.sh to create the keystore"
    exit 1
fi

print_success "Keystore configuration verified"

# Step 1: Clean the project
print_status "Cleaning project..."
./gradlew clean
if [ $? -eq 0 ]; then
    print_success "Project cleaned successfully"
else
    print_error "Failed to clean project"
    exit 1
fi

# Step 2: Run tests (optional)
read -p "Do you want to run tests before building? (y/n): " run_tests
if [ "$run_tests" = "y" ] || [ "$run_tests" = "Y" ]; then
    print_status "Running tests..."
    ./gradlew test
    if [ $? -eq 0 ]; then
        print_success "All tests passed"
    else
        print_warning "Some tests failed, but continuing with build..."
    fi
fi

# Step 3: Build release APK
print_status "Building release APK..."
./gradlew assembleRelease
if [ $? -eq 0 ]; then
    print_success "Release APK built successfully"
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        print_success "APK location: $APK_PATH"
        print_success "APK size: $APK_SIZE"

        # Verify APK signature
        print_status "Verifying APK signature..."
        if command -v apksigner &> /dev/null; then
            apksigner verify "$APK_PATH"
            if [ $? -eq 0 ]; then
                print_success "APK signature verified successfully"
            else
                print_warning "APK signature verification failed"
            fi
        else
            print_warning "apksigner not found, skipping signature verification"
        fi
    fi
else
    print_error "Failed to build release APK"
    exit 1
fi

# Step 4: Build release AAB (Android App Bundle)
print_status "Building release AAB..."
./gradlew bundleRelease
if [ $? -eq 0 ]; then
    print_success "Release AAB built successfully"
    AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
    if [ -f "$AAB_PATH" ]; then
        AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
        print_success "AAB location: $AAB_PATH"
        print_success "AAB size: $AAB_SIZE"
    fi
else
    print_error "Failed to build release AAB"
    exit 1
fi

# Step 5: Generate checksums
print_status "Generating checksums..."
if [ -f "$APK_PATH" ]; then
    APK_SHA256=$(shasum -a 256 "$APK_PATH" | cut -d' ' -f1)
    echo "$APK_SHA256  app-release.apk" > app-release.apk.sha256
    print_success "APK SHA256: $APK_SHA256"
fi

if [ -f "$AAB_PATH" ]; then
    AAB_SHA256=$(shasum -a 256 "$AAB_PATH" | cut -d' ' -f1)
    echo "$AAB_SHA256  app-release.aab" > app-release.aab.sha256
    print_success "AAB SHA256: $AAB_SHA256"
fi

# Step 6: Create release directory
RELEASE_DIR="release_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RELEASE_DIR"

# Copy files to release directory
if [ -f "$APK_PATH" ]; then
    cp "$APK_PATH" "$RELEASE_DIR/"
    cp "app-release.apk.sha256" "$RELEASE_DIR/"
fi

if [ -f "$AAB_PATH" ]; then
    cp "$AAB_PATH" "$RELEASE_DIR/"
    cp "app-release.aab.sha256" "$RELEASE_DIR/"
fi

# Get version information
VERSION_NAME=$(grep versionName app/build.gradle | sed 's/.*"\(.*\)".*/\1/')
VERSION_CODE=$(grep versionCode app/build.gradle | sed 's/.*\([0-9]\+\).*/\1/')

# Copy release notes template
cat > "$RELEASE_DIR/RELEASE_NOTES.md" << EOF
# SMS Forward App Release Notes

## Version: $VERSION_NAME (Build $VERSION_CODE)
## Build Date: $(date)
## Package: com.cht.smsforward

### 📱 What's New in This Release
- SMS转发功能优化
- 邮件发送稳定性提升
- 通知拦截兼容性改进
- 用户界面优化

### 🔧 Technical Details
- Target SDK: 36 (Android 14+)
- Minimum SDK: 34 (Android 14)
- Architecture: Universal APK

### 📦 Installation Instructions
1. 下载 APK 文件
2. 在 Android 设置中启用"未知来源安装"
3. 安装 APK 文件
4. 授予必要的权限：
   - 通知访问权限
   - 短信读取权限（如需要）
   - 网络访问权限

### 🔐 Security Information
- APK 已使用发布密钥签名
- SHA256 校验和已提供用于验证文件完整性
- 建议从官方 GitHub Releases 页面下载

### 📋 Checksums
- APK SHA256: $APK_SHA256
- AAB SHA256: $AAB_SHA256

### 🐛 Known Issues
- 部分设备可能需要手动配置通知权限
- 首次启动时需要完成权限设置

### 📞 Support
如有问题，请在 GitHub Issues 页面报告：
https://github.com/[your-username]/SMSForward3/issues
EOF

# Create installation guide
cat > "$RELEASE_DIR/INSTALLATION_GUIDE.md" << EOF
# SMS Forward App 安装指南

## 系统要求
- Android 14 (API 34) 或更高版本
- 至少 50MB 可用存储空间

## 安装步骤

### 1. 下载应用
从 GitHub Releases 页面下载最新的 APK 文件：
\`app-release.apk\`

### 2. 启用未知来源安装
1. 打开 Android 设置
2. 进入"安全"或"隐私"设置
3. 启用"未知来源"或"安装未知应用"
4. 或者在安装时选择"允许此来源"

### 3. 安装应用
1. 点击下载的 APK 文件
2. 按照屏幕提示完成安装
3. 如果出现安全警告，选择"仍要安装"

### 4. 首次设置
1. 打开 SMS Forward 应用
2. 授予通知访问权限：
   - 设置 → 通知 → 通知访问权限
   - 找到 SMS Forward 并启用
3. 配置邮件设置（如需要）
4. 测试功能是否正常

## 验证安装
使用以下 SHA256 校验和验证下载文件的完整性：
\`\`\`
$APK_SHA256  app-release.apk
\`\`\`

在终端中运行：
\`\`\`bash
shasum -a 256 app-release.apk
\`\`\`

## 故障排除

### 安装失败
- 确保 Android 版本为 14 或更高
- 检查存储空间是否充足
- 尝试重新下载 APK 文件

### 权限问题
- 手动进入设置授予所需权限
- 重启应用后重新尝试

### 功能异常
- 检查通知访问权限是否正确授予
- 确认网络连接正常
- 查看应用日志或联系支持

## 卸载
在设置 → 应用管理中找到 SMS Forward 并卸载。
EOF

print_success "Release files created in: $RELEASE_DIR"

# Step 7: Optional - Install on connected device
read -p "Do you want to install the APK on a connected device? (y/n): " install_apk
if [ "$install_apk" = "y" ] || [ "$install_apk" = "Y" ]; then
    print_status "Installing APK on connected device..."
    adb install -r "$APK_PATH"
    if [ $? -eq 0 ]; then
        print_success "APK installed successfully"
    else
        print_warning "Failed to install APK (make sure device is connected and USB debugging is enabled)"
    fi
fi

echo ""
print_success "🎉 Release build completed successfully!"
print_status "Release files created in: $RELEASE_DIR"
echo ""
print_status "📋 Next steps for GitHub release:"
echo "  1. Test the release build thoroughly"
echo "  2. Review and update RELEASE_NOTES.md if needed"
echo "  3. Commit any final changes to git"
echo "  4. Create and push a git tag:"
echo "     git tag v$VERSION_NAME"
echo "     git push origin v$VERSION_NAME"
echo "  5. Create GitHub release:"
echo "     - Go to GitHub repository → Releases → Create new release"
echo "     - Use tag: v$VERSION_NAME"
echo "     - Upload files from $RELEASE_DIR/"
echo "     - Copy content from RELEASE_NOTES.md as release description"
echo "  6. Or use GitHub CLI (if installed):"
echo "     gh release create v$VERSION_NAME $RELEASE_DIR/* --title \"SMS Forward v$VERSION_NAME\" --notes-file $RELEASE_DIR/RELEASE_NOTES.md"
echo ""
print_status "📁 Release package contents:"
ls -la "$RELEASE_DIR/"
echo ""
