package com.chickenyoung.moreclip

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ClipboardTileService : TileService() {

    // タイルが表示され始めた時（プルダウン時など）
    override fun onStartListening() {
        super.onStartListening()
        // タイルを「使用可能」状態に設定
        qsTile?.let { tile ->
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

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
