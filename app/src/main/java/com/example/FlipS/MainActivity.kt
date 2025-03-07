package com.example.FlipS

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.FlipS.SoundManager
import android.content.Context


class MainActivity : ComponentActivity() {
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        soundManager = SoundManager(this)

        setContent {
            var gameStarted by remember { mutableStateOf(false) }

            if (!gameStarted) {
                StartScreen {
                    gameStarted = true
                }
            } else {
                GameScreen()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

@Composable
fun GameScreen() {
    var characterY by remember { mutableStateOf(550f) }
    var isOnTop by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    val gravitySwitchSpeed = 10f
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val obstacleWidth = with(density) { 70.dp.toPx() }
    val obstacleHeight = with(density) { 140.dp.toPx() }
    var backgroundOffset by remember { mutableStateOf(0f) }
    var isGameOver by remember { mutableStateOf(false) }
    var obstacles by remember { mutableStateOf(generateObstacles(screenWidth, obstacleWidth, obstacleHeight)) }
    var gameRunning by remember { mutableStateOf(true) }
    var obstacleGenerationRate by remember { mutableStateOf(200) } // Initially set the spawn rate to 200
    var lastObstacleTime by remember { mutableStateOf(0L) }
    var minObstacleInterval by remember { mutableStateOf(1000L) } // 1 วินาที
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    if (score % 400 == 0) { // Every 100 points, spawn new obstacles more quickly
        obstacleGenerationRate -= 5 // Decrease spawn rate (shorter time between obstacles)
    }
    // Animation frames for the character (slime)
    val slimeFrames = listOf(
        painterResource(id = R.drawable.slime1),
        painterResource(id = R.drawable.slime2),
        painterResource(id = R.drawable.slime3)
    )
    val characterWidth = with(density) { slimeFrames[0].intrinsicSize.width.toDp().toPx() }
    val characterHeight = with(density) { slimeFrames[0].intrinsicSize.height.toDp().toPx() }
    var slimeFrameIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        soundManager.playBackgroundMusic()
    }
    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

    LaunchedEffect(gameRunning) {
        while (gameRunning) {
            delay(100) // สลับเฟรมทุก 100ms
            slimeFrameIndex = (slimeFrameIndex + 1) % slimeFrames.size
        }
    }

    var difficulty by remember { mutableStateOf(1f) }
    var obstacleSpeed by remember { mutableStateOf(5f) }
    LaunchedEffect(score) {
        // เพิ่มความยากทุก 1000 คะแนน
        difficulty = 1f + (score / 1000f)
        obstacleSpeed = 5f + (difficulty * 2f)
        minObstacleInterval = (1000L.toFloat() - (difficulty * 100f)).coerceAtLeast(500f).toLong()
    }

    val backgroundMoveSpeed = 1.8f
    LaunchedEffect(gameRunning) {
        while (gameRunning) {
            delay(32)
            val groundLevel = screenHeight - 900 // กำหนดระดับพื้น

            // อัปเดตตำแหน่ง Y ของตัวละคร
            characterY += if (isOnTop) -gravitySwitchSpeed else gravitySwitchSpeed
            characterY = characterY.coerceIn(
                40f + characterHeight / 2, // ขอบบน
                groundLevel - characterHeight / 2 // ขอบล่าง
            )

            obstacles = obstacles.map {
                // ใช้ความเร็วเฉพาะของแต่ละ obstacle
                val updatedObstacle = it.copy(
                    x = it.x - (it.speed + (difficulty * 2f))
                )
                updatedObstacle
            }
                .filter { it.x > -obstacleWidth }
                .toMutableList()

            val currentTime = System.currentTimeMillis()
            // สร้างสิ่งกีดขวางตามเงื่อนไขเวลาและจำนวน
            if (currentTime - lastObstacleTime > minObstacleInterval && obstacles.size < 12) {
                val numberOfObstacles = Random.nextInt(1, 4)
                repeat(numberOfObstacles) {
                    val newObstacle = generateNewObstacle(screenWidth, screenHeight, obstacleWidth, obstacleHeight)
                    obstacles.add(newObstacle)
                }
                lastObstacleTime = currentTime
            }

            // Update background position
            backgroundOffset = ((backgroundOffset - backgroundMoveSpeed) % screenWidth)

            score++

            // Check for collision with obstacles
            for (obstacle in obstacles) {
                if (characterHitsObstacle(screenWidth / 8, characterY, characterWidth, characterHeight, obstacle)) {
                    soundManager.playCollisionSound()
                    soundManager.stopBackgroundMusic()
                    soundManager.playGameOverSound()
                    isGameOver = true
                    gameRunning = false
                }
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (!isGameOver) {
                    isOnTop = !isOnTop
                    soundManager.playJumpSound()  // เล่นเสียงเมื่อกระโดด
                }
            }
    ) {
        val backgroundImage = painterResource(id = R.drawable.windrise)
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = backgroundImage,
                contentDescription = "Background 1",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = backgroundOffset
                    },
                contentScale = ContentScale.FillBounds
            )
            Image(
                painter = backgroundImage,
                contentDescription = "Background 2",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = backgroundOffset + screenWidth
                    },
                contentScale = ContentScale.FillBounds
            )
        }

