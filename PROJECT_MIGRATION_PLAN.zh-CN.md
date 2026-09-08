# Piru Web 与 Android 移植计划（供评审）

> 状态：执行中；已完成契约、catalog、本地 Journal/备份基础和 PK 首个跨平台切片
> 目标：在不牺牲隐私、数据准确性、药理模型完整性和现有 iOS 体验的前提下，使用 coding agent 将 Piru 扩展到 Web 与 Android。  
> 原则：这是一个跨平台产品重构计划，不是把 SwiftUI 逐行翻译成另一种 UI 语言。

## 1. 评审结论摘要

已确认的产品决策：

- Web 采用“本地 PWA + 加密导出”，暂不加入同步服务。
- Android 最低 API 24（Android 7），同时支持 Google Play 和无 Google 服务发行渠道。
- Web 支持至少两年前版本的 Chrome；继续完全无遥测。
- 允许引入共享核心；首版仍要求现有能力全部具备，包括 Health Connect、条码、通知、库存、耐受、洞察和 PDF。
- 接受浏览器可能清除本地数据，并在产品中明确手动备份责任。

建议采用：

1. **共享领域契约 + 平台原生 UI**：将纯领域模型、计算引擎、数据格式和测试契约抽出为跨平台核心；Web 使用 TypeScript/React，Android 使用 Kotlin/Jetpack Compose。
2. **默认本地优先、无同步服务**：首个跨平台版本不引入账号、云端或同步服务；只支持本地数据库、加密备份和显式导入导出。未来若重新提出同步需求，必须另行评审。
3. **SQLite 继续作为药物/物质只读数据的发布格式**：复用现有 Python pipeline 和 `manifest.json`/checksum 机制；Web 通过受控下载或构建时导入，Android 使用本地 SQLite/Room 只读副本。
4. **用户日志采用平台适配存储**：Web 使用 IndexedDB/SQLite WASM，Android 使用 Room，iOS 保留 SwiftData。三端通过版本化 JSON 数据交换契约互通，而不是强行共享 SwiftData。
5. **先做垂直切片再扩展功能**：优先完成“物质库 → 快速记录 → 会话时间线/PK → 备份恢复”闭环，再迁移交互、洞察、通知、设备集成等边缘能力。

## 2. 当前基线与主要约束

### 2.1 已有能力

- iOS/macOS 主应用基于 SwiftUI、SwiftData、Swift 6，最低 iOS/macOS 26。
- 领域层包含 `Substance`、剂量单位、给药途径、剂量区间、持续时间等值类型。
- `Shared/Models` 中有约 20 个 SwiftData 模型，涵盖剂量、会话、每日用药、库存、颜色、用户配置、耐受状态等。
- `Shared/Engines` 已有 PK/PD、效应、时间线曲线和会话聚类等计算能力。
- 物质库由 Python pipeline 从多来源构建为约 1,700+ 条目的 SQLite；来源优先级、来源归属、引用和 curated overlay 是核心产品能力。
- 交互引擎按药理类别和规则工作，不应在移植时退化成简单的名称匹配。
- 现有能力还包括通知、HealthKit、WatchConnectivity、Widget/Live Activity、条码/产品码、加密备份、导入导出和三种语言本地化。
- 现有产品强调本机存储、无账号、无服务器；备份是用户主动触发且加密。

### 2.2 不应直接照搬的部分

- SwiftUI View、`@Query`、SwiftData `@Model`、`@Observable` 单例和 `AppNavigator` 不能直接跨到 Web/Android。
- WidgetKit、ActivityKit、HealthKit、WatchConnectivity、UIKit/AppKit 和 iCloud Keychain 都需要平台替代品或明确的能力降级。
- Apple 的 Liquid Glass、导航语义、通知权限和后台执行模型不能作为跨平台产品契约。
- 直接共享三端数据库 schema 会把 SwiftData 的实现细节扩散到所有平台，增加迁移和数据损坏风险。
- 直接引入云端账户会改变产品的隐私承诺；必须单独评审并获得产品决策。

