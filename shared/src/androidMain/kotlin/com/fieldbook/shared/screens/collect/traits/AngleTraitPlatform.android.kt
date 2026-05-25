package com.fieldbook.shared.screens.collect.traits

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.fieldbook.shared.AndroidAppContextHolder
import kotlin.math.atan2
import kotlin.math.sqrt

actual class PlatformAngleController {
    private val context = AndroidAppContextHolder.context
    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val gravitySensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private var angleCallback: ((Float) -> Unit)? = null
    private var currentAngle = 0f

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            if (event.sensor.type != Sensor.TYPE_GRAVITY &&
                event.sensor.type != Sensor.TYPE_ACCELEROMETER
            ) {
                return
            }

            val gravityReading = FloatArray(3)
            System.arraycopy(event.values, 0, gravityReading, 0, gravityReading.size)
            val remappedGravity = remapGravityByRotation(gravityReading, getDeviceRotation())
            val gx = remappedGravity[0]
            val gy = remappedGravity[1]
            val gz = remappedGravity[2]

            val rawRoll = Math.toDegrees(
                atan2(gx.toDouble(), sqrt((gy * gy + gz * gz).toDouble()))
            ).toFloat()

            currentAngle = lowPassFilter(rawRoll, currentAngle)
            angleCallback?.invoke(currentAngle)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    actual fun start(onAngleChanged: (Float) -> Unit) {
        angleCallback = onAngleChanged
        gravitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    actual fun stop() {
        sensorManager.unregisterListener(listener)
        angleCallback = null
    }

    private fun getDeviceRotation(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        return windowManager.defaultDisplay?.rotation ?: Surface.ROTATION_0
    }

    private fun remapGravityByRotation(gravity: FloatArray, rotation: Int): FloatArray {
        val remapped = FloatArray(3)

        when (rotation) {
            Surface.ROTATION_0 -> {
                remapped[0] = gravity[0]
                remapped[1] = gravity[1]
                remapped[2] = gravity[2]
            }
            Surface.ROTATION_90 -> {
                remapped[0] = -gravity[1]
                remapped[1] = gravity[0]
                remapped[2] = gravity[2]
            }
            Surface.ROTATION_180 -> {
                remapped[0] = -gravity[0]
                remapped[1] = -gravity[1]
                remapped[2] = gravity[2]
            }
            Surface.ROTATION_270 -> {
                remapped[0] = gravity[1]
                remapped[1] = -gravity[0]
                remapped[2] = gravity[2]
            }
        }

        return remapped
    }
}

private fun lowPassFilter(input: Float, output: Float): Float {
    return output + 0.5f * (input - output)
}
