package com.example

import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.Assert.assertNotNull

class ColorTest {
    @Test
    fun testColor() {
        val color = Color(0xFF0F172A)
        println("A: ${color.alpha}, R: ${color.red}, G: ${color.green}, B: ${color.blue}")
    }
}
