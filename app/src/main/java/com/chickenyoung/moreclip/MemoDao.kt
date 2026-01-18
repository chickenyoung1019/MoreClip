package com.chickenyoung.moreclip

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MemoDao {
    @Insert
    suspend fun insert(memo: MemoEntity)

    @Query("SELECT * FROM memos ORDER BY createdAt DESC")
    suspend fun getAllMemos(): List<MemoEntity>

    @Delete
    suspend fun delete(memo: MemoEntity)

    @Update
    suspend fun update(memo: MemoEntity)

    // フォルダ一覧を取得（重複なし、並び替えはアプリ側で行う）
    @Query("SELECT DISTINCT folder FROM memos WHERE isTemplate = 1 AND folder IS NOT NULL")
    suspend fun getFolders(): List<String>

    // 履歴を取得（並び替えなし）
    @Query("SELECT * FROM memos WHERE isTemplate = 0")
    suspend fun getHistoryMemos(): List<MemoEntity>

    // フォルダ内の定型文を取得（並び替えなし）
    @Query("SELECT * FROM memos WHERE isTemplate = 1 AND folder = :folderName")
    suspend fun getTemplatesByFolder(folderName: String): List<MemoEntity>

    // フォルダなしの定型文を取得（並び替えなし）
    @Query("SELECT * FROM memos WHERE isTemplate = 1 AND folder IS NULL")
    suspend fun getTemplatesWithoutFolder(): List<MemoEntity>

    // 全定型文を取得（IME用）
    @Query("SELECT * FROM memos WHERE isTemplate = 1 ORDER BY displayOrder ASC")
    suspend fun getAllTemplates(): List<MemoEntity>
}