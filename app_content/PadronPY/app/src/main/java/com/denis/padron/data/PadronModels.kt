package com.denis.padron.data

data class ConsultaResult(
    val campos: List<Pair<String, String>> = emptyList(),
    val mensajeExtra: String? = null
)

sealed class PadronState {
    object Idle : PadronState()
    object Loading : PadronState()
    data class Success(val result: ConsultaResult) : PadronState()
    data class NotFound(val mensaje: String = "No se encontraron resultados") : PadronState()
    data class Error(val mensaje: String) : PadronState()
}
