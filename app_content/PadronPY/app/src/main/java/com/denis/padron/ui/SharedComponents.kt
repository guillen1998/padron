package com.denis.padron.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denis.padron.data.PadronState
import kotlin.math.cos
import kotlin.math.sin

// ─── Bandera Paraguay circular ────────────────────────────────────────────────

@Composable
fun ParaguayFlagBadge(size: Dp = 52.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w  = this.size.width
        val h  = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r  = minOf(w, h) / 2f

        // Círculo con tres franjas (gradiente de stops duros)
        drawCircle(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.000f to Color(0xFFD52B1E),
                    0.333f to Color(0xFFD52B1E),
                    0.334f to Color.White,
                    0.666f to Color.White,
                    0.667f to Color(0xFF002B7F),
                    1.000f to Color(0xFF002B7F)
                ),
                startY = 0f,
                endY   = h
            ),
            radius = r
        )

        // Borde blanco
        drawCircle(
            color  = Color.White.copy(alpha = 0.50f),
            radius = r - 1.dp.toPx(),
            style  = Stroke(1.8.dp.toPx())
        )

        // Estrella dorada (5 puntas)
        val ro = r * 0.50f
        val ri = r * 0.20f
        val star = Path().apply {
            for (i in 0 until 5) {
                val oa = Math.toRadians(-90.0 + i * 72.0)
                val ia = Math.toRadians(-90.0 + i * 72.0 + 36.0)
                val ox = (cx + ro * cos(oa)).toFloat()
                val oy = (cy + ro * sin(oa)).toFloat()
                val ix = (cx + ri * cos(ia)).toFloat()
                val iy = (cy + ri * sin(ia)).toFloat()
                if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                lineTo(ix, iy)
            }
            close()
        }
        drawPath(star, color = Color(0xFFFFD700))
        drawPath(star, color = Color(0xFFA07820), style = Stroke(0.6.dp.toPx()))
    }
}

// ─── Franja de la bandera paraguaya ──────────────────────────────────────────

@Composable
private fun FlagStrip(height: Dp = 4.dp) {
    Row(modifier = Modifier.fillMaxWidth().height(height)) {
        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD52B1E)))
        Box(Modifier.weight(1f).fillMaxHeight().background(Color.White))
        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF002B7F)))
    }
}

// ─── Header Home (centrado + bandera) ────────────────────────────────────────

@Composable
fun AppHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlagStrip(height = 5.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF6B0000), Color(0xFF0A0E1A), Color(0xFF001966))
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ParaguayFlagBadge(size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Padrón General del Paraguay",
                        color      = Color.White,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    )
                    Text(
                        "2026  •  Desarrollado por Denis Guillen",
                        color     = Color(0xFFFFD700).copy(.80f),
                        fontSize  = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }

        FlagStrip(height = 3.dp)
    }
}

// ─── Header sub-pantalla ─────────────────────────────────────────────────────

@Composable
fun PadronScreenHeader(
    title          : String,
    subtitle       : String,
    accentColor    : Color,
    backgroundColor: Color = Color(0x99383838),
    onBack         : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(title,    color = Color.White,  fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = accentColor,  fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Desarrollado por Denis Guillen",
                    color     = Color(0xFFFFD700).copy(.75f),
                    fontSize  = 10.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

// ─── SearchCard ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchCard(
    cedula         : String,
    onCedulaChange : (String) -> Unit,
    onSearch       : () -> Unit,
    isLoading      : Boolean,
    accentColor    : Color,
    extraEnabled   : Boolean = false,
    extraContent   : (@Composable ColumnScope.() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val doSearch = { focusManager.clearFocus(); onSearch() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
        elevation= CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value         = cedula,
                onValueChange = { if(it.length <= 12) onCedulaChange(it.filter(Char::isDigit)) },
                label         = { Text("Número de Cédula") },
                placeholder   = { Text("Ej: 1234567") },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = accentColor) },
                keyboardOptions= KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions= KeyboardActions(onSearch = { doSearch() }),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = fieldColors(accentColor),
                shape         = RoundedCornerShape(14.dp)
            )

            extraContent?.let { it() }

            Button(
                onClick  = doSearch,
                enabled  = (cedula.isNotBlank() || extraEnabled) && !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor        = accentColor,
                    disabledContainerColor= accentColor.copy(.4f)
                )
            ) {
                Text(
                    if (isLoading) "Consultando…" else "Consultar",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun fieldColors(accentColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = accentColor,
    unfocusedBorderColor      = Color.White.copy(.4f),
    focusedLabelColor         = accentColor,
    unfocusedLabelColor       = Color.White.copy(.6f),
    focusedTextColor          = Color.White,
    unfocusedTextColor        = Color.White,
    cursorColor               = accentColor,
    focusedPlaceholderColor   = Color.White.copy(.35f),
    unfocusedPlaceholderColor = Color.White.copy(.35f)
)

// ─── Sección de resultados ────────────────────────────────────────────────────

@Composable
fun PadronResultSection(state: PadronState, accentColor: Color) {
    AnimatedContent(
        targetState  = state,
        transitionSpec = { fadeIn(initialAlpha = 0f) togetherWith fadeOut() },
        label        = "result_anim"
    ) { s ->
        when (s) {
            is PadronState.Idle    -> Box(Modifier.fillMaxWidth())
            is PadronState.Loading -> {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Consultando el padrón…", color = Color.White.copy(.7f), fontSize = 14.sp)
                    }
                }
            }
            is PadronState.Success  -> ResultCard(s.result, accentColor)
            is PadronState.NotFound -> StatusCard("🔍", "No encontrado", s.mensaje, Color(0xFFFFA726))
            is PadronState.Error    -> StatusCard("⚠️", "Error", s.mensaje, Color(0xFFEF5350))
        }
    }
}

@Composable
fun ResultCard(result: com.denis.padron.data.ConsultaResult, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0x28FFFFFF)),
        elevation= CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Box(Modifier.size(8.dp).background(accentColor, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(8.dp))
                Text("Datos encontrados", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = Color.White.copy(.15f))
            Spacer(Modifier.height(10.dp))
            result.campos.forEach { (label, value) ->
                if (label.isEmpty()) {
                    Text(value, color = Color.White.copy(.8f), fontSize = 14.sp)
                } else {
                    Text(label.uppercase(), color = Color.White.copy(.5f), fontSize = 10.sp,
                        fontWeight = FontWeight.Medium, letterSpacing = .8.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun StatusCard(emoji: String, title: String, message: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(.15f)),
        elevation= CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(message, color = Color.White.copy(.75f), fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

// ─── Info card ────────────────────────────────────────────────────────────────

@Composable
fun InfoCard(emoji: String, title: String, desc: String, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = accentColor.copy(.15f)),
        elevation= CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Column {
                Text(title, color = accentColor,           style = MaterialTheme.typography.labelLarge)
                Text(desc,  color = Color.White.copy(.65f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
