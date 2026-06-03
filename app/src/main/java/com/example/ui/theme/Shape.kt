package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape

// Material 3 Expressive Structural Containment and Tension
val M3EShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp), // M3E Large Corner Radius
    large = RoundedCornerShape(32.dp), // M3E Extra-Large Corner Radius
    extraLarge = RoundedCornerShape(48.dp) // M3E Super-large shape
)