## 3. 目标架构

```text
                         ┌────────────────────────────┐
                         │  版本化跨平台领域契约       │
                         │  JSON schema / 文档 / fixtures│
                         └──────────────┬─────────────┘
                                        │
             ┌──────────────────────────┼──────────────────────────┐
             │                          │                          │
       iOS/macOS                     Android                     Web
    SwiftUI + SwiftData       Kotlin + Compose + Room       React + TypeScript
    现有实现逐步适配             原生平台能力                    PWA/桌面浏览器
             │                          │                          │
             └──────────────┬───────────┴──────────────┬───────────┘
                            │                          │
                   相同计算结果与 fixtures       相同物质库发布产物
                            │                          │
                 本地加密备份/显式导入导出       Python 数据构建 pipeline
```

### 3.1 分层职责

**A. Product contract**

- 定义实体、字段、单位、枚举、时间语义、来源归属、置信度和错误语义。
- 以 JSON Schema、示例 fixture、兼容性规则和变更日志作为唯一跨平台协议。
- 所有平台必须能读取旧版本数据，并拒绝或隔离未知的破坏性字段。

**B. Domain/engine**

- 纯函数优先：单位换算、剂量格式化、PK/PD 曲线、会话聚类、活性窗口、交互规则、耐受计算、分析聚合。
- 不依赖 UI、平台通知、数据库上下文、Keychain、HealthKit 或 SwiftData。
- 第一阶段可保留各平台实现，但必须以同一组 golden fixtures 校验；第二阶段再评估 Kotlin Multiplatform、Rust/WASM 或共享算法包。

**C. Read-only substance catalog**

- 继续由 `pipeline/build.sh` 构建完整 SQLite。
- 发布 `manifest.json`、schema version、checksum、数据版本和来源/许可元数据。
- Web 和 Android 只读打开本地副本；用户自定义覆盖层独立保存并在读取时叠加。
- 不允许手工修改二进制数据库，不允许平台各自生成不同的物质库。

**D. User journal**

- 平台本地数据库分别实现：iOS SwiftData、Android Room、Web IndexedDB 或 SQLite WASM。
- 通过版本化导入/导出格式同步，不通过内部 ORM 表直接互换。
- 所有保存操作需要幂等 ID、创建/更新时间、软删除或 tombstone、schema version 和迁移策略。

**E. Optional sync**

- 首版不实现账号、服务器、同步 API 或后台同步。
- Web/Android 之间通过 Piru Native 加密备份手动搬迁数据。
- 不把本地数据库文件作为跨平台交换格式，也不上传官方物质库或用户日志。

## 4. 跨平台数据契约

### 4.1 先盘点并冻结的实体

按优先级分组：

1. **核心日志**：DoseEntry、Session、QuickLogDose、SessionNote、RoutineOccurrence。
2. **用户配置**：UserProfileRecord、NotificationPreferences、FavoriteSubstance、SubstanceColor、UserColor。
3. **医学/分析相关**：DailyDoseItem、ToleranceState、InventoryItem、LabMeasurement。
4. **可选和平台相关**：健康数据样本、设备状态、Live Activity 状态、Widget 快照。

每个实体明确：

- 稳定 ID 的生成方式和跨设备唯一性；
- 时间统一使用 UTC 存储，并保存显示时区/日界线所需信息；
- 质量、体积、浓度和剂量单位的 canonical 表示；
- 给药途径、盐型、品牌、来源和自定义覆盖的编码；
- 删除、撤销、重复导入和冲突合并行为；
- 是否属于敏感数据、是否默认包含在导出/同步中。

### 4.2 交换格式

- 建立 `data-contract/`（名称可在评审后确定），包含 JSON Schema、样例、迁移说明和错误码。
- 保留现有 PsyLog-compatible JSON 导出作为兼容输入，当前跨平台规范为 Piru Native v1。
- 为每个模型提供最小、完整、未知字段和旧版本样例。
- 用同一批 fixture 测试 Swift、Kotlin、TypeScript 的编码/解码、单位换算和日期边界。
- 明确禁止将本地数据库文件作为跨平台同步协议。