        // Draw slime character
        Image(
            painter = slimeFrames[slimeFrameIndex],
            contentDescription = "Slime Character",
            modifier = Modifier
                .size(characterWidth.dp, characterHeight.dp)
                .offset(
                    x = (screenWidth / 8 - characterWidth / 2).dp,
                    y = (characterY - characterHeight / 2).dp //
                )
        )

        // Draw obstacles
        obstacles.forEach { obstacle ->
            DrawObstacle(obstacle)
        }


        val CustomFont = FontFamily(Font(R.font.slackey))

        Text(
            text = "Score: $score",
            color = Color.White,
            modifier = Modifier.padding(top = 20.dp, start = 15.dp),
            style = TextStyle(
                fontFamily = CustomFont,
                fontSize = 32.sp
            )
        )


        if (isGameOver) {
            GameOverScreen(
                score = score,
                onRestart = {
                    characterY = screenHeight / 2
                    isOnTop = false
                    isGameOver = false
                    obstacles = generateObstacles(screenWidth, obstacleWidth, obstacleHeight)
                    score = 0
                    backgroundOffset = 0f
                    gameRunning = true
                    soundManager.playBackgroundMusic()
                }
            )
        }
    }
}

@Composable
fun StartScreen(onStartClick: () -> Unit) {
    val CustomFont = FontFamily(Font(R.font.slackey))
    val NameFont = FontFamily(Font(R.font.mansalva))
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val sharedPrefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    val highScore = remember { sharedPrefs.getInt("high_score", 0) }

    LaunchedEffect(Unit) {
        soundManager.playBackgroundMusic()
    }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.windrise),
            contentDescription = "Start Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title
            Text(
                text = "FlyS",
                style = TextStyle(
                    fontFamily = NameFont,
                    fontSize = 250.sp,
                    color = Color.White,
                ),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // High Score
            Text(
                text = "High Score: $highScore",
                style = TextStyle(
                    fontFamily = CustomFont,
                    fontSize = 32.sp,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(3f, 3f),
                        blurRadius = 5f
                    )
                ),
                modifier = Modifier.padding(bottom = 15.dp)
            )

            // Start Button
            Button(
                onClick = {
                    soundManager.stopBackgroundMusic()
                    onStartClick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF191970),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .width(200.dp)
                    .height(80.dp)
            ) {
                Text(
                    "START",
                    style = TextStyle(
                        fontFamily = CustomFont,
                        fontSize = 36.sp
                    )
                )
            }
        }
    }
}

