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
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(36), dp(24), dp(24))
        }

        val title = TextView(this).apply {
            text = "DualStick Overlay"
            textSize = 28f
            setTextColor(0xFF171717.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "Experimental floating touch-control prototype"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(22))
        }
        root.addView(subtitle, matchWrap())

        val privacyNote = TextView(this).apply {
            text = "Accessibility is used only to display the floating controls and send gestures when you touch them. This app does not retrieve screen contents or send your data to a server. Android may still show a security warning because gesture access is sensitive."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(18))
        }
        root.addView(privacyNote, matchWrap())

        statusText = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(24))
        }
        root.addView(statusText, matchWrap())

        val settingsButton = Button(this).apply {
            text = "Review Accessibility Permission"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Sensitive permission")
                    .setMessage(
                        "DualStick Overlay needs Android Accessibility gesture access to send touch gestures when you use its floating controls. It does not request permission to read screen contents. Only continue if you understand and trust this experimental app."
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Open settings") { _, _ ->
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .show()
            }
        }
        root.addView(settingsButton, matchWrap())

        val refreshButton = Button(this).apply {
            text = "Check Connection"
            setOnClickListener { refreshStatus() }
        }
        root.addView(refreshButton, matchWrap())

        val note = TextView(this).apply {
            text = "To remove the floating controls, turn this service off in Android Accessibility settings. The joystick input is experimental and may not work correctly in every game."
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        }
        root.addView(note, matchWrap())

        setContentView(root)
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) refreshStatus()
    }

    private fun refreshStatus() {
        val enabled = isAccessibilityServiceEnabled()
        statusText.text = if (enabled) {
            "Accessibility service: ENABLED"
        } else {
            "Accessibility service: NOT ENABLED"
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
