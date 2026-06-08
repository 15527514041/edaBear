# 易达熊 MVP 架构与实施计划

## 一、项目概览

| 项 | 内容 |
|---|---|
| 项目名 | 易达熊（YiDaXiong） |
| 包名 | `com.yidaxiong.app` |
| 目标 | 低配安卓平板（2-4GB RAM, Android 8.0+, CPU only） |
| 语言 | Kotlin + Jetpack Compose |
| 架构 | MVVM + Clean Architecture 轻量版 |
| MVP 范围 | 主页 + 坐姿检测/分心检测（Mock API） |
| 开发者背景 | 7 年 Vue 前端，首次 Android 开发，配合 AI 辅助 |

---

## 二、技术栈选型

| 类别 | 选型 | 原因 |
|---|---|---|
| UI 框架 | **Jetpack Compose** | 声明式 + 组件化 → Vue 开发者过渡自然 |
| 导航 | Navigation Compose | 官方推荐 |
| 架构 | MVVM (ViewModel + StateFlow) | Android 标准模式 |
| DI | **Hilt** | 官方 DI，减少样板代码 |
| 网络 | Retrofit + OkHttp + **MockInterceptor** | MVP 阶段 mock 切换便捷 |
| 序列化 | Kotlinx Serialization | 比 Gson 轻量 |
| 相机 | CameraX | 比 Camera2 API 更易用，兼容性好 |
| AI 推理 | MediaPipe Android SDK + TFLite | 纯 CPU 推理，低配平板可用 |
| TTS | Android TextToSpeech API | 系统内置，离线可用 |
| 异步 | Kotlin Coroutines + Flow | 协程天然支持 |
| 图片加载 | Coil | Compose 原生支持，轻量 |
| 本地存储 | DataStore (偏好) + Room (结构化) | MVP 用 DataStore 足够 |

---

## 三、完整项目结构

