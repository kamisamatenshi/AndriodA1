package com.koi.thepiece

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.koi.thepiece.scenemanagement.AppNavGraph
import com.koi.thepiece.ui.theme.ThePieceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ThePieceTheme {
                val audio = remember { AudioManager(applicationContext) }

                //Auto playing BGM here
                DisposableEffect(Unit) {
                    //Audio is set here, the folder is at app/src/res/raw/
                    audio.playBgm(R.raw.bgm, loop = true)
                    onDispose { audio.onDestroy() }
                }

                AppNavGraph(audioManager = audio)
            }
        }
    }
}