# Uni ToolKit — 开发文档

> 面向大学生的一站式便捷工具 APP
> 版本：v1.1.0
> 最后更新：2026-09-10
>
> 版本更新记录见 [CHANGELOG.md](./CHANGELOG.md)

---

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 技术选型](#2-技术选型)
- [3. 功能模块详细设计](#3-功能模块详细设计)
  - [3.1 学习模块](#31-学习模块)
  - [3.2 生活模块](#32-生活模块)
  - [3.3 娱乐模块](#33-娱乐模块)
- [4. 数据结构与存储方案](#4-数据结构与存储方案)
- [5. UI/UX 设计规范](#5-uiux-设计规范)
  - [5.1 主界面（概览）](#51-主界面概览)
  - [5.2 导航条与工具箱](#52-导航条与工具箱)
  - [5.3 设置页](#53-设置页)
  - [5.4 配色方案](#54-配色方案)
- [6. 项目目录结构](#6-项目目录结构)
- [7. 开发计划](#7-开发计划)

---

## 1. 项目概述

**Uni ToolKit**（应用显示名：Solium的工具箱）是一款专为大学生设计的本地优先（Local-First）工具集合应用，覆盖学习、生活、娱乐三大场景。所有核心数据默认存储在本地，支持导入/导出，尊重用户数据主权。

### 核心设计原则

| 原则 | 说明 |
|------|------|
| **本地优先** | 数据默认存本地，不依赖云端；用户主动导出才产生外部文件 |
| **轻量无感** | 每个工具独立可用，不强制登录，不收集隐私 |
| **可扩展** | 自定义便捷链接、自定义预设、自定义配色 |
| **沉浸专注** | 番茄钟沉浸式模式、悬浮窗音游隐条等场景化设计 |

### 目标平台

| 平台 | 优先级 | 说明 |
|------|--------|------|
| Android | P0 | 原生实现（Kotlin + Compose），支持悬浮窗、后台运行、读写系统日程 |

> 本项目采用 Android 原生技术栈，聚焦单端最佳体验；iOS / 桌面端不在当前范围内。

---

## 2. 技术选型

### 2.1 框架

| 层级 | 选型 | 理由 |
|------|------|------|
| **语言** | Kotlin 2.0 | 官方语言、协程、空安全 |
| **UI** | Jetpack Compose + Material 3 | 声明式 UI、扁平化设计、响应式 |
| **架构** | MVVM（ViewModel + Repository） | 状态与 UI 分离、生命周期感知 |
| **状态管理** | StateFlow + `collectAsState` | 响应式单向数据流 |
| **路由** | Navigation Compose | 声明式路由、底部导航 |

### 2.2 本地存储

| 用途 | 选型 | 说明 |
|------|------|------|
| 结构化数据（课表、账单、绩点等） | **Room** | SQLite 之上的 ORM，查询灵活、支持 Flow 响应式监听 |
| Key-Value 配置（主题、预设、首页卡片） | **DataStore (Preferences)** | 轻量配置项异步读写 |
| 大文本（笔记 Markdown 原文） | Room `TEXT` 字段 | 原文直接入库，索引与内容统一管理 |
| JSON/MD 导入导出 | `kotlinx.serialization` + `java.io` / SAF | 序列化与文件读写 |

### 2.3 关键原生能力

| 功能 | 原生方案 | 说明 |
|------|----------|------|
| 悬浮窗（音游隐条） | `WindowManager` + `TYPE_APPLICATION_OVERLAY` | 申请 `SYSTEM_ALERT_WINDOW` 权限 |
| 保持亮屏（番茄钟） | `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` | 阻止系统休眠 |
| 系统日程读写 | `CalendarContract`（Calendar Provider） | 需 `READ/WRITE_CALENDAR` 权限 |
| WebView（自定义功能） | `android.webkit.WebView` | 应用内打开网页 |
| 后台运行 | Foreground Service（`specialUse`） | 番茄钟计时、悬浮窗常驻 |
| 通知 | `NotificationManager` + `NotificationChannel` + `AlarmManager` | 番茄铃响、课表下一节提醒、前台通知 |
| 桌面小组件 | `AppWidgetProvider` + `RemoteViews` | 2×1「下一节课」半透明圆角小组件 + 2×2「快捷入口」圆角底块纯图标小组件 |
| 音频/震动 | `ToneGenerator` + `Vibrator` | 提示音与震动反馈 |
| 文件读写 | `java.io` + SAF | 导入导出 JSON/MD |
| 日期/时间选择 | `DatePicker` / `TimePicker`（系统原生弹窗） | 全局统一调用 Android 系统选择器 |

---

## 3. 功能模块详细设计

### 3.1 学习模块

#### 3.1.1 课表管理 (Course Schedule)

**功能清单**：
- [x] 以"周"为视图展示课表（周一至周日，支持切换当前周）
- [x] 添加/编辑/删除课程（课程名、教师、教室、星期、开始时间、结束时间、颜色标签、备注）
- [x] 课程按**具体时间**（如 08:00-08:45）而非"节次"组织，支持任意时间点
- [x] 支持单周 / 双周 / 每周 / **仅一次**（指定第几周）四种上课模式
- [x] 支持多选周次与指定多个日期上课（v1.0.1）
- [x] 上课星期支持多选 / 不选（不选视为每天）（v1.0.2）
- [x] 支持自定义课表预设：名称必填、其余字段可选；新建课程可一键套用（v1.0.1）
- [x] 支持设置**学期长度**（周数），周次切换与「仅一次」周次受其约束
- [x] 支持设置**学期起始日期**（第一周开始的任意日期），自动对齐当前周次
- [x] 导出为 **Markdown**（周视图表格格式）或 **JSON**
- [x] 数据存储在应用内 Room 数据库
- [ ] 增强时间轴的可视化显示（待 v1.0.0-dev2）
- [x] 新增“三日时间轴 / 日历式”课表布局（v1.0.1）
- [x] 三日时间轴升级为左侧 7:00-22:00 时间刻度、三天并排、课程彩色方块（v1.0.2）
- [x] 课表默认三日时间轴，时间轴方块仅显示名称 / 开始时间 / 教室，小时刻度加高（v1.0.3）
- [x] 小时刻度与课程开始时间对齐刻度线，不落在刻度中间空白（v1.0.3-bugfix1）
- [x] 新增「第几节课时间预设」：可分上 / 下午逐节设置起止时间，与课程预设相互独立；时间轴按命中节次套用预设时间显示（v1.0.4-rc1）
- [x] 课程编辑「具体时间」处新增「从节次时间预设快速填写」；**清空默认节次预设**（由用户自行配置）；原「套用预设」改为按钮 + 弹窗选择后再套用（v1.0.4-rc2）

**课程数据模型**（见 [第4章](#4-数据结构与存储方案)）

**Markdown 导出示例**：
```markdown
# 我的课表 — 第 7 周

| 时间 | 周一 | 周二 | 周三 | 周四 | 周五 |
|------|------|------|------|------|------|
| 08:00-08:45 | 高等数学 A | | 英语 | | 线性代数 |
| 08:55-09:40 | ↑ 同上 | | ↑ 同上 | | ↑ 同上 |
| 10:00-10:45 | | 数据结构 | | 操作系统 | |
```

#### 3.1.2 笔记速记 (Quick Notes)

**功能清单**：
- [x] 基本 Markdown 编辑（粗体、斜体、标题 1-3、有序/无序列表、代码块、引用、链接）
- [x] 编辑 / 预览切换
- [x] 笔记列表（按修改时间排序，支持搜索标题和正文）
- [x] 导出为 `.md` 文件
- [x] 导入 `.md` 文件

**编辑器方案**：手写轻量 Markdown 编辑器（Compose），基础语法足够、依赖为零。

#### 3.1.3 TodoList

**功能清单**：
- [x] 添加/编辑/删除待办（标题、截止日期、优先级：低/中/高、标签、备注）
- [x] 完成/取消完成
- [x] 筛选（全部 / 今日到期 / 已完成）

#### 3.1.4 番茄钟 (Pomodoro Timer)

**功能清单**：
- [x] 自定义时长（工作时长、休息时长、长休息时长、长休息间隔轮数）
- [x] 预设方案（专注 25/5 / 深度 50/10 / 快速 15/3）
- [ ] 自定义预设方案（可保存，待 v1.0.0-dev2）
- [x] **保持亮屏开关**
- [x] **沉浸式模式**（黑底白字，仅显示剩余时间大字，全屏）
- [x] 提示音 + 震动（工作结束、休息结束各一声）
- [x] 后台运行 + 前台通知倒计时
- [x] 今日完成统计（完成轮数、累计专注分钟数）

**沉浸式模式交互**：
- 进入：番茄钟运行中，点击"沉浸"按钮
- 退出：点击屏幕 / 按返回键退出

---

### 3.2 生活模块

#### 3.2.1 水电费缴交 (Utility Payment)

**功能清单**：
- [x] 管理一个或多个缴费网页快捷方式（名称、URL、图标）
- [x] 点击后在 **应用内 WebView** 打开（避免跳转系统浏览器）

#### 3.2.2 日程提醒 (Schedule Reminder)

**功能清单**：
- [x] 读写设备系统日程（`CalendarContract`）
- [x] 自动读取系统日历事件并展示（与自建日程合并、按 `deviceEventId` 去重）
- [x] 在应用内展示"今日剩余日程"卡片（主界面卡片）
- [x] 写入日程到系统日历
- [x] 编辑 / 删除已添加的日程
- [x] 提供「刷新系统日程」主动读取系统日历（v1.0.2）
- [x] 系统日历按 Instances 实例展开读取，并保留事件表回退（v1.0.3）
- [x] 日程直接读写系统日历，不再保留本地副本；首页日程卡片同步系统日历（v1.0.3-bugfix1）
- [x] 添加日程默认无提醒，不再提供提醒选择（v1.0.3-bugfix1）
- [x] 修复删除日程找不到目标行的问题（Instances 实例/事件 ID 顺序修正）（v1.0.4pre1）

#### 3.2.3 绩点计算器 (GPA Calculator)

**功能清单**：
- [x] 录入课程（课程名、学分、成绩）
- [x] 按设置中的计算公式计算 GPA
- [x] 显示：已修总学分、加权平均分、GPA
- [x] 历史学期管理（每学期独立列表）
- [x] 常用评分等级预设（4.0 制 / 5.0 制 / 自定义映射）

**计算公式设置**（在绩点页内）：
- 默认加权平均：`GPA = Σ(课程绩点 × 学分) / Σ(学分)`
- 支持自定义绩点映射表（分数下限 → 绩点）

#### 3.2.4 便捷链接收藏 (Quick Links)

**功能清单**：
- [x] 添加自定义功能（名称 + 网页链接 + lucide 图标）
- [x] 管理列表（编辑、删除）
- [x] 点击后在应用内 WebView 打开
- [x] 可添加到主界面首页「快捷入口」（与工具统一管理，避免重复维护）
- [x] 分类管理（学习 / 生活 / 娱乐 / 自定义）

#### 3.2.5 简易账单 (Simple Ledger)

**功能清单**：
- [x] 记录收入/支出（金额、分类、备注、时间、支付方式）
- [x] 月度汇总（总收入、总支出、结余）
- [x] 分类预设（餐饮、交通、购物、学习、娱乐、其他；可添加自定义预设）
- [x] 选择「餐饮」分类时自动填写备注「日期 + 早饭/午饭/晚饭/夜宵」（v1.0.2）
- [x] 按日期分组展示账单记录
- [ ] 删除二次确认（附谚语）、一键清除（开发用，二次确认）
- [ ] 按月管理：自动添加当前月份，可查看其他月份账单
- [ ] 支付方式：微信 / 支付宝 / 银行卡 / 自定义

#### 3.2.6 采购清单 (Shopping List)

**功能清单**：
- [x] 按「采购活动」管理清单（每次采购为一条活动记录）
- [x] 活动类型：生活超市 / 朴朴 / 山姆 / 奶茶店 / 面包店 / 淘宝网购 / 线下菜市场
- [x] 采购项目按 TodoList 方式展示，支持勾选完成、手动编辑添加
- [x] 「暂缓采购」选项：标记后的项目在下次新建**同类型**活动时自动带入
- [x] 「采购完成」按钮：一键将未购项目移入暂缓采购区，可选输入总金额并同步到账单（支出）
- [x] 主界面「暂缓采购区」标签页：集中查看全部暂缓项目，可移回 / 删除
- [x] 支持导入 `.txt` 文件（多行项目名），提供可复制的模板
- [x] 新建活动先选类型，自动填入「类型 + 时间」作为名称；手动改名后不覆盖（v1.0.2）
- [x] 删除「采购活动」前增加二次确认（v1.0.4-rc1）

**数据模型**（见 [第4章](#4-数据结构与存储方案)）：`shopping_activities` + `shopping_items`（活动—项目二级结构）。

#### 3.2.7 个人名片 (Personal Card)

**功能清单**：
- [x] 生活模块新增「个人名片」入口，名片按屏幕可用高度缩放，尽量一屏内完整展示
- [x] 展示头像、姓名 / 称呼、手机号、QQ、QQ 二维码、微信二维码、B站昵称与 UID
- [x] B站昵称与 UID 合并展示，支持自定义信息栏（v1.0.1）
- [x] 设置页新增「个人名片信息」，可填写上述文字资料并选择本地图片
- [x] 图片复制到应用私有目录，文字与路径以 JSON 形式保存在 DataStore `personal_profile`

**交互**：
- 工具箱 → 生活 → 个人名片：查看名片，右上角「编辑」进入编辑页
- 设置 → 个人名片信息：直接填写 / 修改

---

### 3.3 娱乐模块

#### 3.3.1 音游上隐条 (Game Overlay Bar)

> 注："上隐条"指遮挡屏幕上方音符、**减小读谱压力**的悬浮图片（非判定线）。

**功能清单**（v1.0.0-dev2 重构，dev3 完善）：
- [x] 用户自选图片悬浮在屏幕上方
- [x] 支持纯色 / 文字内容，图片支持拉伸与裁切（v1.0.1）
- [x] 配置与图片保存在应用私有目录并持久化，无需每次重进重新选择（v1.0.1）
- [x] 默认高度与高度上限减半；导出备份提示图片无法随配置导出（v1.0.2）
- [x] 离开页面时立即保存图片与配置；同名图片直接覆盖（v1.0.3）
- [x] 手动调整图片宽度、高度、透明度（调节条拖动后**自动应用**，无需手动确认）
- [x] 应用外可拖拽移动（水平、垂直，拖拽位置实时同步）
- [x] 应用内一键开启 / 停止（前台通知提供"停止"操作）
- [x] ~~预设方案~~（已删除：各音游上隐条一致，无需预设）

**权限需求**：
- Android：`SYSTEM_ALERT_WINDOW` 悬浮窗权限（首次使用请求）

---

### 3.4 系统能力（小组件 & 通知）

#### 3.4.1 桌面小组件 (Widget)

- [x] 2×1「下一节课」小组件（半透明圆角卡片）：紧凑显示课程名称、开始时间与地点，点击直接进入课表
- [x] 2×2「快捷入口」小组件：半透明圆角卡片 + 纯图标展示设置中勾选的内置工具与便捷链接（最多前 4 项），点击直达对应页面
- [x] 两个小组件均为半透明圆角卡片，配色跟随应用当前主题（浅/深色 + 12 套配色）
- [x] 点击「下一节课」小组件进入课表；点击「快捷入口」中的图标直达对应工具 / 网页
- [x] 「快捷入口」小组件的内容**独立于**首页快捷入口配置：分别由 `widget_quick_tools` 与 `quick_tools` 控制，均支持勾选与排序；`links.isHomeCard` 两边共用（v1.0.4-rc4 起分离并支持排序）
- [x] 「下一节课」小组件随课程数据变化即时刷新：课程增删改、备份导入 / 清空、界面编辑后自动重绘（v1.0.4-rc1）

> 说明：快捷入口小组件每个入口使用圆角正方形底块，图标以主题色渲染；release 签名使用正式 keystore（`keystore/release.jks`）。

#### 3.4.2 通知

- [x] 番茄钟进行中：前台通知实时展示倒计时
- [x] 番茄钟手动停止 / 计时结束后：立即移除前台通知，不在通知栏残留
- [x] 课表「下一节课」提醒：`AlarmManager` 定时触发（提前 10 分钟），推送下一节课名称、时间、教室（支持单/双周 / 仅一次）

---

## 4. 数据结构与存储方案

### 4.1 Room 表结构

#### courses（课程表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 自增 |
| name | TEXT | 课程名称 |
| teacher | TEXT | 教师 |
| location | TEXT | 教室 |
| weekday | INTEGER | 星期几（1-7） |
| startTime | TEXT | 开始时间 "HH:mm" |
| endTime | TEXT | 结束时间 "HH:mm" |
| colorTag | TEXT | 颜色标签（hex） |
| note | TEXT | 备注 |
| weekType | TEXT | every / odd(单周) / even(双周) / once(仅一次) |
| onceWeek | INTEGER | weekType == "once" 时的具体周次 |
| createdAt | INTEGER | unix timestamp |

#### notes（笔记）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | |
| title | TEXT | 标题 |
| content | TEXT | Markdown 原文 |
| updatedAt | INTEGER | |
| createdAt | INTEGER | |

#### todos（待办）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | |
| title | TEXT | |
| note | TEXT | 备注 |
| priority | INTEGER | 0低 / 1中 / 2高 |
| dueDate | TEXT | "yyyy-MM-dd HH:mm" |
| tags | TEXT | 逗号分隔 |
| isDone | INTEGER | 0/1 |
| sortOrder | INTEGER | 排序 |
| createdAt | INTEGER | |

#### pomodoro_records（番茄钟历史）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | |
| presetName | TEXT | 使用的预设名 |
| focusMinutes | INTEGER | 本轮专注分钟数 |
| completedAt | INTEGER | 完成时间戳 |

#### gpa_courses（绩点课程）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | |
| semester | TEXT | 学期 |
| name | TEXT | |
| credits | REAL | 学分 |
| rawScore | REAL | 原始成绩 |
| gradePoint | REAL | 换算后绩点 |

#### links（便捷链接）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | |
| name | TEXT | |
| url | TEXT | |
| category | TEXT | 分类 |
| icon | TEXT | 图标名称（lucide） |
| sortOrder | INTEGER | |
| isHomeCard | INTEGER | 是否显示在首页快捷入口（0/1） |

#### ledger_entries（账单）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | |
| type | TEXT | "income" / "expense" |
| amount | REAL | |
| category | TEXT | |
| paymentMethod | TEXT | 支付方式（wechat / alipay / bankcard / 自定义） |
| note | TEXT | |
| date | TEXT | "yyyy-MM-dd HH:mm" |

#### ledger_categories（账单分类预设）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | |
| name | TEXT | |
| type | TEXT | "income" / "expense" |
| icon | TEXT | |
| isCustom | INTEGER | 用户自定义（0/1） |

#### schedules（应用内日程记录）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | |
| deviceEventId | TEXT | 系统日历事件 ID（关联用） |
| title | TEXT | |
| startTime | TEXT | "yyyy-MM-dd HH:mm" |
| endTime | TEXT | |
| reminderMinutes | INTEGER | 提前提醒分钟数 |

#### shopping_activities（采购活动）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 自增 |
| name | TEXT | 活动名 |
| type | TEXT | 生活超市 / 朴朴 / 山姆 / 奶茶店 / 面包店 / 淘宝网购 / 线下菜市场 |
| createdAt | INTEGER | 创建时间戳 |

#### shopping_items（采购项目）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 自增 |
| activityId | INTEGER | 所属活动 id |
| name | TEXT | 项目名 |
| isPostponed | INTEGER | 是否「暂缓采购」（0/1） |
| isDone | INTEGER | 是否已购（0/1） |
| createdAt | INTEGER | |

#### overlay_config（悬浮窗配置）
> v1.0.0-dev2 起由预设改为单一图片配置，存于 DataStore（见 4.2），不再使用 Room 表。

### 4.2 DataStore 键

| 键 | 类型 | 说明 |
|----|------|------|
| `app_theme` | String | 当前主题 key |
| `app_ui_mode` | String | 深浅色模式（SYSTEM / LIGHT / DARK） |
| `gpa_formula` | String | 绩点公式 JSON |
| `pomodoro_presets` | String | 番茄钟预设列表 JSON |
| `pomodoro_keep_screen_on` | Boolean | 番茄钟保持亮屏开关 |
| `screen_brightness_immersive` | Float | 沉浸式模式亮度（0.0-1.0） |
| `timetable_current_week` | Int | 课表当前周数 |
| `timetable_semester_weeks` | Int | 课表学期长度（周数） |
| `timetable_semester_start` | String | 课表学期第一周起始日期（`yyyy-MM-dd`） |
| `is_first_launch` | Boolean | 首次启动 |
| `home_cards` | String | 首页卡片类型与排序 JSON |
| `quick_tools` | String | **首页**快捷入口工具 key 的勾选与排序 JSON |
| `widget_quick_tools` | String | **桌面小组件**快捷入口工具 key 的勾选与排序 JSON（v1.0.4-rc4 起与首页独立） |
| `theme_group_order` | String | 自定义主题分组的展示顺序 JSON（v1.0.4-rc4） |
| `personal_profile` | String | 个人名片信息 JSON（头像 / 二维码图片存本地路径） |
| `overlay_config` | String | 上隐条模式 / 颜色 / 文字 / 缩放与坐标 JSON |
| `course_presets` | String | 课表预设列表 JSON |
| `period_timings` | String | 第几节课时间预设列表 JSON（分上 / 下午，v1.0.4-rc1；v1.0.4-rc2 起默认清空） |
| `custom_themes` | String | 自定义主题列表 JSON（.sthm 可导入导出） |

### 4.3 数据备份（Backup）

设置页「数据管理」提供纯文本 JSON 备份能力，由 `BackupManager` + `BackupDao` 实现：

- **导出数据**：将全部 Room 表序列化为单个 JSON 文件（`BackupData`，含 `formatVersion`、`exportedAt` 与各表数据），经 SAF（`CreateDocument`）写出。
- **导入数据**：经 SAF（`OpenDocument`）读取 JSON，支持两种模式——
  - *增量*：按主键 `REPLACE` 合并，保留现有数据；
  - *覆盖*：先清空全部表，再写入备份数据。
- **清空数据**：二次确认后删除全部业务表数据（保留数据库结构）。

> 备份格式为 JSON 纯文本，跨设备 / 跨版本兼容，便于用户迁移与归档。v1.0.1 起备份同时包含个人名片、上隐条配置、课表预设、番茄钟预设与自定义主题等 DataStore 数据。

自 v1.0.4-rc2 起，备份升级为 **.stbc** 格式：除数据外还**内嵌全部图片资源**（头像 / 二维码 / 悬浮图以 base64 从本地路径读取内嵌，主题背景图本就为 base64），换机 / 迁移不失效。不再支持读取旧版本（仅存路径）的备份，新格式同样为 JSON 纯文本，扩展名 `.stbc`。同时绑定了 `.sthm` / `.stbc` 的系统「打开方式」，从文件管理器打开文件即可直接导入。

自 **v1.1.0** 起，`BackupData`（`formatVersion = 3`）补齐此前遗漏的 DataStore 配置项并随备份导出 / 还原 / 清空：深色模式 `uiMode`、番茄钟保持亮屏与沉浸亮度、首页卡片 `home_cards`、首页快捷入口 `quick_tools`、桌面小组件快捷入口 `widget_quick_tools`、主题分组顺序 `theme_group_order`、学期长度 `semester_weeks` 与起始日期 `semester_start`、绩点公式 `gpa_formula`、第几节课时间预设 `period_timings`。旧备份缺少这些字段时按默认值处理、不覆盖当前配置。

---

## 5. UI/UX 设计规范

### 5.1 主界面（概览）

首页为**卡片式布局**：圆角卡片突出主题背景色（淡化主题色 + 描边突出边界），卡片内部支持滚动；卡片支持增删、排序（去重，配置持久化到 `home_cards`）。

顶部头部（非卡片）：居中时钟（大号时间 + 日期/星期 + 农历/节气）；**去掉原应用标题**。

「管理卡片」入口位于页面底部（默认收起，点击展开卡片增删 / 排序）。

默认卡片（顺序可由用户自定义）：

```
┌────────────────────────────────────────┐
│ 2026-09-09 周二 · 丙午年八月初八 · 白露 │  ← 头部：日期/星期/农历/节气
└────────────────────────────────────────┘
┌────────────────────────────────────────┐
│ 📚 今天接下来                          │  ← 课表卡（仅当天接下来 3 节课）
│  10:00-10:45 数据结构 (教B-201)        │
│  14:00-14:45 操作系统 (教A-301)        │
│  ─ 距下节课 8 分钟 ─                   │  ← 距上课 <10 分钟小字提醒
└────────────────────────────────────────┘
┌────────────────────────────────────────┐
│ 💰 余额 ¥1,280.00                      │  ← 账单卡（余额+单日账单+快捷记账）
│  今日支出 ¥52.00                       │
│  [+ 记一笔]                            │
└────────────────────────────────────────┘
┌────────────────────────────────────────┐
│ ✅ TodoList                            │  ← TodoList 卡（快捷勾选+入口）
│  ☐ 高数作业    ☑ 交实验报告            │
└────────────────────────────────────────┘
┌────────────────────────────────────────┐
│ 📅 今日剩余日程                        │  ← 日程卡
└────────────────────────────────────────┘
┌────────────────────────────────────────┐
│ 快捷入口                               │  ← 工具 + 便捷链接的聚集地
│  [课表] [番茄钟] [水费] [教务]         │     （统一管理）
└────────────────────────────────────────┘
```

> 账单卡、TodoList 卡与工具内部数据**实时同步**；课表卡仅展示当天接下来三节课。
> 日程卡有内容时保持最小高度，避免与其它卡片长短不一致（v1.0.3）。

### 5.2 导航条与工具箱

**底部导航条（Bottom Navigation Bar）**，三栏：

| Tab | 图标（lucide） | 说明 |
|-----|------|------|
| 概览 | house | 主界面（默认页） |
| 工具箱 | layout-grid | 所有工具入口 |
| 设置 | settings | 设置页 |

> 全局图标统一使用 [lucide](https://lucide.dev/icons/) SVG 图标（`IconPaths.kt` + `AppIcon` 组件），色调由主题 `tint` 统一控制，替代原 emoji 图标；底部导航条使用浅色 `primaryContainer` 容器 + `secondaryContainer` 指示器，随主题配色改浅，并保证「水墨」主题下选中图标可读。

> v1.0.1 起底部菜单与二级页面切换统一使用水平滑动动画；v1.0.4-rc4 起底部栏 tab 切换按相对方向滑动；v1.0.4-rc5 起所有页面切换统一为水平滑动并校正方向（push 新页从右进入、旧页向左退；pop 下层从左侧进入、当前页向右退）；v1.0.4-rc6 起底部栏 tab（概览 / 工具箱 / 设置）切换动画改为**最原始的左右平移**（两页同速平行移动，更自然）——向右切整体向左平移、向左切整体向右平移，子页面的进入 / 返回滑动保持不变。

> 首页「快捷入口」是**工具箱内工具 + 便捷链接**的聚集地，与「便捷链接收藏」统一管理，避免重复维护；v1.0.4-rc4 起首页与桌面小组件快捷入口配置独立，各处支持排序。

**工具箱页面**：
- 图标网格 + 下方文字标签
- 按分类分组，**可折叠/展开**
- 分类结构：

```
▼ 📖 学习
  📅 课表管理   📝 笔记速记   ✅ TodoList   🍅 番茄钟   📊 绩点计算器

▼ 🏠 生活
  💡 水电费缴交   📋 日程提醒
  🔗 便捷链接收藏   💰 简易账单   🛒 采购清单

▼ 🎮 娱乐
  🎵 音游上隐条
```

### 5.3 设置页

设置页为分栏滚动列表：

```
首页
├─ 首页快捷入口（独立勾选 + 排序首页展示的工具与链接）
├─ 桌面快捷入口小组件（与首页各自独立，勾选 + 排序）
├─ 快捷链接管理（添加 / 编辑 / 排序自定义链接）

个人名片
├─ 个人名片信息（头像 / 手机号 / QQ / QQ与微信二维码 / B站昵称与 UID）

外观
├─ 配色主题（12 套，按色调排序，列表可滚动）
├─ 自定义主题（角色字段化编辑，.sthm 导入导出）
├─ 深浅色模式（跟随系统 / 浅色 / 深色）

数据管理
├─ 导出数据（JSON 备份）
├─ 导入数据（增量 / 覆盖）
├─ 清空数据（二次确认）

系统
├─ 更新日志（按版本子菜单查看）
├─ 关于 Solium的工具箱
```

### 5.4 配色方案

内置 **12 套**预设主题，均采用扁平化设计，按**色调排序**，同时定义浅色 + 深色两套色值；v1.0.1 起支持把主题颜色按 UI 角色（主色、页面背景、卡片、文字、边框等）字段化自定义，并支持 .sthm 主题包导入 / 导出；v1.0.3 起颜色选择器使用色调 / 饱和度 / 亮度滑杆；v1.0.4-rc1 起：
- 颜色编辑支持**透明度（Alpha）**滑杆，浅色 / 深色可独立设置；
- 新增**预览页**，标注每个颜色角色分别作用于哪些内容（按钮 / 卡片 / 文字 / 输入框 / 分隔线）；
- 主题新增**背景图与透明度属性**：主页 / 番茄钟 / 番茄钟沉浸模式 / 课表背景图片（base64 内嵌，换机 / 迁移不失效）、课表背景图片透明度、主页卡片全局透明度；
- `.sthm`**向下兼容**：旧主题包缺新字段时自动取默认值导入；
- 自定义主题按**主题包折叠分组**展示。
- v1.0.4-rc2 起：主题包导入**按文件名自动分组**；分组可重命名 / 删除 / **导出为主题包**；自定义主题选择的 UI 改为**主色圆圈 + 主题名称**（同预设主题）。
- v1.0.4-rc3 起：修复「设置」页打开崩溃 —— 自定义主题选择区不再使用嵌套在滚动容器内的无限高度 `LazyVerticalGrid`，改为非懒加载的「主色圆圈 + 主题名称」列表，样式不变。
- v1.0.4-rc4 起：设置页自定义主题选择**按主题包分组、可折叠**；管理页支持新建分组、手动移动主题到某个分组（可输入新分组名）、用 ↑↓ 调整分组顺序，分组顺序持久化到 `theme_group_order`。
- v1.1.0 起：设置页自定义主题分组**默认全部折叠**（点击分组标题展开）。

> UI 提示：主题选择列表支持滚动，并显示「滚动查看更多」描述；`thor`（爱上雷神）采用全局**亮黄 + 亮紫**配色，底部导航条随主题配色改浅。

| 主题 key | 名称 | 主色 | 背景色（浅 / 深） | 文字色（浅 / 深） | 备注 |
|----------|------|------|-------------------|-------------------|------|
| `ink` | 水墨 | `#111111` | `#FFFFFF` / `#111111` | `#111111` / `#FFFFFF` | 黑白极简（原「黑白」更名） |
| `lemon` | 淡黄 | `#E0A800` | `#FFFAEB` / `#2A2400` | `#4A3900` / `#FFE9A3` | 新增，温暖明快 |
| `orange` | 橘色 | `#E86A33` | `#FFF3EC` / `#2E1504` | `#4A2410` / `#FFD9C2` | 新增，活力 |
| `coral` | 珊瑚红 | `#FF6B6B` | `#FFF0EF` / `#2E1212` | `#4A1A1A` / `#FFC9C9` | 新增，明快 |
| `pink` | 粉色 | `#E9749B` | `#FFF5F7` / `#2E121E` | `#4A1C2E` / `#FFC9DA` | 温柔 |
| `moss` | 墨绿 | `#3B6B4F` | `#F4F7F4` / `#0F1712` | `#1F2D25` / `#D9E8DD` | 自然沉稳 |
| `mint` | 薄荷 | `#5BB8A5` | `#EFFAF6` / `#0E1F1A` | `#1E3A33` / `#C9F0E4` | 新增，清爽 |
| `teal` | 青色 | `#2A9D8F` | `#EEF8F6` / `#0D211E` | `#123B36` / `#C4ECE6` | 新增，清透 |
| `sky` | 淡蓝 | `#5B9BD5` | `#F2F7FC` / `#0E1822` | `#1E3A52` / `#C9E2F5` | 清新 |
| `deepblue` | 深蓝 | `#2B4C7E` | `#F0F4FA` / `#0D1622` | `#14243D` / `#C9D8EE` | 新增，沉稳 |
| `lavender` | 淡紫 | `#9B7FB8` | `#F6F2F9` / `#150F1C` | `#3C2E4A` / `#DECFE8` | 柔和 |
| `thor` | 爱上雷神 | `#F6BD00` + `#8B5CF6` | `#FFFBF2` / `#17120A` | `#2E1F00` / `#F3E5C0` | 全局**亮黄 + 亮紫**，导航条改浅 |

---

## 6. 项目目录结构

```
UniToolKit/
├── settings.gradle.kts                # 仓库与模块配置
├── build.gradle.kts                   # 根构建脚本（插件版本）
├── gradle.properties
├── local.properties                   # SDK 路径
├── AGENTS.md                          # 协作规范（每次开发前必读）
├── DEV-DOC.md                         # 开发文档（本文档）
├── CHANGELOG.md                       # 更新记录（按版本）
├── themes/                            # 官方 .sthm 主题包（可手动导入）
│
└── app/
    ├── build.gradle.kts               # 应用模块依赖管理
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml        # 权限与组件声明
        ├── res/
        │   ├── values/                # strings / colors / themes
        │   ├── drawable/              # 应用图标 / 小组件背景 / 通知图标
        │   ├── mipmap-anydpi/         # 自适应与旧版应用图标
        │   ├── xml/                   # 小组件配置
        │   └── layout/                # 小组件布局
        └── java/com/unitoolkit/
            ├── MainActivity.kt        # 入口 Activity
            ├── UniToolkitApp.kt       # Application（DB + Settings 初始化）
            │
            ├── core/                  # 核心能力
            │   ├── theme/Theme.kt     # 12 套主题 + 深浅色
            │   ├── database/          # Room Entities / Daos / AppDatabase / BackupDao / BackupManager
            │   ├── storage/           # DataStore 封装（SettingsRepository）
            │   ├── model/             # 领域模型（@Serializable）
            │   └── utils/             # 日期、颜色、导出、ViewModel 工厂
            │
            ├── ui/                    # 界面层（每模块一个 Screen + ViewModel）
            │   ├── navigation/        # AppRoot + 路由定义
            │   ├── home/              # 概览（卡片式布局，可增删排序）
            │   ├── toolbox/           # 工具箱
            │   ├── settings/          # 设置（含更新日志、数据备份）
            │   ├── components/        # 可复用组件（卡片、顶栏、MarkdownText 等）
            │   ├── course/            # 课表（按时间 + 导出 + 时间轴可视化）
            │   ├── note/              # 笔记（Markdown 编辑）
            │   ├── todo/              # TodoList
            │   ├── pomodoro/          # 番茄钟 + 沉浸式模式
            │   ├── life/              # 水电费、日程、绩点、链接、账单、WebView
            │   ├── shopping/          # 采购清单（活动 + 项目 + 暂缓采购 + 导入）
            │   ├── overlay/           # 音游上隐条（图片悬浮窗）
            │   ├── profile/           # 个人名片（展示 + 编辑）
            │   └── widget/            # 桌面小组件（UniWidgetProvider / QuickEntryWidgetProvider）
            │
            └── service/               # 前台服务 / 通知
                ├── PomodoroService / PomodoroManager   # 番茄钟后台计时
                ├── OverlayService / OverlayManager     # 图片悬浮窗渲染
                └── CourseNotifier / CourseNotificationReceiver  # 课表下一节提醒
```

---

## 7. 开发计划

### 阶段划分

| 阶段 | 内容 |
|------|------|
| **P0 基础设施** | 项目脚手架、主题系统、Room 数据层、导航框架 |
| **P1 核心工具集** | 课表 + TodoList + 番茄钟 + 笔记速记 |
| **P2 生活工具集** | 日程提醒 + 绩点 + 便捷链接 + 简易账单 + 水电费 |
| **P3 首页整合** | 概览页 4 张卡片 + 数据联动 |
| **P4 设置与主题** | 设置页 + 12 套主题 + 深浅色模式 |
| **P5 娱乐模块** | 音游上隐条（图片悬浮窗） |
| **P6 打磨发布** | 沉浸式模式、后台稳定、通知、导入导出测试、APK 交付 |

> 版本更新记录见 [CHANGELOG.md](./CHANGELOG.md)。

### 关键依赖（app/build.gradle.kts）

```kotlin
dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // 导航
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Room + KSP
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WebView
    implementation("androidx.webkit:webkit:1.12.0")

    // 序列化 + 协程
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

---

## 附录 A：番茄钟默认预设

| 预设名 | 工作(min) | 短休(min) | 长休(min) | 长休间隔 |
|--------|-----------|-----------|-----------|----------|
| 专注 25/5 | 25 | 5 | 15 | 4 轮 |
| 深度 50/10 | 50 | 10 | 30 | 3 轮 |
| 快速 15/3 | 15 | 3 | 10 | 4 轮 |

---

*— END OF DOC —*
