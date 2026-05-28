package com.denis.padron.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denis.padron.data.PadronState
import com.denis.padron.data.PadronViewModel

// NOTA: "Setiembre" es la grafía que usa la TSJE (sin p)
private val MESES = listOf(
    "Enero","Febrero","Marzo","Abril","Mayo","Junio",
    "Julio","Agosto","Setiembre","Octubre","Noviembre","Diciembre"
)

@Composable
fun TsjeScreen(viewModel: PadronViewModel, onBack: () -> Unit) {
    val state by viewModel.tsjeState.collectAsStateWithLifecycle()
    var cedula   by remember { mutableStateOf("") }
    var diaSelec by remember { mutableIntStateOf(1) }
    var mesSelec by remember { mutableIntStateOf(1) }
    var anoText  by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val accentRed = Color(0xFFD52B1E)

    Box(modifier = Modifier.fillMaxSize()
        .background(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.000f to Color(0xFF1A0505),
                    0.333f to Color(0xFF1A0505),
                    0.334f to Color(0xFF101520),
                    0.666f to Color(0xFF101520),
                    0.667f to Color(0xFF050A1A),
                    1.000f to Color(0xFF050A1A)
                )
            )
        )) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0x22D52B1E), size.width*.55f, Offset(0f, size.height*.20f))
            drawCircle(Color(0x11FFFFFF), size.width*.45f, Offset(size.width*.5f, size.height*.5f))
            drawCircle(Color(0x22002B7F), size.width*.55f, Offset(size.width, size.height*.80f))
        }
        Box(Modifier.fillMaxWidth().height(5.dp)
            .background(Brush.horizontalGradient(listOf(accentRed, Color.White, Color(0xFF002B7F)))))

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(5.dp))
            PadronScreenHeader(
                title = "Padrón General",
                subtitle = "Tribunal Superior de Justicia Electoral",
                accentColor = Color(0xFFFF8A80),
                backgroundColor = Color(0xDD8B0000),
                onBack = { viewModel.resetTsje(); onBack() }
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InfoCard("🇵🇾","Padrón Electoral Nacional",
                    "Ingresá tu cédula y fecha de nacimiento para consultar tu local de votación.",
                    accentRed)

                // Formulario
                TsjeForm(
                    cedula=cedula, onCedulaChange={ cedula=it },
                    diaSelec=diaSelec, onDiaChange={ diaSelec=it },
                    mesSelec=mesSelec, onMesChange={ mesSelec=it },
                    anoText=anoText, onAnoChange={ anoText=it },
                    isLoading=state is PadronState.Loading,
                    accentColor=accentRed,
                    onSearch={
                        focusManager.clearFocus()
                        if (cedula.isNotBlank() && anoText.length == 4)
                            viewModel.consultarTsje(cedula, diaSelec, mesSelec, anoText)
                    }
                )

                PadronResultSection(state=state, accentColor=accentRed)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TsjeForm(
    cedula:String, onCedulaChange:(String)->Unit,
    diaSelec:Int, onDiaChange:(Int)->Unit,
    mesSelec:Int, onMesChange:(Int)->Unit,
    anoText:String, onAnoChange:(String)->Unit,
    isLoading:Boolean, accentColor:Color, onSearch:()->Unit
) {
    var showDia by remember { mutableStateOf(false) }
    var showMes by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=Color(0x22FFFFFF)),
        elevation=CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {

            OutlinedTextField(
                value=cedula,
                onValueChange={ if(it.length<=12) onCedulaChange(it.filter(Char::isDigit)) },
                label={ Text("Número de Cédula") }, placeholder={ Text("Ej: 1234567") },
                leadingIcon={ Icon(Icons.Default.Search, null, tint=accentColor) },
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number, imeAction=ImeAction.Next),
                singleLine=true, modifier=Modifier.fillMaxWidth(),
                colors=fieldColors(accentColor), shape=RoundedCornerShape(14.dp)
            )

            Text("Fecha de Nacimiento", color=Color.White.copy(.6f), fontSize=13.sp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                // Día
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick={ showDia=true }, Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(14.dp),
                        colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White),
                        border=androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(.6f))
                    ) { Text("Día: $diaSelec", color=Color.White, fontSize=13.sp) }
                    DropdownMenu(expanded=showDia, onDismissRequest={ showDia=false }) {
                        (1..31).forEach { d ->
                            DropdownMenuItem(text={ Text("$d") }, onClick={ onDiaChange(d); showDia=false })
                        }
                    }
                }
                // Mes
                Box(Modifier.weight(1.6f)) {
                    OutlinedButton(onClick={ showMes=true }, Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(14.dp),
                        colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White),
                        border=androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(.6f))
                    ) { Text(MESES[mesSelec-1], color=Color.White, fontSize=13.sp) }
                    DropdownMenu(expanded=showMes, onDismissRequest={ showMes=false }) {
                        MESES.forEachIndexed { i, n ->
                            DropdownMenuItem(text={ Text(n) }, onClick={ onMesChange(i+1); showMes=false })
                        }
                    }
                }
                // Año
                OutlinedTextField(
                    value=anoText, onValueChange={ if(it.length<=4) onAnoChange(it.filter(Char::isDigit)) },
                    label={ Text("Año") }, placeholder={ Text("1998") },
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number, imeAction=ImeAction.Search),
                    keyboardActions=KeyboardActions(onSearch={ onSearch() }),
                    singleLine=true, modifier=Modifier.weight(1.3f),
                    colors=fieldColors(accentColor), shape=RoundedCornerShape(14.dp)
                )
            }

            Button(
                onClick=onSearch,
                enabled=cedula.isNotBlank() && anoText.length==4 && !isLoading,
                modifier=Modifier.fillMaxWidth().height(50.dp),
                shape=RoundedCornerShape(14.dp),
                colors=ButtonDefaults.buttonColors(
                    containerColor=accentColor, disabledContainerColor=accentColor.copy(.4f))
            ) {
                Text(if(isLoading) "Consultando…" else "Consultar",
                    color=Color.White, fontSize=16.sp, fontWeight=FontWeight.Bold)
            }
        }
    }
}