```
YiDaXiong/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/yidaxiong/app/
│   │   │   │   ├── YiDaXiongApp.kt              # Application 类
│   │   │   │   ├── MainActivity.kt               # 单 Activity 入口
│   │   │   │   │
│   │   │   │   ├── di/                           # Hilt 依赖注入
│   │   │   │   │   ├── AppModule.kt              # 全局依赖
│   │   │   │   │   ├── NetworkModule.kt          # Retrofit + OkHttp
│   │   │   │   │   └── DetectionModule.kt        # 检测引擎依赖
│   │   │   │   │
│   │   │   │   ├── data/                         # 数据层
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── api/
│   │   │   │   │   │   │   ├── YiDaXiongApi.kt           # API 接口定义
│   │   │   │   │   │   │   └── ApiResponse.kt            # 通用响应包装
│   │   │   │   │   │   ├── mock/
│   │   │   │   │   │   │   ├── MockInterceptor.kt        # Mock 拦截器
│   │   │   │   │   │   │   └── mock_data.json            # Mock 数据
│   │   │   │   │   │   └── dto/                          # 数据传输对象
│   │   │   │   │   │       ├── TaskDto.kt
│   │   │   │   │   │       ├── ReportDto.kt
│   │   │   │   │   │       └── UserDto.kt
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── TaskRepository.kt
│   │   │   │   │   │   └── UserRepository.kt
│   │   │   │   │   └── local/
│   │   │   │   │       └── PreferencesManager.kt         # DataStore 封装
│   │   │   │   │
│   │   │   │   ├── domain/                       # 领域层
│   │   │   │   │   ├── model/                             # 纯 Kotlin 领域模型
│   │   │   │   │   │   ├── Task.kt
│   │   │   │   │   │   ├── DetectionResult.kt
│   │   │   │   │   │   └── UserProfile.kt
│   │   │   │   │   └── usecase/                           # 用例（可省略直接调ViewModel）
│   │   │   │   │       └── GetHomeDataUseCase.kt
│   │   │   │   │
│   │   │   │   ├── ui/                           # UI 层
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── NavGraph.kt                    # 导航图
│   │   │   │   │   │   └── Screen.kt                      # 路由定义
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Theme.kt                       # 主题色/字体
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   ├── components/                        # 通用组件
│   │   │   │   │   │   ├── YidaxiongTopBar.kt
│   │   │   │   │   │   ├── LoadingIndicator.kt
│   │   │   │   │   │   └── AlertDialog.kt
│   │   │   │   │   ├── home/                              # 主页模块
│   │   │   │   │   │   ├── HomeScreen.kt                  # 主页 Composable
│   │   │   │   │   │   ├── HomeViewModel.kt               # 主页 ViewModel
│   │   │   │   │   │   └── components/                    # 主页子组件
│   │   │   │   │   │       ├── TaskCard.kt
│   │   │   │   │   │       ├── HonorStars.kt
│   │   │   │   │   │       └── StatusPanel.kt
│   │   │   │   │   ├── detection/                         # 检测模块
│   │   │   │   │   │   ├── DetectionScreen.kt             # 检测主界面
│   │   │   │   │   │   ├── DetectionViewModel.kt          # 检测 ViewModel
│   │   │   │   │   │   ├── CameraPreview.kt               # 相机预览
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── DetectionOverlay.kt        # 检测信息叠加层
│   │   │   │   │   │       └── AlertBanner.kt             # 违规提醒条
│   │   │   │   │   └── report/                            # 报告页（MVP 占位）
│   │   │   │   │       └── ReportScreen.kt
│   │   │   │   │
│   │   │   │   ├── detection/                     # 检测引擎独立模块
│   │   │   │   │   ├── DetectionEngine.kt                  # 检测引擎（单例）
│   │   │   │   │   ├── frame/
│   │   │   │   │   │   └── FramePreprocessor.kt           # 帧预处理
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── MediaPipeFaceMesh.kt           # Face Mesh 封装
│   │   │   │   │   │   ├── MediaPipePoseLite.kt           # Pose Lite 封装
│   │   │   │   │   │   └── MediaPipeHands.kt              # Hands 封装
│   │   │   │   │   ├── rule/
│   │   │   │   │   │   ├── PostureRuleEngine.kt           # 坐姿规则引擎
│   │   │   │   │   │   ├── DistractionRuleEngine.kt       # 分心规则引擎
│   │   │   │   │   │   └── RuleConfig.kt                  # 可配置阈值
│   │   │   │   │   ├── alert/
│   │   │   │   │   │   ├── AlertManager.kt                # 提醒管理器
│   │   │   │   │   │   └── TtsManager.kt                  # TTS 语音封装
│   │   │   │   │   └── result/
│   │   │   │   │       └── DetectionMetrics.kt            # 检测统计
│   │   │   │   │
│   │   │   │   └── util/
│   │   │   │       ├── PermissionHelper.kt                # 权限申请
│   │   │   │       └── Constants.kt                       # 全局常量
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── raw/                                   # 模型文件(.tflite)
│   │   │   │   │   ├── face_landmarker.task
│   │   │   │   │   ├── pose_landmarker_lite.task
│   │   │   │   │   └── hand_landmarker.task
│   │   │   │   ├── values/
│   │   │   │   │   └── strings.xml
│   │   │   │   └── drawable/                              # 启动图标等
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── mock/                                   # mock 源码集
│   │   │   └── java/com/yidaxiong/app/
│   │   │       └── data/remote/mock/
│   │   │           └── MockDataProvider.kt                # mock 数据提供
│   │   │
│   │   └── prod/                                   # prod 源码集（后续）
│   │
│   ├── build.gradle.kts                          # app 模块构建
│   └── proguard-rules.pro                         # 混淆规则
│
├── local.properties                              # SDK 路径
├── build.gradle.kts                              # 根构建文件
├── settings.gradle.kts                           # 项目设置
└── gradle.properties                             # Gradle 全局属性
```

