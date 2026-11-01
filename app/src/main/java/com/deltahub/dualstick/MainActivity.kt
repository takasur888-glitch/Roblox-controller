package com.deltahub.dualstick

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var speedButton: Button

    private val speedNames = arrayOf("Slow", "Normal", "Fast")
    private val speedValues = floatArrayOf(0.55f, 1.0f, 1.7f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(30), dp(24), dp(24))
        }

        val title = TextView(this).apply {
            text = "DualStick Overlay"
            textSize = 28f
            setTextColor(0xFF171717.toInt())
            gravity = Gravity.CENTER
        }
        content.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "Two floating controls for Roblox mobile"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(18))
        }
        content.addView(subtitle, matchWrap())

        val privacyNote = TextView(this).apply {
            text = "This app uses Android Accessibility only to show its floating controls and send touch gestures when you use them. It does not read screen contents, request internet access, or upload your data. Gesture access is sensitive, so enable it only if you trust this app."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(18))
        }
        content.addView(privacyNote, matchWrap())

        statusText = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(18))
        }
        content.addView(statusText, matchWrap())

        val settingsButton = Button(this).apply {
            text = "Enable floating controls"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Sensitive permission")
                    .setMessage(
                        "DualStick needs Accessibility gesture access to control touch points for the floating MOVE, CAMERA, and JUMP controls. It does not request permission to read screen contents. Only enable it if you understand and trust this experimental app."
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Open settings") { _, _ ->
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .show()
            }
        }
        content.addView(settingsButton, matchWrap())

        speedButton = Button(this).apply {
            setOnClickListener { showCameraSpeedDialog() }
        }
        content.addView(speedButton, matchWrap())

        val calibrationButton = Button(this).apply {
            text = "Calibrate touch positions"
            setOnClickListener { showCalibrationDialog() }
        }
        content.addView(calibrationButton, matchWrap())

        val refreshButton = Button(this).apply {
            text = "Check connection"
            setOnClickListener { refreshStatus() }
        }
        content.addView(refreshButton, matchWrap())

        val instructions = TextView(this).apply {
            text = "How to use\n1. Enable the service above.\n2. Open Roblox and enter a game using Fixed Thumbstick.\n3. Use MOVE on the left and CAMERA on the right at the same time.\n4. Use Camera speed and Calibrate touch positions in this app to adjust the controls.\n5. JUMP taps the calibrated jump area.\n6. Tap STOP at the top of the screen to release all controls immediately.\n\nStart with the default positions, then calibrate if a touch misses its Roblox button. Test in a safe game area first."
            textSize = 14f
            gravity = Gravity.START
            setPadding(0, dp(20), 0, dp(12))
        }
        content.addView(instructions, matchWrap())

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
        setContentView(scroll)
        refreshStatus()
        refreshSpeedButton()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) refreshStatus()
    }

    private fun showCameraSpeedDialog() {
        val prefs = getSharedPreferences("dualstick", MODE_PRIVATE)
        val current = prefs.getFloat("camera_speed", 1.0f)
        val selected = speedValues.indices.minByOrNull {
            kotlin.math.abs(speedValues[it] - current)
        } ?: 1

        AlertDialog.Builder(this)
            .setTitle("Camera drag speed")
            .setSingleChoiceItems(speedNames, selected) { dialog, which ->
                prefs.edit().putFloat("camera_speed", speedValues[which]).apply()
                refreshSpeedButton()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCalibrationDialog() {
        val prefs = getSharedPreferences("dualstick", MODE_PRIVATE)
        val values = linkedMapOf<String, Float>()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        fun addPosition(title: String, xKey: String, yKey: String, defaultX: Float, defaultY: Float) {
            val heading = TextView(this).apply {
                text = title
                textSize = 16f
                setTextColor(0xFF202020.toInt())
                setPadding(0, dp(14), 0, dp(4))
            }
            panel.addView(heading, matchWrap())
            addCoordinateSlider(panel, "Horizontal position", xKey, defaultX, values, prefs)
            addCoordinateSlider(panel, "Vertical position", yKey, defaultY, values, prefs)
        }

        addPosition("MOVE touch point", "move_x_ratio", "move_y_ratio", 0.115f, 0.67f)
        addPosition("CAMERA touch point", "camera_x_ratio", "camera_y_ratio", 0.68f, 0.63f)
        addPosition("JUMP touch point", "jump_x_ratio", "jump_y_ratio", 0.91f, 0.76f)

        val scroll = ScrollView(this).apply {
            addView(panel)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(440)
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Calibrate touch positions")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Reset defaults") { _, _ ->
                prefs.edit()
                    .putFloat("move_x_ratio", 0.115f)
                    .putFloat("move_y_ratio", 0.67f)
                    .putFloat("camera_x_ratio", 0.68f)
                    .putFloat("camera_y_ratio", 0.63f)
                    .putFloat("jump_x_ratio", 0.91f)
                    .putFloat("jump_y_ratio", 0.76f)
                    .apply()
            }
            .setPositiveButton("Save positions") { _, _ ->
                val editor = prefs.edit()
                values.forEach { (key, value) -> editor.putFloat(key, value) }
                editor.apply()
            }
            .show()
    }

    private fun addCoordinateSlider(
        panel: LinearLayout,
        title: String,
        key: String,
        defaultValue: Float,
        values: MutableMap<String, Float>,
        prefs: android.content.SharedPreferences
    ) {
        val label = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF333333.toInt())
        }
        val initial = (prefs.getFloat(key, defaultValue) * 100f).toInt().coerceIn(0, 100)
        values[key] = initial / 100f
        label.text = "$title: $initial%"

        val slider = SeekBar(this).apply {
            max = 100
            progress = initial
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    values[key] = progress / 100f
                    label.text = "$title: $progress%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        panel.addView(label, matchWrap())
        panel.addView(slider, matchWrap())
    }

    private fun refreshSpeedButton() {
        val current = getSharedPreferences("dualstick", MODE_PRIVATE)
            .getFloat("camera_speed", 1.0f)
        val index = speedValues.indices.minByOrNull {
            kotlin.math.abs(speedValues[it] - current)
        } ?: 1
        speedButton.text = "Camera speed: ${speedNames[index]}"
    }

    private fun refreshStatus() {
        val enabled = isAccessibilityServiceEnabled()
        statusText.text = if (enabled) {
            "Floating controls service: ENABLED"
        } else {
            "Floating controls service: NOT ENABLED"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = ComponentName(this, OverlayAccessibilityService::class.java)
            .flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
