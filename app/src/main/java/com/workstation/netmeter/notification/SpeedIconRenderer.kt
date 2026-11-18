package com.workstation.netmeter.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import com.workstation.netmeter.util.SpeedFormatter

/**
 * Renders the live status-bar small icon.
 *
 * A single bitmap, canvas and paint are allocated once and reused for every
 * tick, so the 1 Hz update loop performs no steady-state allocations. The
 * bitmap is monochrome: the system only uses the alpha channel of a small
 * notification icon, so the paint is white on a transparent canvas.
 *
 * The text is auto-shrunk when it would otherwise overflow the canvas, which
 * keeps readings such as "↓999K" or "↓1.2M" legible instead of being cropped.
 */
class SpeedIconRenderer {

    private val bitmap: Bitmap =
        Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
    private val canvas: Canvas = Canvas(bitmap)
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isFakeBoldText = true
    }

    /**
     * Redraws the reused bitmap.
     *
     * @param showUpload when true the icon carries two lines (upload top,
     *   download bottom); when false only the download speed is shown.
     */
    fun render(downloadBps: Long, uploadBps: Long, showUpload: Boolean): Bitmap {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        if (showUpload) {
            drawLine("↑" + SpeedFormatter.formatCompact(uploadBps), UP_BASELINE_PX, LINE_TEXT_SIZE_PX, MAX_TEXT_WIDTH_PX)
            drawLine("↓" + SpeedFormatter.formatCompact(downloadBps), DOWN_BASELINE_PX, LINE_TEXT_SIZE_PX, MAX_TEXT_WIDTH_PX)
        } else {
            drawLine(
                "↓" + SpeedFormatter.formatCompact(downloadBps),
                CENTER_BASELINE_PX,
                SINGLE_TEXT_SIZE_PX,
                SINGLE_MAX_TEXT_WIDTH_PX,
            )
        }
        return bitmap
    }

    private fun drawLine(
        text: String,
        baselinePx: Float,
        initialTextSizePx: Float,
        maxTextWidthPx: Float,
    ) {
        paint.textSize = initialTextSizePx
        val measured = paint.measureText(text)
        if (measured > maxTextWidthPx && measured > 0f) {
            paint.textSize = initialTextSizePx * (maxTextWidthPx / measured)
        }
        canvas.drawText(text, SIZE_PX / 2f, baselinePx, paint)
    }

    private companion object {
        /** Matches the 24 dp status-bar icon at xxxhdpi (4x). */
        const val SIZE_PX = 96

        const val SINGLE_TEXT_SIZE_PX = 72f
        const val LINE_TEXT_SIZE_PX = 40f

        const val UP_BASELINE_PX = 40f
        const val DOWN_BASELINE_PX = 90f
        const val CENTER_BASELINE_PX = 70f

        const val MAX_TEXT_WIDTH_PX = 88f
        const val SINGLE_MAX_TEXT_WIDTH_PX = 92f
    }
}