---

## 四、构建配置核心依赖

### build.gradle.kts (app 模块核心依赖)

```kotlin
// 核心框架
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Jetpack Compose (BOM 统一版本)
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.8.2")

// Navigation Compose
implementation("androidx.navigation:navigation-compose:2.7.7")

// CameraX
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// Hilt DI
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-android-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Retrofit + OkHttp
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Kotlinx Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

// MediaPipe Vision (含 FaceMesh、Pose、Hands)
implementation("com.google.mediapipe:tasks-vision:0.20230731")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Coil (图片加载)
implementation("io.coil-kt:coil-compose:2.5.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// 性能监控（可选）
implementation("com.google.android.material:material:1.11.0")
```

### Build 变体配置 (mock / prod)

```kotlin
buildTypes {
    debug {
        buildConfigField("boolean", "USE_MOCK", "true")
    }
    release {
        buildConfigField("boolean", "USE_MOCK", "false")
        isMinifyEnabled = true
        proguardFiles(...)
    }
}
```

---

## 五、MVP 屏幕流程与 Wireflow

```
┌─────────────────────────────────────────────────┐
│                 启动 Launcher                     │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│           PermissionScreen (首次)                 │
│   相机权限 · 悬浮窗权限 · 电池优化排除               │
└──────────────────┬──────────────────────────────┘
                   │ (已有权限则跳过)
                   ▼
┌─────────────────────────────────────────────────┐
│                  HomeScreen                      │
│  ┌───────────────────────────────────────────┐   │
│  │  TopBar: "易达熊" + 数字人头像(placeholder) │   │
│  ├───────────────────────────────────────────┤   │
│  │  今日任务卡片                              │   │
│  │  [📖 语文 20min] [🧮 数学 15min]          │   │
│  ├───────────────────────────────────────────┤   │
│  │  状态面板                                  │   │
│  │  坐姿 ✅ · 专注度 ⭐⭐⭐                     │   │
│  ├───────────────────────────────────────────┤   │
│  │  [ 🚀 开始学习 ] 大按钮                    │   │
│  ├───────────────────────────────────────────┤   │
│  │  🌟 今日已获得 3 颗星星                    │   │
│  └───────────────────────────────────────────┘   │
└──────────────────┬──────────────────────────────┘
                   │ 点击"开始学习"
                   ▼
┌─────────────────────────────────────────────────┐
│              DetectionScreen                     │
│  ┌───────────────────────────────────────────┐   │
│  │  CameraPreview (640×480 前置相机)          │   │
│  │  ┌──────────Detection Overlay──────────┐   │   │
│  │  │   坐姿: ✅ 正常                       │   │   │
│  │  │   专注: ✅ 良好                       │   │   │
│  │  │   时长: 00:12:34                     │   │   │
│  │  └─────────────────────────────────────┘   │   │
│  ├───────────────────────────────────────────┤   │
│  │  当前任务: 语文生字练习                     │   │
│  ├───────────────────────────────────────────┤   │
│  │  [⏸️ 暂停] [✅ 完成任务]                   │   │
│  └───────────────────────────────────────────┘   │
│                                                  │
│  ⚠️ 违规弹窗/语音提醒:                             │
│  "小朋友，坐直一点哦！"                            │
└──────────────────────────────────────────────────┘
```

### 检测页面状态机

```
IDLE ───(点击开始)───▶ DETECTING ───(触发违规)───▶ ALERTING
  ▲                         │                        │
  │                         │ (违规恢复正常)           │ (提醒结束)
  │                         ◀────────────────────────┘
  │
  └──────(点击退出任务/结束)──────▶ FINISHED
```

---

## 六、检测引擎架构设计

### 6.1 引擎架构图

