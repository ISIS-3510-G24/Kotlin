package com.example.unimarket.ui.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

@Composable
fun ShakeDetector(
    onShake: () -> Unit
) {
    val context = LocalContext.current
    // Get the SensorManager from the context
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // Get the default accelerometer sensor
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    // Updated threshold for shake detection (increased for lower sensitivity)
    val shakeThreshold = 16f
    // Minimum time between shake events in milliseconds
    val shakeTimeWindow = 500L
    val lastShakeTime = remember { mutableStateOf(0L) }

    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* No action */ }
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]
                    // Calculate the net acceleration
                    val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
                    if (acceleration > shakeThreshold) {
                        val now = System.currentTimeMillis()
                        if (now - lastShakeTime.value > shakeTimeWindow) {
                            lastShakeTime.value = now
                            onShake() // Trigger the shake callback
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(sensorManager, accelerometer) {
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
}
