# AGENTS.md — 协作约定

## 必须遵守

- 每次开始新的开发任务前，**必须先完整阅读** `DEV-DOC.md` 与 `CHANGELOG.md`，掌握当前版本、已实现功能与技术约定后再动手。
- 每次完成项目代码修改后，**必须同步更新**以下内容：
  1. `DEV-DOC.md`：更新版本号、功能说明、UI/UX、目录结构等受影响章节；
  2. `CHANGELOG.md`：追加当前版本的更新记录；
  3. 应用内“更新日志”页面（`app/src/main/java/com/unitoolkit/ui/settings/ChangelogScreen.kt`）中的对应版本内容。
- 涉及 UI 文案、桌面小组件、图标资源等修改时，同时核对 `strings.xml`、`AndroidManifest.xml` 与对应布局/资源文件。
- 文档与代码同步完成，禁止只改代码不更新文档。