## 5. Web 端计划

### 5.1 产品边界

首个 Web 版本定位为“可安装 PWA + 桌面浏览器”，不承诺所有原生能力：

- 支持物质库、搜索、详情、快速记录、会话、时间线、PK/交互分析、洞察、导入导出和设置。
- 支持离线启动、离线读取和离线记录；数据不因无网络而丢失。
- 用 Web Notifications 提供可选提醒，但必须处理权限撤销、浏览器休眠和时区变化。
- 不在首版承诺 HealthKit、后台精确通知、相机条码扫描、桌面小组件或跨浏览器后台同步。

### 5.2 技术方向

- React + TypeScript，组件库和设计 token 独立于 SwiftUI。
- PWA service worker 只负责静态资源和 catalog 缓存，不缓存敏感日志到可公开的 HTTP cache。
- IndexedDB 保存 journal；若需要复杂 SQL 分析，评估 SQLite WASM，但必须做包体、内存和 Safari 兼容性测试。
- Web Crypto API 使用用户提供的 passphrase 或浏览器生成密钥保护本地备份；不假设浏览器安全存储永不丢失。
- 路由使用 URL 可分享的非敏感页面状态；绝不把剂量、日志内容或密钥放入 URL、分析服务或 crash payload。

### 5.3 Web 特有工作

- 设计响应式布局、键盘可访问性、屏幕阅读器标签、触摸/鼠标/键盘交互。
- 处理多标签页并发、IndexedDB 事务失败、存储配额不足、浏览器清理站点数据和离线更新。
- 建立 service worker 升级和数据库迁移回滚策略。
- 明确“在公共设备上使用”的退出、锁定、自动清理和导出提醒。

## 6. Android 端计划

### 6.1 产品边界

- Kotlin + Jetpack Compose + Room，遵循 Android 的 back stack、Material 3 和无障碍规范。
- 优先覆盖手机；平板/折叠屏采用自适应布局，不复制 iPad 的固定尺寸。
- WorkManager 负责可容忍延迟的后台任务；AlarmManager 只用于确有必要的用户提醒，并处理 Doze 和电池限制。
- 通知、相机条码、文件选择器、分享、备份恢复先做平台原生实现。

### 6.2 Android 特有能力

- Android Health Connect 作为 HealthKit 的可选替代，明确字段映射、授权撤销和数据删除。
- Keystore + Android 加密存储保护本地密钥；禁止把密钥放在 SharedPreferences 明文或日志中。
- Room migration 必须有版本化测试；数据库迁移失败时提供只读恢复/导出路径，不静默删除用户数据。
- Android Auto Backup、设备迁移和手动加密备份分别评估，避免未经用户理解地上传敏感日志。
- 后台通知文案保持中性、可关闭、无敏感 substance 名称泄露到锁屏，除非用户明确选择。

## 7. iOS/macOS 保持与适配

- 不在移植期间大规模重写现有 SwiftUI；先抽出契约和可测试引擎，降低回归风险。
- 将跨平台契约测试接入现有 Swift Testing。
- 保持 `SubstanceLibrary` 作为物质解析入口，继续通过 pipeline 发布完整 SQLite。
- 对 iOS 专属集成建立 capability matrix，注明 Android/Web 的对应能力、降级文案和测试状态。
- 若未来共享算法，优先从无平台依赖的 `Shared/Engines` 开始，而不是从 View 或 SwiftData 模型开始。

## 8. 后端与同步（条件性阶段）

只有在确认“跨设备同步”是硬需求后实施：

