package com.fieldbook.shared.screens.collect.traits

expect class PlatformAngleController() {
    fun start(onAngleChanged: (Float) -> Unit)
    fun stop()
}
