# 网络监测 APK

Android 应用，用于实时监测 4G/5G 网络环境信息。

## 功能特性

- ✅ 实时读取并显示 4G LTE 基站信息（MCC、MNC、TAC、CI、PCI、EARFCN）
- ✅ 实时读取并显示 5G NR 基站信息（MCC、MNC、TAC、NCI、PCI、NR ARFCN）
- ✅ 实时显示信号指标（RSRP、RSRQ、SINR、RSSI、信号等级）
- ✅ 使用 MPAndroidChart 实现 RSRP 实时波形曲线图
- ✅ 简洁模块化卡片式布局，圆角白色卡片设计
- ✅ 每秒自动刷新并记录数据
- ✅ 一键导出历史数据为 CSV 文件到 Download 目录
- ✅ 完整权限申请处理

## 系统要求

- Android 9.0 (API 28) 及以上
- 需要授予以下权限：
  - READ_PHONE_STATE
  - ACCESS_FINE_LOCATION
  - ACCESS_COARSE_LOCATION
  - POST_NOTIFICATIONS

## 构建说明

1. 使用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 点击 Run 按钮构建并安装

或使用命令行：
```bash
cd NetworkMonitor
./gradlew assembleDebug
```

生成的 APK 位于：`app/build/outputs/apk/debug/app-debug.apk`

## 使用说明

1. 首次启动时授予所需权限
2. 应用将自动开始监测网络信息
3. 数据每秒自动刷新
4. 点击"导出历史数据"按钮可将记录保存为 CSV 文件

## 技术栈

- Kotlin
- Android SDK
- MPAndroidChart (图表库)
- Material Design Components
