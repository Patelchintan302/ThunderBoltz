package com.example.thunderboltz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thunderboltz.ui.theme.ThunderBoltzTheme
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThunderBoltzTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Speed(
                        modifier = Modifier.padding(innerPadding)
                    )
                    Battry(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@Composable
fun SemiCircleSpeedGauge(
    speed: Double,
    maxSpeed: Double,
    modifier: Modifier = Modifier,
    size: Dp,
    strokeWidth: Dp = 20.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // Calculate the percentage of speed relative to the max speed and map it to 0-180 degrees.
    val sweepAngle = ((speed / maxSpeed).coerceIn(0.0, 1.0) * 180).toFloat()

    // Animate the sweep angle for a smooth transition.
    val animatedSweepAngle by animateFloatAsState(
        targetValue = sweepAngle,
        animationSpec = tween(durationMillis = 800),
        label = "SpeedGaugeAnimation"
    )

    Canvas(modifier = modifier.size(size)) {
        val sweep = animatedSweepAngle
        val strokeThickness = strokeWidth.toPx()
        val padding = strokeThickness / 2f

        // Size of the area where the arc is drawn (full circle bounding box)
        val arcSize = Size(size.toPx() - strokeThickness, size.toPx() - strokeThickness)

        // 1. Draw the inactive track (the background arc)
        drawArc(
            color = Color.LightGray.copy(alpha = 0.5f),
            startAngle = 180f, // Start at the left horizontal axis
            sweepAngle = 180f, // Sweep half a circle
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = arcSize,
            style = Stroke(strokeThickness, cap = StrokeCap.Round)
        )

        // 2. Draw the active progress (the speed arc)
        drawArc(
            color = color,
            startAngle = 180f, // Start at the left horizontal axis
            sweepAngle = sweep, // Animated sweep angle
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = arcSize,
            style = Stroke(strokeThickness, cap = StrokeCap.Round)
        )
    }
}


@Composable
fun Speed(modifier: Modifier = Modifier) {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    var speedText by remember { mutableStateOf("Loading...") }
    var speedValue by remember { mutableDoubleStateOf(0.0) }

    // Define the max speed for the gauge
    val maxSpeed = 150.0
    val gaugeSize = 250.dp // Define the size for the gauge

    LaunchedEffect(Unit) {
        try {
            db.collection("Collection").document("Status")
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        speedText = "Error: ${error.message}"
                        speedValue = 0.0
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // FIX: Safely retrieve the field value as a Number to prevent crashes.
                        val firestoreValue = documentSnapshot.get("Speed")

                        // Safely convert to Double. If null or wrong type (like a String), defaults to 0.0
                        val newSpeedValue = (firestoreValue as? Number)?.toDouble() ?: 0.0

                        speedValue = newSpeedValue
                        // Format the number for display (e.g., "123.4")
                        speedText = newSpeedValue.toInt().toString()
                    } else {
                        speedText = "Document Missing"
                        speedValue = 0.0
                    }
                }
        } catch (e: Exception) {
            speedText = "Failed to listen: ${e.message}"
            speedValue = 0.0
        }
    }

    Column (
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Use a Box to stack the Gauge and the Text in the center
        Box(
            // Use BottomCenter to ensure the semi-circle aligns correctly
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .size(gaugeSize) // Set the box size to match the gauge
                .padding(bottom = 32.dp)
        ) {
            // 1. Semi-Circular Gauge (Background Layer)
            SemiCircleSpeedGauge(
                speed = speedValue,
                maxSpeed = maxSpeed,
                size = gaugeSize,
            )



            Row(
                // 💡 Key Change 1: Align the contents of the Row vertically to ensure "km/h" is centered with the speed number
                verticalAlignment = Alignment.CenterVertically,
                // 💡 Key Change 2: Apply the vertical shift (offset) to the parent Row container
                modifier = Modifier.offset(y = (-60).dp)
            ) {
                Text(
                    text = speedText,
                    fontSize = 70.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                // Add some horizontal space between the number and the unit
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km/h",
                    fontSize = 30.sp,
                )
            }
        }


    }
}


