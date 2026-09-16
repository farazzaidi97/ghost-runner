package com.ghostrunner.core.engine

class MovingAverageFilter(val windowSize: Int = DEFAULT_WINDOW) {

    init {
        require(windowSize >= 1) { "windowSize must be >= 1" }
    }

    private val buffer = FloatArray(windowSize)
    private var fill: Int = 0
    private var head: Int = 0

    fun add(value: Float): Float {
        buffer[head] = value
        head = (head + 1) % windowSize
        if (fill < windowSize) fill++

        var sum = 0f
        for (i in 0 until fill) sum += buffer[i]
        return sum / fill
    }

    fun reset() {
        buffer.fill(0f)
        fill = 0
        head = 0
    }

    val isPrimed: Boolean get() = fill == windowSize

    companion object {
        const val DEFAULT_WINDOW: Int = 3
    }
}
