// SoundManager.kt
package com.example.FlipS

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool

class SoundManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val soundPool = SoundPool.Builder().setMaxStreams(5).build()

    // โหลดเสียงเอฟเฟกต์
    private val jumpSound = soundPool.load(context, R.raw.jump, 1)
    private val collisionSound = soundPool.load(context, R.raw.hit, 1)
    private val gameoverSound = soundPool.load(context, R.raw.gameover, 1)

    // เล่นเพลงพื้นหลัง
    fun playBackgroundMusic() {
        mediaPlayer = MediaPlayer.create(context, R.raw.bgm)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    // หยุดเพลงพื้นหลัง
    fun stopBackgroundMusic() {
        mediaPlayer?.stop()
    }

    // เล่นเสียงเอฟเฟกต์
    fun playJumpSound() {
        soundPool.play(jumpSound, 1f, 1f, 1, 0, 1f)
    }

    fun playCollisionSound() {
        soundPool.play(collisionSound, 1f, 1f, 1, 0, 1f)
    }

    fun playGameOverSound() {
        soundPool.play(gameoverSound, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        mediaPlayer?.release()
        soundPool.release()
    }
}