@Composable
fun GameOverScreen(score: Int, onRestart: () -> Unit) {
    val CustomFont = FontFamily(Font(R.font.slackey))
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    val highScore = remember { sharedPrefs.getInt("high_score", 0) }

    // Update high score immediately
    LaunchedEffect(Unit) {
        if (score > highScore) {
            sharedPrefs.edit().putInt("high_score", score).commit() // ใช้ commit() แทน apply()
        }
    }

    // animation สำหรับแสดง GameOver screen
    var opacity by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        opacity = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .graphicsLayer(alpha = opacity),
        contentAlignment = Alignment.Center
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "GAME OVER",
                color = Color.Red,
                style = TextStyle(
                    fontFamily = CustomFont,
                    fontSize = 60.sp,
                    shadow = Shadow(
                        color = Color.Red.copy(alpha = 0.3f),
                        offset = Offset(5f, 5f),
                        blurRadius = 10f
                    )
                )
            )

            Text(
                text = "Score: $score",
                color = Color.White,
                style = TextStyle(
                    fontFamily = CustomFont,
                    fontSize = 42.sp
                )
            )

            Text(
                text = "High Score: ${maxOf(score, highScore)}",
                color = Color.Yellow,
                style = TextStyle(
                    fontFamily = CustomFont,
                    fontSize = 32.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .width(280.dp)
                    .height(70.dp)
            ) {
                Text(
                    "PLAY AGAIN",
                    style = TextStyle(
                        fontFamily = CustomFont,
                        fontSize = 28.sp
                    )
                )
            }
        }
    }
}


fun generateNewObstacle(screenWidth: Float, screenHeight: Float, obstacleWidth: Float, obstacleHeight: Float): Obstacle {
    // สุ่มเลือกระดับก่อน (0: top, 1: middle, 2: bottom)
    val level = Random.nextInt(3)

    val (type, yPosition) = when (level) {
        0 -> "crow" to 0f  // บนสุด: crow เท่านั้น
        1 -> (if (Random.nextBoolean()) "crow" else "arrow") to (screenHeight / 2 - obstacleHeight)  // กลาง: crow หรือ arrow
        2 -> (if (Random.nextBoolean()) "knight" else "ghost") to (screenHeight - obstacleHeight)  // ล่าง: knight หรือ ghost เท่านั้น
        else -> "crow" to 0f
    }

    val speed = when (type) {
        "arrow" -> 8f
        else -> 5f
    }

    return Obstacle(
        x = screenWidth,
        y = yPosition,
        width = obstacleWidth,
        height = obstacleHeight,
        type = type,
        animationFrame = 0,
        speed = speed
    )
}

fun generateObstacles(screenWidth: Float, obstacleWidth: Float, obstacleHeight: Float): MutableList<Obstacle> {
    val obstacles = mutableListOf<Obstacle>()
    // เพิ่มระยะห่างระหว่างสิ่งกีดขวาง
    for (i in 0 until 12) {
        val newObstacle = generateNewObstacle(
            screenWidth = screenWidth + i * 300,  // เพิ่มระยะห่าง
            screenHeight = 720f,
            obstacleWidth = obstacleWidth,
            obstacleHeight = obstacleHeight
        )
        obstacles.add(newObstacle)
    }
    return obstacles
}

fun characterHitsObstacle(
    characterX: Float,
    characterY: Float,
    characterWidth: Float,
    characterHeight: Float,
    obstacle: Obstacle
): Boolean {
    // แยก padding สำหรับแกน X และ Y
    val horizontalPadding = when (obstacle.type) {
        "arrow" -> 30f      // Reduced from 40f to make horizontal collision more precise
        "ghost" -> 55f
        else -> 60f
    }

    val verticalPadding = when (obstacle.type) {
        "arrow" -> 40f      // Reduced from 80f to make vertical collision more accurate
        "ghost" -> 70f
        else -> 55f
    }

    // Calculate character collision bounds
    val characterBounds = Box(
        left = characterX + horizontalPadding,
        right = characterX + characterWidth - horizontalPadding,
        top = characterY + verticalPadding,
        bottom = characterY + characterHeight - verticalPadding
    )

// Calculate obstacle collision bounds with special handling for arrows
    val obstacleBounds = Box(
        left = obstacle.x + when(obstacle.type) {
            "arrow" -> horizontalPadding
            else -> horizontalPadding
        },
        right = obstacle.x + obstacle.width - horizontalPadding,
        top = obstacle.y + when(obstacle.type) {
            "arrow" -> verticalPadding * 0.8f  // Tighter vertical bounds for arrows
            else -> verticalPadding
        },
        bottom = obstacle.y + obstacle.height - when(obstacle.type) {
            "arrow" -> verticalPadding * 0.8f  // Tighter vertical bounds for arrows
            else -> verticalPadding
        }
    )
    // ตรวจสอบระยะห่างในแนวนอนก่อน
    val horizontalOverlap = characterBounds.right > obstacleBounds.left &&
            characterBounds.left < obstacleBounds.right

    // ถ้ามีการซ้อนทับในแนวนอน จึงตรวจสอบแนวตั้ง
    // If there's horizontal overlap, check vertical
    if (horizontalOverlap) {
        val safetyMargin = when (obstacle.type) {
            "arrow" -> 10f  // Reduced from 20f for more precise arrow collisions
            "ghost" -> 25f
            else -> 15f
        }

        // Special collision check for arrows
        val isCollision = if (obstacle.type == "arrow") {
            // For arrows, check if character is within a narrower vertical range
            val arrowVerticalCenter = obstacle.y + (obstacle.height / 2)
            val characterVerticalCenter = characterY + (characterHeight / 2)

            // Calculate vertical distance between centers
            val verticalDistance = Math.abs(characterVerticalCenter - arrowVerticalCenter)

            // More precise vertical collision for arrows
            verticalDistance < (obstacle.height / 3)
        } else {
            // Normal collision check for other obstacles
            characterBounds.bottom > obstacleBounds.top - safetyMargin &&
                    characterBounds.top < obstacleBounds.bottom + safetyMargin
        }
        return isCollision
    }

    return false
}

