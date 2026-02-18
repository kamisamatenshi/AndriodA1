package com.koi.thepiece

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import coil.ImageLoader
import com.koi.thepiece.core.image.AppImageLoader
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.koi.thepiece.scenemanagement.AppNavGraph
import com.koi.thepiece.ui.theme.ThePieceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val imageLoader: ImageLoader = remember { AppImageLoader.build(this) }
            ThePieceTheme {
                val audio = remember { AudioManager(applicationContext) }


                //Auto playing BGM here
                DisposableEffect(Unit) {
                    //Audio is set here, the folder is at app/src/res/raw/
                    audio.playBgm(R.raw.bgm, loop = true)
                    onDispose { audio.onDestroy() }
                }

                AppNavGraph(imageLoader,audioManager = audio)

            }
        }
    }
}