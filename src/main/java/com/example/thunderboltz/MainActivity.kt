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

// --- MainActivity Class ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThunderBoltzTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // All three main components integrated here:
                    Indicator(
                        modifier = Modifier.padding(innerPadding)
                    )
                    Speed(
                        modifier = Modifier.padding(innerPadding)
                    )
                    Battry(
                        modifier = Modifier.padding(innerPadding)
                    )
                    HeadlightIndicator(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------
// --- 1. Speed Gauge Composable (Center Screen) ---
// -----------------------------------------------------------------------------------

@Composable
fun SemiCircleSpeedGauge(
    speed: Double,
    maxSpeed: Double,
    modifier: Modifier = Modifier,
    size: Dp,
    strokeWidth: Dp = 20.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // Calculate the percentage of speed and map it to 0-180 degrees.
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

        val arcSize = Size(size.toPx() - strokeThickness, size.toPx() - strokeThickness)

        // 1. Draw the inactive track (the background arc)
        drawArc(
            color = Color.LightGray.copy(alpha = 0.5f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = arcSize,
            style = Stroke(strokeThickness, cap = StrokeCap.Round)
        )

        // 2. Draw the active progress (the speed arc)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = sweep,
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
    var speedText by remember { mutableStateOf("0") }
    var speedValue by remember { mutableDoubleStateOf(0.0) }

    val maxSpeed = 150.0
    val gaugeSize = 250.dp

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
                        val firestoreValue = documentSnapshot.get("Speed")
                        val newSpeedValue = (firestoreValue as? Number)?.toDouble() ?: 0.0

                        speedValue = newSpeedValue
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

        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .size(gaugeSize)
                .padding(bottom = 32.dp)
        ) {
            // 1. Semi-Circular Gauge
            SemiCircleSpeedGauge(
                speed = speedValue,
                maxSpeed = maxSpeed,
                size = gaugeSize,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(y = (-60).dp)
            ) {
                Text(
                    text = speedText,
                    fontSize = 70.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km/h",
                    fontSize = 30.sp,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------
// --- 2. Battery Gauge Composable (Top Right) ---
// -----------------------------------------------------------------------------------

@Composable
fun CircleBattryGauge(
    battry: Double,
    modifier: Modifier = Modifier,
    size: Dp,
    strokeWidth: Dp = 20.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // Calculate the percentage of battery and map it to 0-360 degrees.
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

        val arcSize = Size(size.toPx() - strokeThickness, size.toPx() - strokeThickness)
        val startAngle = 270f

        // 1. Draw the inactive track (the background FULL circle)
        drawArc(
            color = Color.LightGray.copy(alpha = 0.5f),
            startAngle = startAngle,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = arcSize,
            style = Stroke(strokeThickness, cap = StrokeCap.Round)
        )

        // 2. Draw the active progress (the battery arc)
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = arcSize,
            style = Stroke(strokeThickness, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun Battry(modifier: Modifier = Modifier) {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    var battryText by remember { mutableStateOf("Loading...") }
    var battryValue by remember { mutableDoubleStateOf(0.0) }

    val gaugeSize = 80.dp

    // Data fetching logic (Firebase Firestore)
    LaunchedEffect(Unit) {
        try {
            db.collection("Collection").document("Status")
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
    }

    // Outer Box for Top-Right placement
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        // Inner Box to stack the Gauge and the Text
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(gaugeSize)
        ) {
            // Dynamic Color Definitions
            val RedBattery = Color(0xFFF56662)
            val YellowBattery = Color(0xFFE7DA69)
            val GreenBattery = Color(0xFF70E174)
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

            // Digital Battery Display
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

// -----------------------------------------------------------------------------------
// --- 3. Indicator Composable (Bottom Center) ---
// -----------------------------------------------------------------------------------

@Composable
fun Indicator(modifier: Modifier = Modifier) {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    var indicatorValue by remember { mutableDoubleStateOf(0.0) }

    // State to hold the text to display below the arrow
    var directionText by remember { mutableStateOf("") }

    // Data Fetching (Firebase Firestore Listener)
    LaunchedEffect(Unit) {
        try {
            db.collection("Collection").document("Status")
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        indicatorValue = 0.0
                        directionText = "" // Clear text on error
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val firestoreValue = documentSnapshot.get("Indicator")
                        val newValue = (firestoreValue as? Number)?.toDouble() ?: 0.0
                        indicatorValue = newValue

                        // Update the text based on the new value
                        directionText = when (newValue.toInt()) {
                            1 -> "RIGHT"
                            -1 -> "LEFT"
                            else -> ""
                        }
                    } else {
                        indicatorValue = 0.0
                        directionText = ""
                    }
                }
        } catch (e: Exception) {
            indicatorValue = 0.0
            directionText = ""
        }
    }

    // UI Placement (Centered at the bottom)
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Determine which arrow and color to use
        val indicatorArrow: String
        val color: Color

        when (indicatorValue.toInt()) {
            1 -> { // Right Indicator
                indicatorArrow = "→"
                color = Color.Green
            }
            -1 -> { // Left Indicator
                indicatorArrow = "←"
                color = Color.Green
            }
            else -> { // Off (0)
                indicatorArrow = ""
                color = Color.Transparent
            }
        }

        // Display the Arrow and the Direction Text in a Column if active
        if (indicatorArrow.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Arrow Symbol
                Text(
                    text = indicatorArrow,
                    fontSize = 100.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(200.dp)
                )

                // 2. Direction Text (New Addition)
                Text(
                    text = directionText,
                    fontSize = 24.sp, // Slightly smaller font size
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp) // Little space above the text
                )
            }
        }
    }
}

@Composable
fun HeadlightIndicator(modifier: Modifier = Modifier) {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    var lightValue by remember { mutableDoubleStateOf(0.0) }

    // Data Fetching (Firestore Listener)
    LaunchedEffect(Unit) {
        try {
            db.collection("Collection").document("Status")
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        lightValue = 0.0
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val firestoreValue = documentSnapshot.get("Headlights")
                        lightValue = (firestoreValue as? Number)?.toDouble() ?: 0.0
                    } else {
                        lightValue = 0.0
                    }
                }
        } catch (e: Exception) {
            lightValue = 0.0
        }
    }

    // UI Placement (Centered at the top, below the Battery/Top Right)
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val lightSymbol: String
        val color: Color


        when (lightValue.toInt()) {
            2 -> { // High Beam
                // Standard symbol for high beam (D with 5 horizontal lines)
                lightSymbol = "\uD83D\uDCA3" // Using a symbol that might represent brightness/power
                color = Color.Blue // Standard color for high beam indicator
            }
            1 -> { // Low Beam
                // Standard symbol for low beam (semi-circle with 3 diagonal lines)
                lightSymbol = "\uD83D\uDCA1" // Using a symbol that might represent light/low power
                color = Color.Green // Common color for low beam indicator
            }
            else -> { // Off (0)
                lightSymbol = ""
                color = Color.Transparent
            }
        }

        if (lightSymbol.isNotEmpty()) {
            Text(
                text = lightSymbol,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// -----------------------------------------------------------------------------------
// --- Preview Composable (for Android Studio) ---
// -----------------------------------------------------------------------------------

@Preview(showSystemUi = true)
@Composable
fun Preview() {
    ThunderBoltzTheme {
        // Preview the main composable setup
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold { innerPadding ->
                Indicator(modifier = Modifier.padding(innerPadding))
                Speed(modifier = Modifier.padding(innerPadding))
                Battry(modifier = Modifier.padding(innerPadding))
                HeadlightIndicator(modifier = Modifier.padding(innerPadding))

            }
        }
    }
}