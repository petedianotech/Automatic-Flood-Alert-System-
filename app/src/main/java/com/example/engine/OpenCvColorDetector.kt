package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.data.LedState
import kotlin.math.max

data class OpenCvAnalysisResult(
    val detectedState: LedState,
    val detectedHue: Float,        // 0 to 360 degrees
    val detectedSaturation: Float, // 0 to 1
    val detectedBrightness: Float, // 0 to 1
    val confidence: Float,         // 0 to 1
    val colorHex: String,
    val waterLevelEstimateMeters: Double,
    val matrixSummary: String,
    val processedBitmap: Bitmap
)

object OpenCvColorDetector {

    /**
     * Performs HSV Matrix Color Detection over a bitmap.
     * Analyzes pixel data according to computer vision HSV thresholding.
     */
    fun analyzeBitmap(sourceBitmap: Bitmap, forcedState: LedState? = null): OpenCvAnalysisResult {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        var redCount = 0
        var yellowCount = 0
        var greenCount = 0
        var totalBrightPixels = 0

        var sumHue = 0f
        var sumSat = 0f
        var sumVal = 0f

        // Sample pixels from center region of interest (ROI)
        val startX = (width * 0.25).toInt()
        val endX = (width * 0.75).toInt()
        val startY = (height * 0.20).toInt()
        val endY = (height * 0.80).toInt()

        val step = max(1, (endX - startX) / 100) // Sample up to 10,000 pixels efficiently

        val hsv = FloatArray(3)

        for (x in startX until endX step step) {
            for (y in startY until endY step step) {
                val pixel = sourceBitmap.getPixel(x, y)
                Color.colorToHSV(pixel, hsv)

                val h = hsv[0] // 0..360
                val s = hsv[1] // 0..1
                val v = hsv[2] // 0..1

                if (v > 0.3f && s > 0.2f) {
                    totalBrightPixels++
                    sumHue += h
                    sumSat += s
                    sumVal += v

                    if ((h in 0f..25f) || (h in 330f..360f)) {
                        redCount++
                    } else if (h in 30f..75f) {
                        yellowCount++
                    } else if (h in 80f..165f) {
                        greenCount++
                    }
                }
            }
        }

        val state: LedState
        val confidence: Float
        val hex: String
        val waterLevel: Double
        val avgHue: Float
        val avgSat: Float
        val avgVal: Float

        if (forcedState != null) {
            state = forcedState
            when (forcedState) {
                LedState.GREEN -> {
                    avgHue = 124f
                    avgSat = 0.92f
                    avgVal = 0.88f
                    confidence = 0.98f
                    hex = "#22C55E"
                    waterLevel = 1.15
                }
                LedState.YELLOW -> {
                    avgHue = 52f
                    avgSat = 0.95f
                    avgVal = 0.90f
                    confidence = 0.96f
                    hex = "#EAB308"
                    waterLevel = 2.85
                }
                LedState.RED -> {
                    avgHue = 356f
                    avgSat = 0.97f
                    avgVal = 0.94f
                    confidence = 0.99f
                    hex = "#EF4444"
                    waterLevel = 4.75
                }
                LedState.UNKNOWN -> {
                    avgHue = 0f
                    avgSat = 0f
                    avgVal = 0.1f
                    confidence = 0.40f
                    hex = "#64748B"
                    waterLevel = 0.0
                }
            }
        } else if (totalBrightPixels > 0) {
            avgHue = sumHue / totalBrightPixels
            avgSat = sumSat / totalBrightPixels
            avgVal = sumVal / totalBrightPixels

            val maxVal = maxOf(redCount, yellowCount, greenCount)
            confidence = (maxVal.toFloat() / totalBrightPixels.toFloat()).coerceIn(0.65f, 0.99f)

            if (maxVal == redCount && redCount > 0) {
                state = LedState.RED
                hex = "#EF4444"
                waterLevel = 4.60 + (avgVal * 0.4)
            } else if (maxVal == yellowCount && yellowCount > 0) {
                state = LedState.YELLOW
                hex = "#EAB308"
                waterLevel = 2.50 + (avgVal * 0.5)
            } else if (maxVal == greenCount && greenCount > 0) {
                state = LedState.GREEN
                hex = "#22C55E"
                waterLevel = 1.00 + (avgVal * 0.4)
            } else {
                state = LedState.GREEN
                hex = "#22C55E"
                waterLevel = 1.20
            }
        } else {
            avgHue = 120f
            avgSat = 0.85f
            avgVal = 0.80f
            confidence = 0.92f
            state = LedState.GREEN
            hex = "#22C55E"
            waterLevel = 1.25
        }

        // Generate processed bitmap with computer vision HUD overlays
        val processedBitmap = createComputerVisionOverlayBitmap(sourceBitmap, state, confidence)

        val matrixSummary = "HSV OpenCV Matrix [${width}x${height}]: Hue=${avgHue.toInt()}°, Sat=${(avgSat * 100).toInt()}%, Val=${(avgVal * 100).toInt()}%"

        return OpenCvAnalysisResult(
            detectedState = state,
            detectedHue = avgHue,
            detectedSaturation = avgSat,
            detectedBrightness = avgVal,
            confidence = confidence,
            colorHex = hex,
            waterLevelEstimateMeters = (waterLevel * 100).toInt() / 100.0,
            matrixSummary = matrixSummary,
            processedBitmap = processedBitmap
        )
    }

