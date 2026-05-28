package com.denis.padron.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PadronViewModel : ViewModel() {

    private val repo = PadronRepository()

    // TSJE
    private val _tsjeState = MutableStateFlow<PadronState>(PadronState.Idle)
    val tsjeState: StateFlow<PadronState> = _tsjeState.asStateFlow()

    fun consultarTsje(cedula: String, dia: Int, mes: Int, ano: String) {
        if (cedula.isBlank() || ano.length != 4) return
        _tsjeState.value = PadronState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            _tsjeState.value = repo.consultarTsje(cedula.trim(), dia, mes, ano.trim())
        }
    }

    fun resetTsje() { _tsjeState.value = PadronState.Idle }

    // PLRA
    private val _plraState = MutableStateFlow<PadronState>(PadronState.Idle)
    val plraState: StateFlow<PadronState> = _plraState.asStateFlow()

    fun consultarPlra(cedula: String, nombre: String = "") {
        if (cedula.isBlank() && nombre.isBlank()) return
        _plraState.value = PadronState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            _plraState.value = repo.consultarPlra(cedula.trim(), nombre.trim())
        }
    }

    fun resetPlra() { _plraState.value = PadronState.Idle }

    // ANR se maneja localmente en AnrScreen con WebView
    // (es una SPA React/Vue que requiere JavaScript del navegador)
}
