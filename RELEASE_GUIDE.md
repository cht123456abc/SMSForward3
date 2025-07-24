# SMS Forward App 发布指南

## 📋 发布流程概述

本指南详细说明了如何为 SMS Forward 应用创建签名版本并在 GitHub 上发布。

## 🔐 第一步：创建签名密钥库

### 自动创建（推荐）
```bash
./create_keystore.sh
```

### 手动创建
```bash
keytool -genkey -v -keystore smsforward-release.keystore -alias smsforward -keyalg RSA -keysize 2048 -validity 9125
```

**重要提醒：**
- 妥善保管密钥库文件和密码
- 制作密钥库文件的备份
- 所有应用更新都必须使用相同的密钥库签名

## 🏗️ 第二步：本地构建发布版本

### 使用构建脚本（推荐）
```bash
./build_release.sh
```

### 手动构建
```bash
# 清理项目
./gradlew clean

# 构建发布版本
./gradlew assembleRelease
./gradlew bundleRelease

# 验证签名
apksigner verify app/build/outputs/apk/release/app-release.apk
```

## 🚀 第三步：GitHub 发布

### 方法一：使用 GitHub Actions（推荐）

1. **设置 GitHub Secrets**
   - 进入 GitHub 仓库 → Settings → Secrets and variables → Actions
   - 添加以下 secrets：
     ```
     KEYSTORE_BASE64: [密钥库文件的 base64 编码]
     KEYSTORE_PASSWORD: [密钥库密码]
     KEY_ALIAS: smsforward
     KEY_PASSWORD: [密钥密码]
     ```

2. **获取密钥库的 base64 编码**
   ```bash
   base64 -i smsforward-release.keystore | pbcopy
   ```

3. **创建发布标签**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

4. **自动构建和发布**
   - GitHub Actions 将自动触发
   - 构建签名的 APK 和 AAB
   - 创建 GitHub Release
   - 上传发布文件

### 方法二：手动发布

1. **构建发布版本**
   ```bash
   ./build_release.sh
   ```

2. **创建 Git 标签**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **在 GitHub 上创建 Release**
   - 进入 GitHub 仓库 → Releases → Create a new release
   - 选择刚创建的标签
   - 上传 release_* 目录中的文件
   - 复制 RELEASE_NOTES.md 内容作为发布说明

### 方法三：使用 GitHub CLI

```bash
# 构建发布版本
./build_release.sh

# 使用 gh CLI 创建发布
gh release create v1.0.0 release_*/app-release.apk release_*/app-release.aab \
  --title "SMS Forward v1.0.0" \
  --notes-file release_*/RELEASE_NOTES.md
```

## 📁 发布文件说明

每次发布包含以下文件：
- `app-release.apk` - 用户安装的 APK 文件
- `app-release.aab` - Google Play 商店格式
- `app-release.apk.sha256` - APK 校验和
- `app-release.aab.sha256` - AAB 校验和
- `RELEASE_NOTES.md` - 发布说明
- `INSTALLATION_GUIDE.md` - 安装指南

## 🔍 版本管理

### 版本号规则
- 格式：`major.minor.patch`
- 示例：`1.0.0`, `1.1.0`, `1.1.1`

### 更新版本号
在 `app/build.gradle` 中更新：
```gradle
defaultConfig {
    versionCode 2      // 每次发布递增
    versionName "1.1.0" // 语义化版本号
}
```

## 🛡️ 安全最佳实践

1. **密钥库安全**
   - 不要将密钥库文件提交到版本控制
   - 定期备份密钥库文件
   - 使用强密码保护密钥库

2. **GitHub Secrets**
   - 定期轮换密钥库密码
   - 限制仓库访问权限
   - 监控 Actions 执行日志

3. **发布验证**
   - 验证 APK 签名
   - 检查文件校验和
   - 测试安装和基本功能

## 🐛 故障排除

### 构建失败
- 检查 Java 环境配置
- 确认密钥库文件存在
- 验证 keystore.properties 配置

### 签名失败
- 检查密钥库密码
- 确认密钥别名正确
- 验证密钥有效期

### GitHub Actions 失败
- 检查 Secrets 配置
- 查看 Actions 日志
- 验证工作流文件语法

## 📞 支持

如遇问题，请：
1. 查看本指南的故障排除部分
2. 检查 GitHub Issues 中的已知问题
3. 创建新的 Issue 描述问题详情

---

**注意：** 首次发布前，请务必在测试设备上验证应用功能正常。
