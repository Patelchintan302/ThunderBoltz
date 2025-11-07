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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
            db.collection("Collection").document("Speed")
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        speedText = "Error: ${error.message}"
                        speedValue = 0.0
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // FIX: Safely retrieve the field value as a Number to prevent crashes.
                        val firestoreValue = documentSnapshot.get("speed")

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

            // 2. Digital Speed Display (Foreground Layer, centered visually)
            Text(
                text = speedText,
                fontSize = 50.sp, // Reduced size for better fit
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                // Shift the text up into the visual center of the semi-circle
                modifier = Modifier.offset(y = (-60).dp)
            )
        }


    }
}

@Preview(showSystemUi = true)
@Composable
fun Preview() {
    ThunderBoltzTheme {
        // Preview the main composable
        Speed(modifier = Modifier.padding(3.dp))
    }
}