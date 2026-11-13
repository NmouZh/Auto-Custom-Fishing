# Auto Custom Fishing — 开发者指南

Minecraft 26.2 / 1.21.11 / Fabric 客户端 mod：自动完成 Custom-Fishing 服务端插件的河钓小游戏。
本文件面向在本仓库工作的开发者与 AI agent。

## 常用命令

```bash
./gradlew build              # 构建默认版本（26.2，产物在 build/libs/）
./gradlew compileClientJava  # 只编译客户端源码，最快的语法检查
./gradlew test               # 纯 JVM 单元测试，不启动 Minecraft
./gradlew runClient          # 启动开发客户端（26.2 需要 JDK 25）

# 构建 1.21.11：覆盖版本属性（JDK 21）
./gradlew build -Pminecraft_version=1.21.11 -Pjava_version=21 \
  -Ploader_version=0.20.1 -Pfabric_version=0.141.6+1.21.11 \
  -Pcloth_config_version=21.11.153+fabric -Pmodmenu_version=17.0.1
```

GitHub Actions 用同一个矩阵并行构建两个版本，配置见 `.github/workflows/build.yml`。

## 架构

依赖方向只能自上而下：

```text
mixin/            只做事件转发（HudMixin、FishingHookMixin）
  └─ ModRuntime           装配层：配置、状态机、快捷键、tick
       ├─ detection/      协议解码 + 测速 + 画面快照（纯逻辑，可单测）
       ├─ fishing/        状态机 + 点击预测 + 日志（纯逻辑，可单测）
       │    └─ MinecraftFishingEnvironment   唯一的 Minecraft 动作胶水
       ├─ config/         配置数据 + 读写 + 曲线缓存（数据层无 MC 依赖）
       └─ ui/             Cloth Config 界面、曲线编辑器、快捷键、Mod Menu
```

共享代码在 `src/client/java`。与 Minecraft 版本相关的胶水代码在
`src/versions/<minecraft_version>/java`，构建时只加入当前版本的目录。目前有 7 个文件按版本分叉：

| 文件 | 26.2 | 1.21.11 |
| --- | --- | --- |
| `mixin/HudMixin` | `net.minecraft.client.gui.Hud` | `net.minecraft.client.gui.Gui` |
| `ModRuntime` | `client.gui.setScreen` / `client.gui.screen()` | `client.setScreen` / `client.screen` |
| `fishing/MinecraftFishingEnvironment` | `sendOverlayMessage` | `displayClientMessage(msg, true)` |
| `ui/KeyBindings` | `KeyMappingHelper` | `KeyBindingHelper` |
| `ui/CurveEditorScreen` | `GuiGraphicsExtractor.extractRenderState` | `GuiGraphics.render` |
| `ui/CurveEditorWidget` | `extractWidgetRenderState` | `renderWidget` |
| `ui/OpenCurveEditorEntry` | Cloth `extractRenderState` | Cloth `render` |

### 一帧 UI 的数据流

```text
Hud.setSubtitle (HudMixin)
  → ComponentScanner.scan        Component → List<TextPart>（文本 + 有效字体）
  → FishingUiDetector.onUi       解码 bar/指针位置，记录采样并测速
  → FishingController.onGameText 状态机判断 + ClickPredictor.decide
  → FishingEnvironment.reelIn    在同一个调用栈内点击（无 tick 排队延迟）
```

### 状态机

```text
IDLE ──cast──▶ CASTING ──hook──▶ WAITING_BITE ──bite──▶ WAITING_GAME ──UI──▶ PLAYING
  ▲                                  │                        │                 │
  │                                  │超时                     │500ms 无 UI      │点击
  │                                  ▼                        ▼                 ▼
  └──── RECAST_DELAY ◀───────────────┴────────────────────────┴────────────── REELING
```

- `WAITING_GAME`：收杆后最多等 500ms 小游戏 UI；等不到视为钓到普通物品。
- `PLAYING`：1 秒收不到新 UI 帧即认为小游戏结束（可能漏点），自动重抛。
- 兜底：抛竿后超过 `autoRecastMinutes` 强制收杆；鱼钩消失超过 60 秒强制重抛。

