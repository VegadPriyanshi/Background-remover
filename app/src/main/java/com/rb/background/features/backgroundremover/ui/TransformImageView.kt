package com.rb.background.features.backgroundremover.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.sqrt

class TransformImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val imageMatrixValue = Matrix()

    private val startPoint = PointF()
    private val midPoint = PointF()

    private var oldDistance = 1f
    private var mode = NONE

    var isTransformEnabled: Boolean = true

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = imageMatrixValue
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (!isTransformEnabled) {
            return false
        }

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {

            if (!isTouchOnImage(event.x, event.y)) {
                return false
            }
        }

        // Your existing drag / zoom code continues here
        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                imageMatrixValue.set(imageMatrix)

                startPoint.set(
                    event.x,
                    event.y
                )

                mode = DRAG

                parent?.requestDisallowInterceptTouchEvent(true)

                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {

                oldDistance = spacing(event)

                if (oldDistance > 10f) {

                    midPoint(
                        midPoint,
                        event
                    )

                    imageMatrixValue.set(imageMatrix)

                    mode = ZOOM
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                when (mode) {

                    DRAG -> {

                        val dx = event.x - startPoint.x
                        val dy = event.y - startPoint.y

                        imageMatrixValue.set(imageMatrix)

                        imageMatrixValue.postTranslate(
                            dx,
                            dy
                        )

                        imageMatrix = imageMatrixValue

                        startPoint.set(
                            event.x,
                            event.y
                        )
                    }

                    ZOOM -> {

                        val newDistance = spacing(event)

                        if (newDistance > 10f) {

                            val scale =
                                newDistance / oldDistance

                            imageMatrixValue.set(imageMatrix)

                            imageMatrixValue.postScale(
                                scale,
                                scale,
                                midPoint.x,
                                midPoint.y
                            )

                            imageMatrix = imageMatrixValue

                            oldDistance = newDistance
                        }
                    }
                }

                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {

                mode = NONE

                parent?.requestDisallowInterceptTouchEvent(false)

                return true
            }
        }

        return true
    }

    private fun spacing(event: MotionEvent): Float {

        if (event.pointerCount < 2) {
            return 0f
        }

        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)

        return sqrt(
            x * x + y * y
        )
    }

    private fun midPoint(
        point: PointF,
        event: MotionEvent
    ) {

        point.set(
            (event.getX(0) + event.getX(1)) / 2f,
            (event.getY(0) + event.getY(1)) / 2f
        )
    }

    fun resetTransform() {

        imageMatrixValue.reset()

        imageMatrix = imageMatrixValue
    }

    fun getCurrentMatrix(): Matrix {

        return Matrix(imageMatrix)
    }

    private fun isTouchOnImage(
        x: Float,
        y: Float
    ): Boolean {

        val drawable = drawable ?: return false

        val inverseMatrix = Matrix()

        if (!imageMatrix.invert(inverseMatrix)) {
            return false
        }

        val point = floatArrayOf(x, y)

        inverseMatrix.mapPoints(point)

        val bitmap = (drawable as? BitmapDrawable)?.bitmap
            ?: return false

        val bitmapX = point[0].toInt()
        val bitmapY = point[1].toInt()

        if (
            bitmapX < 0 ||
            bitmapY < 0 ||
            bitmapX >= bitmap.width ||
            bitmapY >= bitmap.height
        ) {
            return false
        }

        val pixel = bitmap.getPixel(
            bitmapX,
            bitmapY
        )

        return Color.alpha(pixel) > 20
    }
}