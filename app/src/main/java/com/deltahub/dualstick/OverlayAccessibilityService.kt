package com.deltahub.dualstick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.min

class OverlayAccessibilityService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private val overlayViews = mutableListOf<View>()
    private var lastLeftDispatch = 0L
    private var lastRightDispatch = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showControls()
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        removeControls()
    }

    override fun onDestroy() {
        removeControls()
        super.onDestroy()
    }

    private fun showControls() {
        if (overlayViews.isNotEmpty()) return

        val leftPad = JoystickPad(this, "MOVE") { x, y, active ->
            if (active) dispatchStickGesture(left = true, x = x, y = y)
        }
        addOverlay(
            leftPad,
            WindowManager.LayoutParams(
                dp(154), dp(154),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.LEFT
                x = dp(18)
                y = dp(26)
            }
        )

        val rightPad = JoystickPad(this, "CAMERA") { x, y, active ->
            if (active) dispatchStickGesture(left = false, x = x, y = y)
        }
        addOverlay(
            rightPad,
            WindowManager.LayoutParams(
                dp(154), dp(154),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.RIGHT
                x = dp(18)
                y = dp(26)
            }
        )

        val jump = TextView(this).apply {
            text = "JUMP"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(220, 55, 55))
                setStroke(dp(2), Color.WHITE)
            }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) dispatchJumpTap()
                true
            }
        }
        addOverlay(
            jump,
            WindowManager.LayoutParams(
                dp(76), dp(76),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.RIGHT
                x = dp(44)
                y = dp(198)
            }
        )
    }

    private fun addOverlay(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.addView(view, params)
            overlayViews.add(view)
        } catch (_: Exception) {
            // If the system refuses an overlay, leave the other controls available.
        }
    }

    private fun removeControls() {
        if (!::windowManager.isInitialized) return
        overlayViews.toList().forEach { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // Ignore views already detached by the system.
            }
        }
        overlayViews.clear()
    }

    private fun dispatchStickGesture(left: Boolean, x: Float, y: Float) {
        val now = System.currentTimeMillis()
        if (left && now - lastLeftDispatch < 65L) return
        if (!left && now - lastRightDispatch < 45L) return
        if (left) lastLeftDispatch = now else lastRightDispatch = now

        val metrics = resources.displayMetrics
        val startX = if (left) dp(105).toFloat() else metrics.widthPixels - dp(105).toFloat()
        val startY = metrics.heightPixels - dp(135).toFloat()
        val distance = if (left) dp(48).toFloat() else dp(72).toFloat()
        val endX = (startX + x * distance).coerceIn(2f, metrics.widthPixels - 2f)
        val endY = (startY + y * distance).coerceIn(2f, metrics.heightPixels - 2f)

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 55)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun dispatchJumpTap() {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels - dp(78).toFloat()
        val y = metrics.heightPixels - dp(205).toFloat()
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + 1f, y + 1f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 70))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private class JoystickPad(
        context: android.content.Context,
        private val label: String,
        private val onMove: (Float, Float, Boolean) -> Unit
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x88303030.toInt()
        }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFDF4545.toInt()
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 11f * density
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
        }
        private var pointerId = MotionEvent.INVALID_POINTER_ID
        private var knobX = 0f
        private var knobY = 0f
        private var pressed = false

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = min(width, height) * 0.43f
            val knobRadius = radius * 0.38f

            canvas.drawCircle(cx, cy, radius, backgroundPaint)
            canvas.drawCircle(cx, cy, radius, ringPaint)
            val drawX = if (pressed) knobX else cx
            val drawY = if (pressed) knobY else cy
            canvas.drawCircle(drawX, drawY, knobRadius, knobPaint)
            canvas.drawText(label, cx, height - 7f * density, textPaint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pointerId = event.getPointerId(0)
                    pressed = true
                    updateFrom(event, 0)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val index = event.findPointerIndex(pointerId)
                    if (index >= 0) updateFrom(event, index)
                    return true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val index = event.actionIndex
                    if (event.getPointerId(index) == pointerId) release()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    release()
                    return true
                }
            }
            return true
        }

        private fun updateFrom(event: MotionEvent, index: Int) {
            val cx = width / 2f
            val cy = height / 2f
            val maxRadius = min(width, height) * 0.32f
            var dx = event.getX(index) - cx
            var dy = event.getY(index) - cy
            val length = kotlin.math.sqrt(dx * dx + dy * dy)
            if (length > maxRadius) {
                val scale = maxRadius / length
                dx *= scale
                dy *= scale
            }
            knobX = cx + dx
            knobY = cy + dy
            invalidate()
            onMove(dx / maxRadius, dy / maxRadius, true)
        }

        private fun release() {
            pressed = false
            pointerId = MotionEvent.INVALID_POINTER_ID
            knobX = width / 2f
            knobY = height / 2f
            invalidate()
            onMove(0f, 0f, false)
        }
    }
}
