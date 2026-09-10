package com.solium.Unitoolkit.core.utils

import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String, fallback: Color = Color(0xFF8B5CF6)): Color {
    return try {
        var h = hex.removePrefix("#").trim()
        if (h.length == 6) h = "FF$h"
        if (h.length == 8) Color(h.toLong(16))
        else fallback
    } catch (e: Exception) { fallback }
}