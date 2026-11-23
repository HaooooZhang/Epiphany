# R1 最小数据包

将 `r1-datapack` 放入开发服测试世界的 `datapacks/` 目录后启动。五个注册表各含一个 `epiphany_r1:sample`，数据包用于独立服与客户端登录检查。

- 服务端执行 `epiphany_r1 registries` 检查五类条目、默认值和分派。执行 `epiphany_r1 fallback` 检查未知条件类型是否回退到 `always`，此命令会生成一条预期的解码错误日志。
- 让两个开发客户端连接，在服务端分别执行 `epiphany_r1 seed <玩家> <阅历>`。用 `epiphany_r1 progress <玩家>` 查看服务端的完整记录。客户端可添加 JVM 参数 `-Depiphany.r1.log=true` 查看收到的注册表、进度和成长规则。
- 执行 `epiphany_r1 rules 17`，核对两个客户端收到上限 17。执行死亡重生、主世界和下界往返、末地传送门返回、退出重进及重启服务端，核对玩家的进度和规则。测试命令设置的规则仅在当前服务端运行期间生效。
- 修改 `path/sample.json` 的名称，在 `/reload` 和重启后分别执行 `epiphany_r1 registries` 并核对客户端登录日志。Forge 47.4.28 在本次实测中于重启时加载修改后的五类动态注册表数据。

`epiphany_r1` 命令由开发环境注册，需要权限等级 2。

## R2 核心玩法数据包

将 `r2-datapack` 放进新建测试世界的 `datapacks/` 后启动。测试条件、奖励和阅历来源使用 1.3.0 JSON 字段。以下命令在服务端控制台执行，以 `R2Alpha` 为测试玩家。

1. 使用旧线性成长规则，起始消耗 10、斜率 1、心得点上限 10。执行 `execute at R2Alpha run summon minecraft:item ~ ~ ~ {Item:{id:"minecraft:cobblestone",Count:1b},PickupDelay:0s}`，让玩家拾取。预期 3 心得点、9 阅历并自动解锁模块。
2. 执行 `attribute R2Alpha epiphany:aptitude_gain_multiplier base set 2`，再拾取一次圆石。预期 5 心得点、6 阅历。拾取泥土可检查排除规则。
3. 依次执行 `epiphany module try_select R2Alpha epiphany_r2:practice`、`epiphany insight try_select R2Alpha epiphany_r2:practice`、`epiphany epiphany try_select R2Alpha epiphany_r2:practice`。预期剩余 2 心得点，模块完成后有 1 个槽位，顿悟使用该槽位。生命上限 24、攻击力 4、永久速度，模块完成奖绿宝石 1 个。
4. 使用 `epiphany_r1 progress R2Alpha` 查看完整状态，重复步骤 3 检查幂等性。用 `attribute`、`data get entity R2Alpha ActiveEffects` 和 `clear R2Alpha minecraft:emerald 0` 分别查询属性、效果和物品。
5. 从测试世界的数据包里移除模块、心得、顿悟的 `practice.json`，运行 `reload` 并查询进度。随后重启并连续登录两次。预期首次登录退款 2 点、清空选择顺序、释放已用槽位，第二次保持相同进度。

## R3 界面测试数据包

将 `r2-datapack` 和 `r3-datapack` 一起放进测试世界的 `datapacks/`。前者提供拾取阅历和完整选择链，后者添加一棵含五个节点的树、第二条道路、可选与锁定顿悟以及物品图标。

- 在隔离服的 `config/epiphany-common.toml` 设置 `moduleSelectCost = 2`、`maxSelectedModules = 3`、`maxEpiphanySlots = 4`。两个玩家分别打开界面，核对格数和各自的点数。
- 用旧线性成长规则（起始成本 10、斜率 1、上限 10）测试。将玩家的原版经验设到一级（`experience set <玩家> 1 levels`），达到 R2 样本的模块解锁条件。让同一个玩家实际拾取两次圆石，保持阅历倍率为 1。界面应显示 4 心得点。
- 使用 K 打开主界面，依次选 **R2 Practice** 模块、树中的心得、顿悟槽及 **R2 Epiphany**。预期累计扣 4 点、顿悟占用 1 槽、得到 1 个绿宝石。用 `epiphany_r1 progress <玩家>` 查看服务器状态。
- 在有余额时选 **R3 Branching Practice**。查看两项根节点、两项下一层心得及最后一项心得的前置状态。让 **R3 Locked Epiphany** 与 **R3 Focus** 一起显示，可检查道路分组、锁定开关和图标回退。

## R4 可选集成测试样本

使用新建测试世界，将 `r4/datapack` 放到世界的 `datapacks/` 下。FTB Quests 组合把 `r4/ftbquests/quests/chapters/r4.snbt` 放到服务器 `config/ftbquests/quests/chapters/`。KubeJS 组合把 `r4/kubejs/server_scripts/r4_epiphany.js` 放到服务器 `kubejs/server_scripts/`。用 JDK 17 和 Forge 47.4.28，服务端和客户端装相同的可选模组。固定依赖见 [构建文件](../build.gradle)。

- **FTB Quests**。使用起始成本 10、斜率 1 的默认曲线。让新玩家获取木棍，等待 FTB Quests 检测库存，再用 `epiphany_r1 progress <玩家>` 查询。预期章节开始模块已解锁，阅历为 0，任务和章节完成模块仍锁定。再获取羽毛，预期任务、章节和标签计数条件满足，首轮总计发放 17 阅历，剩余 7 阅历、1 心得点。为便于复现，可从控制台用 `give <玩家> minecraft:stick` 和 `give <玩家> minecraft:feather`，由 FTB Quests 自己完成两项库存任务。
- **KubeJS**。用 `epiphany insight points add <玩家> 5` 准备余额，再用菜单或 `epiphany module try_select <玩家> epiphany_r4:scripted` 请求选择。初始脚本取消选择。把 `r4-a`、`r4Cancel = true` 分别改为 `r4-b`、`r4Cancel = false`，执行 `kubejs reload server_scripts` 后重新选择。用 `epiphany insight try_select <玩家> epiphany_r4:stage` 选择心得。此时脚本取消模块完成。再把 `r4-b` 改为 `r4-c` 并重载，用 `epiphany module reset <玩家> epiphany_r4:scripted` 重置后重做选择，应成功获得顿悟槽。
- **阶段奖励与绑定**。完成模块后，用 `epiphany epiphany try_select <玩家> epiphany_r4:stage` 移除 `r4_insight`，再用 `epiphany epiphany reset <玩家> epiphany_r4:stage` 恢复该阶段。以玩家身份执行 `kubejs custom_command r4_stage_check`，查询阶段；重置模块后再次查询，两项阶段均为 false。仅 KubeJS 的 `r4-c` 测试中可执行 `kubejs custom_command r4_binding_check`，让脚本增加 1 阅历并调用正常选择和强制选择。控制台可加 `execute as <玩家> run`。
- **无集成**。只装必需模组，保留测试数据包。FTB Quests 和阶段条件保持锁定。给玩家 3 心得点，正常选择样本的模块、心得和顿悟，检查点数与槽位。
