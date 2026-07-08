# AGENTS.md — Epiphany (顿悟)

NeoForge 1.21.1 library mod providing a modular skill-tree system (~120 Java files). Data-driven, API-first. Design docs: [Docs/README.md](Docs/README.md) | dev plan: [Docs/todo.md](Docs/todo.md).

## Build & Run

```bash
./gradlew build          # Full build
./gradlew runClient      # Launch client
./gradlew runServer      # Launch server
./gradlew runData        # Data generators (currently unused — datapack JSONs are hand-authored)
```

- **Java 21**, **Gradle 9.2.1** with NeoGradle (`net.neoforged.moddev` 2.0.141), **Parchment** (2024.11.17)
- modid: `epiphany` | package: `ink.myumoon.epiphany` | NeoForge: `21.1.234`
- No mixins. No access transformers. Configuration cache enabled.

## Package Map

```
ink.myumoon.epiphany/
  Epiphany.java              @Mod 主类 — 注册 registry, attachment, command, listeners, UI factory
  EpiphanyClient.java        @Mod(dist=CLIENT) — K 键绑定 + Config screen factory
  Config.java                ModConfigSpec (8+ options)
  EpiphanyConstants.java     "type" / "always" / "no_op" 字符串常量

  api/                       公共静态 API → 其他模组调用入口
    AptitudeManager.java     get/set/add aptitude, insightPoints
    AptitudeFormula.java     calcRequiredAptitude(totalSpent, points)
    AptitudeSourceManager.java  grant(sp, behaviorId, targetId, registry) — 第三方调用入口
    AptitudeSourceResolver.java  resolve(...) → Resolution 纯函数
    ModuleManager.java       isUnlocked/isSelected/isCompleted, setUnlocked, select, deselect, complete, reset, cleanupOrphanedData
    InsightManager.java      isSelected/isModuleSelected, select
    EpiphanyManager.java     isUnlocked/isSelected, setUnlocked, select, deselect, forceSelect, cleanupOrphanedData

  registry/                  所有注册表定义 (均使用 DeferredRegister)
    EpiphanyRegistries.java  ResourceKey + Registry 对象 (3 builtin + 4 datapack + 1 aptitude)
    EpiphanyAttachmentTypes.java  单个 AttachmentType<PlayerEpiphanyData>
    EpiphanyConditionTypes.java   20+ condition MapCodec 注册
    EpiphanyInsightRewardTypes.java  14 insight reward MapCodec 注册
    EpiphanyEpiphanyRewardTypes.java 14 epiphany reward MapCodec 注册

  attachment/                玩家持久化数据
    PlayerEpiphanyData.java  record: aptitude, insightPoints, totalSpent, per-module/insight/epiphany Map, slots, claimedFirsts
    ModulePlayerState.java   record: unlocked, selected, completed, unlockedInsights (Set)
    InsightPlayerState.java  record: selected, moduleSelected
    EpiphanyPlayerState.java record: selected, unlocked

  content/                   数据记录 + 工具类 (Codec-based, 由 datapack JSON 驱动)
    ModuleData.java          模块定义: name, description, icon, condition, initialState, insights[], on_select/complete_reward, weight
    InsightData.java         心得定义: name, description, icon, cost, reward, weight
    EpiphanyData.java        顿悟定义: name, description, icon, path, condition, initialState, reward, weight
    PathData.java            道路定义: name, description, icon, weight
    InsightEntry.java        record: id + depth (模块内部对心得的引用)
    InsightTreeResolver.java 树的祖先查找 / canUnlock 判断
    InitialState.java        enum: LOCKED / SELECTABLE
    AptitudeSourceConfig.java 行为→阅历映射 JSON 结构

    condition/               条件系统 (~30 files)
      Condition.java         接口: codec() + test(ServerPlayer) + isEventDriven()
      Comparison.java        枚举: EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL
      logic/                 Always, Never, And, Or, Not
      builtin/               Advancement, Aptitude, Attribute, Biome, BlockBroken, Dimension, Effect,
                             EpiphanySelected, ExperienceLevel, InsightPoints, InsightSelected, Item, ItemUsed,
                             KillEntity, ModuleCompleted, ModuleSelected, Statistic, Structure
      builtin/ftbq/          FTBQQuest, FTBQChapterStarted, FTBQChapterCompleted, FTBQTag + FTBQHelper/FTBQInternal
      builtin/kubejs/        KubeJSStage + KubeJSHelper/KubeJSInternal

    reward/                  奖励系统 (~18 files, InsightReward 和 EpiphanyReward 共享实现类)
      InsightReward.java     接口: codec() + apply(sp, sourceId) + remove(sp, sourceId)
      EpiphanyReward.java    同上, 用于顿悟
      PersistentReward.java  标记接口 + reapplyAll(sp) 静态方法 — 死亡/维度穿越后重新生效
      实现类: Attribute, Item, Command, Experience, ExperienceLevel, Effect, Particle,
              UnlockModule, UnlockEpiphany, LockModule, LockEpiphany, Aptitude, InsightPoints, KubeJSStage,
              NoOpInsight, NoOpEpiphany

  event/                     16 个事件类
    基类: EpiphanyEvent(ServerPlayer)
    Pre (可取消): ModuleUnlock, ModuleSelect, ModuleComplete, InsightSelect, EpiphanyUnlock, EpiphanySelect
    Post (通知): ModuleUnlocked, ModuleSelected, ModuleCompleted, InsightSelected, EpiphanyUnlocked, EpiphanySelected
    独立: AptitudeChanged, AptitudeLevelUp, InsightPointsChanged

  listener/                  4 个 @EventBusSubscriber 监听器
    AutoUnlockListener.java  每 10 tick 轮询 + 事件触发 = 自动解锁 locked+condition 的 Module/Epiphany
    NotificationListener.java  聊天消息 + 音效通知 (临时方案, 计划改为 Toast)
    AptitudeGainListener.java  9 种内置行为监听 (kill_entity, mine_block, advancement_earn 等)
    AptitudeStateListener.java  enter_biome + enter_structure (需持久状态追踪)

  client/                    客户端代码 (@OnlyIn 或 @Mod(dist=CLIENT))
    EpiphanyDebugHud.java    调试 HUD 叠加层
    EpiphanyIcons.java       图标资源常量
    ui/
      EpiphanyUIFactory.java    PlayerUIMenuType 注册 + openFor/requestOpenFromClient
      TopBarController.java     顶部状态栏 (阅历进度条 + 心得点)
      ClientData.java           客户端侧 player data 快照 POJO
      ItemIconElement.java      物品图标渲染 Widget
      module/
        ModuleGridController.java   模块网格 (响应式, 自动刷新, 悬停 tooltip, 点击 popup)
        ModuleSelectController.java 模块详情 popup + 选择/取消按钮
      insight/
        InsightTreeView.java        心得树渲染 Widget
        InsightNodeElement.java     单个心得节点
        InsightLineElement.java     节点间连线
        InsightClickHandler.java    心得选择 + 预览逻辑
        InsightIcons.java           心得节点图标资源
      epiphany/
        EpiphanySlotColumnController.java  顿悟槽位列
        EpiphanySelectController.java      顿悟选择界面
      overlay/Overlay.java        通用叠加层组件

  command/
    EpiphanyCommand.java    /epiphany 根命令 (admin-only), 子命令: aptitude, insight, module, epiphany, path, reset, open

  util/
    DefaultedCodec.java     解析失败时回退到默认值的 Codec 包装器 (来自 Origins 模式)
```

