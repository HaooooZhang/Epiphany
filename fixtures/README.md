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
