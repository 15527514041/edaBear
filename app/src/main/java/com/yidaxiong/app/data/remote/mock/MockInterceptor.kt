package com.yidaxiong.app.data.remote.mock

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Mock 拦截器
 *
 * 拦截 HTTP 请求，根据 URL 路径返回预设的 JSON 响应。
 * 适用于 MVP 阶段和后端并行开发。
 */
class MockInterceptor : Interceptor {

    private val jsonMediaType = "application/json".toMediaType()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method

        val json = when {
            // GET /tasks/today
            path.contains("tasks/today") && method == "GET" -> MOCK_TASKS
            // POST /tasks/{id}/complete
            path.contains("tasks") && path.contains("complete") && method == "POST" -> MOCK_SUCCESS
            // GET /report/daily
            path.contains("report") && method == "GET" -> MOCK_REPORT
            // GET /user/profile
            path.contains("user/profile") && method == "GET" -> MOCK_USER
            // POST /detection/record
            path.contains("detection/record") && method == "POST" -> MOCK_SUCCESS
            // 未匹配 — 透传到真实后端（如果有）
            else -> null
        }

        return if (json != null) {
            Response.Builder()
                .code(200)
                .message("OK")
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .body(json.toResponseBody(jsonMediaType))
                .build()
        } else {
            chain.proceed(request)
        }
    }

    companion object {
        private val MOCK_SUCCESS = """{"code":200,"message":"success"}"""

        private val MOCK_TASKS = """{
            "code": 200,
            "message": "success",
            "data": [
                {"id":"1","title":"📖 语文生字练习","subject":"语文","duration":20,"isCompleted":false,"createdAt":"2026-06-06"},
                {"id":"2","title":"🧮 数学口算题","subject":"数学","duration":15,"isCompleted":false,"createdAt":"2026-06-06"},
                {"id":"3","title":"📝 英语单词跟读","subject":"英语","duration":10,"isCompleted":false,"createdAt":"2026-06-06"}
            ]
        }"""

        private val MOCK_REPORT = """{
            "code": 200,
            "message": "success",
            "data": {
                "date": "2026-06-06",
                "totalTasks": 3,
                "completedTasks": 2,
                "totalStars": 5,
                "postureScore": 85,
                "focusScore": 78,
                "suggestions": ["坐姿需要改善，注意不要低头","专注度良好，继续保持"]
            }
        }"""

        private val MOCK_USER = """{
            "code": 200,
            "message": "success",
            "data": {
                "id": "user_001",
                "nickname": "小明",
                "avatar": "",
                "totalStars": 42
            }
        }"""
    }
}
