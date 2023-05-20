package com.LPSoundworks.chromatictuner

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.view.WindowInsetsController

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import com.LPSoundworks.chromatictuner.ui.theme.ChromaticTunerTheme

import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*

import android.view.WindowManager

class MainActivity : ComponentActivity() {

    /*
     * App Variables
     */
    val disposable: CompositeDisposable = CompositeDisposable()

    /*
     * Surface references
     */
    private lateinit var audioView: AudioView
    /*
     * App State Handling
     */

    // App startup
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        audioView = AudioView()

        // Make the activity fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Hide the system UI (status bar and navigation bar)
        window.decorView.systemUiVisibility = (
                window.decorView.systemUiVisibility or
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE or
                        WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_SWIPE
                )


        setContent {
            ChromaticTunerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    DrawCanvas(audioView)
                }
            }
        }

    }

    // App resume
    override fun onResume() {
        super.onResume()

        //restart audio capture
        if (requestAudio() && disposable.size() == 0)
            start()
    }

    // App stop
    override fun onStop() {
        //stop audio capture
        stop()
        super.onStop()
    }

    /*
     * Microphone Audio Subscription Handling
     */
    // Start mic stream
    private fun start() {
        val src = AudioSource().stream()
        val noise = Noise.real(4096)

        disposable.add(
            src.observeOn(Schedulers.newThread())
                .subscribe
                    { e -> audioView.onAudio(e) }
                )
        disposable.add(
            src.observeOn(Schedulers.newThread())
                .map {
                    for (i in it.indices)
                        it[i] *= 2.0f
                    return@map it
                }
                .map { noise.fft(it, FloatArray(4096+2)) }
                .subscribe({ fft -> audioView.onFFT(fft) })
        )

    }

    // Stop mic stream
    private fun stop() {
        disposable.clear()
    }


    // Audio permission request
    private fun requestAudio(): Boolean {
        if (checkSelfPermission(RECORD_AUDIO) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(RECORD_AUDIO), 1337)
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PERMISSION_GRANTED)
            start()
    }

}

@Composable
fun DrawCanvas(audioView: AudioView) {
    // Animation HACK to force COMPOSE to RE-RENDER CANVAS
    val animatedProgress = remember { Animatable(0.001f) }
    LaunchedEffect(animatedProgress) {
        animatedProgress.animateTo(1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing)
            )
        )
    }
    // Draw canvas
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        audioView.drawCanvas(this, animatedProgress.value)
    }

    Text(text = "FFT Spectrum Visualiser using Noise and kissfft libraries, by Luigi Pizzolito")
}