1. 明确威胁模型：服务端运营者、数据库泄露、设备丢失、恶意客户端、重放和回滚。
2. 设计客户端端到端加密 envelope：版本、对象 ID、设备 ID、序列号、密文、认证标签和最小元数据。
3. 使用经过审计的密码库和标准 AEAD；不自行设计加密算法。
4. 采用 passphrase + 设备密钥的恢复方案，清楚说明“忘记恢复材料无法找回数据”。
5. 服务端实现最小 API：注册设备、公钥/密钥包、上传变更、下载变更、删除账户/数据、速率限制。
6. 设计冲突策略：日志实体优先保留并合并，设置类字段按明确的版本策略，删除使用 tombstone 保留窗口。
7. 不收集广告标识符，不在服务端记录剂量内容，不把日志用于模型训练，提供完全删除和可验证导出。
8. 进行独立安全评审、依赖审计、渗透测试和密钥轮换演练后才开放默认入口。

## 9. 功能迁移顺序

### 阶段 0：决策与基线

- [x] 确认坚持“无账号/无服务器/无遥测”为默认产品承诺。
- [x] 确认 Web 为本地 PWA + 加密导出。
- [x] 确认 Android 最低 API 24，并支持 Google Play 与无 Google 服务发行渠道。
- [x] 盘点现有实体、功能、权限、通知和平台依赖。
- [ ] 建立功能矩阵：iOS、macOS、Android、Web、不可支持/降级。
- [x] 记录 SQLite manifest、导出样本、契约 fixture 和 PK 关键计算结果，形成迁移基线。

### 阶段 1：契约与测试基座

- [x] 冻结首批实体、枚举、单位、时间和错误码。
- [x] 编写 JSON Schema、示例数据、迁移规则和字段敏感性分类。
- [x] 从现有模型生成/手写互操作 fixture，不直接复制 SwiftData schema。
- [ ] 为 PK/PD、单位换算、时间线、交互、耐受和聚类建立 golden fixtures。
- [x] 建立契约、catalog、Web、Android CI 检查和失败诊断命令。

### 阶段 2：物质库与只读参考

- [x] 将现有 SQLite manifest/checksum 校验约束落到平台 bootstrap 协议。
- [/] 实现 Web 的只读 catalog 导入、缓存和搜索；模糊搜索、来源展示待完成。
- [/] 实现 Android 的只读 catalog 导入、构建期 asset 和搜索；来源展示待完成。
- [x] 对生成 snapshot 的条目数量、名称唯一性和字段完整性做自动校验。
- [ ] 验证 curated overlay、区域名称、中文内容和官方数据库更新不会产生平台分叉。

### 阶段 3：日志垂直切片

- [/] Web：已完成 IndexedDB 剂量记录、删除、加密导入/导出；物质选择、会话详情、编辑/撤销和完整实体待完成。
- [/] Android：已完成 API 24 SQLite 剂量记录；Room migration、导入/导出、进程被杀恢复和完整实体待完成。
- [x] iOS：已固化 Piru Native v1 导出契约，不改变既有用户数据路径。
- [/] 已有契约 fixture 和 Web/Python 检查；重复记录、日期边界及 Kotlin/Swift round-trip 待完成。

### 阶段 4：计算与时间线

- [/] Web/Android 已接入基础 PK 剩余比例计算、Android tolerance/PDF 基础和共享 fixture；三端 PK/PD、active window、session clustering 和图表待完成。
- [/] 已建立 PK 采样点 fixture；峰值、Tmax、零阶清除和时区转换待完成。
- [ ] 交互结果按类别/规则/证据级别展示，并保留来源文本和“不是医疗建议”边界。
- [ ] 对极端剂量、空数据、缺失半衰期、跨午夜会话、夏令时和离线状态做测试。

### 阶段 5：备份、恢复与本地化

- [/] Web 已支持 PBKDF2/AES-GCM 加密备份导入/导出；Android 已加入同参数 encrypted envelope 工具，完整实体序列化/UI 导入导出待完成。
- [ ] 设计密码错误、部分损坏、旧版本、重复导入和中断恢复行为。
- [ ] 迁移英文、简体中文、繁体中文目录；不把平台默认翻译当作药理内容翻译。
- [ ] 验证所有用户可见字符串、辅助功能标签、通知和错误消息均已本地化。

### 阶段 6：平台集成

