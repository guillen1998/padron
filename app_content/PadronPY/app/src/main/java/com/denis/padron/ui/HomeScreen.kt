package com.denis.padron.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Ballot
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigateToTsje: () -> Unit,
    onNavigateToAnr: () -> Unit,
    onNavigateToPlra: () -> Unit
) {
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }

    // Double-back to exit — only active while HomeScreen is composed
    BackHandler(enabled = true) {
        if (backPressedOnce) {
            (context as? android.app.Activity)?.finishAffinity()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Presioná de nuevo para salir", Toast.LENGTH_SHORT).show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                backPressedOnce = false
            }, 2000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF080C14), Color(0xFF0F1823), Color(0xFF080C14))
                )
            )
    ) {
        // Subtle background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x10D52B1E),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.15f, size.height * 0.25f)
            )
            drawCircle(
                color = Color(0x10002B7F),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.85f, size.height * 0.75f)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Seleccioná el padrón a consultar",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                // ── TSJE ──────────────────────────────────────────────────
                PadronHomeCard(
                    title = "Padrón General",
                    subtitle = "TRIBUNAL SUPERIOR DE JUSTICIA ELECTORAL",
                    description = "Consultá tu inscripción en el padrón electoral nacional y tu local de votación",
                    icon = Icons.Default.HowToVote,
                    gradient = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.000f to Color(0xCCD52B1E),
                            0.333f to Color(0xCCD52B1E),
                            0.334f to Color(0x66FFFFFF),
                            0.666f to Color(0x66FFFFFF),
                            0.667f to Color(0xCC002B7F),
                            1.000f to Color(0xCC002B7F)
                        )
                    ),
                    glowColor = Color(0x22D52B1E),
                    accentColor = Color(0xFFFFFFFF),
                    flagColors = listOf(Color(0xFFD52B1E), Color.White, Color(0xFF002B7F)),
                    onClick = onNavigateToTsje
                )

                // ── ANR ───────────────────────────────────────────────────
                PadronHomeCard(
                    title = "Padrón ANR",
                    subtitle = "ASOCIACIÓN NACIONAL REPUBLICANA",
                    description = "Verificá tu afiliación al Partido Colorado y tu seccional de inscripción",
                    icon = Icons.Default.Ballot,
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xCC6B0000), Color(0xCCCC0000), Color(0xCC8B0000)),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 400f)
                    ),
                    glowColor = Color(0x33CC0000),
                    accentColor = Color(0xFFFF5252),
                    flagColors = listOf(Color(0xFFCC0000), Color(0xFFFF6666), Color(0xFF8B0000)),
                    onClick = onNavigateToAnr
                )

                // ── PLRA ──────────────────────────────────────────────────
                PadronHomeCard(
                    title = "Padrón PLRA",
                    subtitle = "PARTIDO LIBERAL RADICAL AUTÉNTICO",
                    description = "Consultá tu inscripción en el padrón del Partido Liberal",
                    icon = Icons.Default.People,
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xCC0A2472), Color(0xCC1565C0), Color(0xCC0D47A1)),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 400f)
                    ),
                    glowColor = Color(0x331565C0),
                    accentColor = Color(0xFF82B1FF),
                    flagColors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5), Color(0xFF0D47A1)),
                    onClick = onNavigateToPlra
                )

                // Footer
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Padrón General Paraguay  •  2026\nDatos provistos por TSJE, ANR y PLRA\nDesarrollado por Denis Guillén",
                    color = Color.White.copy(alpha = 0.28f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PadronHomeCard(
    title: String,
    subtitle: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    glowColor: Color,
    accentColor: Color,
    flagColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(glowColor)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // Decorative background circles
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.height * 0.9f,
                    center = Offset(size.width * 0.88f, size.height * 0.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = size.height * 0.5f,
                    center = Offset(size.width * 0.08f, size.height * 0.85f)
                )
            }

            // Bottom flag strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.horizontalGradient(colors = flagColors))
            )

            // Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = subtitle,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
