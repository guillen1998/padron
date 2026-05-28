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

// ─── Logo mapa Paraguay ───────────────────────────────────────────────────────

@Composable
fun ParaguayMapLogo(size: Dp = 52.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val mapPath = Path().apply {
            moveTo(.06f*w,.12f*h); lineTo(.64f*w,.05f*h); lineTo(.97f*w,.22f*h)
            lineTo(.95f*w,.76f*h); lineTo(.80f*w,.96f*h); lineTo(.42f*w,.97f*h)
            lineTo(.13f*w,.83f*h); lineTo(.04f*w,.46f*h); close()
        }
        drawPath(mapPath, color = Color(0xFFD52B1E).copy(.92f))
        val chacoPath = Path().apply {
            moveTo(.06f*w,.12f*h); lineTo(.51f*w,.05f*h)
            quadraticTo(.47f*w,.51f*h,.44f*w,.97f*h)
            lineTo(.42f*w,.97f*h); lineTo(.13f*w,.83f*h); lineTo(.04f*w,.46f*h); close()
        }
        drawPath(chacoPath, color = Color(0xFF002B7F).copy(.80f))
        val band = Path().apply {
            moveTo(.04f*w,.47f*h); lineTo(.95f*w,.47f*h)
            lineTo(.95f*w,.56f*h); lineTo(.04f*w,.56f*h); close()
        }
        drawPath(band, color = Color.White.copy(.22f))
        val river = Path().apply { moveTo(.51f*w,.05f*h); quadraticTo(.47f*w,.51f*h,.44f*w,.97f*h) }
        drawPath(river, color = Color.White.copy(.75f), style = Stroke(1.8.dp.toPx()))
        drawPath(mapPath, color = Color.White.copy(.55f), style = Stroke(1.5.dp.toPx()))
        val cx=.70f*w; val cy=.52f*h; val ro=.085f*w; val ri=.038f*w
        val star = Path().apply {
            for (i in 0 until 5) {
                val oa=Math.toRadians(-90.0+i*72.0); val ia=Math.toRadians(-90.0+i*72.0+36.0)
                val ox=(cx+ro*cos(oa)).toFloat(); val oy=(cy+ro*sin(oa)).toFloat()
                val ix=(cx+ri*cos(ia)).toFloat(); val iy=(cy+ri*sin(ia)).toFloat()
                if(i==0) moveTo(ox,oy) else lineTo(ox,oy); lineTo(ix,iy)
            }
            close()
        }
        drawPath(star, color = Color.White)
    }
}

// ─── Header Home ──────────────────────────────────────────────────────────────

@Composable
fun AppHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x99383838))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ParaguayMapLogo(size = 52.dp)
            Column {
                Text("Padrón General", color = Color.White, fontSize = 21.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = .3.sp)
                Text("del Paraguay  •  2026", color = Color.White.copy(.65f), fontSize = 13.sp)
            }
        }
    }
}

// ─── Header de sub-pantalla — acepta color personalizado ─────────────────────

@Composable
fun PadronScreenHeader(
    title          : String,
    subtitle       : String,
    accentColor    : Color,
    backgroundColor: Color = Color(0x99383838),   // ← color por sección
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
                Text(title,    color = Color.White,   fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = accentColor,   fontSize = 12.sp)
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

            // Botón SIN spinner — el único spinner está en la sección de resultados
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
                // Un solo spinner
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

// ─── Info card (cabecera descriptiva de cada sección) ────────────────────────

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
                Text(title, color = accentColor,          style = MaterialTheme.typography.labelLarge)
                Text(desc,  color = Color.White.copy(.65f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