- [/] Android 已建立 API 24 兼容的通知 channel、Android 13+ 通知权限请求、Room 数据层、Health Connect 可选适配边界和平台能力探测；Health Connect 实际授权、条码扫描、分享、文件系统待完成。
- [/] Web/Android 已加入本地、无遥测的 30 天 Journal 基础洞察；完整 UsageAnalytics 待完成。
- [/] Web PWA 已有安装/离线 shell、加密文件导入导出、通知权限、打印/PDF 降级和 BarcodeDetector 相机扫描；后台提醒和多标签页待完成。
- [/] Web/Android 已建立与 iOS Theme 的 accent、背景、卡片、输入面、圆角、间距和深色模式视觉 token；逐屏 1:1 信息架构与截图回归仍待完成。
- [/] 新增 `pipeline/ui_translate.py`，可遍历全部 `Piru/Views` 并为 Web/Android 生成可审阅的 UI 翻译桩和 manifest；桩代码必须逐屏人工修正、补齐行为并通过截图回归后才算完成。
- [/] Journal 首屏已从翻译桩替换为 Web/Android 生产 UI，包含四 Tab 主导航、剂量输入卡片、最近记录和基础洞察；已加入静态 parity 检查，截图回归与其余屏幕仍待完成。
- [/] Android 已有 API 24 AlarmManager 提醒调度边界；需补充设置入口、精确闹钟授权和完整提醒恢复测试。
- [ ] iOS/macOS 现有 HealthKit、Watch、Widget、Live Activity 做回归。
- [/] 已为 Web 条码/通知/PDF 和 Android Health Connect/通知能力提供不支持时的明确边界；完整权限状态、分享/文件系统和恢复测试待完成。

### 阶段 7：可选同步与发布

- [x] 明确首版不实现同步服务、账号或后端。
- [ ] 进行本地离线、权限、隐私评估、安全评审和依赖漏洞扫描。
- [ ] 进行隐私评估、安全评审、依赖漏洞扫描、数据删除演练和事故响应演练。
- [ ] 分阶段灰度发布：内部 → 受邀测试 → 小比例公开 → 全量。

## 10. Coding agent 协作流程

### 10.1 Agent 分工

建议每个 agent 只承担一个边界明确的工作包：

- **Inventory agent**：维护功能/模型/平台能力矩阵，只读调查。
- **Contract agent**：编写 schema、fixture、迁移说明和兼容性测试。
- **Data agent**：研究 SQLite 发布、Web/Android 导入和 catalog 校验。
- **Engine agent**：迁移一个纯计算模块，并报告与 Swift golden fixture 的差异。
- **Web agent**：只负责 Web vertical slice，不修改 Android 或 iOS 业务逻辑。
- **Android agent**：只负责 Android vertical slice，不复制 Web 状态管理。
- **Security agent**：威胁模型、密钥/备份/同步审查和依赖扫描。
- **QA agent**：跨平台 fixture、离线、迁移、无障碍和边界回归。

### 10.2 每个任务的强制交付物

- 变更范围和未解决问题；
- 影响的契约版本/数据库版本；
- 测试命令与结果；
- golden fixture 差异报告；
- 隐私、权限、日志和错误处理说明；
- 不支持能力的明确列表；
- 不得修改的现有行为。

### 10.3 合并门槛

- 任何实体字段变更必须先更新 schema、示例、迁移和三端解码测试。
- 任何算法移植必须通过相同输入的跨平台结果比较；允许误差须写入规范。
- 任何权限、通知、同步或敏感数据变更必须经过安全/隐私审查。
- 不允许 agent 直接手改生成的 SQLite；数据变更必须走 pipeline。
- 不允许为通过测试而删除或弱化既有测试。

## 11. 测试与质量门槛

### 11.1 正确性

- 单元测试：领域模型、单位、日期、PK/PD、交互、耐受、聚类、序列化。
- Golden tests：同一 fixture 在 Swift/Kotlin/TypeScript 结果一致。
- 数据测试：catalog checksum、schema、来源完整性、引用、别名和 curated overlay。
- 迁移测试：每个旧版本导出可导入，损坏输入不会删除现有数据。

