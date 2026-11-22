# R1 最小数据包

将 `r1-datapack` 放入开发服测试世界的 `datapacks/` 目录后启动。五个注册表各含一个 `epiphany_r1:sample`，数据包用于独立服与客户端登录检查。

- 服务端执行 `epiphany_r1 registries` 检查五类条目、默认值和分派。执行 `epiphany_r1 fallback` 检查未知条件类型是否回退到 `always`，此命令会生成一条预期的解码错误日志。
- 让两个开发客户端连接，在服务端分别执行 `epiphany_r1 seed <玩家> <阅历>`。用 `epiphany_r1 progress <玩家>` 查看服务端的完整记录。客户端可添加 JVM 参数 `-Depiphany.r1.log=true` 查看收到的注册表、进度和成长规则。
- 执行 `epiphany_r1 rules 17`，核对两个客户端收到上限 17。执行死亡重生、主世界和下界往返、末地传送门返回、退出重进及重启服务端，核对玩家的进度和规则。测试命令设置的规则仅在当前服务端运行期间生效。
- 修改 `path/sample.json` 的名称，在 `/reload` 和重启后分别执行 `epiphany_r1 registries` 并核对客户端登录日志。Forge 47.4.28 在本次实测中于重启时加载修改后的五类动态注册表数据。

`epiphany_r1` 命令由开发环境注册，需要权限等级 2。
