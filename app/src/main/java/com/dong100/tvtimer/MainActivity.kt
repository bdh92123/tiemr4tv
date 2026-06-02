package com.dong100.tvtimer

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dong100.tvtimer.ui.theme.Timer4tvTheme
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Colors ---
val LavenderPrimary = Color(0xFFA291C7)
val LavenderDark = Color(0xFFA081DE)
val LavenderLight = Color(0xFFF8F8FF)
val GradientStart = Color(0xFF9370DB)
val GradientEnd = Color(0xFFE6E6FA)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Timer4tvTheme {
                MainScreen(
                    onStartTimer = { minutes ->
                        startTimerService(minutes)
                    },
                    onStopTimer = {
                        stopTimerService()
                    },
                    onCheckPermissions = {
                        checkAndRequestPermissions()
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "${packageName}/${SleepTimerService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expectedComponentName)
    }

    private fun startTimerService(minutes: Int) {
        val intent = Intent(this, SleepTimerService::class.java).apply {
            action = SleepTimerService.ACTION_START
            putExtra(SleepTimerService.EXTRA_MINUTES, minutes)
        }
        startService(intent)
    }

    private fun stopTimerService() {
        val intent = Intent(this, SleepTimerService::class.java).apply {
            action = SleepTimerService.ACTION_STOP
        }
        startService(intent)
    }
}

@Composable
fun MainScreen(
    onStartTimer: (Int) -> Unit,
    onStopTimer: () -> Unit,
    onCheckPermissions: () -> Unit
) {
    val presets = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240, 480)
    var showCustomDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LavenderLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sleep Timer TV",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = LavenderDark,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Choose a time to turn off your TV automatically",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(presets) { minutes ->
                    AnimatedTimerButton(
                        text = formatMinutes(minutes),
                        onClick = { onStartTimer(minutes) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedTimerButton(
                    text = "CUSTOM",
                    onClick = { showCustomDialog = true }
                )

                StopButton(
                    onClick = onStopTimer
                )

                AnimatedTimerButton(
                    text = "Setup Permissions",
                    onClick = onCheckPermissions
                )

            }
        }
    }

    if (showCustomDialog) {
        CustomTimerDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { minutes ->
                onStartTimer(minutes)
                showCustomDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Custom Timer", color = LavenderDark) },
        text = {
            Column {
                Text("Enter minutes:")
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.all { char -> char.isDigit() }) text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderDark,
                        cursorColor = LavenderDark
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = text.toIntOrNull()
                    if (minutes != null && minutes > 0) {
                        onConfirm(minutes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LavenderDark)
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = LavenderLight,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun StopButton(
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(60.dp)
            .width(160.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocused) Color(0xFFD32F2F) else Color(0xFFFFCDD2),
            contentColor = if (isFocused) Color.White else Color(0xFFB71C1C)
        )
    ) {
        Text(
            text = "STOP",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TimerButton(
    text: String,
    containerColor: Color = LavenderPrimary,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(60.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocused) LavenderDark else containerColor,
            contentColor = if (isFocused) Color.White else LavenderDark
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnimatedTimerButton(
    text: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // 포커스 시 크기 확대 (tween을 사용하여 spring보다 가볍게 처리)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "scale"
    )

    // 그라데이션 애니메이션 (포커스 시에만 활성화하여 부하 감소)
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    
    val color1 by infiniteTransition.animateColor(
        initialValue = GradientStart,
        targetValue = GradientEnd,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color1"
    )

    val color2 by infiniteTransition.animateColor(
        initialValue = GradientEnd,
        targetValue = GradientStart,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color2"
    )

    Box(
        modifier = Modifier
            .height(65.dp)
            .width(185.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer { // Lambda graphicsLayer로 성능 최적화
                scaleX = scale
                scaleY = scale
            }
            .background(
                brush = if (isFocused) {
                    Brush.horizontalGradient(listOf(color1, color2))
                } else {
                    Brush.linearGradient(listOf(LavenderDark, LavenderDark))
                },
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (isFocused) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = text,
                fontSize = 18.sp, // 텍스트 크기 고정 (버벅임 방지 핵심)
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatMinutes(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

// --- Service ---

class SleepTimerService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var timer: CountDownTimer? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_MINUTES = "EXTRA_MINUTES"
        private const val TAG = "SleepTimerService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
                if (minutes > 0) {
                    startCountdown(minutes)
                }
            }
            ACTION_STOP -> {
                stopCountdown()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCountdown(minutes: Int) {
        stopCountdown() // Clear previous if any

        showOverlay()

        val millis = minutes * 60 * 1000L
        timer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateOverlayText(millisUntilFinished)
            }

            override fun onFinish() {
                updateOverlayText(0)
                performSleepAttempts()
                stopCountdown()
            }
        }.start()
    }

    private fun stopCountdown() {
        timer?.cancel()
        timer = null
        removeOverlay()
    }

    private fun showOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Use a container for easier layout parameter application
        overlayView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null).apply {
            setBackgroundResource(android.R.drawable.toast_frame)
            alpha = 0.8f
            setPadding(20, 10, 20, 10)
        }
        
        val textView = overlayView?.findViewById<TextView>(android.R.id.text1)
        textView?.setTextColor(android.graphics.Color.BLACK)
        textView?.textSize = 14f
        textView?.gravity = Gravity.CENTER

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 50 // Margin from top
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay", e)
        }
    }

    private fun updateOverlayText(millis: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        val timeText = if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }

        val textView = overlayView?.findViewById<TextView>(android.R.id.text1)
        textView?.text = getString(R.string.timer_label, timeText)
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
            overlayView = null
        }
    }

    private fun performSleepAttempts() {
        Log.d(TAG, "Timer expired. Attempting to sleep...")
        
        // Method 1: Accessibility Global Action
        val success = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        if (success) {
            Log.d(TAG, "✓ GLOBAL_ACTION_LOCK_SCREEN success")
            return
        }

        // Method 2: PowerManager.goToSleep
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            val method = pm.javaClass.getMethod("goToSleep", Long::class.javaPrimitiveType)
            method.invoke(pm, SystemClock.uptimeMillis())
            Log.d(TAG, "✓ PowerManager.goToSleep invoked via reflection")
            
            Thread.sleep(1000)
            if (!pm.isInteractive) {
                Log.d(TAG, "✓✓ Device went to sleep via PowerManager!")
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ PowerManager.goToSleep failed: ${e.message}")
        }

        // Method 3: input keyevent 26
        try {
            Runtime.getRuntime().exec(arrayOf("input", "keyevent", "26")).waitFor()
            Log.d(TAG, "✓ Sent KEYCODE_POWER (26) via input command")
        } catch (e: Exception) {
            Log.e(TAG, "✗ input keyevent failed: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        stopCountdown()
    }
}
