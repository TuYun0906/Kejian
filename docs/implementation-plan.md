# 课间 Android Implementation Plan

> For agentic workers: execute inline using executing-plans. Follow task checkboxes and test evidence.

**Goal:** 可在Android 10+安装的单学期课表应用。
**Architecture:** Java原生Activity和本地JSON存储；独立WebView官方登录，以受控evaluateJavascript执行同源课程查询并轮询结果，不向远端页面注入原生桥。课程状态使用无Android依赖的逻辑类。
**Tech Stack:** Java 17、Android SDK 35、AGP 8.7.3、Gradle 8.9；无业务第三方依赖。
**Spec:** requirements.md。

## Global Constraints
Android 10+；北京时间；仅两个主页面；作息按需求；数据失败不清空；不保存密码；不预载用户真实课表。

## Review Focus
- 跨年和日期边界、开始/结束时刻。
- 未获取不等于无课；通用业务失败需显式确认。
- 课间保留课程块进度，下一课程不误指下一小节。
- 远端WebView不可获得本地文件及原生桥，离站导航受限。
- 更新必须校验完整性并原子保存，旋转/取消不得覆盖。

## Tasks
- [x] 1. Core: `ScheduleLogic.java`，先写纯Java `ScheduleLogicTest`，断言08:55课间50%、15:05课间、20:50结束、跨年日期、冲突保留、未知明日。先失败后实现并运行。
- [x] 2. Import/store: `ScheduleRepository.java`和`ImportActivity.java`。验证协议、日期、节次、学期一致性，大小限制；明确按周导入，待确认空周；取消不保存，完整更新才替换旧完整版本。离线存储保持事务。
- [x] 3. UI: `MainActivity.java`。今日/周课表、周次选择、回本周、完整详情、冲突及未知态、明日简表。前台刷新和恢复重算。
- [ ] 4. Package: manifest/build files，编译debug APK、运行核心测试及静态检查，整理源码和实机验证限制。

## Ledger
- 2026-11-05：空白目录无既有仓库，直接在outputs/course-app建立独立项目，不创建Git工作树。
- 2026-11-05：用户确认空课周，但业务错误含义仍不充分；实现显式确认空周，不把此码全局映射成无课。

- 2026-11-05：首版源代码、24项逻辑检查及测试APK已完成；第4项的安卓实机运行验证仍待执行。Gradle构建路径未验证，官方SDK直接构建已通过。
