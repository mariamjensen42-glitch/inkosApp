package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "NOVEL", "SHORT", "PLAY"
    val genre: String, // e.g. "XUANHUAN", "URBAN", "DETECTIVE", "FANTASY"
    val brief: String = "", // 创作简报 / Initial outline
    val storyBible: String = "", // 设定 (故事框架, 规则, 主角人设等)
    val bookRules: String = "", // 长期书级规则
    val coverUrl: String = "",
    val coverPrompt: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "DRAFT", // "DRAFT", "PLAYING", "COMPLETED"
    val currentChapterIndex: Int = 1,
    val totalChapters: Int = 12,
    val salesPackage: String = "", // InkOS Short 简介卖点、大纲等
    val targetWordsPerChapter: Int = 1000
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val title: String,
    val chapterIndex: Int,
    val intent: String = "", // 章节意图/目标 (chapter.intent.md)
    val content: String = "", // Complete content
    val auditLogs: String = "", // 37-dimension audit continuity reports
    val status: String = "DRAFT", // "DRAFT", "APPROVED"
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_states")
data class PlayStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val worldContract: String = "", // 世界契约/规则
    val timeState: String = "12:00 (Day 1)", // 极具代入感的非固定时间
    val currentScene: String = "", // 当前场景详情 + HUD + 可视化描述
    val charactersJson: String = "[]", // 角色属性, 关系, 状态 JSON
    val itemsJson: String = "[]", // 物品/证据 JSON
    val historyLogJson: String = "[]", // 完整的滚动会话历史 logs
    val lastUpdate: Long = System.currentTimeMillis()
)
