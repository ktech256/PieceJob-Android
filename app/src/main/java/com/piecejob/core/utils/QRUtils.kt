package com.piecejob.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QRUtils {
    fun generateQRCode(
        text: String, 
        size: Int = 512, 
        brandingType: String = "NONE",
        primaryColor: Int = Color.BLACK
    ): Bitmap? {
        val bitMatrix: BitMatrix
        try {
            bitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size,
                null
            )
        } catch (e: Exception) {
            return null
        }

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) primaryColor else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        
        if (brandingType != "NONE") {
            return overlayLogo(bitmap, brandingType, primaryColor)
        }
        
        return bitmap
    }

    private fun overlayLogo(qrBitmap: Bitmap, brandingType: String, primaryColor: Int): Bitmap {
        val result = Bitmap.createBitmap(qrBitmap.width, qrBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)

        val logoSize = qrBitmap.width / 4
        val left = (qrBitmap.width - logoSize) / 2f
        val top = (qrBitmap.height - logoSize) / 2f
        val rect = RectF(left, top, left + logoSize, top + logoSize)

        // Draw background for logo to make it readable
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 20f, 20f, bgPaint)

        // Draw Logo Text
        val logoText = when (brandingType) {
            "PIECEJOB" -> "PJ"
            "WORKSPACE" -> "WP" // Simple fallback
            else -> ""
        }

        if (logoText.isNotEmpty()) {
            val logoPaint = Paint().apply {
                color = primaryColor
                style = Paint.Style.FILL
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = logoSize * 0.6f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            
            val textHeight = logoPaint.descent() - logoPaint.ascent()
            val textOffset = textHeight / 2 - logoPaint.descent()
            canvas.drawText(logoText, rect.centerX(), rect.centerY() + textOffset, logoPaint)
        }

        return result
    }
}