// Helper data class สำหรับคำนวณการชน (คงเดิม)
private data class Box(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float
) {
    fun intersects(other: Box): Boolean {
        return right > other.left &&
                left < other.right &&
                bottom > other.top &&
                top < other.bottom
    }
}



@Composable
fun DrawObstacle(obstacle: Obstacle, modifier: Modifier = Modifier) {
    when (obstacle.type) {
        "crow" -> AnimatedCrow(modifier.offset(obstacle.x.dp, obstacle.y.dp), obstacle.width, obstacle.height)
        "knight" -> AnimatedKnight(modifier.offset(obstacle.x.dp, obstacle.y.dp), obstacle.width, obstacle.height)
        "arrow" -> ArrowObstacleDraw(modifier, obstacle.x, obstacle.y)
        "ghost" -> GhostObstacleDraw(modifier, obstacle.x, obstacle.y)
    }
}

@Composable
fun AnimatedCrow(modifier: Modifier = Modifier, width: Float, height: Float) {
    val crowFrames = listOf(
        R.drawable.crow1, R.drawable.crow2, R.drawable.crow3,
        R.drawable.crow4, R.drawable.crow5
    )
    var currentFrame by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            currentFrame = (currentFrame + 1) % crowFrames.size
        }
    }

    Image(
        painter = painterResource(id = crowFrames[currentFrame]),
        contentDescription = "Animated Crow",
        modifier = modifier.size(width.dp, height.dp)
    )
}

@Composable
fun AnimatedKnight(modifier: Modifier = Modifier, width: Float, height: Float) {
    val knightFrames = listOf(
        R.drawable.knight1, R.drawable.knight2, R.drawable.knight3,
        R.drawable.knight4, R.drawable.knight5, R.drawable.knight6
    )
    var currentFrame by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            currentFrame = (currentFrame + 1) % knightFrames.size
        }
    }

    Image(
        painter = painterResource(id = knightFrames[currentFrame]),
        contentDescription = "Animated Knight",
        modifier = modifier.size(width.dp, height.dp)
    )
}

@Composable
fun ArrowObstacleDraw(modifier: Modifier = Modifier, x: Float, y: Float) {
    Image(
        painter = painterResource(id = R.drawable.arrow),
        contentDescription = "Arrow Obstacle",
        modifier = modifier
            .offset(x.dp, y.dp)
            .size(70.dp, 30.dp)
    )
}

@Composable
fun GhostObstacleDraw(modifier: Modifier = Modifier, x: Float, y: Float) {
    Image(
        painter = painterResource(id = R.drawable.ghost),
        contentDescription = "Ghost Obstacle",
        modifier = modifier
            .offset(x.dp, y.dp)
            .size(70.dp, 70.dp)
    )
}