```
CameraX (ImageAnalysis)
       │
       ▼ 每帧回调 (onAnalyze)
┌──────────────────────────────┐
│   FramePreprocessor          │ ← 缩放到 640×480, 格式转换
│   - scaleToInputSize()       │
│   - rotateToOrientation()    │
└──────────┬───────────────────┘
           │ 处理后的 ByteBuffer
           ▼
┌──────────────────────────────┐
│   MediaPipeFaceMesh          │ ← face_landmarker.task
│   - detect() → 468 landmarks│
└──────────┬───────────────────┘
           │
           ▼ (并行/串行取决于性能)
┌────────────────────┐ ┌──────────────────┐
│ MediaPipePoseLite  │ │ MediaPipeHands   │
│ - 17 keypoints     │ │ - 21 keypoints   │
└────────┬───────────┘ └────────┬─────────┘
         │                      │
         ▼                      ▼
┌──────────────────────────────────────────────┐
│           Rule Engine (主线程轮询队列)          │
│                                               │
│  PostureRuleEngine                            │
│  ├─ isHeadDown()         ← FaceMesh 俯仰角    │
│  ├─ isHeadTilted()       ← FaceMesh 耳朵高度差│
│  ├─ isTooClose()         ← FaceMesh 人脸面积   │
│  └─ isBodyLeaning()      ← Pose 肩膀高度差     │
│                                               │
│  DistractionRuleEngine                        │
│  ├─ isGazeAway()         ← FaceMesh 视线向量   │
│  ├─ isHandFidgeting()    ← Hands 帧间位移量    │
│  └─ isDazing()           ← 综合静态判断        │
└───────────────────┬──────────────────────────┘
                    │ 违规事件
                    ▼
┌──────────────────────────────┐
│  AlertManager                │
│  ├─ onViolation(type, msg)   │
│  └─ triggerAlert()           │
│       ├─ TtsManager.speak()  │ ← 语音提醒
│       └─ 发送事件到 UI State  │ ← 弹窗/横幅
└──────────────────────────────┘
```

### 6.2 线程模型

```
┌─────────────┐    ┌──────────────┐    ┌──────────────┐
│ CameraX     │───▶│ Detection    │───▶│ UI (Main)    │
│ Callback    │    │ Thread       │    │ Thread       │
│ (任意线程)   │    │ (单线程串行)  │    │ (StateFlow)  │
└─────────────┘    └──────────────┘    └──────────────┘
```

**关键规则**：
- 所有 MediaPipe 推理在 **单条后台线程** 串行执行
- 检测结果通过 `StateFlow<DetectionState>` 发送到 UI 层
- 违规提醒通过 `SharedFlow<AlertEvent>` 一次性发送
- 推理之间帧缓存实时回收，不堆积

### 6.3 DetectionEngine 核心设计

```kotlin
@Singleton
class DetectionEngine @Inject constructor(
    private val faceMesh: MediaPipeFaceMesh,
    private val poseLite: MediaPipePoseLite,
    private val hands: MediaPipeHands,
    private val postureRule: PostureRuleEngine,
    private val distractionRule: DistractionRuleEngine,
    private val alertManager: AlertManager
) {
    private val _detectionState = MutableStateFlow(DetectionState())
    val detectionState: StateFlow<DetectionState> = _detectionState.asStateFlow()

    private val _alertEvents = MutableSharedFlow<AlertEvent>()
    val alertEvents: SharedFlow<AlertEvent> = _alertEvents.asSharedFlow()

    private val detectionScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() + CoroutineName("DetectionThread")
    )

    fun startDetection() { /* 启动协程循环 */ }
    fun stopDetection() { /* 停止 + 资源释放 */ }
    fun onFrame(imageProxy: ImageProxy) { /* 帧回调入口通道 */ }

    // 内部 - 帧处理协程
    private suspend fun processFrame(frame: FrameData) { ... }
}
```

---

## 七、数据流架构

