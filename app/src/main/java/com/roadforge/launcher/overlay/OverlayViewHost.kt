package com.roadforge.launcher.overlay

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.roadforge.launcher.data.InputMapping
import com.roadforge.launcher.data.TouchButton
import com.roadforge.launcher.data.TouchLayout
import com.roadforge.launcher.data.ButtonType

/**
 * Custom View วาด Touch Overlay ด้วย Canvas — ไม่ใช้ Compose
 * เพื่อประสิทธิภาพสูงสุดและรองรับ multi-touch ถูกต้อง
 *
 * ตำแหน่งปุ่มเก็บเป็น fraction ของหน้าจอ → scale อัตโนมัติทุก resolution
 */
class OverlayViewHost(
    context: Context,
    private val layout: TouchLayout,
    private val onInput: (InputMapping, Boolean) -> Unit,
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    // pointer ID → button ID ที่กดอยู่
    private val activePointers = mutableMapOf<Int, String>()

    // RectF ของแต่ละปุ่มในพิกเซลจริง — คำนวณเมื่อ layout เปลี่ยนหรือ size เปลี่ยน
    private val buttonRects = mutableMapOf<String, RectF>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalcButtonRects(w.toFloat(), h.toFloat())
    }

    private fun recalcButtonRects(w: Float, h: Float) {
        buttonRects.clear()
        for (btn in layout.buttons) {
            if (!btn.isVisible) continue
            buttonRects[btn.id] = RectF(
                btn.xFraction * w,
                btn.yFraction * h,
                (btn.xFraction + btn.widthFraction) * w,
                (btn.yFraction + btn.heightFraction) * h,
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (btn in layout.buttons) {
            if (!btn.isVisible) continue
            val rect = buttonRects[btn.id] ?: continue
            drawButton(canvas, btn, rect)
        }
    }

    private fun drawButton(canvas: Canvas, btn: TouchButton, rect: RectF) {
        val isPressed = activePointers.values.contains(btn.id)
        val radius = minOf(rect.width(), rect.height()) / 2f

        // background
        paint.color = if (isPressed)
            Color.argb((btn.alpha * 255 * 1.3f).toInt().coerceAtMost(255), 60, 60, 60)
        else
            Color.argb((btn.alpha * 255).toInt(), 26, 26, 26)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, radius * 0.5f, radius * 0.5f, paint)

        // border
        paint.color = Color.argb(100, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(rect, radius * 0.5f, radius * 0.5f, paint)
        paint.style = Paint.Style.FILL

        // label
        val cx = rect.centerX()
        val cy = rect.centerY()
        val textSize = minOf(rect.width(), rect.height()) * 0.30f
        textPaint.textSize = textSize.coerceIn(10f, 24f)
        canvas.drawText(btn.label.take(3), cx, cy + textPaint.textSize / 3f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val x = event.getX(idx)
                val y = event.getY(idx)
                hitTest(x, y)?.let { btn ->
                    activePointers[pid] = btn.id
                    btn.inputMapping.let { onInput(it, true) }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                activePointers.remove(pid)?.let { btnId ->
                    layout.buttons.find { it.id == btnId }?.let { btn ->
                        onInput(btn.inputMapping, false)
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // ตรวจสอบ pointer ที่ขยับออกนอกปุ่ม
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val btnId = activePointers[pid] ?: continue
                    val rect = buttonRects[btnId] ?: continue
                    if (!rect.contains(event.getX(i), event.getY(i))) {
                        activePointers.remove(pid)
                        layout.buttons.find { it.id == btnId }?.let { btn ->
                            onInput(btn.inputMapping, false)
                        }
                        invalidate()
                    }
                }
            }
        }
        return true
    }

    private fun hitTest(x: Float, y: Float): TouchButton? {
        for (btn in layout.buttons) {
            if (!btn.isVisible) continue
            val rect = buttonRects[btn.id] ?: continue
            if (rect.contains(x, y)) return btn
        }
        return null
    }
}
