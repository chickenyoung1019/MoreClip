package com.example.myclipboardapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isTemplate: Boolean = false,
    val folder: String? = null,
    val displayOrder: Int = 0  // 並び替え用
)