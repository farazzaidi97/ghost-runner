package com.ghostrunner.core.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MovingAverageFilterTest {

    @Test
    fun `single sample returns itself`() {
        val f = MovingAverageFilter(windowSize = 3)
        assertThat(f.add(4.0f)).isWithin(EPS).of(4.0f)
    }

    @Test
    fun `partial window averages over inserted count`() {
        val f = MovingAverageFilter(windowSize = 3)
        f.add(2.0f)
        assertThat(f.add(4.0f)).isWithin(EPS).of(3.0f)
    }

    @Test
    fun `full window averages over all three`() {
        val f = MovingAverageFilter(windowSize = 3)
        f.add(2.0f); f.add(4.0f)
        assertThat(f.add(6.0f)).isWithin(EPS).of(4.0f)
        assertThat(f.isPrimed).isTrue()
    }

    @Test
    fun `window slides discarding oldest sample`() {
        val f = MovingAverageFilter(windowSize = 3)
        f.add(2.0f); f.add(4.0f); f.add(6.0f) // window = [2,4,6]
        // adding 9 evicts the 2 → window [4,6,9]; average = 19/3
        assertThat(f.add(9.0f)).isWithin(EPS).of(19f / 3f)
    }

    @Test
    fun `reset clears state so next add returns input`() {
        val f = MovingAverageFilter(windowSize = 3)
        f.add(2.0f); f.add(4.0f); f.add(6.0f)
        f.reset()
        assertThat(f.isPrimed).isFalse()
        assertThat(f.add(8.0f)).isWithin(EPS).of(8.0f)
    }

    @Test
    fun `window size of 1 acts as passthrough`() {
        val f = MovingAverageFilter(windowSize = 1)
        assertThat(f.add(3.0f)).isWithin(EPS).of(3.0f)
        assertThat(f.add(9.0f)).isWithin(EPS).of(9.0f)
    }

    companion object {
        private const val EPS = 1e-5f
    }
}
