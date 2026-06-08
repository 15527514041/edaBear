package com.yidaxiong.app.data.remote.mock

/**
 * Mock 数据提供器
 *
 * 用于在 MVP 阶段提供丰富的模拟数据。
 * 使用 mock 源码集（app/src/mock），仅 debug 构建包含。
 */
object MockDataProvider {

    fun getMockTasksJson(): String = """{
        "code": 200,
        "message": "success",
        "data": [
            {"id":"1","title":"📖 语文生字练习","subject":"语文","duration":20,"isCompleted":false,"createdAt":"2026-06-06"},
            {"id":"2","title":"🧮 数学口算题","subject":"数学","duration":15,"isCompleted":false,"createdAt":"2026-06-06"},
            {"id":"3","title":"📝 英语单词跟读","subject":"英语","duration":10,"isCompleted":false,"createdAt":"2026-06-06"},
            {"id":"4","title":"🎨 美术涂色","subject":"美术","duration":15,"isCompleted":true,"createdAt":"2026-06-06"}
        ]
    }"""

    fun getMockReportJson(): String = """{
        "code": 200,
        "message": "success",
        "data": {
            "date": "2026-06-06",
            "totalTasks": 4,
            "completedTasks": 3,
            "totalStars": 8,
            "postureScore": 82,
            "focusScore": 75,
            "suggestions": [
                "坐姿需要改善，注意不要低头",
                "专注度良好，继续保持",
                "写字时手部小动作较多"
            ]
        }
    }"""
}
