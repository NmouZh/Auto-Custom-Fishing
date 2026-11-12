# Auto Custom Fishing for CustomFishing

> **English**: [README_EN.md](README_EN.md) | **中文**: 本文件

## 说明

本项目代码来自 **100% AI 生成**。

这意味着：

- 代码能用，但实现未必优雅
- 工程结构和细节处理可能比较混乱
- 这个仓库更像是一个“可运行的思路验证 + 可继续改进的基础”

如果你是来读代码的，建议把它当成一个思路参考，不要预期它已经被人工精修过。

## 当前缺陷

历史版本最大的问题是：

**不能做到在收到正确 Title/Subtitle 信息的那一刻就立刻、稳定地完成收杆。**

原因是旧实现把“识别到正确时机”和“执行点击”拆到了不同的 tick：解码发生在收包时，
真正点击却要等下一个客户端 tick（最多 50ms），这段时间在快速 bar 上足以让指针离开成功区。

当前版本在架构上修正了这一点：

- 点击决策与执行都在 `Hud.setSubtitle` 的同一次调用栈内完成，不再经过 tick 排队
- 速度预测与提前量曲线仍然保留，作为高难度 bar 的补偿手段
- 实际进服效果仍需在游戏中验证；如果仍有漏鱼，优先调曲线，而不是加延迟

仓库里还保留 **提前量曲线编辑工具**，用于按不同 bar、不同速度调节提前点击量。
默认参数已经能钓到大部分鱼，但不是百分之百。欢迎提 Issue / PR 反馈实机表现。

## 项目简介