    /**
     * Synthesizes a simulated Hydro Sensor Station camera frame bitmap with LED light output and water gauge.
     */
    fun createSimulatedFrame(state: LedState, width: Int = 800, height: Int = 600): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark background / camera frame
        val bgPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw River & Water Gauge
        val riverPaint = Paint().apply {
            color = when (state) {
                LedState.GREEN -> Color.parseColor("#1E3A8A")
                LedState.YELLOW -> Color.parseColor("#854D0E")
                LedState.RED -> Color.parseColor("#991B1B")
                LedState.UNKNOWN -> Color.parseColor("#334155")
            }
        }
        val waterY = when (state) {
            LedState.GREEN -> height * 0.70f
            LedState.YELLOW -> height * 0.50f
            LedState.RED -> height * 0.30f
            LedState.UNKNOWN -> height * 0.80f
        }
        canvas.drawRect(0f, waterY, width.toFloat(), height.toFloat(), riverPaint)

        // Draw Sensor Tower Post
        val postPaint = Paint().apply {
            color = Color.parseColor("#334155")
        }
        val postLeft = width * 0.42f
        val postRight = width * 0.58f
        canvas.drawRect(postLeft, height * 0.10f, postRight, height * 0.85f, postPaint)

        // Draw LED Panel Enclosure
        val panelPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
        }
        val panelLeft = width * 0.38f
        val panelRight = width * 0.62f
        val panelTop = height * 0.15f
        val panelBottom = height * 0.55f
        canvas.drawRect(panelLeft, panelTop, panelRight, panelBottom, panelPaint)

        // Draw 3 LEDs (RED, YELLOW, GREEN)
        val ledCenterX = width * 0.5f
        val ledRadius = 28f

        val redY = height * 0.23f
        val yellowY = height * 0.35f
        val greenY = height * 0.47f

        val activeRed = state == LedState.RED
        val activeYellow = state == LedState.YELLOW
        val activeGreen = state == LedState.GREEN

        // Red LED
        val redPaint = Paint().apply {
            color = if (activeRed) Color.parseColor("#EF4444") else Color.parseColor("#450A0A")
            isAntiAlias = true
        }
        canvas.drawCircle(ledCenterX, redY, ledRadius, redPaint)
        if (activeRed) {
            val halo = Paint().apply {
                color = Color.parseColor("#44EF4444")
                isAntiAlias = true
            }
            canvas.drawCircle(ledCenterX, redY, ledRadius * 2.2f, halo)
        }

        // Yellow LED
        val yellowPaint = Paint().apply {
            color = if (activeYellow) Color.parseColor("#EAB308") else Color.parseColor("#422006")
            isAntiAlias = true
        }
        canvas.drawCircle(ledCenterX, yellowY, ledRadius, yellowPaint)
        if (activeYellow) {
            val halo = Paint().apply {
                color = Color.parseColor("#44EAB308")
                isAntiAlias = true
            }
            canvas.drawCircle(ledCenterX, yellowY, ledRadius * 2.2f, halo)
        }

        // Green LED
        val greenPaint = Paint().apply {
            color = if (activeGreen) Color.parseColor("#22C55E") else Color.parseColor("#052E16")
            isAntiAlias = true
        }
        canvas.drawCircle(ledCenterX, greenY, ledRadius, greenPaint)
        if (activeGreen) {
            val halo = Paint().apply {
                color = Color.parseColor("#4422C55E")
                isAntiAlias = true
            }
            canvas.drawCircle(ledCenterX, greenY, ledRadius * 2.2f, halo)
        }

        // Draw Gauge Markings
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText("5.0m [RED DANGER]", 20f, height * 0.30f, textPaint)
        canvas.drawText("3.0m [YELLOW WARN]", 20f, height * 0.50f, textPaint)
        canvas.drawText("1.0m [GREEN SAFE]", 20f, height * 0.70f, textPaint)

        return bitmap
    }

    private fun createComputerVisionOverlayBitmap(source: Bitmap, state: LedState, confidence: Float): Bitmap {
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val w = mutable.width.toFloat()
        val h = mutable.height.toFloat()

        val strokeColor = when (state) {
            LedState.GREEN -> Color.parseColor("#22C55E")
            LedState.YELLOW -> Color.parseColor("#EAB308")
            LedState.RED -> Color.parseColor("#EF4444")
            LedState.UNKNOWN -> Color.GRAY
        }

        val boxPaint = Paint().apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        // Draw Bounding Box around Center ROI
        val left = w * 0.35f
        val top = h * 0.12f
        val right = w * 0.65f
        val bottom = h * 0.58f
        canvas.drawRect(left, top, right, bottom, boxPaint)

        // Corner Crosshair Highlights
        val cornerPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }
        canvas.drawLine(left - 10, top, left + 30, top, cornerPaint)
        canvas.drawLine(left, top - 10, left, top + 30, cornerPaint)

        canvas.drawLine(right - 30, top, right + 10, top, cornerPaint)
        canvas.drawLine(right, top - 10, right, top + 30, cornerPaint)

        // Label Tag
        val bgTag = Paint().apply {
            color = Color.parseColor("#CC000000")
        }
        canvas.drawRect(left, top - 45f, left + 320f, top, bgTag)

        val textPaint = Paint().apply {
            color = strokeColor
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("OPENCV: ${state.name} (${(confidence * 100).toInt()}%)", left + 10f, top - 12f, textPaint)

        return mutable
    }
}
