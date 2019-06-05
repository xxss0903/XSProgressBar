package com.xxss0903.xsprogressbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlinx.coroutines.*

class CustomProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {

    private val DEFAULT_TEXT_SIZE = 10f
    private val DEFAULT_INDICATOR_SIZE = 20f
    private val PROGRESS_TYPE_CIRCLE = 2
    private val PROGRESS_TYPE_LINE = 1
    private val TEXT_POSITION_TOP = 1
    private val TEXT_POSITION_CENTER = 2
    private val TEXT_POSITION_BOTTOM = 3
    private val DEFAULT_MARGIN = 0f
    private val DEFAULT_PROGRESS_HEIGHT = 20f
    private val DEFAULT_PROGRESS_TIME = 1000L
    private val DEFAULT_CIRCLE_RADIUS = -1f
    private val DEFAULT_REPEATE_TIMES = 100

    var progress = 0f
        set(value) {
            field = value
            postInvalidate()
        }
    var progressTotal = DEFAULT_PROGRESS_TIME
    private var timeSpace = progressTotal / DEFAULT_REPEATE_TIMES
        get() {
            return progressTotal / DEFAULT_REPEATE_TIMES
        }
    private var progressSpace = progressTotal / DEFAULT_REPEATE_TIMES
        get() {
            return progressTotal / DEFAULT_REPEATE_TIMES
        }
    private var progressFraction = 0f
        get() {
            return progress / progressTotal
        }
    private var progressPercent: Int = 0
        get() {
            return (progressFraction * DEFAULT_REPEATE_TIMES).toInt()
        }
    var textColor: Int
    var textSize: Float
    var indicator: Int
    var indicatorSize: Int
    var indicatorColor = -1
    var progressColor = -1
    var progressBgColor = -1
    var progressType = 1
    var progressMargin = DEFAULT_MARGIN
    var progressHeight = DEFAULT_PROGRESS_HEIGHT
    var topTextHeight = 0f
    var progressText: String? = ""
    var drawText = true
    var drawIndicator = true
    var circleRadius = DEFAULT_CIRCLE_RADIUS
    var textPosition = TEXT_POSITION_TOP
    var capSize = 0f
    private var rectRight = 0f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progressBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progressRect = RectF()
    private var progressBgRect = RectF()
    private var indicatorRect = Rect()
    private var textRect = Rect()
    private var circleRect = RectF()
    private var indicatorBitmap: Bitmap? = null

    private var progressJob: Job? = null
    var progressStateListener: OnProgressChangeListener? = null

    private var isRunning = false
    private var isBgColorDrawed = false


    init {
        val typedArray = context.obtainStyledAttributes(attrs,
            R.styleable.CustomProgressView
        )
        indicator = typedArray.getResourceId(R.styleable.CustomProgressView_progressIndicator, -1)
        drawIndicator = typedArray.getBoolean(R.styleable.CustomProgressView_progressDrawIndicator, true)
        indicatorSize =
            typedArray.getDimension(R.styleable.CustomProgressView_progressIndicatorSize, DEFAULT_INDICATOR_SIZE)
                .toInt()
        if (indicator == -1) {
            indicatorColor = typedArray.getColor(R.styleable.CustomProgressView_progressIndicatorColor, Color.RED)
        } else {
            indicatorBitmap = BitmapFactory.decodeResource(resources, indicator)
        }

        drawText = typedArray.getBoolean(R.styleable.CustomProgressView_progressDrawText, true)
        progressText = typedArray.getString(R.styleable.CustomProgressView_progressText)
        progressText = if (progressText == null) "%d%%" else progressText + "%d%%"
        textColor = typedArray.getColor(R.styleable.CustomProgressView_progressTextColor, Color.BLACK)
        textPosition = typedArray.getInt(R.styleable.CustomProgressView_progressTextPosition, TEXT_POSITION_TOP)
        textSize = typedArray.getDimensionPixelSize(
            R.styleable.CustomProgressView_progressTextSize,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_TEXT_SIZE, resources.displayMetrics).toInt()
        ).toFloat()

