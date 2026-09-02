package com.example.triviaquiz

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity

/**
 * Minimal splash/landing screen.
 * Shows ONLY the logo centered on a cream background for ~1.5 seconds,
 * then fades into the Home screen (MainActivity).
 */
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // After 1.5 seconds, fade out the logo and transition to MainActivity
        handler.postDelayed({
            // Fade out the splash logo
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 400
                interpolator = DecelerateInterpolator()
                fillAfter = true
            }
            findViewById<android.widget.ImageView>(R.id.splashLogo).startAnimation(fadeOut)

            // After fade completes, start MainActivity
            handler.postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                // Apply a smooth cross-fade transition
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
                } else {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
                finish()
            }, 400)
        }, 1500)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
