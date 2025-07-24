# Android应用打包和发布指南

## 1. 准备发布版本

### 1.1 更新版本信息
在 `app/build.gradle` 中更新版本：
```gradle
android {
    defaultConfig {
        versionCode 1        // 每次发布都要递增
        versionName "1.0"    // 用户看到的版本号
    }
}
```

### 1.2 检查应用权限
确认 `AndroidManifest.xml` 中的权限都是必需的：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
```

## 2. 生成签名密钥

### 2.1 创建密钥库
```bash
keytool -genkey -v -keystore smsforward-release-key.keystore -alias smsforward -keyalg RSA -keysize 2048 -validity 10000
```

### 2.2 配置签名
在 `app/build.gradle` 中添加：
```gradle
android {
    signingConfigs {
        release {
            storeFile file('smsforward-release-key.keystore')
            storePassword 'your_store_password'
            keyAlias 'smsforward'
            keyPassword 'your_key_password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## 3. 构建发布版本

### 3.1 清理项目
```bash
./gradlew clean
```

### 3.2 构建Release APK
```bash
./gradlew assembleRelease
```

### 3.3 构建AAB (推荐)
```bash
./gradlew bundleRelease
```

生成的文件位置：
- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

## 4. 测试发布版本

### 4.1 安装测试
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### 4.2 功能测试清单
- [ ] 通知权限申请
- [ ] 邮件配置功能
- [ ] SMS转发功能
- [ ] 界面显示正常
- [ ] 应用图标显示正确

## 5. 发布渠道

### 5.1 Google Play Store
1. 创建开发者账户 (一次性费用 $25)
2. 上传AAB文件
3. 填写应用信息
4. 设置内容分级
5. 提交审核

### 5.2 其他应用商店
- 华为应用市场
- 小米应用商店
- OPPO软件商店
- vivo应用商店
- 应用宝 (腾讯)

### 5.3 直接分发
- 通过网站提供APK下载
- 通过GitHub Releases
- 通过邮件分享

## 6. 应用商店优化 (ASO)

### 6.1 应用信息
- **应用名称**: SMS Verification Code Forwarder
- **简短描述**: 自动转发短信验证码到邮箱
- **详细描述**: 
  ```
  SMS Verification Code Forwarder 是一款简单易用的短信转发工具，
  可以自动识别短信中的验证码并转发到您的邮箱。
  
  主要功能：
  • 自动识别短信验证码
  • 邮件转发功能
  • 简洁的用户界面
  • 安全的邮件配置
  
  适用场景：
  • 双卡用户管理验证码
  • 工作手机与个人邮箱同步
  • 重要验证码备份
  ```

### 6.2 关键词
- SMS转发
- 验证码
- 邮件转发
- 短信管理
- 验证码管理

### 6.3 应用截图
准备5-8张应用截图，展示：
- 主界面
- 权限设置
- 邮件配置
- 短信列表
- 设置页面

## 7. 发布后维护

### 7.1 用户反馈
- 监控应用商店评论
- 收集用户建议
- 及时回复用户问题

### 7.2 版本更新
- 修复bug
- 添加新功能
- 优化性能
- 更新依赖库

### 7.3 数据分析
- 下载量统计
- 用户留存率
- 崩溃报告分析
- 用户行为分析

## 8. 注意事项

### 8.1 隐私政策
由于应用涉及短信读取，需要制定隐私政策说明：
- 数据收集范围
- 数据使用目的
- 数据存储方式
- 用户权利

### 8.2 合规要求
- 遵守各应用商店政策
- 符合当地法律法规
- 获得必要的权限说明
- 提供用户协议

### 8.3 安全考虑
- 邮件密码加密存储
- 网络传输安全
- 权限最小化原则
- 定期安全审计
