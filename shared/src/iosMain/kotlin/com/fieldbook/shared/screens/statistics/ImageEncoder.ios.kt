package com.fieldbook.shared.screens.statistics

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun encodePng(image: ImageBitmap): ByteArray? {
    return runCatching {
        val skiaImage = Image.makeFromBitmap(image.asSkiaBitmap())
        skiaImage.encodeToData(EncodedImageFormat.PNG, 100)?.bytes
    }.getOrNull()
}
