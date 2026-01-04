package com.chickenyoung.moreclip

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.TileService

class ClipboardTileService : TileService() {

    // タイルがタップされた時
    override fun onClick() {
        // アプリを起動
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        startActivityAndCollapse(pendingIntent)
    }
}