### 11.2 平台质量

- Web：Chrome/Safari/Firefox、移动 Safari、离线、存储配额、多标签页、PWA 更新。
- Android：至少一台新设备、一台低端设备、横竖屏、进程回收、Doze、不同字体/语言。
- iOS/macOS：现有构建、测试、数据库获取、导入导出和 HealthKit/Watch 回归。
- 无障碍：VoiceOver、TalkBack、键盘导航、动态字体、对比度和减少动画。

### 11.3 安全与隐私

- 改动文件提交前做 secrets scan；依赖增加前做漏洞数据库检查。
- 日志、崩溃报告和遥测默认不含 substance 名称、剂量、笔记和健康样本。
- 检查 URL、剪贴板、通知锁屏、分享预览、备份临时文件和浏览器缓存泄露。
- 加密备份使用标准库、认证加密和明确的密钥丢失行为。

## 12. 主要风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 三端算法逐渐漂移 | 时间线和交互结论不一致 | 共享契约、golden fixtures、误差预算、版本化算法 |
| 强行共享 SwiftData schema | 迁移困难、数据损坏 | 版本化 JSON 契约，平台数据库独立 |
| Web 本地存储被清理 | 用户数据丢失 | 明显的加密导出提醒、恢复流程、可选同步 |
| 浏览器后台限制 | 通知不准 | 不承诺精确后台行为，使用清晰的能力降级 |
| Android 厂商后台限制 | 提醒遗漏 | WorkManager/AlarmManager 分级并显示权限状态 |
| 引入服务器破坏隐私承诺 | 信任和合规风险 | 同步作为 opt-in，端到端加密，独立安全评审 |
| 物质库平台分叉 | 药理信息不一致 | 统一 manifest/checksum 和同一发布 artifact |
| 药理内容误译或过度声称 | 现实伤害和合规风险 | 来源字段保留、人工审校、证据等级、禁止把模型变成建议 |
| UI 逐屏翻译导致低质量体验 | 可用性差、维护成本高 | 共享行为契约，平台原生信息架构和设计评审 |
| Agent 并行互相覆盖 | 回归和审查困难 | 小 PR、单边界 ownership、契约先行、合并门槛 |

## 13. 需要产品/维护者先确认的问题

1. Web 是否必须支持账号和跨设备同步，还是“本地 PWA + 加密导出”即可？
2. Android 是否需要 Google Play，是否也要支持无 Google 服务的发行渠道？
3. Web/Android 的最低浏览器、最低 Android API 和首发语言范围是什么？
4. 是否允许引入一个新的共享核心（Kotlin Multiplatform、Rust/WASM 等），还是先接受三端纯实现？
5. 哪些现有能力必须在首个跨平台版本具备：Health Connect、条码、通知、库存、耐受、洞察、PDF？
6. 是否接受 Web 上“浏览器可能清除本地数据”的产品警示和手动备份责任？
7. 同步服务若未来加入，谁负责密钥恢复、账户删除和运营维护？
8. 是否允许为平台适配增加少量遥测（仅性能/错误、不含敏感内容），还是继续完全无遥测？

## 14. 首个可验收里程碑

在开始大规模功能迁移前，先完成一个小型端到端证明：

- Web 和 Android 都能下载并校验同一份物质库 manifest；
- 都能搜索同一物质、显示同一来源和基础药理字段；
- 都能离线新增一条剂量记录并重启后恢复；
- 都能导出同一份 Piru Native v2 JSON，并由 iOS 导入；
- 都能对同一 PK fixture 生成误差在规范范围内的曲线；
- 运行自动化测试、手动无障碍检查和敏感数据泄露检查；
- 由维护者评审数据契约、隐私边界、技术栈和首批功能范围后，才进入完整迁移。

这个里程碑通过后，再按阶段 3–7 扩展，而不是先把全部页面翻译完再验证底层模型。