        capSize = typedArray.getDimension(R.styleable.CustomProgressView_progressCapSize, 0f)
        progressBgColor = typedArray.getColor(R.styleable.CustomProgressView_progressBgColor, Color.TRANSPARENT)
        progressColor = typedArray.getColor(R.styleable.CustomProgressView_progressColor, Color.GREEN)
        progressType = typedArray.getInt(R.styleable.CustomProgressView_progressType, PROGRESS_TYPE_CIRCLE)
        if (progressType == PROGRESS_TYPE_CIRCLE) {
            circleRadius =
                typedArray.getDimension(R.styleable.CustomProgressView_progressCircleRadius, DEFAULT_CIRCLE_RADIUS)
        }
        progressMargin = typedArray.getDimension(R.styleable.CustomProgressView_progressMargin, DEFAULT_MARGIN)
        progressHeight = typedArray.getDimensionPixelSize(
            R.styleable.CustomProgressView_progressHeight,
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_PROGRESS_HEIGHT,
                resources.displayMetrics
            ).toInt()
        ).toFloat()
        typedArray.recycle()

        if (drawText) {
            textPaint.textSize = textSize
            textPaint.color = textColor
            textPaint.textAlign = Paint.Align.CENTER
        }
        if (drawIndicator) {
            indicatorPaint.isFilterBitmap = true
        }
        progressBgPaint.color = progressBgColor
        progressBgPaint.isFilterBitmap = true
        when(progressType) {
            PROGRESS_TYPE_LINE->{
                progressPaint.color = progressColor
                progressPaint.style = Paint.Style.FILL
                progressPaint.strokeWidth = progressHeight

                progressBgPaint.style = Paint.Style.FILL
            }
            PROGRESS_TYPE_CIRCLE->{
                circlePaint.isFilterBitmap = true
                circlePaint.color = progressColor
                circlePaint.style = Paint.Style.STROKE
                circlePaint.strokeWidth = progressHeight

                progressBgPaint.style = Paint.Style.STROKE
                progressBgPaint.strokeWidth = progressHeight
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        drawProgressBgColor(canvas)
        drawProgress(canvas)
        if (drawIndicator) {
            drawIndicator(canvas)
        }
        if (drawText) {
            drawText(canvas)
        }
    }

    private fun drawProgressBgColor(canvas: Canvas) {
        when(progressType) {
            PROGRESS_TYPE_CIRCLE -> {
                drawCircleBgColor(canvas)
            }
            PROGRESS_TYPE_LINE -> {
                drawLineBgColor(canvas)
            }
        }
    }

    private fun drawLineBgColor(canvas: Canvas) {
        canvas.drawRoundRect(
            progressBgRect, capSize, capSize, progressBgPaint
        )
    }

    private fun drawCircleBgColor(canvas: Canvas) {
        canvas.drawCircle(circleRect.centerX(), circleRect.centerY(), circleRadius, progressBgPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val finalWidth: Int
        val finalHeight: Int

        val (textWidth, textHeight) = measureText()
        progressHeight = measureProgress()
        if (widthMode == MeasureSpec.EXACTLY) {
            finalWidth = widthSize
        } else {
            finalWidth =
                Math.max(resources.displayMetrics.widthPixels / 2, textWidth) + paddingStart + paddingEnd

        }
        topTextHeight = textHeight + progressMargin
        if (heightMode == MeasureSpec.EXACTLY) {
            finalHeight = heightSize
        } else {
            if (progressType == PROGRESS_TYPE_CIRCLE) {
                finalHeight = finalWidth
            } else {
                when (textPosition) {
                    TEXT_POSITION_TOP, TEXT_POSITION_BOTTOM -> {
                        finalHeight = (topTextHeight + progressHeight + paddingBottom).toInt()
                    }
                    TEXT_POSITION_CENTER -> {
                        finalHeight =
                            (Math.max(textHeight.toFloat(), progressHeight) + paddingBottom + paddingTop).toInt()
                    }
                    else -> {
                        finalHeight = (topTextHeight + progressHeight + paddingBottom).toInt()
                    }
                }

            }
        }
        setMeasuredDimension(finalWidth, finalHeight)

        measureSubComponentsRect()
    }

    private fun measureSubComponentsRect() {
        val tempTextWidth = textRect.width()
        val tempTextHeight = textRect.height()
        when (progressType) {
            PROGRESS_TYPE_CIRCLE -> {
                when (textPosition) {
                    TEXT_POSITION_TOP -> {
                        textRect.top = (paddingTop + tempTextHeight)
                        textRect.left = measuredWidth / 2
                        textRect.right = textRect.left + tempTextWidth
                        textRect.bottom = textRect.top + tempTextHeight

                        val rWidth = measuredWidth - paddingStart - paddingEnd - progressHeight
                        val rHeight = measuredHeight - tempTextHeight - progressMargin - paddingBottom
                        val tempLeft: Float
                        if (rWidth > rHeight) {
                            tempLeft = (rWidth - rHeight) / 2
                            if (circleRadius == -1f) {
                                circleRadius = (rHeight - progressHeight) / 2
                            }
                        } else {
                            tempLeft = 0f
                            if (circleRadius == -1f) {
                                circleRadius = (rWidth - progressHeight) / 2
                            }
                        }
                        circleRect.top = textRect.top + progressHeight / 2 + progressMargin
                        circleRect.left = tempLeft + progressHeight
                        val circleLength = 2 * circleRadius
                        circleRect.right = circleRect.left + circleLength
                        circleRect.bottom = circleRect.top + circleLength
                    }
                    TEXT_POSITION_CENTER -> {
                        val rWidth = measuredWidth - paddingStart - paddingEnd
                        val rHeight = measuredHeight - paddingBottom - paddingTop
                        val tempLeft: Float
                        if (rWidth > rHeight) {
                            tempLeft = ((rWidth - rHeight) / 2).toFloat()
                            if (circleRadius == -1f) {
                                circleRadius = (rHeight - progressHeight) / 2
                            }
                        } else {
                            tempLeft = 0f
                            if (circleRadius == -1f) {
                                circleRadius = (rWidth - progressHeight) / 2
                            }
                        }
                        circleRect.top = paddingTop.toFloat() + progressHeight / 2
                        circleRect.left = tempLeft + progressHeight / 2
                        val circleLength = 2 * circleRadius
                        circleRect.right = circleRect.left + circleLength
                        circleRect.bottom = circleRect.top + circleLength

                        textRect.left = circleRect.centerX().toInt()
                        textRect.top = circleRect.centerY().toInt() + textRect.height() / 2
                        textRect.right = textRect.left + textRect.width()
                        textRect.bottom = textRect.top + textRect.height()
                    }
                    TEXT_POSITION_BOTTOM -> {
                        val rWidth = measuredWidth - paddingStart - paddingEnd - progressHeight
                        val rHeight = measuredHeight - paddingBottom - progressHeight - progressMargin
                        val tempLeft: Float
                        if (rWidth > rHeight) {
                            tempLeft = (rWidth - rHeight) / 2
                            if (circleRadius == -1f) {
                                circleRadius = (rHeight - progressHeight) / 2
                            }
                        } else {
                            tempLeft = 0f
                            if (circleRadius == -1f) {
                                circleRadius = (rWidth - progressHeight) / 2
                            }
                        }
                        circleRect.top = paddingTop.toFloat() + progressHeight / 2
                        circleRect.left = tempLeft + progressHeight
                        val circleLength = 2 * circleRadius
                        circleRect.right = circleRect.left + circleLength
                        circleRect.bottom = circleRect.top + circleLength

                        textRect.top =
                            (circleRect.bottom + progressMargin + progressHeight / 2).toInt() + tempTextHeight
                        textRect.left = circleRect.centerX().toInt()
                        textRect.right = textRect.left + tempTextWidth
                        textRect.bottom = (textRect.top + tempTextHeight)
                    }
                }

            }
            PROGRESS_TYPE_LINE -> {
                when (textPosition) {
                    TEXT_POSITION_TOP -> {
                        textRect.top = paddingTop + tempTextHeight
                        textRect.left = measuredWidth / 2
                        textRect.right = textRect.left + tempTextWidth
                        textRect.bottom = textRect.top + tempTextHeight

                        progressRect.top = (textRect.top + progressMargin).toFloat()
                        progressRect.left = ((indicatorSize) + paddingStart).toFloat()
                        progressRect.right = progressRect.left
                        progressRect.bottom = (progressRect.top + progressHeight)
                    }
                    TEXT_POSITION_CENTER -> {
                        if (progressHeight > tempTextHeight) {
                            progressRect.top = paddingTop.toFloat()
                            progressRect.left = ((indicatorSize) + paddingStart).toFloat()
                            progressRect.right = progressRect.left
                            progressRect.bottom = (progressRect.top + progressHeight)

                            textRect.top = ((progressHeight - textRect.height()) / 2 + textRect.height()).toInt()
                            textRect.left = measuredWidth / 2
                            textRect.right = textRect.left + tempTextWidth
                            textRect.bottom = textRect.top + tempTextHeight
                        } else {
                            textRect.top = paddingTop + tempTextHeight
                            textRect.left = measuredWidth / 2
                            textRect.right = textRect.left + tempTextWidth
                            textRect.bottom = textRect.top + tempTextHeight

                            progressRect.top = ((tempTextHeight - progressHeight) / 2)
                            progressRect.left = ((indicatorSize) + paddingStart).toFloat()
                            progressRect.right = progressRect.left
                            progressRect.bottom = (progressRect.top + progressHeight)
                        }

                    }
                    TEXT_POSITION_BOTTOM -> {
                        progressRect.top = paddingTop.toFloat()
                        progressRect.left = ((indicatorSize) + paddingStart).toFloat()
                        progressRect.right = progressRect.left
                        progressRect.bottom = (progressRect.top + progressHeight)

                        textRect.left = measuredWidth / 2
                        textRect.top = (progressRect.bottom + progressMargin + tempTextHeight).toInt()
                        textRect.bottom = textRect.top + tempTextHeight
                        textRect.right = textRect.left + tempTextWidth
                    }
                }
                val progressLength = measuredWidth - progressRect.left
                rectRight = (progressRect.left + (progressLength - indicatorSize) * 1)
                progressBgRect = RectF(progressRect.left, progressRect.top, progressRect.left + progressLength, progressRect.bottom)
            }
        }
        if (drawIndicator) {
            indicatorRect.left = ((progressRect.left - indicatorSize / 2).toInt())
            indicatorRect.top = ((progressRect.centerY() - indicatorSize / 2).toInt())
            indicatorRect.right = indicatorRect.left + indicatorSize
            indicatorRect.bottom = indicatorRect.top + indicatorSize
        }
    }

    fun startByCoroutines() {
        resetProgress()
        progressJob = GlobalScope.launch {
            isRunning = true
            progressStateListener?.progressStart()
            try {
                repeat(DEFAULT_REPEATE_TIMES) {
                    progress += progressSpace
                    progressStateListener?.progress(progressPercent)
                    delay(timeSpace)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            progressStateListener?.progressEnd()
            isRunning = false
        }
    }

    fun resetProgress() {
        progress = 0f
        isBgColorDrawed = false
        isRunning = false
        postInvalidate()
    }

    private fun measureProgress(): Float {
        val pHeight: Float
        if (drawIndicator) {
            pHeight = Math.max((indicatorSize * 2).toFloat(), progressHeight)
        } else {
            pHeight = progressHeight
        }
        return pHeight
    }

    private fun measureText(): Pair<Int, Int> {
        if (drawText) {
            val rectText = if (progressText == "%d%%") "100%" else progressText
            textPaint.getTextBounds(rectText, 0, progressText!!.length, textRect)
            return Pair(textRect.width(), textRect.height() + paddingTop)
        } else {
            return Pair(0, 0)
        }
    }

    private fun drawText(canvas: Canvas) {
        val tempProgressText = String.format(progressText!!, progressPercent)
        canvas.drawText(tempProgressText, textRect.left.toFloat(), (textRect.top).toFloat(), textPaint)
    }

    private fun drawIndicator(canvas: Canvas) {
        if (indicatorBitmap == null) {
            if (circleRadius == DEFAULT_CIRCLE_RADIUS) {
                val cx = progressRect.right
                val cy = progressRect.centerY()
                canvas.drawCircle(cx, cy, indicatorSize.toFloat(), progressPaint)
            } else {
                val cx = progressRect.right
                val cy = progressRect.centerY()
                canvas.drawCircle(cx, cy, indicatorSize.toFloat(), progressPaint)
            }
        } else {
            val left = progressRect.right - indicatorSize / 2f
            val top = indicatorRect.top.toFloat()
            canvas.drawBitmap(
                indicatorBitmap!!,
                left,
                top,
                indicatorPaint
            )
        }
    }

    private fun drawProgress(canvas: Canvas) {
        when (progressType) {
            PROGRESS_TYPE_CIRCLE -> {
                drawCircleProgress(canvas)
            }
            PROGRESS_TYPE_LINE -> {
                drawLineProgress(canvas)
            }
        }
    }

    private fun drawCircleProgress(canvas: Canvas) {
        val sweepAngle = progressFraction * 360
        canvas.drawArc(circleRect, -90f, sweepAngle, false, circlePaint)
    }

    private fun drawLineProgress(canvas: Canvas) {
        val progressLength = measuredWidth - progressRect.left
        progressRect.right = (progressRect.left + (progressLength - indicatorSize) * progressFraction)
        canvas.drawRoundRect(progressRect, capSize, capSize, progressPaint)
    }

    fun cancelCoroutine() {
        progressJob?.cancel()
    }

    interface OnProgressChangeListener {
        fun progressStart()
        fun progress(progress: Int)
        fun progressEnd()
    }
}