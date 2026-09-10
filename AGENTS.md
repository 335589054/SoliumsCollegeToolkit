# AGENTS.md — 协作约定

## 必须遵守

- 每次开始新的开发任务前，**必须先完整阅读** `DEV-DOC.md` 与 `CHANGELOG.md`，掌握当前版本、已实现功能与技术约定后再动手。
- 每次完成项目代码修改后，**必须同步更新**以下内容：
  1. `DEV-DOC.md`：更新版本号、功能说明、UI/UX、目录结构等受影响章节；
  2. `CHANGELOG.md`：追加当前版本的更新记录；
  3. 应用内“更新日志”页面（`app/src/main/java/com/solium/Unitoolkit/ui/settings/ChangelogScreen.kt`）中的对应版本内容。
- 涉及 UI 文案、桌面小组件、图标资源等修改时，同时核对 `strings.xml`、`AndroidManifest.xml` 与对应布局/资源文件。
- 文档与代码同步完成，禁止只改代码不更新文档。
- **每次变更版本号时**，必须同步更新以下位置的版本信息，避免不一致：
  1. `app/build.gradle.kts` 的 `versionName`（与 `versionCode` 递增）；
  2. 设置页「关于」对话框（`app/src/main/java/com/solium/Unitoolkit/ui/settings/SettingsScreen.kt`）中的版本文案；
  3. `DEV-DOC.md` 与 `CHANGELOG.md` 的当前版本。

## 构建环境

- Gradle：`8.7`（wrapper 发行版）
  - 本机路径：`C:\Users\33558\.gradle\wrapper\dists\gradle-8.7-bin\157ge0dm0jrajsz3pjvqs98mf\gradle-8.7\bin\gradle.bat`
- AGP（Android Gradle Plugin）：`8.5.2`
- Kotlin：`2.0.21`
- Compose 编译器插件 / Serialization：`2.0.21`
- KSP：`2.0.21-1.0.28`
- JDK：`17`（compileOptions / jvmTarget）
- SDK：compileSdk / targetSdk `35`，minSdk `24`

> 以上版本以根目录 `build.gradle.kts` 与 `app/build.gradle.kts` 为准，升级依赖时需同步更新本段。
