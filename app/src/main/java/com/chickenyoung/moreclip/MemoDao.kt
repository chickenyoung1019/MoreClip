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

    // バッチ更新（複数件を一度に更新）
    @Update
    suspend fun updateAll(memos: List<MemoEntity>)

    // 履歴のdisplayOrderを+1（指定ID以外）
    @Query("UPDATE memos SET displayOrder = displayOrder + 1 WHERE isTemplate = 0 AND id != :excludeId")
    suspend fun incrementHistoryDisplayOrderExcept(excludeId: Int)

    // 全履歴のdisplayOrderを+1
    @Query("UPDATE memos SET displayOrder = displayOrder + 1 WHERE isTemplate = 0")
    suspend fun incrementAllHistoryDisplayOrder()

    // フォルダ内定型文のdisplayOrderを+1
    @Query("UPDATE memos SET displayOrder = displayOrder + 1 WHERE isTemplate = 1 AND (folder = :folder OR (:folder IS NULL AND folder IS NULL))")
    suspend fun incrementTemplateDisplayOrderInFolder(folder: String?)

    // フォルダ内定型文のdisplayOrderを指定数だけ増加
    @Query("UPDATE memos SET displayOrder = displayOrder + :amount WHERE isTemplate = 1 AND (folder = :folder OR (:folder IS NULL AND folder IS NULL))")
    suspend fun incrementTemplateDisplayOrderInFolderBy(folder: String?, amount: Int)

    // バッチ削除（複数件を一度に削除）
    @Delete
    suspend fun deleteAll(memos: List<MemoEntity>)

    // フォルダ名を一括変更
    @Query("UPDATE memos SET folder = :newName WHERE isTemplate = 1 AND folder = :oldName")
    suspend fun renameFolder(oldName: String, newName: String)

    // フォルダ内の定型文を全削除
    @Query("DELETE FROM memos WHERE isTemplate = 1 AND folder = :folderName")
    suspend fun deleteTemplatesInFolder(folderName: String)

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