```
┌──────────┐     ┌────────────┐     ┌────────────┐     ┌──────────┐
│  UI      │────▶│ ViewModel  │────▶│ Repository  │────▶│ Api      │
│ Compos-  │     │ (State)    │     │ (数据汇总)   │     │ Retrofit │
│ able     │◀────│            │◀────│             │◀────│ + Mock   │
│          │     │ StateFlow  │     │ Flow        │     │          │
└──────────┘     └────────────┘     └────────────┘     └──────────┘
```

### 7.1 Mock 策略

```kotlin
// MockInterceptor.kt — 根据 URL 路径返回 Mock JSON
class MockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        val json = when {
            path.contains("/tasks") -> mockTaskList
            path.contains("/user")  -> mockUserProfile
            path.contains("/report")-> mockReport
            else -> null
        }
        return Response.Builder()
            .code(200)
            .body(json?.toResponseBody(JSON_MEDIA_TYPE) ?: chain.proceed(chain.request()).body!!)
            .request(chain.request())
            .build()
    }
}

// OkHttp 构建时根据 BuildConfig 决定是否添加
val client = OkHttpClient.Builder().apply {
    if (BuildConfig.USE_MOCK) addInterceptor(MockInterceptor())
    addInterceptor(HttpLoggingInterceptor().apply { level = BODY })
}.build()
```

**对比纯 mock 数据 vs 真实 API**：只需修改 `BuildConfig.USE_MOCK` 一个开关，零代码改动。

---

## 八、API 接口定义（MVP 范围）

```kotlin
interface YiDaXiongApi {
    @GET("tasks/today")
    suspend fun getTodayTasks(): ApiResponse<List<TaskDto>>

    @POST("tasks/{id}/complete")
    suspend fun completeTask(@Path("id") taskId: String): ApiResponse<Unit>

    @GET("report/daily")
    suspend fun getDailyReport(date: String): ApiResponse<ReportDto>

    @GET("user/profile")
    suspend fun getUserProfile(): ApiResponse<UserDto>

    @POST("detection/record")
    suspend fun uploadDetectionRecord(
        @Body record: DetectionRecordDto
    ): ApiResponse<Unit>
}
```

---

## 九、开发路线图（7 阶段）

### Phase 1：环境搭建 + 项目骨架（预估 1-2 天）

| 步骤 | 具体内容 | 产出 |
|------|---------|------|
| 1.1 | 安装 Android Studio + SDK + JDK 17 | 开发就绪 |
| 1.2 | 创建新项目 (Empty Compose Activity) | 项目骨架 |
| 1.3 | 配置 build.gradle.kts (compose, hilt, retrofit, camera, mediapipe) | 构建通过 |
| 1.4 | 配置 ProGuard 保留模型文件 | proguard-rules.pro |
| 1.5 | 创建包结构 (data/ui/detection/di/util) | 目录就绪 |
| 1.6 | 配置 mock/prod build 变体 | BuildConfig 开关 |
| 1.7 | 配置 AndroidManifest (权限声明、screenOrientation=landscape) | 清单文件 |
| 🎯 | **验证：`./gradlew assembleDebug` 编译通过** | |

### Phase 2：基础 UI — 主页（预估 2-3 天）

| 步骤 | 具体内容 |
|------|---------|
| 2.1 | 定义主题 (Theme.kt) + 色彩系统 |
| 2.2 | 创建 Navigation + Screen 路由 |
| 2.3 | 实现 HomeScreen 布局（顶部栏 + 任务卡片 + 状态面板 + 按钮） |
| 2.4 | 实现 HomeViewModel (StateFlow 驱动) |
| 2.5 | 搭建 Retrofit + MockInterceptor |
| 2.6 | 实现 Mock 数据获取 → 主页展示 |
| 🎯 | **验证：运行 App 看到 Mock 数据填充的主页** |

### Phase 3：相机集成（预估 1-2 天）

