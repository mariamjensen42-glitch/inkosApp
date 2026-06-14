# GitHub Actions CI/CD 配置

本项目使用 GitHub Actions 进行持续集成和持续部署。

## 工作流说明

### 1. android-ci.yml
**基本 CI 工作流**
- 触发条件：push 和 PR 到 main/develop 分支
- 任务：
  - Build & Test：构建项目并运行单元测试
  - Lint Check：代码风格检查
  - Assemble Debug APK：生成调试版本 APK

### 2. android-cicd.yml
**完整的 CI/CD 工作流**
- 触发条件：push 和 PR 到 main/develop 分支，每周一自动运行
- 任务：
  - Code Quality：代码质量检查（ktlint、detekt、Android Lint）
  - Unit Tests：单元测试和覆盖率报告
  - Build Debug：构建调试版本
  - Build Release：构建发布版本（仅 main 分支）
  - Dependency Submission：依赖图提交

### 3. inkos-tests.yml
**InkOS 专项测试工作流**
- 触发条件：push 和 PR 到 main/develop 分支
- 任务：
  - 运行单元测试
  - 运行 instrumented 测试（使用 Android 模拟器）
  - 生成测试报告
  - 在 PR 中自动评论测试结果

### 4. quality-gates.yml
**质量门禁工作流**
- 触发条件：push 和 PR 到 main/develop 分支
- 任务：
  - 代码覆盖率检查（最低 70%）
  - 代码风格检查
  - 静态分析
  - 安全漏洞检查
  - 生成质量报告

## 签名配置

本项目使用硬编码的 keystore 配置，无需配置 Secrets：

```kotlin
// app/build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file("${rootDir}/app/release.keystore")
        storePassword = "inkos123"
        keyAlias = "inkos"
        keyPassword = "inkos123"
    }
}
```

**注意：** 生产环境建议使用 GitHub Secrets 或环境变量管理敏感信息。

## 本地运行测试

### 运行所有测试
```bash
./gradlew test
```

### 运行代码质量检查
```bash
./gradlew ktlintCheck detekt lint
```

### 生成覆盖率报告
```bash
./gradlew jacocoTestReport
```

### 构建调试版本
```bash
./gradlew assembleDebug
```

### 构建发布版本
```bash
./gradlew assembleRelease
```

## 测试报告位置

- 单元测试报告：`app/build/reports/tests/`
- 代码覆盖率报告：`app/build/reports/jacoco/`
- Lint 报告：`app/build/reports/lint-results/`
- ktlint 报告：`app/build/reports/ktlint/`
- detekt 报告：`app/build/reports/detekt/`

## 质量门禁标准

| 检查项 | 标准 |
|--------|------|
| 代码覆盖率 | ≥ 70% |
| ktlint | 无错误 |
| detekt | 无严重问题 |
| Android Lint | 无错误 |
| 安全漏洞 | 无高危漏洞 |

## 故障排除

### 构建失败

1. 检查 JDK 版本是否为 17
2. 检查 Gradle 缓存是否正常
3. 查看详细错误日志

### 测试失败

1. 查看测试报告：`app/build/reports/tests/`
2. 本地运行测试复现问题
3. 检查测试环境配置

### 代码质量检查失败

1. 运行 `./gradlew ktlintCheck` 查看代码风格问题
2. 运行 `./gradlew detekt` 查看静态分析问题
3. 根据报告修复问题

## 最佳实践

1. **提交前检查**：在本地运行 `./gradlew check` 确保所有检查通过
2. **小步提交**：保持每次提交的变更范围小，便于问题定位
3. **及时修复**：CI 失败时及时修复，避免阻塞其他开发者
4. **覆盖率提升**：逐步提升代码覆盖率，确保关键功能有测试覆盖
