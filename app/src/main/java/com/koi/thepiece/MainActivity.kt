package com.koi.thepiece

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val imageLoader: ImageLoader = remember { AppImageLoader.build(this) }
            val audio = remember { AudioManager(applicationContext) }

            var darkTheme by rememberSaveable { mutableStateOf(false) }

            ThePieceTheme(darkTheme = darkTheme) {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    DisposableEffect(Unit) {
                        audio.playBgm(R.raw.bgm, loop = true)
                        onDispose { audio.onDestroy() }
                    }

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
}
