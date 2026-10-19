package com.hiennv.flutter_callkit_incoming

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * WhatsApp-style caller avatar: the caller picture in a circle, with the app
 * icon (Wimi / Kroqi, per flavor) in a white ring at the bottom-right.
 */
object CallerAvatarWimi {
    private const val TAG = "CallerAvatarWimi"

    /** Badge radius, as a fraction of the output size. */
    private const val BADGE_RATIO = 0.17f

    /** White ring width, as a fraction of the output size. */
    private const val RING_RATIO = 0.025f

    /** Launcher icon of the host app, [sizePx] square. Null if unavailable. */
    fun appIcon(context: Context, sizePx: Int): Bitmap? = try {
        drawableToBitmap(context.packageManager.getApplicationIcon(context.packageName), sizePx)
    } catch (e: Exception) {
        Log.w(TAG, "app icon unavailable", e)
        null
    }

    /**
     * [avatar] (or the default avatar when null) cropped to a circle filling
     * a [sizePx] square, with the app icon badge on top.
     */
    fun withAppBadge(context: Context, avatar: Bitmap?, sizePx: Int): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val size = sizePx.toFloat()

        val picture = avatar ?: defaultAvatar(context, sizePx)
        if (picture != null) drawCircle(canvas, picture, size / 2f, size / 2f, size / 2f)

        val badgeRadius = size * BADGE_RATIO
        val center = size - badgeRadius
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(center, center, badgeRadius, ring)
        val iconRadius = badgeRadius - size * RING_RATIO
        appIcon(context, (iconRadius * 2).toInt().coerceAtLeast(1))?.let {
            drawCircle(canvas, it, center, center, iconRadius)
        }
        return out
    }

    /** The app icon cropped to a circle, for the badge view of the call screen. */
    fun circularAppIcon(context: Context, sizePx: Int): Bitmap? {
        val icon = appIcon(context, sizePx) ?: return null
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val half = sizePx / 2f
        drawCircle(Canvas(out), icon, half, half, half)
        return out
    }

    private fun defaultAvatar(context: Context, sizePx: Int): Bitmap? =
        ContextCompat.getDrawable(context, R.drawable.ic_default_avatar)
            ?.let { drawableToBitmap(it, sizePx) }

    /** Draws [bitmap] center-cropped into an anti-aliased circle. */
    private fun drawCircle(canvas: Canvas, bitmap: Bitmap, cx: Float, cy: Float, radius: Float) {
        val diameter = radius * 2f
        val scale = diameter / minOf(bitmap.width, bitmap.height)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(
                cx - bitmap.width * scale / 2f,
                cy - bitmap.height * scale / 2f,
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                .apply { setLocalMatrix(matrix) }
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }
}
