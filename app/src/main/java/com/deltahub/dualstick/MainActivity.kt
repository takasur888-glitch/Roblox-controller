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

        val refreshButton = Button(this).apply {
            text = "Check connection"
            setOnClickListener { refreshStatus() }
        }
        content.addView(refreshButton, matchWrap())

        val instructions = TextView(this).apply {
            text = "How to use\n1. Enable the service above.\n2. Open Roblox and enter a game using Fixed Thumbstick.\n3. Use MOVE on the left and CAMERA on the right at the same time.\n4. JUMP taps the usual lower-right jump area.\n5. Tap STOP at the top of the screen to release all controls immediately.\n\nThe touch positions are initial defaults, not universal settings. Test in a safe game area first. If the controls do not land correctly, tap STOP."
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
