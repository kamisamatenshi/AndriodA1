package com.koi.thepiece

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil.ImageLoader
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.core.image.AppImageLoader
import com.koi.thepiece.scenemanagement.AppNavGraph
import com.koi.thepiece.ui.theme.ThePieceTheme

class MainActivity : ComponentActivity() {

    private lateinit var audio: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        audio = AudioManager(applicationContext)

        setContent {
            val imageLoader: ImageLoader = remember { AppImageLoader.build(this) }


            var darkTheme by rememberSaveable { mutableStateOf(false) }

            ThePieceTheme(darkTheme = darkTheme) {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        imageLoader = imageLoader,
                        audioManager = audio,
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = !darkTheme }
                    )
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        //audio.playBgm(R.raw.bgm, loop = true) // or audio.resumeBgm()
        // play whatever user selected
        audio.playSelectedBgm(loop = true)
    }

    override fun onResume() {
        super.onResume()
        audio.resumeBgm()
    }

    override fun onStop() {
        super.onStop()
        audio.pauseBgm() // or audio.pauseBgm()
    }

    override fun onDestroy() {
        super.onDestroy()
        audio.onDestroy()
    }
}