| 步骤 | 具体内容 |
|------|---------|
| 3.1 | 实现 PermissionHelper (相机 + 悬浮窗) |
| 3.2 | 创建 CameraPreview Composable |
| 3.3 | 配置 CameraX + ImageAnalysis (640×480, RGB) |
| 3.4 | 实现 FramePreprocessor (缩小、格式转换) |
| 🎯 | **验证：DetectionScreen 显示实时相机预览** |

### Phase 4：MediaPipe 检测集成（预估 2-3 天）

| 步骤 | 具体内容 |
|------|---------|
| 4.1 | 下载 MediaPipe 模型文件 (.task)，放入 res/raw |
| 4.2 | 实现 MediaPipeFaceMesh 封装类 |
| 4.3 | 实现 MediaPipePoseLite 封装类 |
| 4.4 | 实现 MediaPipeHands 封装类 |
| 4.5 | 实现 DetectionEngine 主引擎（单线程串行推理） |
| 🎯 | **验证：检测引擎能输出 landmarks 数据** |

### Phase 5：规则引擎 + 提醒系统（预估 2-3 天）

| 步骤 | 具体内容 |
|------|---------|
| 5.1 | 实现 PostureRuleEngine（低头/歪头/过近/侧身） |
| 5.2 | 实现 DistractionRuleEngine（走神/小动作/发呆） |
| 5.3 | 实现 RuleConfig（可配置阈值 + 时间窗口） |
| 5.4 | 实现 TtsManager（离线语音提醒） |
| 5.5 | 实现 AlertManager（语音+弹窗+事件分发） |
| 5.6 | 实现 DetectionOverlay（检测状态叠加层） |
| 5.7 | 实现 DetectionScreen 完整交互 |
| 🎯 | **验证：完整检测流程 —— 摄像头 → 推理 → 规则判定 → 语音/弹窗提醒** |

### Phase 6：Mock API 完善 + 数据联动（预估 1 天）

| 步骤 | 具体内容 |
|------|---------|
| 6.1 | 完善 Mock 数据（任务列表、报告、用户信息） |
| 6.2 | 后端接口联调适配 |

### Phase 7：性能调优 + 低配兼容验证（预估 1 天）

| 步骤 | 具体内容 |
|------|---------|
| 7.1 | 内存分析 (Android Profiler) → < 100MB |
| 7.2 | 帧率监控 → 5-10 FPS |
| 7.3 | 低配平板真机测试 |
| 7.4 | 后台保活验证 |

---

## 十、各阶段预估人天

| Phase | 内容 | 预估人天 |
|-------|------|---------|
| 1 | 环境搭建 | 1-2d |
| 2 | 主页 UI | 2-3d |
| 3 | 相机集成 | 1-2d |
| 4 | MediaPipe 集成 | 2-3d |
| 5 | 规则引擎 + 提醒 | 2-3d |
| 6 | Mock API 完善 | 1d |
| 7 | 性能调优 | 1d |
| **合计** | | **10-15d** |

---

## 十一、关键文件清单（MVP 需创建的 30+ 文件）

### 核心配置
- `app/build.gradle.kts` — 构建配置
- `proguard-rules.pro` — 混淆规则
- `AndroidManifest.xml` — 清单

### 入口
- `YiDaXiongApp.kt` — Application
- `MainActivity.kt` — 单 Activity

### DI
- `AppModule.kt`, `NetworkModule.kt`, `DetectionModule.kt`

### UI
- `Theme.kt`, `Color.kt`, `Type.kt`
- `NavGraph.kt`, `Screen.kt`
- `HomeScreen.kt`, `HomeViewModel.kt`
- `DetectionScreen.kt`, `DetectionViewModel.kt`
- `CameraPreview.kt`
- `TaskCard.kt`, `HonorStars.kt`, `StatusPanel.kt`
- `DetectionOverlay.kt`, `AlertBanner.kt`

### 网络
- `YiDaXiongApi.kt`, `ApiResponse.kt`
- `MockInterceptor.kt`, `MockDataProvider.kt`
- `TaskRepository.kt`, `UserRepository.kt`

