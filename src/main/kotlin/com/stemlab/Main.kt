package com.stemlab

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.stemlab.app.AppController
import com.stemlab.ui.StemAgentLabApp
import java.awt.RenderingHints

fun main() = application {
    val controller = remember { AppController() }
    var isVisible by remember { mutableStateOf(true) }

    // Programmatic tray icon: green circle with "S"
    val trayIcon = remember {
        val size = 64
        val img = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = java.awt.Color(76, 175, 80)
        g.fillOval(4, 4, size - 8, size - 8)
        g.color = java.awt.Color.WHITE
        g.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 36)
        val fm = g.getFontMetrics()
        val x = (size - fm.stringWidth("S")) / 2
        g.drawString("S", x, 46)
        g.dispose()
        BitmapPainter(img.toComposeImageBitmap())
    }

    Tray(
        icon = trayIcon,
        tooltip = "Stem Agent Lab",
        onAction = { isVisible = true },
        menu = {
            Item("Open Stem Agent Lab", onClick = { isVisible = true })
            Separator()
            Item("Quit", onClick = ::exitApplication)
        }
    )

    Window(
        onCloseRequest = { isVisible = false },   // hide to tray, don't exit
        visible = isVisible,
        title = "Stem Agent Lab",
        state = WindowState(width = 1120.dp, height = 780.dp)
    ) {
        StemAgentLabApp(controller, onQuit = ::exitApplication)
    }
}
