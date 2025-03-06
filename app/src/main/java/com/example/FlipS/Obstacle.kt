package com.example.FlipS

data class Obstacle(
    var x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: String,  // "crow", "knight", "arrow", "ghost"
    var animationFrame: Int = 0, // Track current animation frame
    val speed: Float
)
