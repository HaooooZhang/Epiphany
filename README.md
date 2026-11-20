# Epiphany · 1.20.1 Forge

顿悟（Epiphany）为整合包提供数据驱动的技能树，包括模块、心得、顿悟、道路和阅历来源。本分支正在将 NeoForge 1.3.0 移植到 Minecraft 1.20.1 Forge。当前可构建内容是 Forge 模组入口，技能树、命令和界面将在后续阶段接入。

## 开发

使用 JDK 17。若终端默认使用其他 Java 版本，先将 `JAVA_HOME` 指向 JDK 17。开发入口见 [移植计划表](Docs/forge-port-plan.md)、[项目规范](AGENTS.md) 和 [平台调研](Docs/forge-port.md)。

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer -PwithoutIntegrations
```

Linux 和 macOS 使用 `bash ./gradlew`。客户端目录为 `run/`，独立服务端目录为 `run-server/`。默认开发运行环境包含 FTB Quests 与 KubeJS，传入 `-PwithoutIntegrations` 可仅加载必需依赖。

[NeoForge 源码基线](https://github.com/HaooooZhang/Epiphany/tree/1cbffb31ef4474fec73d66f1a888b4db27c4a71d) · [MIT License](LICENSE)
