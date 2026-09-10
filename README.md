# Uni ToolKit（Solium的工具箱）

> 面向大学生的一站式本地优先（Local-First）便捷工具 APP，覆盖**学习 / 生活 / 娱乐**三大场景。

- 版本：v1.0.3-bugfix1（`versionCode 6`）
- 平台：Android（Kotlin + Jetpack Compose）
- 文档：[开发文档 DEV-DOC.md](./DEV-DOC.md) ｜ [更新记录 CHANGELOG.md](./CHANGELOG.md)

---

## 项目简介

Uni ToolKit 是一款专为大学生设计的本地优先工具集合应用，将日常高频事务整合进一个轻量、无需登录的 APP 中。核心数据默认存储在本地，支持导入 / 导出，尊重用户数据主权。

### 核心设计原则

| 原则 | 说明 |
|------|------|
| **本地优先** | 数据默认存本地，不依赖云端；用户主动导出才产生外部文件 |
| **轻量无感** | 每个工具独立可用，不强制登录，不收集隐私 |
| **可扩展** | 自定义便捷链接、自定义预设、自定义配色 |
| **沉浸专注** | 番茄钟沉浸式模式、悬浮窗音游隐条等场景化设计 |

---

## 功能概览

### 📖 学习模块

- **课表管理** — 按具体时间组织课程，支持三种时间轴视图、单/双周/每周/仅一次上课模式、多星期与多周次选择、自定义预设、学期起始与长度设置，导出 Markdown / JSON
- **笔记速记** — 轻量 Markdown 编辑器（编辑 / 预览切换）、列表搜索、导入导出 `.md`
- **TodoList** — 优先级、标签、截止日期、完成状态、多维度筛选
- **番茄钟** — 自定义时长与预设、保持亮屏、沉浸式全屏模式、后台前台通知倒计时、今日专注统计

### 🏠 生活模块

- **水电费缴交** — 缴费网页快捷方式 + 应用内 WebView
- **日程提醒** — 直接读写系统日历（`CalendarContract`），首页展示今日剩余日程
- **绩点计算器** — 自定义公式、学期管理、评分等级预设（4.0 / 5.0 / 自定义）
- **便捷链接收藏** — 自定义链接、分类管理、可加入首页快捷入口
- **简易账单** — 收支记录、月度汇总、分类预设、支付方式、按日期分组
- **采购清单** — 按“采购活动”管理项目，支持“暂缓采购”自动带入、导入 `.txt`、一键同步账单
- **个人名片** — 头像、姓名、联系方式、QQ / 微信二维码、B站信息，支持自定义信息栏

### 🎮 娱乐模块

- **音游上隐条** — 用户自选图片悬浮于屏幕上方，减小音游读谱压力；支持纯色 / 文字、透明度和尺寸调节、应用外拖拽、前台服务常驻

### ⚙️ 系统能力

- **桌面小组件** — 2×1「下一节课」+ 2×2「快捷入口」，半透明圆角样式，配色跟随应用主题
- **通知能力** — 番茄钟前台倒计时、课表“下一节课”提前 10 分钟提醒（`AlarmManager`）
- **数据管理** — JSON 备份 / 增量导入 / 覆盖导入 / 二次确认清空

### 🎨 主题系统

内置 **12 套**扁平化预设主题（水墨、淡黄、橘色、珊瑚红、粉色、墨绿、薄荷、青色、淡蓝、深蓝、淡紫、爱上雷神），均含浅色 + 深色双模式；支持**自定义主题**（按 UI 角色字段化编辑）及 `.sthm` 主题包导入 / 导出。

---

## 技术栈

| 层级 | 选型 |
|------|------|
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM（ViewModel + Repository） |
| 状态管理 | StateFlow + `collectAsState` |
| 路由 | Navigation Compose |
| 数据库 | Room 2.6（SQLite ORM） |
| 配置存储 | DataStore Preferences |
| 序列化 | kotlinx.serialization |
| 网页能力 | androidx.webkit（WebView） |

**原生能力**：悬浮窗（`SYSTEM_ALERT_WINDOW`）、保持亮屏、系统日历读写（`CalendarContract`）、前台服务、通知 / `AlarmManager`、桌面小组件（`AppWidgetProvider`）、提示音 + 震动、SAF 文件读写。

**构建参数**：`compileSdk 35` / `minSdk 24` / `targetSdk 35`，Java / Kotlin target 17。

---

## 目录结构

```
UniToolKit/
├── settings.gradle.kts      # 仓库与模块配置
├── build.gradle.kts         # 根构建脚本
├── AGENTS.md                # 协作规范（开发前必读）
├── DEV-DOC.md               # 开发文档
├── CHANGELOG.md             # 更新记录
└── app/
    ├── build.gradle.kts     # 应用模块依赖管理
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/             # 图标 / 小组件 / 主题资源
        └── java/com/unitoolkit/
            ├── MainActivity.kt / UniToolkitApp.kt
            ├── core/        # 主题 / Room / DataStore / 模型 / 工具
            ├── ui/          # 各模块 Screen + ViewModel + 导航
            └── service/     # 番茄钟 / 悬浮窗 / 课表提醒服务
```

---

## 构建与运行

### 环境要求

- Android Studio（支持 Kotlin 2.0 与 Gradle 8.7）
- JDK 17
- Android SDK（API 35）

### 构建

```bash
# debug APK
./gradlew assembleDebug

# release APK（使用 keystore/release.jks 正式签名）
./gradlew assembleRelease
```

产物生成于 `app/build/outputs/apk/`。

### 安装运行

```bash
# 无线调试 / USB 连接设备后
./gradlew installDebug
```

或直接将生成的 APK 安装到 `Android 7.0（API 24）` 及以上的设备。

---

## 文档

| 文档 | 说明 |
|------|------|
| [DEV-DOC.md](./DEV-DOC.md) | 详细开发文档：功能设计、数据结构、UI/UX 规范、目录结构 |
| [CHANGELOG.md](./CHANGELOG.md) | 按版本的完整更新记录 |
| [AGENTS.md](./AGENTS.md) | 工程协作约定（每次开发前必读） |

---

## 版本历史

| 版本 | 说明 |
|------|------|
| v1.0.3-bugfix1 | 课表时间刻度对齐修复；日程页闪退修复；日程改为直接读写系统日历 |
| v1.0.3 | 课表三日时间轴、自定义主题颜色选择器、系统日程 Instances 读取、上隐条自动保存 |
| v1.0.2 | 颜色点选、课表星期多选与日历时间轴、采购 / 账单自动名称、系统日程刷新 |
| v1.0.1 | 页面滑动动画、课表三日时间轴与预设、上隐条持久化、自定义主题 |
| v1.0.0 | 正式发布：release 签名、小组件半透明圆角、相关 bug 修复 |

完整内容见 [CHANGELOG.md](./CHANGELOG.md)。

---

*由 Solium 维护 · Local-First · 尊重你的数据主权*