## Architecture Deep-Dive

### Five datapack registries (JSON-driven, Codec-based)

| Registry | Key | Purpose |
|----------|-----|---------|
| `module` | `EpiphanyRegistries.MODULE_REGISTRY_KEY` | 独立技能树单元, 含 Insights 列表 |
| `insight` | `EpiphanyRegistries.INSIGHT_REGISTRY_KEY` | 模块内增量升级节点 |
| `epiphany` | `EpiphanyRegistries.EPIPHANY_REGISTRY_KEY` | 完成模块后的质变能力奖励 |
| `path` | `EpiphanyRegistries.PATH_REGISTRY_KEY` | 顿悟的 UI 分组标签 |
| `aptitude` | `EpiphanyRegistries.APTITUDE_SOURCE_REGISTRY_KEY` | 行为→阅历值映射配置 |

前四个通过 `@EventBusSubscriber` 在 `EpiphanyRegistries.newDataPackRegistries()` 中注册。`aptitude` 额外注册。

### Three builtin registries (MapCodec dispatch)

| Registry | Key | Purpose |
|----------|-----|---------|
| `condition_type` | `CONDITION_SERIALIZERS_KEY` | 条件类型 → MapCodec |
| `insight_reward_type` | `INSIGHT_REWARD_SERIALIZERS_KEY` | 心得奖励类型 → MapCodec |
| `epiphany_reward_type` | `EPIPHANY_REWARD_SERIALIZERS_KEY` | 顿悟奖励类型 → MapCodec |

自定义 Registry（非 Vanilla/NeoForge），使用 `DefaultedMappedRegistry` + `NewRegistryEvent` + `DeferredRegister<MapCodec<?>>`。

### Dispatch Codec 模式

所有 Condition / InsightReward / EpiphanyReward 使用 `DefaultedCodec.registryDispatch()`:
```java
Codec<Condition> CODEC = DefaultedCodec.registryDispatch(
    CONDITION_SERIALIZERS, Condition::codec, identity(), () -> AlwaysCondition.INSTANCE
);
```
JSON 中的 `"type"` 字段 → 查注册表 → 对应的 MapCodec → 反序列化为具体类型。解析失败回退到默认值（Always / NoOp），不崩溃。

### Player data flow