@Composable
fun CircleBattryGauge(
    battry: Double,
    modifier: Modifier = Modifier,
    size: Dp,
    strokeWidth: Dp = 20.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // Calculate the percentage of battery relative to 100 and map it to 0-360 degrees.
    val sweepAngle = ((battry / 100.0).coerceIn(0.0, 1.0) * 360.0).toFloat()

    // Animate the sweep angle for a smooth transition.
    val animatedSweepAngle by animateFloatAsState(
        targetValue = sweepAngle,
        animationSpec = tween(durationMillis = 800),
        label = "BatteryGaugeAnimation"
    )

    Canvas(modifier = modifier.size(size)) {
        val sweep = animatedSweepAngle
        val strokeThickness = strokeWidth.toPx()
        val padding = strokeThickness / 2f

        // Size of the area where the arc is drawn (full circle bounding box)
        val arcSize = Size(size.toPx() - strokeThickness, size.toPx() - strokeThickness)

        // Start angle at 270f (the top/12 o'clock position) for a clockwise fill.
        val startAngle = 270f

        // 1. Draw the inactive track (the background FULL circle)
        drawArc(
            color = Color.LightGray.copy(alpha = 0.5f),
            startAngle = startAngle,
            sweepAngle = 360f, // Sweep a full circle
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = arcSize,
            style = Stroke(strokeThickness, cap = StrokeCap.Round)
        )

        // 2. Draw the active progress (the battery arc)
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep, // Animated sweep angle
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = arcSize,
            style = Stroke(strokeThickness, cap = StrokeCap.Round)
        )
    }
}

// --- 2. Battry Composable (Top Right Placement) ---

@Composable
fun Battry(modifier: Modifier = Modifier) {
    // NOTE: Replace this placeholder with the actual FirebaseFirestore.getInstance()
    // once you integrate this code into your Android project.
    val db: Any = FirebaseFirestore.getInstance()

    var battryText by remember { mutableStateOf("Loading...") }
    var battryValue by remember { mutableDoubleStateOf(0.0) }

    val gaugeSize = 80.dp // Define the size for the gauge

    // Data fetching logic (Firebase Firestore)
    LaunchedEffect(Unit) {
        // NOTE: The actual Firebase listener code needs to be adapted to your project's
        // Firebase types and context. This section is a structural placeholder.
        try {
            (db as FirebaseFirestore).collection("Collection").document("Status")
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        battryText = "Error: ${error.message}"
                        battryValue = 0.0
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val firestoreValue = documentSnapshot.get("Battry")
                        val newBattryValue = (firestoreValue as? Number)?.toDouble() ?: 0.0

                        battryValue = newBattryValue
                        battryText = "${newBattryValue.toInt()}"
                    } else {
                        battryText = "Document Missing"
                        battryValue = 0.0
                    }
                }
        } catch (e: Exception) {
            battryText = "Failed to listen: ${e.message}"
            battryValue = 0.0
        }

        // Placeholder for immediate testing without Firebase
        battryValue = 75.0 // Example initial value
        battryText = "75"
    }

    // 💡 Outer Box for Top-Right placement
    Box(
        modifier = modifier
            .fillMaxSize() // Fills the entire screen
            .padding(16.dp), // Padding from the screen edges
        contentAlignment = Alignment.TopEnd // Aligns content to the Top Right
    ) {
        // Inner Box to stack the Gauge and the Text
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(gaugeSize)
        ) {
            // 1. Circular Gauge (Background Layer)
            // --- Dynamic Color Definitions ---
            val RedBattery = Color(0xFFF56662)     // Red for 0-9%
            val YellowBattery = Color(0xFFE7DA69)  // Yellow for 10-19%
            val GreenBattery = Color(0xFF70E174)    // Green for 20-100%
            val battryColor = when (battryValue) {
                in 0.0..9.0 -> RedBattery
                in 10.0..19.0 -> YellowBattery
                else -> GreenBattery
            }
            CircleBattryGauge(
                battry = battryValue,
                size = gaugeSize,
                strokeWidth = 10.dp,
                color = battryColor
            )

            // 2. Digital Battery Display (Foreground Layer, Centered)
            Text(
                text = battryText,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = battryColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun Preview() {
    ThunderBoltzTheme {
        // Preview the main composable
        androidx.compose.material3.Surface { // Surface provides background and elevation context
            Battry()
        }
    }
}