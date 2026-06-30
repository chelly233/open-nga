# JDK 17 与 Target API 升级评估

更新时间：2026-06-30

## 当前状态

- 当前运行 JDK：OpenJDK 17.0.19。
- Gradle Wrapper：7.3。
- Android Gradle Plugin：7.0.4。
- Kotlin Gradle Plugin：1.5.0。
- `compileSdkVersion`：31。
- `targetSdkVersion`：31。
- `minSdkVersion`：26。
- Java 源码兼容级别：Java 8。
- 应用包名：`me.chelly.opennga`。

结论：当前机器已经在用 JDK 17 编译，但项目构建链仍停留在 AGP 7.0.4 / Gradle 7.3 / Kotlin 1.5.0。继续只改 `targetSdkVersion` 风险较高，建议把构建链和 Target API 分阶段升级。

## 外部要求

Google Play 对新应用和应用更新有 target API 要求。按当前公开要求，2025-08-31 起应用更新通常需要 target Android 15，也就是 API 35。到 2026 年继续提交商店更新时，`targetSdkVersion = 31` 已明显落后。

如果只是自己安装 APK，不上架 Google Play，target API 没有硬性上架限制，但系统行为仍会逐步按新 target 变更。

## 推荐目标

短期目标：

- JDK 固定为 17。
- Gradle 升到 8.x。
- Android Gradle Plugin 升到 8.x。
- Kotlin 升到与 AGP 8 兼容的 1.9.x。
- `compileSdkVersion` 升到 35。
- `targetSdkVersion` 升到 35。
- Java/Kotlin 语言级别先继续保持 Java 8，不急着改成 Java 17 语法。

不建议一步到位 JDK 21：

- Android Gradle Plugin 8.x 官方主路径是 JDK 17。
- JDK 21 对旧插件、注解处理器、Kotlin daemon 的兼容收益不明显，风险更高。
- 当前项目依赖较旧，先稳定到 JDK 17 更务实。

## 主要风险

### 构建链风险

- AGP 8 要求 JDK 17，当前环境满足。
- Gradle 7.3 不能作为 AGP 8 的长期搭配，需要同步升级 Gradle Wrapper。
- Kotlin 1.5.0 偏旧，升级 AGP 8 后应同步升级 Kotlin Gradle Plugin。
- 当前 `gradle.properties` 有旧版 javac `--add-exports` 参数，升级后需要重新验证是否还需要。
- 构建时曾出现 Kotlin daemon `FileChannelUtil` 相关异常，但 Gradle fallback 后能成功；升级 Kotlin/Gradle 后这类问题大概率改善。

### Android 行为变更风险

- 存储权限：代码仍在申请 `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE`。target 33+ 后传统外部存储权限行为变化明显，需要改为应用私有目录、系统 Photo Picker，或 `READ_MEDIA_IMAGES` 等新权限。
- 通知权限：target 33+ 后通知需要 `POST_NOTIFICATIONS` 运行时权限。如果 App 有通知功能，需要补权限和降级逻辑。
- 前台服务与后台限制：如果后续加入后台下载、通知常驻服务，需要按新 API 声明服务类型。
- 组件导出：Manifest 当前已有 `android:exported`，但升级后仍需完整检查所有带 intent-filter 的组件。
- 文件分享：项目使用 `FileProvider`，需要验证 Android 14/15 上分享图片、保存图片流程。

### 依赖风险

当前依赖年代较早，升级 SDK 后重点关注：

- ButterKnife 10.2.1：长期停更。短期可保留，长期建议迁到 ViewBinding。
- ARouter 1.2.4 / compiler 1.1.4：较旧，AGP 8 下需验证注解处理生成代码。
- Retrofit 2.3.0 / RxJava2 / RxLifecycle2：可先不动，但后续建议集中升级。
- AndroidX AppCompat `1.2.0-alpha03`、Core KTX `1.3.0`、Preference `1.1.1`：建议升到稳定版本，避免新 SDK 兼容问题。
- Room 2.4.1：AGP/Kotlin 升级后建议至少升到 2.6.x。
- `com.android.databinding:viewbinding:4.0.1`：项目已启用 Android 插件内置 ViewBinding，这个依赖可以评估移除。

## 分阶段方案

### 阶段 1：锁定 JDK 17

目标：明确项目以 JDK 17 作为构建 JDK。

建议：

