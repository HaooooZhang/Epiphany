# R5 统合回归数据包

在 Minecraft 1.20.1、Forge 47.4.28 和 Java 17 中，把 [datapack](test/datapack) 整个目录复制到测试世界的 `datapacks/`，然后启动世界。默认回归组合使用 Epiphany、LDLib2 和 Kotlin for Forge。原有 R1–R4 样本的注册 ID 保留，已有样本测试世界可把旧包替换为这一个包。四组旧样本的版本可在提交 `bb3f81a` 查阅。

| 样本 | 核对内容 |
| --- | --- |
| R1 Module!、R1 Insight、R1 Epiphany、R1 Path | 五类注册表的样本、格式化文本、默认值、`always` 和 `no_op` |
| R2 Practice | 拾取阅历、成长曲线、模块与心得选择、顿悟槽、永久效果、属性、一次性奖励及退款 |
| R3 Branching Practice、R3 Focus、R3 Locked Epiphany | 分支树前置、道路分组、物品图标、顿悟锁定状态 |
| R4 Scripted 和其余 R4 锁定模块 | 可选集成缺省行为、FTB Quests 队伍进度和 KubeJS 阶段、事件及脚本重载 |

## 读数与初始化

测试从没有 Epiphany 进度的玩家开始。以下命令在服务端控制台输入，单机可在启用作弊后加 `/` 输入。用实际玩家名替换 `R5Alpha`。新世界默认线性曲线起始成本为 10、斜率为 1、总心得点上限为 -1，通用配置默认模块成本为 1。

```mcfunction
datapack list enabled
epiphany path list
epiphany aptitude query R5Alpha
epiphany module query R5Alpha epiphany_r2:practice
epiphany insight query R5Alpha epiphany_r2:practice
epiphany epiphany query R5Alpha epiphany_r2:practice
data get entity R5Alpha ForgeCaps."epiphany:epiphany_data"
```

按 K 打开界面，观察当前玩家的阅历、点数、已选模块顺序和顿悟槽。用两个玩家各自的界面核对同步。原版 `data get entity` 可读出 Capability 的 `claimedFirsts`、`displayData`、`epiphanySlots` 和 `usedEpiphanySlots`，按需将字段接到示例路径末尾。

## 核心链路与界面

1. 执行 `experience set R5Alpha 1 levels` 达到 R2 解锁条件。在玩家脚边生成圆石并实际拾取。首轮 42 阅历结算为 3 心得点和 9 余量，第二次拾取后为 4 心得点和 8 余量。用 `attribute R5Alpha epiphany:aptitude_gain_multiplier base set 2` 可另测倍率。拾取泥土可检查排除规则。
2. 通过菜单依次选 **R2 Practice**、**R2 Insight** 和 **R2 Epiphany**。默认累计扣 3 点，剩 1 点，得到并占用 1 个顿悟槽。也可依次运行下方的正式尝试命令。重复相同请求，核对余额与一次性奖励。
3. 用 `attribute R5Alpha minecraft:generic.max_health get`、`attribute R5Alpha minecraft:generic.attack_damage get`、`data get entity R5Alpha ActiveEffects` 和 `clear R5Alpha minecraft:emerald 0` 检查生命上限 24、攻击力 4、永久速度和 1 个绿宝石。死亡、往返维度、末地出口返回、重连及重启后检查选择和持久奖励。
4. 另用有余额的玩家选择 **R3 Branching Practice**。两项根节点起初可学，后续两项分支与末节点受前置限制。观察 **R3 Focus** 与 **R3 Locked Epiphany** 在第二条道路的显示。可在测试服的 `config/epiphany-common.toml` 设置模块成本 2、模块上限 3 和顿悟槽上限 4，再让两个客户端查看各自余额与相同上限。
5. 在隔离世界修改 `serverconfig/epiphany-progression-server.toml`，对比两端的成长规则和各自进度。测试退款时，在已取得 R2 奖励的世界副本中，从包内移除 R2 的 `module`、`insight` 和 `epiphany` 下的 `practice.json`，分别记录 `/reload` 与重启后的结果。默认成本下首次登录退 3 点，清理选择、已用槽位和对应持久奖励。再次登录后余额应与首次相同。

```mcfunction
execute at R5Alpha run summon minecraft:item ~ ~ ~ {Item:{id:"minecraft:cobblestone",Count:1b},PickupDelay:0s}
epiphany module try_select R5Alpha epiphany_r2:practice
epiphany insight try_select R5Alpha epiphany_r2:practice
epiphany epiphany try_select R5Alpha epiphany_r2:practice
```

## 可选集成

服务端与参与测试的客户端使用同一组可选模组。 [固定依赖版本](../build.gradle)

- **无可选模组**。查询 `epiphany_r4:chapter_started`、`quest_complete`、`chapter_complete`、`tag_count` 和 `stage_gate`，这些模块应保持锁定。给玩家 3 心得点后，仍可正常选择 `epiphany_r4:scripted`，再选择心得和顿悟 `epiphany_r4:stage`。
- **FTB Quests**。把 [r4.snbt](test/ftbquests/quests/chapters/r4.snbt) 复制到实例的 `config/ftbquests/quests/chapters/`。新玩家先拿木棍再拿羽毛。等待每次库存检测，分别用 `epiphany module query R5Alpha epiphany_r4:chapter_started` 和 `epiphany aptitude query R5Alpha` 检查。木棍使章节开始条件通过，阅历仍为 0。羽毛使任务、章节完成和标签条件通过，首轮总共结算 17 阅历，剩 7 阅历和 1 心得点。在同队、异队与离线重连时，用同一批查询分别核对两名玩家。
- **KubeJS**。把 [r4_epiphany.js](test/kubejs/server_scripts/r4_epiphany.js) 复制到实例的 `kubejs/server_scripts/`。给新玩家 5 点，用菜单或 `epiphany module try_select R5Alpha epiphany_r4:scripted` 请求选择。初始脚本取消请求。将脚本顶部的 `const r4Generation = 'r4-a'` 改为 `const r4Generation = 'r4-b'`，将 `r4Cancel` 改为 `false`，执行 `kubejs reload server_scripts` 后重试。执行 `epiphany insight try_select R5Alpha epiphany_r4:stage` 后，心得保留，模块完成被取消。将顶部的 `r4Generation` 改为 `'r4-c'` 再重载，保留完成监听中对 `'r4-b'` 的判断，用 `epiphany module reset R5Alpha epiphany_r4:scripted` 重置，重做时模块完成并获得 1 槽。每次尝试只应发出当轮脚本的对应回调。
- **阶段与两者同装**。选择 `epiphany_r4:stage` 顿悟会移除 `r4_insight`，重置该顿悟后阶段恢复。以玩家身份执行 `kubejs custom_command r4_stage_check` 查询阶段。重置模块后重复查询，两项均为 false。两者同装时，开始阶段使用菜单测试取消和放行，随后关闭菜单完成 FTB Quests 任务，日志应收到任务和章节两次阅历回调。
