package com.deltahub.dualstick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.min

class OverlayAccessibilityService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private val overlayViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())

    private var leftActive = false
    private var rightActive = false
    private var leftX = 0f
    private var leftY = 0f
    private var rightX = 0f
    private var rightY = 0f
    private var pendingJump = false
    private var paused = false
    private var dispatching = false

    private val leftPointer = PointerTrack()
    private val rightPointer = PointerTrack()

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var leftBaseX = 0f
    private var leftBaseY = 0f
    private var rightBaseX = 0f
    private var rightBaseY = 0f

    private val segmentDuration = 70L

    private data class PointerTrack(
        var stroke: GestureDescription.StrokeDescription? = null,
        var x: Float = 0f,
        var y: Float = 0f
    )

    private data class BuiltPointer(
        val stroke: GestureDescription.StrokeDescription,
        val endX: Float,
        val endY: Float,
        val staysDown: Boolean
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        refreshScreenGeometry()
        showControls()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshScreenGeometry()
    }

    private fun refreshScreenGeometry() {
        val metrics = resources.displayMetrics
        val newWidth = metrics.widthPixels.toFloat()
        val newHeight = metrics.heightPixels.toFloat()
        if (newWidth == screenWidth && newHeight == screenHeight) return

        screenWidth = newWidth
        screenHeight = newHeight
        leftBaseX = dp(105).toFloat()
        leftBaseY = screenHeight - dp(135).toFloat()
        rightBaseX = screenWidth * 0.68f
        rightBaseY = screenHeight * 0.63f

        if (leftPointer.stroke == null) {
            leftPointer.x = leftBaseX
            leftPointer.y = leftBaseY
        }
        if (rightPointer.stroke == null) {
            rightPointer.x = rightBaseX
            rightPointer.y = rightBaseY
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        stopAllInputs()
        removeControls()
    }

    override fun onDestroy() {
        stopAllInputs()
        removeControls()
        super.onDestroy()
    }

    private fun showControls() {
        if (overlayViews.isNotEmpty()) return

        val leftPad = JoystickPad(this, "MOVE") { x, y, active ->
            leftActive = active && !paused
            leftX = x
            leftY = y
            pumpGestures()
        }
        addOverlay(
            leftPad,
            overlayParams(dp(154), dp(154), Gravity.BOTTOM or Gravity.LEFT).apply {
                x = dp(18)
                y = dp(26)
            }
        )

        val rightPad = JoystickPad(this, "CAMERA") { x, y, active ->
            rightActive = active && !paused
            rightX = x
            rightY = y
            pumpGestures()
        }
        addOverlay(
            rightPad,
            overlayParams(dp(154), dp(154), Gravity.BOTTOM or Gravity.RIGHT).apply {
                x = dp(18)
                y = dp(26)
            }
        )

        val jump = TextView(this).apply {
            text = "JUMP"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(220, 55, 55))
                setStroke(dp(2), Color.WHITE)
            }
            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN && !paused) {
                    pendingJump = true
                    pumpGestures()
                }
                true
            }
        }
        addOverlay(
            jump,
            overlayParams(dp(76), dp(76), Gravity.BOTTOM or Gravity.RIGHT).apply {
                x = dp(44)
                y = dp(198)
            }
        )

        val stop = TextView(this).apply {
            text = "STOP"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = roundedBackground(Color.rgb(185, 40, 40))
            setOnClickListener {
                paused = !paused
                if (paused) {
                    stopAllInputs()
                    text = "START"
                    background = roundedBackground(Color.rgb(35, 145, 75))
                } else {
                    text = "STOP"
                    background = roundedBackground(Color.rgb(185, 40, 40))
                }
            }
        }
        addOverlay(
            stop,
            overlayParams(dp(76), dp(42), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                y = dp(18)
            }
        )
    }

    private fun overlayParams(width: Int, height: Int, gravityValue: Int) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = gravityValue
        }

    private fun roundedBackground(color: Int) = GradientDrawable().apply {
        cornerRadius = dp(12).toFloat()
        setColor(color)
        setStroke(dp(1), Color.WHITE)
    }

    private fun addOverlay(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.addView(view, params)
            overlayViews.add(view)
        } catch (_: Exception) {
            // Keep any controls that the system did allow.
        }
    }

    private fun stopAllInputs() {
        paused = true
        leftActive = false
        rightActive = false
        pendingJump = false
        pumpGestures()
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

    /**
     * Sends both joystick pointers in ONE GestureDescription. Continued strokes
     * keep the pointers down while the next short segment is dispatched.
     * This avoids dispatching one joystick over the top of the other.
     */
    private fun pumpGestures() {
        if (dispatching || !::windowManager.isInitialized) return

        val hasWork = leftActive || rightActive || pendingJump ||
            leftPointer.stroke?.willContinue() == true ||
            rightPointer.stroke?.willContinue() == true
        if (!hasWork) return

        val leftTargetX = (leftBaseX + leftX * dp(48)).coerceIn(2f, screenWidth - 2f)
        val leftTargetY = (leftBaseY + leftY * dp(48)).coerceIn(2f, screenHeight - 2f)

        val leftBuilt = buildPointer(
            leftPointer, leftActive, leftBaseX, leftBaseY,
            leftTargetX, leftTargetY, leftActive
        )

        var rightEndX = rightPointer.x
        var rightEndY = rightPointer.y
        var rightStaysDown = rightActive
        if (rightActive) {
            val speed = getSharedPreferences("dualstick", MODE_PRIVATE)
                .getFloat("camera_speed", 1.0f)
            val step = dp(9).toFloat() * speed
            val rawX = (if (rightPointer.stroke == null) rightBaseX else rightPointer.x) +
                rightX * step
            val rawY = (if (rightPointer.stroke == null) rightBaseY else rightPointer.y) +
                rightY * step
            val minX = screenWidth * 0.36f
            val maxX = screenWidth * 0.94f
            val minY = screenHeight * 0.18f
            val maxY = screenHeight * 0.82f
            rightEndX = rawX.coerceIn(minX, maxX)
            rightEndY = rawY.coerceIn(minY, maxY)
            val reachedEdge = abs(rightEndX - rawX) > 0.5f || abs(rightEndY - rawY) > 0.5f
            rightStaysDown = !reachedEdge
        }

        val rightBuilt = buildPointer(
            rightPointer, rightActive, rightBaseX, rightBaseY,
            rightEndX, rightEndY, rightStaysDown
        )

        val builder = GestureDescription.Builder()
        if (leftBuilt != null) builder.addStroke(leftBuilt.stroke)
        if (rightBuilt != null) builder.addStroke(rightBuilt.stroke)

        val jumpAdded = pendingJump
        if (jumpAdded) {
            val jumpX = screenWidth - dp(78).toFloat()
            val jumpY = screenHeight - dp(205).toFloat()
            val jumpPath = Path().apply {
                moveTo(jumpX, jumpY)
                lineTo(jumpX + 1f, jumpY + 1f)
            }
            builder.addStroke(GestureDescription.StrokeDescription(jumpPath, 0, 65))
        }

        if (leftBuilt == null && rightBuilt == null && !jumpAdded) return

        dispatching = true
        pendingJump = false
        val accepted = dispatchGesture(
            builder.build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    applyBuiltPointer(leftPointer, leftBuilt, leftBaseX, leftBaseY)
                    applyBuiltPointer(rightPointer, rightBuilt, rightBaseX, rightBaseY)
                    dispatching = false
                    handler.post { pumpGestures() }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    resetPointer(leftPointer, leftBaseX, leftBaseY)
                    resetPointer(rightPointer, rightBaseX, rightBaseY)
                    dispatching = false
                    if (leftActive || rightActive || pendingJump) {
                        handler.postDelayed({ pumpGestures() }, 90L)
                    }
                }
            },
            handler
        )

        if (!accepted) {
            resetPointer(leftPointer, leftBaseX, leftBaseY)
            resetPointer(rightPointer, rightBaseX, rightBaseY)
            dispatching = false
        }
    }

    private fun buildPointer(
        track: PointerTrack,
        active: Boolean,
        baseX: Float,
        baseY: Float,
        targetX: Float,
        targetY: Float,
        staysDown: Boolean
    ): BuiltPointer? {
        val previous = track.stroke
        if (previous == null && !active) return null

        val startX = if (previous != null) track.x else baseX
        val startY = if (previous != null) track.y else baseY
        val path = Path().apply {
            moveTo(startX, startY)
            if (abs(targetX - startX) > 0.5f || abs(targetY - startY) > 0.5f) {
                lineTo(targetX, targetY)
            }
        }

        val stroke = if (previous != null && previous.willContinue()) {
            previous.continueStroke(path, 0, segmentDuration, staysDown)
        } else {
            GestureDescription.StrokeDescription(path, 0, segmentDuration, staysDown)
        }
        return BuiltPointer(stroke, targetX, targetY, staysDown)
    }

    private fun applyBuiltPointer(
        track: PointerTrack,
        built: BuiltPointer?,
        baseX: Float,
        baseY: Float
    ) {
        if (built == null) return
        track.stroke = built.stroke.takeIf { built.staysDown }
        if (built.staysDown) {
            track.x = built.endX
            track.y = built.endY
        } else {
            track.x = baseX
            track.y = baseY
        }
    }

    private fun resetPointer(track: PointerTrack, baseX: Float, baseY: Float) {
        track.stroke = null
        track.x = baseX
        track.y = baseY
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
