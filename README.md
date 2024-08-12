# 代码沙箱 —— Java实现

**代码沙箱的定位**：只负责接受代码和输入，返回编译运行的结果（可以作为独立的项目 / 服务，提供给其他的需要执行代码的项目去使用）

由于代码沙箱是能够通过 API 调用的**独立服务**，所以新建一个 Spring Boot Web 项目。最终这个项目要提供一个能够执行代码、操作代码沙箱的接口。

使用 IDEA 的 Spring Boot 项目初始化工具，选择 **Java 8、Spring Boot 2.7 版本**。

开发日志请查看 **星海OJ在线判题系统** 项目 README.md 文件的第五大点。

相关项目：星海OJ在线判题系统 https://github.com/guiyi886/starseaoj_backend
