# open-nga

此项目是 https://github.com/mlzzen/open-nga 的分支

由 Claude Code 以及 CodeX 构建

## 本分支改动

1. 构建环境升级到 JDK 17、Gradle 8.9、Android Gradle Plugin 8.6.1、Kotlin 1.9.24。
2. `compileSdkVersion` 和 `targetSdkVersion` 升级到 35，`minSdkVersion` 保持 26。
3. Java/Kotlin 编译目标升级到 17。
4. 补齐各模块 `namespace`，迁移 AGP 8 相关 DSL，并保留旧 R 类兼容开关以兼容现有代码。
5. 适配 Android 13+ 通知和媒体权限，限制旧存储权限的适用版本。
6. 保存图片改为 Android 10+ 使用 MediaStore，旧系统继续走传统文件保存。
7. 选图、发帖图片预览、缓存导入等流程适配 scoped storage。
8. 增加 HTML 解析器作为帖子内容解析兜底。
9. 支持帖子自动预加载下一页。
10. 升级 Room、ARouter、AppCompat、Material、Core KTX、Preference、Glide 等依赖。
11. 网络栈升级到 Retrofit 2.11.0 + OkHttp 4.12.0。

## 构建要求

- JDK 17
- Android SDK 35
- Gradle Wrapper 8.9（使用仓库内 `./gradlew`）

常用验证命令：

```bash
./gradlew clean :nga_phone_base_3.0:assembleDebug
./gradlew :nga_phone_base_3.0:lintDebug
```

## 下载

[github](https://github.com/chelly/open-nga/releases)