这是一个为 [Custom-Fishing](https://github.com/Xiao-MoMi/Custom-Fishing) 服务端插件编写的客户端自动钓鱼 Mod，运行在 `Minecraft 26.2 + Fabric`。

项目目标很直接：针对 Custom-Fishing 的“仿星露谷”钓鱼小游戏，把重复的抛竿、等待、识别小游戏、判断时机、收杆这一整套流程自动化。

当前版本只实现了河钓模式的自动化，不支持海钓模式。

## 这项目是做什么的

Custom-Fishing 的河钓并不是原版 Minecraft 那种“浮漂下沉就收杆”，而是在鱼咬钩后进入一个带指针和成功区间的小游戏。这个 Mod 做的事情是：

- 自动抛竿
- 监听鱼咬钩并自动收杆进入小游戏
- 解析服务器发来的 Title / Subtitle UI 数据
- 反推出当前指针所在位置和移动速度
- 在最合适的时机自动点击完成小游戏
- 完成后自动重新抛竿，进入下一轮

它本质上是一个“针对 Custom-Fishing 河钓 UI 协议做定制解析”的客户端自动化工具。

## 当前支持范围

- 支持：河钓 `accurate_click` 类小游戏
- 不支持：海钓 / tension 类玩法
- 不支持：通用服务器自动钓鱼
- 依赖：客户端必须能正常接收到 Custom-Fishing 发出的标题组件数据

## 核心逻辑

这个项目的核心不是图像识别，也不是读屏，而是直接分析客户端收到的界面组件数据。

整体流程如下：

1. `FishingHookMixin` 监听本地玩家鱼钩实体的 `biting` 状态，判断是否咬钩，并立即触发收杆。
2. `HudMixin` 拦截 `Hud#setTitle`、`setSubtitle`、`setOverlayMessage`，把组件交给 `ComponentScanner`。
3. `ComponentScanner` 把组件拆成“文本 + 有效字体”的片段，`UiDecoder` 从中解析 Custom-Fishing 的进度条字符和偏移字符。
4. 通过偏移量反推出指针进度，再映射成当前 bar 上的逻辑格子位置。
5. `PointerTracker` 记录最近一段时间的位置变化，计算平均速度。
6. `ClickPredictor` 根据当前 bar 的成功区、速度、配置里的“提前量曲线”判断点击时机；
   点击在收包的同一次调用栈内执行，不经过 tick 排队。
7. `FishingController` 用状态机驱动整套流程：抛竿、等咬钩、进入小游戏、点击、收杆、重抛。

简单说就是：

`咬钩检测 + 标题解析 + 位置测速 + 提前量预测 + 状态机控制`

## 主要功能

- 河钓全流程自动化
- 自动重抛
- 超时自动收杆并重新抛竿
- 基于不同 bar 类型分别配置提前量
- 支持全局提前量微调
- 内置曲线编辑器，可按速度调提前量
- 配置界面集成 Cloth Config / Mod Menu
- 记录最近 20 次点击日志，便于调参

## 使用方式

### 运行环境

- Minecraft: `26.2`
- Java: `25`
- Fabric Loader: `0.19.3+`
- Fabric API: `0.157.0+26.2`
- Cloth Config / Mod Menu（可选）

### 安装

1. 编译项目，或使用已构建好的 jar。
2. 将 Mod 放入客户端 `mods` 目录。
3. 安装并进入启用了 Custom-Fishing 的服务器。
4. 手持钓鱼竿。
5. 按快捷键启用自动钓鱼。

### 默认快捷键

- `R`：开关自动钓鱼
- `O`：打开配置界面

如果安装了 Mod Menu，也可以直接从 Mod Menu 打开配置页。

## 配置说明

配置文件位于：

`config/autofishing.json`

当前主要配置项：

- `modEnabled`：总开关
- `globalAdvance`：全局提前量微调，作用于所有 bar
- `autoRecastMinutes`：抛竿后超时自动收杆的时间，单位分钟，设为 `0` 可关闭
- `castIntervalSeconds`：收杆后到重新抛竿的基础等待时间，单位秒
- `castIntervalRandomSeconds`：抛竿间隔随机浮动范围，单位秒；每次实际间隔在“基础值 ± 范围”内随机，模拟真人操作，设为 `0` 关闭随机
- `barCurves`：每个 bar 的提前量曲线，按“速度(ms/section) -> 提前量”配置

默认思路是：

- 低难度 bar 提前量较小
- 中高难度 bar 提前量更积极
- 速度越快，通常越需要更早点击

## 项目结构

```text
src/main/java/cafe/yamds/
  AutoFishingMod.java                Mod 公共入口

src/client/java/cafe/yamds/
  AutoFishingClient.java             客户端入口（只做接线）
  ModRuntime.java                    运行时装配、事件转发
  detection/
    ComponentScanner.java            Component → 文本片段
    UiDecoder.java                   Custom-Fishing 协议解码（纯逻辑）
    BarTable.java                    bar 参数与成功率表（唯一来源）
    PointerTracker.java              指针测速
    FishingUiDetector.java           最新画面与活跃判定
  fishing/
    FishingController.java           状态机（纯逻辑）
    ClickPredictor.java              点击时机预测（纯逻辑）
    MinecraftFishingEnvironment.java Minecraft 动作胶水
    FishingLog.java                  点击日志
  config/
    AutoFishConfig.java              配置数据与校验
    ConfigStore.java                 读写与曲线缓存
    AdvanceCurve.java / CurvePoint.java
  ui/
    AutoFishConfigScreen.java        Cloth Config 界面
    CurveEditorScreen.java           曲线编辑器
    BarVisualizer.java               bar 可视化
    KeyBindings.java / ModMenuIntegration.java
  mixin/
    FishingHookMixin.java            咬钩检测
    HudMixin.java                    Title / Subtitle / ActionBar 拦截

src/test/java/                       纯 JVM 单元测试（不启动 Minecraft）
```

## 开发与测试

```bash
./gradlew build      # 构建
./gradlew test       # 运行单元测试
```

架构约定、状态机图和扩展指南见 [AGENTS.md](AGENTS.md)。

## 适合谁看

如果你打开这个仓库是想快速理解代码，最值得先看的几个类是：

- `FishingController`（状态机）
- `FishingUiDetector` / `UiDecoder`（协议解析与测速）
- `ClickPredictor`（点击时机）
- `BarTable`（bar 数据）
- `HudMixin` / `FishingHookMixin`（事件入口）

这几个文件基本就把核心行为串起来了。

## 构建

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

构建产物默认在：

`build/libs/`

## 已知限制

- 当前只做了河钓自动化
- 逻辑强依赖 Custom-Fishing 当前的标题组件结构和自定义字体偏移规则
- 点击已改为收包时立即执行，但网络与服务端处理仍会带来固有延迟；高难度 bar 依旧依赖速度预测 + 提前量补偿
- 如果服务端插件后续调整 UI 编码方式，这个 Mod 可能需要同步更新
- 这是一个高度定制项目，不保证对其他钓鱼插件兼容

## License

仓库当前沿用项目内已有的 `LICENSE` 文件。
