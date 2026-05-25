@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.fieldbook.shared.screens.collect.traits

import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

actual class PlatformAngleController {
    private val motionManager = CMMotionManager()
    private var angleCallback: ((Float) -> Unit)? = null
    private var currentAngle = 0f

    actual fun start(onAngleChanged: (Float) -> Unit) {
        angleCallback = onAngleChanged
        if (!motionManager.deviceMotionAvailable) return

        motionManager.deviceMotionUpdateInterval = 0.1
        motionManager.startDeviceMotionUpdatesToQueue(
            NSOperationQueue.mainQueue
        ) { motion, _ ->
            val gravity = motion?.gravity ?: return@startDeviceMotionUpdatesToQueue
            val gx = gravity.useContents { x }.toFloat()
            val gy = gravity.useContents { y }.toFloat()
            val gz = gravity.useContents { z }.toFloat()

            val rawRoll = ((atan2(gx.toDouble(), sqrt((gy * gy + gz * gz).toDouble())) * 180.0) / PI).toFloat()

            currentAngle = lowPassFilter(rawRoll, currentAngle)
            angleCallback?.invoke(currentAngle)
        }
    }

    actual fun stop() {
        motionManager.stopDeviceMotionUpdates()
        angleCallback = null
    }
}

private fun lowPassFilter(input: Float, output: Float): Float {
    return output + 0.5f * (input - output)
}