- 在 README 或构建文档中声明需要 JDK 17。
- 不把 Java 源码级别立刻改成 17，继续保留：

```groovy
sourceCompatibility JavaVersion.VERSION_1_8
targetCompatibility JavaVersion.VERSION_1_8
```

验收：

```bash
java -version
./gradlew :nga_phone_base_3.0:assembleDebug
```

### 阶段 2：升级 Gradle / AGP / Kotlin

目标：先让构建链进入 AGP 8 时代，但不急着改业务行为。

建议目标版本：

- Gradle Wrapper：8.7 或 8.9。
- Android Gradle Plugin：8.6.x 左右。
- Kotlin Gradle Plugin：1.9.24 左右。

需要修改：

- `gradle/wrapper/gradle-wrapper.properties`
- 根目录 `build.gradle`
- 各 module 的 Android DSL 兼容项。

注意点：

- AGP 8 下 `namespace` 必须存在。主 app 已有，库 module 也要确认并补齐。
- 旧 `lintOptions` 需要迁移为 `lint {}`。
- 验证 annotationProcessor，包括 ButterKnife、Glide、ARouter、Room。

验收：

```bash
./gradlew clean
./gradlew :nga_phone_base_3.0:assembleDebug
./gradlew :nga_phone_base_3.0:lintDebug
```

### 阶段 3：升级 compileSdk 到 35

目标：先让项目能在 API 35 SDK 上编译。

建议：

```groovy
compileSdkVersion = 35
targetSdkVersion = 31
```

先只升 `compileSdkVersion`，不改变 target 行为。这样可以提前发现资源、Manifest、依赖编译问题。

验收重点：

- 所有 module 编译通过。
- Manifest merge 无错误。
- 图片保存、附件打开、分享、登录、帖子浏览正常。

### 阶段 4：升级 targetSdk 到 35

目标：进入新的系统行为模型。

建议：

```groovy
targetSdkVersion = 35
```

必须验证：

- 首次启动权限流程。
- 保存图片到相册。
- 从相册选择图片发帖。
- 下载附件。
- 通知权限和通知点击。
- 横屏、刘海、状态栏、导航栏沉浸表现。
- 登录 Cookie、WebView 打开外链、分享图片。

可能要改：

- 移除或限制 `WRITE_EXTERNAL_STORAGE`，改用 MediaStore 或应用私有目录。
- Android 13+ 补 `POST_NOTIFICATIONS`。
- Android 13+ 图片读取改 `READ_MEDIA_IMAGES` 或 Photo Picker。
- 对 Android 14/15 的前台服务和后台启动限制做兜底。

### 阶段 5：依赖清理

目标：降低长期维护成本。

优先级：

1. 移除显式 `com.android.databinding:viewbinding:4.0.1`，使用 AGP 内置 ViewBinding。
2. AppCompat、Core KTX、Preference、Material 升到稳定版。
3. Room 升到 2.6.x。
4. Glide 升到 4.16.x。
5. 评估 ButterKnife 到 ViewBinding 的迁移。
6. ARouter 升级或替换为显式 Intent/Navigation。

## 推荐执行顺序

1. 创建单独分支：`upgrade/jdk17-target35`。
2. 先只文档化 JDK 17，并确认当前构建可重复。
3. 升 Gradle Wrapper、AGP、Kotlin。
4. 补齐所有 module 的 `namespace` 和 DSL 迁移。
5. 升 `compileSdkVersion` 到 35。
6. 修编译和 lint。
7. 升 `targetSdkVersion` 到 35。
8. 专项修权限、存储、通知。
9. 真机回归。
10. 再考虑依赖升级和 ButterKnife 迁移。

## 回滚策略

- 每个阶段单独提交。
- `compileSdk` 和 `targetSdk` 分开提交。
- 权限模型改动单独提交。
- 如果 target 35 行为问题太多，可以先保留 compile 35 + target 31 的构建链改造结果，再继续分支修行为。

## 建议结论

有必要升级 JDK 17 和 target API，但不建议一次性同时升级 JDK、AGP、Kotlin、target API、依赖和业务权限模型。

最稳路线是：

1. JDK 17 固定为构建环境。
2. AGP/Gradle/Kotlin 先升级并保证能编译。
3. `compileSdk` 升到 35。
4. `targetSdk` 升到 35。
5. 最后处理权限与依赖现代化。

