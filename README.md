# 课间 · Kejian

课间是一款 Android 课程表与备忘录应用。打开「今日」即可查看正在上的课、课间状态和下一门课，导入后也可以离线查看课表。

## 功能

- **今日与课表**：显示上课进度、倒计时、教室和教师，支持左右滑动切换教学周、手动编辑课程，以及保存和切换本地课表。
- **导入**：提供潍坊学院、安徽理工大学的专用入口。「通用教务」是实验功能，可在官方 HTTPS 页面登录后读取部分整学期课表布局。保存前请核对教学周、校历和作息。
- **通知**：支持课前提醒和上课期间的常驻通知，具体表现取决于设备的通知和提醒权限。
- **备忘**：支持文字、清单、图片和手绘，可搜索、置顶、归档和按标签整理。
- **外观**：支持浅色、深色和跟随系统，并可自定义 RGB 主题色。

## 安装

需要 Android 10 或以上版本。发布后可从本仓库的 **Releases** 下载 APK。覆盖安装需要沿用相同的应用 ID 和签名证书，新版本的 versionCode 也应递增。

## 从源码构建

需要 JDK 17、Android SDK Platform 35。项目使用 Gradle Wrapper 8.9 和 Android Gradle Plugin 8.7.3。

用 Android Studio 打开仓库根目录并同步 Gradle，或在已配置 Android SDK 的终端运行：

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS / Linux 可运行 `sh ./gradlew :app:assembleDebug`。生成文件位于 `app/build/outputs/apk/debug/app-debug.apk`。本机构建的调试签名可能与已分发 APK 不同。

## 配置应用内检查更新

1. 将 `app/src/main/assets/update-repository.txt` 的内容替换为公开仓库的 `OWNER/REPO`，然后重新构建 APK。
2. 在 GitHub Releases 创建正式版本，标签使用 `v主版本.次版本.修订号`，例如 `v0.8.14`。
3. 将已签名 APK 作为 Release 附件上传，文件名须与版本匹配，例如 `kejian-0.8.14.apk`。Release 正文会显示为应用内更新说明。
4. 后续发布新版前，同步递增 `app/build.gradle` 中的 `versionCode` 和 `versionName`，并使用同一签名证书构建。

配置后，应用在打开时每天最多自动检查一次，也可以在「更多 → 关于·更新」手动检查。发现新版时，用户可以打开浏览器下载。

## 测试

运行课表读取与登录脚本测试：

```powershell
$tests = (Get-ChildItem tests -Filter *.test.cjs).FullName
node --test $tests
```

通用教务仍需用各校登录后的实际课表验证覆盖范围。遇到识别问题时，可以提供学校教务网址和脱敏课表样本。

## 联系

- 开发者：TuYun
- QQ：1937747879
- QQ 群：976368889
