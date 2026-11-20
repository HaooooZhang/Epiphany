# Epiphany 1.20.1 Forge 项目规范

目标为 Minecraft 1.20.1 Forge、Java 17 和现有单一 Gradle 项目。保留 `epiphany` 模组 ID 与包根 `ink.myumoon.epiphany`。开发依赖以 `build.gradle`、`gradle.properties` 和 Wrapper 为准。实施顺序、当前阶段和退出条件见 [移植计划表](Docs/forge-port-plan.md)。

## 行为基线

- 移植行为以 NeoForge 1.3.0 源码提交 `1cbffb3` 为准。迁移前对照该提交和目标版本依赖的实际签名。遇到平台差异时，记录受影响的数据、API 或玩家操作及可复现证据。涉及对外行为差异时，将决策落到移植方案。 [平台差异与资料](Docs/forge-port.md)
- 保留模块、心得、顿悟、道路与阅历来源的数据包 ID、路径、字段与默认值。公共 Manager 的方法语义、命令权限、可取消 Pre 事件和对应 Post 事件沿用基线。条件与两类奖励的默认分派同样按基线核对。
- 进度、条件与奖励由服务端裁定。模块/心得/顿悟选择、消耗与退款由 Manager 统一处理。客户端从权威快照刷新，命令、界面和脚本统一调用相应的服务端 API。

## 代码职责

沿用上游领域包。以下职责按阶段接入，目录随实际实现加入。

| 位置 | 职责与依赖 |
| --- | --- |
| 模组入口、`registry`、`config` | 入口注册总线、配置和网络，注册表负责类型与数据包定义。服务端配置负责生效及保存成长规则，并在生效时触发同步 |
| `content`、`content.condition`、`content.reward` | 定义内容 Codec、条件与奖励。扩展奖励时检查心得和顿悟两套类型注册 |
| `api`、`command`、`event`、`listener` | Manager 持有玩法规则与写入入口。命令和事件监听转发玩家意图或游戏行为，公开事件按基线顺序发出 |
| 玩家状态、Capability、网络 | 复用 `PlayerEpiphanyData` 及其状态记录。Forge Capability 管理持久化与 Clone 交接，`SimpleChannel` 传输玩家进度和规则快照。包处理在对应游戏线程应用快照 |
| `client` 与 LDLib2 菜单入口 | 客户端负责按键、图形、配置界面和当前连接的快照。菜单共享入口保持可由独立服加载，屏幕、渲染类和 XML 初始化由客户端加载 |
| FTB Quests 与 KubeJS 适配 | 第三方类集中在现有集成边界，依赖存在时才加载。集成通过 Manager、公开事件或绑定转接，保持最小依赖配置可运行 |

## 资源与验证

- 在 `src/main/resources/assets/epiphany/` 放置 LDLib2 XML、纹理与双语文本。数据包定义按 `data/<namespace>/epiphany/<registry>/` 组织，示例使用 1.20.1 可识别的注册 ID 与资源格式。手写资源与 `src/generated/resources/` 分开维护。
- 代码改动使用 JDK 17 执行 `gradlew build`。迁移纯逻辑时复用基线的成长曲线、阅历和配置测试。改动玩家存档和网络时，按计划验证登录、死亡、末地返回、切维、重连、重启及配置重载。改动客户端与集成时，验证相关的安装组合和独立服务端。
- 将实际命令、日志或玩家观察记录到计划表。构建结果用于证明编译和打包，运行结果用于验收相应的游戏场景。使用 [计划表的验收记录](Docs/forge-port-plan.md#验收记录) 追踪进度。
