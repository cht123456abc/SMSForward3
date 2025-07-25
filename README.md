# SMSForward - SMS 验证码转发器

一个专为 Android 14+ 设计的 SMS 验证码自动转发应用，通过通知拦截技术将短信验证码自动转发到指定邮箱。

## 📱 应用信息

- **应用名称**: SMSForward
- **包名**: com.cht.smsforward
- **当前版本**: 1.0.5
- **目标 SDK**: 36 (Android 14+)
- **最低 SDK**: 34 (Android 14)

## 🚀 主要功能

### 核心功能
- **SMS 验证码自动转发**: 自动识别并转发短信中的验证码到指定邮箱
- **通知拦截技术**: 使用 NotificationListenerService 拦截短信通知（兼容 Android 14/Meizu 设备）
- **邮件发送配置**: 支持 QQ 邮箱 SMTP 配置，安全存储邮箱凭据
- **权限管理界面**: 统一的权限状态显示和管理

### 技术特性
- **加密存储**: 使用 EncryptedSharedPreferences 安全存储邮箱配置
- **网络诊断**: 内置网络连接诊断功能
- **多协议支持**: 支持 TLS/SSL 邮件发送协议
- **实时状态**: 实时显示通知访问和邮件转发状态

## 📦 安装说明

### 系统要求
- Android 14 (API 34) 或更高版本
- 网络连接权限
- 通知访问权限

### 安装步骤
1. 从 [Releases](https://github.com/cht123456abc/SMSForward3/releases) 页面下载最新的 APK 文件
2. 在 Android 设置中启用"未知来源安装"
3. 安装下载的 APK 文件
4. 启动应用并按照设置向导配置权限

## ⚙️ 配置说明

### 1. 启用通知访问权限
- 打开应用后点击"Enable Notification Access"
- 在系统设置中找到"SMSForward"并启用通知访问权限
- 返回应用确认权限状态

### 2. 配置邮箱转发
- 点击"Email Settings"进入邮箱配置页面
- 填写以下信息：
  - **发送方邮箱**: 您的 QQ 邮箱地址
  - **授权码**: QQ 邮箱的 SMTP 授权码（非 QQ 密码）
  - **接收方邮箱**: 接收验证码的邮箱地址

### 3. QQ 邮箱 SMTP 设置
1. 登录 QQ 邮箱网页版
2. 进入"设置" → "账户"
3. 开启"SMTP服务"
4. 生成授权码（用于应用中的密码字段）
5. 在应用中使用授权码而非 QQ 密码

## 🔧 使用方法

1. **完成初始配置**后，应用将自动在后台运行
2. **接收短信**时，应用会自动检测验证码
3. **自动转发**检测到的验证码到配置的邮箱
4. **查看历史**：在主界面可以查看最近接收的短信记录
5. **复制验证码**：点击验证码可以快速复制到剪贴板

## 🛠️ 技术架构

### 核心组件
- **MainActivity**: 主界面，显示权限状态和短信历史
- **EmailConfigActivity**: 邮箱配置界面
- **SmsNotificationListener**: 通知监听服务，拦截短信通知
- **EmailSender**: 邮件发送组件，支持多种 SMTP 配置

### 技术栈
- **开发语言**: Java
- **UI 框架**: Android Views + Material Design
- **数据存储**: EncryptedSharedPreferences
- **邮件发送**: JavaMail API
- **网络请求**: 原生 Android 网络 API

## 🔒 隐私与安全

- **本地存储**: 所有邮箱配置信息均加密存储在本地
- **无云服务**: 应用不使用任何云服务，数据不会上传到服务器
- **权限最小化**: 仅请求必要的权限（通知访问、网络访问）
- **开源透明**: 完整源代码开放，可自行审查安全性

## 🐛 故障排除

### 常见问题

**Q: 收不到验证码转发邮件？**
A: 请检查：
- 通知访问权限是否已启用
- 邮箱配置是否正确（特别是授权码）
- 网络连接是否正常
- 垃圾邮件文件夹

**Q: 邮件发送失败？**
A: 请尝试：
- 确认 QQ 邮箱 SMTP 服务已开启
- 检查授权码是否正确
- 使用应用内的"Test Email"功能测试配置
- 检查网络连接和防火墙设置

**Q: 在某些设备上无法正常工作？**
A: 本应用专为解决 Android 14/Meizu 设备的兼容性问题而设计，使用通知拦截而非传统 SMS API。

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 开发环境
- Android Studio 2024.1+
- JDK 17+
- Android SDK 34+

### 构建说明
```bash
git clone https://github.com/cht123456abc/SMSForward3.git
cd SMSForward3
./gradlew assembleRelease
```

## 📞 支持

如遇问题，请在 [GitHub Issues](https://github.com/cht123456abc/SMSForward3/issues) 页面报告。

## 🔄 更新日志

### v1.0.5 (最新)
- 优化邮件发送稳定性
- 改进通知拦截兼容性
- 用户界面优化
- 修复已知问题

查看完整更新日志：[Releases](https://github.com/cht123456abc/SMSForward3/releases)

---

**注意**: 本应用仅用于个人验证码转发用途，请遵守相关法律法规和服务条款。
