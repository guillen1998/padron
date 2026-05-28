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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denis.padron.data.PadronState
import com.denis.padron.data.PadronViewModel

@Composable
fun PlraScreen(viewModel: PadronViewModel, onBack: () -> Unit) {
    val state by viewModel.plraState.collectAsStateWithLifecycle()
    var cedula by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val accentBlue = Color(0xFF1565C0)
    val lightBlue  = Color(0xFF82B1FF)

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF030B18), Color(0xFF071528))))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0x221565C0), size.width*.65f, Offset(size.width*.15f, size.height*.2f))
            drawCircle(Color(0x161565C0), size.width*.50f, Offset(size.width*.90f, size.height*.78f))
        }
        Box(Modifier.fillMaxWidth().height(5.dp)
            .background(Brush.horizontalGradient(listOf(Color(0xFF0D47A1), accentBlue, Color(0xFF42A5F5)))))

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(5.dp))
            PadronScreenHeader(
                title="Padrón PLRA", subtitle="Partido Liberal Radical Auténtico",
                accentColor=lightBlue, backgroundColor=Color(0xDD0D47A1),
                onBack={ viewModel.resetPlra(); onBack() }
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(14.dp)
            ) {
                InfoCard("🔵","Registro PLRA",
                    "Buscá por cédula o por nombre y apellido para consultar tu inscripción en el partido.",
                    lightBlue)

                SearchCard(
                    cedula=cedula,
                    onCedulaChange={ cedula=it; if(it.isNotEmpty()) nombre="" },
                    isLoading=state is PadronState.Loading,
                    accentColor=accentBlue,
                    extraEnabled=nombre.isNotBlank(),
                    onSearch={
                        focusManager.clearFocus()
                        if(cedula.isNotBlank() || nombre.isNotBlank())
                            viewModel.consultarPlra(cedula, nombre)
                    },
                    extraContent = {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Center) {
                            HorizontalDivider(Modifier.weight(1f).padding(top=8.dp), color=Color.White.copy(.2f))
                            Text("  ó  ", color=Color.White.copy(.4f))
                            HorizontalDivider(Modifier.weight(1f).padding(top=8.dp), color=Color.White.copy(.2f))
                        }
                        OutlinedTextField(
                            value=nombre,
                            onValueChange={ nombre=it; if(it.isNotEmpty()) cedula="" },
                            label={ Text("Nombres y Apellidos") },
                            placeholder={ Text("Ej: García Juan") },
                            leadingIcon={ Icon(Icons.Default.Person, null, tint=accentBlue) },
                            keyboardOptions=KeyboardOptions(
                                keyboardType=KeyboardType.Text,
                                capitalization=KeyboardCapitalization.Words,
                                imeAction=ImeAction.Search
                            ),
                            keyboardActions=KeyboardActions(onSearch={
                                focusManager.clearFocus()
                                if(nombre.isNotBlank()) viewModel.consultarPlra("", nombre)
                            }),
                            singleLine=true, modifier=Modifier.fillMaxWidth(),
                            colors=fieldColors(accentBlue), shape=RoundedCornerShape(14.dp)
                        )
                    }
                )

                PadronResultSection(state=state, accentColor=lightBlue)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
