# 私了吗 (PrivateCheck) 🛡️

**"如果有一天我倒下了，希望这个世界还有人知道。"**

> A "Dead Man's Switch" Android Application for solitary living.
> 专为独居人士设计的“数字遗嘱/安全确认”应用。

![Latest Check-in](https://img.shields.io/badge/Check--in-Active-success) ![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue) ![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-green) ![License](https://img.shields.io/badge/License-MIT-purple)

## 📖 简介 (Introduction)

**私了吗 (RUOK)** 是一款基于 **Android** 的本地安全确认应用。它的逻辑非常简单且克制：
用户每天需要在应用内点击 **"我还好" (Check-in)** 按钮进行打卡。
如果用户 **连续 24 小时** 未打卡，应用会自动进入 **"预警状态"**。
如果 **连续 48 小时** 未打卡，应用将自动通过 **SMTP** 后台服务，向预设的 **紧急联系人** 发送求救邮件（包含定位/状态信息）。

这款应用运行在本地，**不上传任何隐私数据到服务器**，所有的逻辑判断、定时任务、SMTP 发信均在您的手机端完成。

## ✨ 核心功能 (Features)

* **📅 打卡热力图**：
  * GitHub 风格的**年度热力图**。
  * **时间渐变色**：根据早中晚打卡时间不同，显示紫、绿、红、蓝等不同颜色的格子。
* **⏰ 自动守望机制**：
  * **WorkManager** 后台守护：无惧杀进程，每日定时检测。
  * **开机自启**：手机重启后自动恢复监测，无需手动打开 APP。
* **📧 紧急联络**：
  * 支持配置 **SMTP 邮箱** (QQ/Gmail/Outlook)。
  * 支持添加 **3 位紧急联系人**。
  * **加密存储**：邮箱授权码通过 `EncryptedSharedPreferences` (AES-256) 存储。
* **🎨 极简美学**：
  * **莫兰迪 (Morandi)** 配色方案。
  * **桌面小组件 (Widget)**：支持自定义文字、图片背景、对比度调节，放在桌面随时打卡。
  * 支持 **深色模式 (Dark Mode)**，完美适配系统 Edge-to-Edge 沉浸体验。

## 🛠️ 技术栈 (Tech Stack)

* **Language**: Kotlin
* **UI**: Jetpack Compose (Material 3)
* **Architecture**: MVVM (View - Logic - Data)
* **Persistence**: DataStore (Preferences), EncryptedSharedPreferences (Security)
* **Background**: WorkManager (PeriodicWorkRequest), BroadcastReceiver (BOOT_COMPLETED)
* **Network**: JavaMail (SMTP)
* **Widget**: Glance (AppWidget)

## 🚀 快速开始 (Getting Started)

### 前置要求

* Android Studio Ladybug (2024.2.1) 或更高版本
* JDK 17
* Android SDK 35 (VanillaIceCream)

### 安装与构建

1. **Clone 仓库**

    ```bash
    git clone https://github.com/your-username/RUOK.git
    cd RUOK
    ```

2. **构建 APK**

    ```bash
    ./gradlew :app:assembleDebug
    ```

### 配置指南

1. 进入 **设置 (Settings)** 页面。
2. 填写您的邮箱设置 (SMTP 服务器, 端口 465/587)。
3. **强烈建议使用 SMTP 授权码** 而非登录密码（应用会自动加密存储）。
4. 添加至少 1 位紧急联系人邮箱。
5. 点击 **"发送测试邮件"** 验证配置成功。

## 🤝 贡献 (Contributing)

欢迎提交 Issue 或 Pull Request。虽然这只是一个实习项目，但我希望能帮助到更多人。

## 📜 许可证 (License)

MIT License. See [LICENSE](LICENSE) for details.

---
Made with ❤️ by **实习生摆啊糖**