## 必须保持的不变量

1. **只在客户端线程**：所有状态机、解码、配置访问都在客户端线程执行，不加锁。
2. **点击在收包时执行**：点击决策发生在 `Hud.setSubtitle` 的调用栈里，不要改回“下一 tick 再点”。
3. **`BarTable` 是 bar 元数据的唯一来源**：格子数、每格宽度、成功率只能从这里取。
   配置界面的可视化也必须用它，避免运行时和显示各有一份表。
4. **核心逻辑不依赖 Minecraft 类型**：`detection/`、`fishing/FishingController`、
   `config/` 的数据类都不应 import `net.minecraft.*`。只有 `ComponentScanner`、
   `MinecraftFishingEnvironment`、`ModRuntime`、`ui/`、`mixin/` 允许接触 MC。
5. **版本差异只放在 `src/versions/<mc>/`**：共享代码不能出现只在某个版本存在的
   API（如 `GuiGraphicsExtractor`、`sendOverlayMessage`）。改版本胶水时两份都要同步。
6. **时间由调用方传入**：核心逻辑接收毫秒时间戳，不直接读系统时钟，方便测试。
7. **配置向后兼容**：`config/autofishing.json` 的字段名是用户接口。缺字段靠
   `AutoFishConfig` 的字段初始值补默认值；改字段名必须做迁移。
8. **新逻辑要带测试**：纯逻辑测试放在 `src/test/java`，用假环境或构造的 `TextPart` 驱动。

## 目录速查

| 路径 | 职责 |
| --- | --- |
| `src/client/java/cafe/yamds/AutoFishingClient.java` | 客户端入口，只做接线 |
| `src/client/java/cafe/yamds/detection/UiDecoder.java` | Custom-Fishing 协议解码（纯） |
| `src/client/java/cafe/yamds/detection/BarTable.java` | 9 种 bar 的参数与成功率表 |
| `src/client/java/cafe/yamds/detection/FishingUiDetector.java` | 最新帧、测速、活跃判定 |
| `src/client/java/cafe/yamds/fishing/FishingController.java` | 状态机（纯） |
| `src/client/java/cafe/yamds/fishing/ClickPredictor.java` | 提前量判定（纯） |
| `src/client/java/cafe/yamds/config/ConfigStore.java` | 配置加载/保存/曲线缓存 |
| `src/client/java/cafe/yamds/ui/` | 配置界面、曲线编辑器、Mod Menu（跨版本共享部分） |
| `src/versions/26.2/java/` | 26.2 专用胶水（HUD/渲染/快捷键） |
| `src/versions/1.21.11/java/` | 1.21.11 专用胶水 |
| `src/test/java/` | 单元测试 |

## 扩展指南

- **新增/修改 bar**：改 `BarTable` 的成功率表；默认提前量曲线在
  `AutoFishConfig.defaultCurveFor`。不要在两个地方各写一份成功率。
- **改 UI 协议解析**：只改 `UiDecoder`（纯函数），用 `TestUiFrames` 构造帧做测试；
  `ComponentScanner` 只负责把 `Component` 拆成 `TextPart`。
- **新增游戏类型（如海钓 tension）**：在 `detection/` 加一个解码器，在 `fishing/`
  加一个策略/状态机，由 `ModRuntime` 选择；不要把新逻辑塞进现有控制器。
- **调参**：游戏内 ActionBar 会实时显示位置/速度/成功率；配置界面的“钓鱼日志”
  记录最近 20 次点击（点击位置与预测目标）。

## 调试

- 日志级别：核心路径用 `debug`，状态切换和错误用 `info`/`error`，不要在每个 UI 帧刷 `info`。
- 配置路径：`config/autofishing.json`（相对游戏目录）。
- 常见问题：如果解码一直为空，先确认服务器仍在使用 `customfishing:default` /
  `customfishing:offset_chars` 字体，以及字幕是否仍是 4 段结构。