```
ServerPlayer.setData(EPIPHANY_DATA, newData)
  ├─ .serialize(CODEC) → NBT 持久化
  ├─ .sync(STREAM_CODEC) → 自动发包到客户端
  └─ .copyOnDeath() → 死亡保留
```

`PlayerEpiphanyData` 是 immutable record，所有修改通过 `with*()` 方法返回新实例。客户端通过 `ClientData` POJO 读取。

### Event conventions

- **Pre** 事件 (`ModuleUnlockEvent`, `ModuleSelectEvent`, `ModuleCompleteEvent`, `InsightSelectEvent`, `EpiphanyUnlockEvent`, `EpiphanySelectEvent`) → extends `EpiphanyEvent`, 可取消
- **Post/无前缀** 事件 (`ModuleUnlockedEvent`, `ModuleSelectedEvent`, `ModuleCompletedEvent`, `InsightSelectedEvent`, `EpiphanyUnlockedEvent`, `EpiphanySelectedEvent`) → 通知, 不可取消
- 特殊事件: `AptitudeChangedEvent`, `AptitudeLevelUpEvent`, `InsightPointsChangedEvent`
- 全部 fire on `NeoForge.EVENT_BUS`

### Manager API pattern

所有 Manager 方法签名: `public static ReturnType methodName(ServerPlayer player, ResourceLocation id, ...)` 
- 内部通过 `player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)` 获取数据
- 通过 `player.server.registryAccess().registryOrThrow(...)` 查找 datapack 注册表
- 修改后调用 `player.setData(...)` 持久化

### Network (no dedicated network package)

不使用自定义 packet。客户端→服务端通信通过 LDLib2 的 RPC 机制 (`RPCPacketDistributor.rpcToServer`)。服务端→客户端同步通过 Attachment 的 `STREAM_CODEC` 自动处理。

## Key Design Decisions

| 决策 | 详情 |
|------|------|
| Insight tree via depth | Module JSON 中 `"insights": [{"id":"...", "depth":N}]`, 同 depth = AND 关系, 父节点 = 前方最近的 depth-1 |
| Module select cost | 统一, `Config.MODULE_SELECT_COST` (默认 1). 非每模块独立 |
| Epiphany→Path | 单向: Epiphany.`path` → Path. Path 无 epiphany 列表 |
| `initial_state`: `locked` vs `selectable` | `locked`+condition→满足时自动解锁; `locked` 无条件→仅 API/命令; `selectable`→立即可选 |
| Module 选择上限 | `Config.MAX_SELECTED_MODULES` (默认 8), 非无限制 |
| 顿悟槽上限 | `Config.MAX_EPIPHANY_SLOTS` (默认 8), 完成模块 +1 slot |
| ModuleComplete cancel | Pre 事件取消时无奖励/无 slot/无 Post 事件, 模块留待重新触发 |
| 阅历公式 | `cap = baseAptitudeCap + totalEarned × aptitudeCapGrowth` |
| `AptitudeGainMultiplier` | Global multiplier applied to *all* datapack aptitude rewards before granting |
| 重置 | `/epiphany reset all` (全清), `/epiphany reset select` (保留 aptitude/points). 无单点撤销 |
| Registry entry removal | 自动退还已消耗资源 (points/slots) |
| 通知 | 当前用 chat message + sound (临时), 计划改为 Toast. 可配置开关 |
| 持久奖励 | `PersistentReward` 接口标记 → `PlayerRespawnEvent` 时 `reapplyAll()` |

## Dependencies

- **LDLib2** `com.lowdragmc.ldlib2:ldlib2-neoforge-1.21.1:2.2.26:all` — UI 框架 (XML-driven), sync, RPC. Docs: `Docs/ldlib2/`
- **FTB Quests** `curse.maven:ftb-quests-forge-289412:8253474` — soft dependency, Architectury event listeners
- **KubeJS** `dev.latvian.mods:kubejs-neoforge:2101.7.2-build.368` — soft dependency
- **Architectury** `compileOnly files("run/mods/architectury-13.0.8-neoforge.jar")` — 仅编译, FTBQ 事件桥接

## Dev Notes

- UI XML 位于 `src/main/resources/assets/epiphany/ui/main.xml`, 由 `EpiphanyUIFactory` 加载
- Datapack 示例 JSON 位于 `src/main/resources/data/epiphany/epiphany/` (module/insight/epiphany/path/aptitude 子目录)
- 语言文件: `assets/epiphany/lang/zh_cn.json` + `en_us.json`
- 配置生成到 `run/config/epiphany-common.toml`
- Module reward descriptions (`on_select_reward_description`, `on_complete_reward_description`) 是可读文本，非自动生成 — UI tooltip 中直接展示
- All InsightReward/EpiphanyReward 实现类被两种类型**共享** (同一类实现两个接口), 因此修改 reward 需检查两侧注册
- FTBQ / KubeJS 的 Helper/Internal 类使用 try-catch ClassNotFoundException 做软依赖隔离
