# SMS Forward App - 发布说明

## 🚀 快速开始

### 1. 首次发布设置

```bash
# 1. 创建签名密钥库
./create_keystore.sh

# 2. 构建发布版本
./build_release.sh

# 3. 创建 Git 标签并推送
git tag v1.0.0
git push origin v1.0.0
```

### 2. GitHub Actions 自动发布

在 GitHub 仓库中设置以下 Secrets：

| Secret Name | Description | 获取方法 |
|-------------|-------------|----------|
| `KEYSTORE_BASE64` | 密钥库文件的 base64 编码 | `base64 -i smsforward-release.keystore \| pbcopy` |
| `KEYSTORE_PASSWORD` | 密钥库密码 | 创建密钥库时设置的密码 |
| `KEY_ALIAS` | 密钥别名 | `smsforward` |
| `KEY_PASSWORD` | 密钥密码 | 创建密钥库时设置的密钥密码 |

设置完成后，推送标签即可自动触发发布：
```bash
git tag v1.0.0
git push origin v1.0.0
```

## 📁 项目文件说明

### 发布相关脚本
- `create_keystore.sh` - 创建签名密钥库
- `build_release.sh` - 构建发布版本
- `RELEASE_GUIDE.md` - 详细发布指南

### GitHub Actions 工作流
- `.github/workflows/release.yml` - 自动发布工作流
- `.github/workflows/build.yml` - 构建测试工作流

### 配置文件
- `keystore.properties` - 密钥库配置（不提交到版本控制）
- `smsforward-release.keystore` - 签名密钥库文件（不提交到版本控制）

## 🔐 安全注意事项

1. **密钥库文件安全**
   - 密钥库文件已添加到 `.gitignore`
   - 请妥善保管密钥库文件和密码
   - 建议制作备份并存储在安全位置

2. **GitHub Secrets**
   - 所有敏感信息都通过 GitHub Secrets 管理
   - 不要在代码中硬编码密码或密钥信息

3. **版本控制**
   - 密钥库相关文件不会被提交到版本控制
   - 发布文件会被自动排除

## 📦 发布产物

每次发布会生成以下文件：
- `app-release.apk` - Android 安装包
- `app-release.aab` - Google Play 商店格式
- `*.sha256` - 文件校验和
- `RELEASE_NOTES.md` - 发布说明
- `INSTALLATION_GUIDE.md` - 安装指南

## 🔄 版本更新流程

1. **更新版本号**
   ```gradle
   // 在 app/build.gradle 中
   defaultConfig {
       versionCode 2      // 递增
       versionName "1.1.0" // 更新版本号
   }
   ```

2. **提交更改**
   ```bash
   git add .
   git commit -m "Release v1.1.0"
   git push origin main
   ```

3. **创建发布标签**
   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```

4. **自动发布**
   - GitHub Actions 自动构建
   - 创建 GitHub Release
   - 上传发布文件

## 🐛 故障排除

### 常见问题

**Q: 构建失败，提示找不到密钥库文件**
A: 确保已运行 `./create_keystore.sh` 创建密钥库

**Q: GitHub Actions 失败，提示 Secrets 未设置**
A: 检查 GitHub 仓库设置中的 Secrets 配置

**Q: APK 签名验证失败**
A: 检查密钥库密码和别名是否正确

**Q: 无法安装 APK**
A: 确保目标设备 Android 版本 ≥ 14

### 获取帮助

1. 查看 `RELEASE_GUIDE.md` 详细说明
2. 检查 GitHub Actions 日志
3. 在 GitHub Issues 中报告问题

## 📋 发布检查清单

发布前请确认：

- [ ] 版本号已更新
- [ ] 功能测试通过
- [ ] 密钥库文件存在
- [ ] GitHub Secrets 已配置
- [ ] 发布说明已准备
- [ ] 目标设备测试通过

## 🎯 下一步

发布完成后：

1. 在测试设备上验证安装
2. 更新项目文档
3. 通知用户新版本发布
4. 监控用户反馈
5. 准备下一个版本的开发计划

---

**提示：** 首次发布建议先在测试环境验证整个流程。