### 检测引擎
- `DetectionEngine.kt`
- `FramePreprocessor.kt`
- `MediaPipeFaceMesh.kt`, `MediaPipePoseLite.kt`, `MediaPipeHands.kt`
- `PostureRuleEngine.kt`, `DistractionRuleEngine.kt`
- `RuleConfig.kt`
- `AlertManager.kt`, `TtsManager.kt`
- `DetectionMetrics.kt`

### 工具
- `PermissionHelper.kt`, `Constants.kt`, `PreferencesManager.kt`

---

## 十二、对 Vue 开发者的关键建议

### 12.1 心智模型映射

| Vue 概念 | Android 对应 |
|---------|-------------|
| `.vue` 组件 (template + script + style) | `@Composable` 函数 |
| `ref()` / `reactive()` | `StateFlow` / `MutableState` |
| `computed` | `derivedStateOf` / `map {}` |
| `watch` | `collectAsState()` |
| `v-if` / `v-for` | `if {}` / `items.forEach {}` |
| `props` | 函数参数 |
| `emits` / `emit()` | lambda 回调 |
| Pinia / Vuex | ViewModel + StateFlow |
| `async/await` | `suspend` + `viewModelScope.launch` |
| `npm install` | `implementation("...")` Gradle |
| `router.push` | `navController.navigate()` |
| CSS Flexbox | Compose `Column`/`Row`/`Box` |
| 响应式绑定 (`{{ }}`) | `text = "$state"` |

### 12.2 开发工具

| 工具 | 用途 |
|------|------|
| **Android Studio** (必须) | IDE + 布局预览 + Profiler |
| **Android Device Monitor** | 真机调试 |
| **adb logcat** | 日志查看 |
| **Android Profiler** | 内存/CPU 性能分析 |

### 12.3 学习建议

1. **第一周** 只看 Compose 基础教程（90% Vue 概念可直接映射）
2. **每个组件** 先写 XML 布局预览再转 Compose（调试用 Preview）
3. **遇到报错** 看 gradle 构建日志 → 90% 是依赖版本冲突
4. **每个 Model 文件** 用 GitHub 下载后检查 → MediaPipe 不需要训练
5. **Hilt 注入** 如果觉得复杂 → MVP 阶段可以手动 `object` 单例代替

---

## 十三、AI 专家协作建议

当你使用 WorkBuddy 的「专家中心」协助开发时，建议按以下顺序调用专家：

| 阶段 | 推荐专家 | 问题示例 |
|------|---------|---------|
| 架构 | Android 架构师 | "验证这个 MVVM 架构设计" |
| UI | UI 设计师 | "帮我设计主页的 Compose 布局" |
| 相机 | Camera 专家 | "CameraX 配置 640×480 最佳实践" |
| AI 推理 | MediaPipe 专家 | "MediaPipe FaceMesh Android SDK 集成" |
| 规则引擎 | 行为检测专家 | "坐姿判定算法如何避免误触？" |
| 性能 | 性能优化专家 | "低配平板 100MB 内存限制如何实现？" |
| 后端 | 后端架构师 | "Review API 接口设计" |
| 测试 | QA 工程师 | "MVP 阶段哪些测试场景必须覆盖？" |

---

## 十四、MVP 交付清单

- [ ] 项目能在 Android Studio 编译通过
- [ ] 主页显示今日任务（Mock 数据）
- [ ] 相机实时预览（640×480）
- [ ] 坐姿检测 4 场景识别 + 语音提醒
- [ ] 分心检测 3+ 场景识别 + 语音提醒
- [ ] 违规弹窗 + TTS 语音播报
- [ ] 检测状态 Overlay 实时显示
- [ ] 内存占用 < 100MB
- [ ] 推理帧率 5-10 FPS
- [ ] APK 可在 2GB/4GB 内存平板安装运行
