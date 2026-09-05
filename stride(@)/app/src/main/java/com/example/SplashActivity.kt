package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.common.StrideBrandLogo
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBlack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val isTransitioned = AtomicBoolean(false)
    private var isUserLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make full-screen edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val alphaAnim = remember { Animatable(0f) }
            val scaleAnim = remember { Animatable(0.85f) }

            LaunchedEffect(Unit) {
                // Check auth concurrently in background
                val authJob = launch(Dispatchers.IO) {
                    try {
                        val app = application as StrideApp
                        isUserLoggedIn = app.authRepository.checkInitialAuthState()
                    } catch (e: Exception) {
                        isUserLoggedIn = false
                    }
                }

                launch {
                    alphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    scaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    )
                }

                authJob.join()
                delay(700)
                proceedToNextScreen()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .scale(scaleAnim.value)
                        .alpha(alphaAnim.value),
                    contentAlignment = Alignment.Center
                ) {
                    StrideBrandLogo(
                        markSize = 72.dp,
                        fontSize = 40.sp,
                        accentColor = ElectricCyan,
                        tagline = "Run • Ride • Explore"
                    )
                }
            }
        }
    }

    private fun proceedToNextScreen() {
        if (!isTransitioned.compareAndSet(false, true)) return

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_IS_LOGGED_IN, isUserLoggedIn)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
