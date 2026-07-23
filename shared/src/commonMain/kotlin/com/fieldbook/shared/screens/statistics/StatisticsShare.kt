@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.fieldbook.shared.screens.statistics

import androidx.compose.ui.graphics.ImageBitmap
import com.fieldbook.shared.generated.resources.Res
import com.fieldbook.shared.generated.resources.dir_media_photos
import com.fieldbook.shared.utilities.getDirectory
import com.fieldbook.shared.utilities.shareFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

suspend fun shareStatisticsSection(
    section: StatisticsSection,
    image: ImageBitmap,
): Boolean {
    val imageBytes = encodePng(image) ?: return false
    val photosDir = getDirectory(Res.string.dir_media_photos) ?: return false
    val fileName = "${section.exportFileStem()}_${Clock.System.now().toEpochMilliseconds()}.png"
    val file = photosDir.createFile("image/png", fileName) ?: return false

    withContext(Dispatchers.Default) {
        file.writeBytes(imageBytes)
    }
    shareFile(file)
    return true
}

expect fun encodePng(image: ImageBitmap): ByteArray?

private fun StatisticsSection.exportFileStem(): String {
    val mode = when (period.mode) {
        StatisticsMode.TOTAL -> "total"
        StatisticsMode.YEAR -> "year"
        StatisticsMode.MONTH -> "month"
    }
    val key = period.key
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "statistics" }

    return "field-book-stats-$mode-$key"
}
