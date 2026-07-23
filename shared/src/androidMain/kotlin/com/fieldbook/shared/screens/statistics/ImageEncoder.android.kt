package com.fieldbook.shared.screens.statistics

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun encodePng(image: ImageBitmap): ByteArray? {
    return runCatching {
        val source = image.asAndroidBitmap()
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }

        ByteArrayOutputStream().use { output ->
            val encoded = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            if (bitmap !== source) {
                bitmap.recycle()
            }
            if (encoded) output.toByteArray() else null
        }
    }.getOrNull()
}
