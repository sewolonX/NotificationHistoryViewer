# Notification History Viewer

一款需要 root 权限的 Android 通知历史查看器。读取系统通知历史文件和内存中的记录，合并去重后按时间展示；点开记录可查看包名、频道、来源等详情。界面使用 Compose 和 Miuix。

本项目由 AI 生成。

## 资料与来源

- 通知历史机制参考 [Android 开源项目的通知历史文档](https://source.android.google.cn/docs/core/display/notification-history?hl=zh-cn)。本项目在目标设备上读取的系统历史文件目录为 `/data/system_ce/0/notification_history/history/`；该路径属于设备上的实现细节，并非 Android 文档承诺的通用路径。
- 界面组件来自 [Compose Miuix UI](https://github.com/compose-miuix-ui/miuix)。

## 功能

- 搜索通知内容与详情字段，可选择搜索范围、使用正则表达式或忽略常见分隔符。
- 首次读取时将尚未备份的系统历史文件复制到应用专属目录，之后不会重复复制同名文件。
- 搜索词和搜索设置保存在应用数据中。

备份目录：`Android/data/app.sewolon.notificationhistoryviewer/files/notification_history/`。备份文件为原始文件副本，未加密；卸载应用可能会删除这些文件，请自行另存重要备份。

## 构建

使用 Android Studio 打开项目，或在项目根目录运行：

```bash
./gradlew :app:assembleRelease
```

APK 位于 `app/build/outputs/apk/release/app-release.apk`。当前 release 使用本机 Android 调试密钥签名，仅适合个人安装和测试；换电脑构建时签名可能不同，无法直接覆盖安装。应用包名为 `app.sewolon.notificationhistoryviewer`。
