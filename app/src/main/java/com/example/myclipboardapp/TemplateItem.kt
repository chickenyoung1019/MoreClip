package com.example.myclipboardapp

sealed class TemplateItem {
    data class Folder(
        val name: String,
        val count: Int
    ) : TemplateItem()

    data class Template(
        val memo: MemoEntity
    ) : TemplateItem()
}