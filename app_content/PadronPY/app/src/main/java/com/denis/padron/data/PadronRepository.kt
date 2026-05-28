package com.denis.padron.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class PadronRepository {

    private val ua = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // ─── TSJE ─────────────────────────────────────────────────────────────────
    // La página usa enctype="multipart/form-data" y requiere el campo oculto buscar=si
    // Campos: cedula (text), dia (1-31), mes (1-12), anio (text), buscar="si"

    fun consultarTsje(cedula: String, dia: Int, mes: Int, ano: String): PadronState {
        return try {
            val url = "https://padron.tsje.gov.py/"

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("cedula", cedula)
                .addFormDataPart("dia",    dia.toString())
                .addFormDataPart("mes",    mes.toString())
                .addFormDataPart("anio",   ano)
                .addFormDataPart("buscar", "si")
                .build()

            val resp = client.newCall(
                Request.Builder()
                    .url(url)
                    .post(body)
                    .header("User-Agent", ua)
                    .header("Referer",    url)
                    .build()
            ).execute()
            val html = resp.body?.string() ?: ""
            resp.close()

            parseHtml(html)
        } catch (e: Exception) {
            PadronState.Error("Error de conexión: ${e.localizedMessage}")
        }
    }

    // ─── PLRA ─────────────────────────────────────────────────────────────────
    // La página es una SPA que llama a buscar_padron.php con GET + Authorization Bearer
    // Devuelve JSON: array de personas

    fun consultarPlra(cedula: String, nombre: String = ""): PadronState {
        return try {
            val param = if (cedula.isNotEmpty())
                "cedula=${URLEncoder.encode(cedula, "UTF-8")}"
            else
                "nombre=${URLEncoder.encode(nombre, "UTF-8")}"

            val url = "https://plra.org.py/public/buscar_padron.php?$param"

            val resp = client.newCall(
                Request.Builder()
                    .url(url)
                    .header("User-Agent",   ua)
                    .header("Authorization","Bearer antijakerclavesecreta321")
                    .header("Referer",      "https://plra.org.py/public/buscar_enrcp.php")
                    .header("Accept",       "application/json")
                    .build()
            ).execute()
            val json = resp.body?.string() ?: ""
            resp.close()

            parsePlraJson(json)
        } catch (e: Exception) {
            PadronState.Error("Error de conexión: ${e.localizedMessage}")
        }
    }

    // ─── Parser JSON PLRA ─────────────────────────────────────────────────────

    private fun parsePlraJson(json: String): PadronState {
        val trimmed = json.trim()
        return try {
            // Respuesta de error del servidor
            if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                val err = obj.optString("error", "")
                if (err.isNotEmpty()) return PadronState.Error(err)
            }

            val arr = JSONArray(trimmed)

            if (arr.length() == 0)
                return PadronState.NotFound("No encontrado en el padrón del PLRA")

            val campos = mutableListOf<Pair<String, String>>()

            if (arr.length() == 1) {
                // Un único resultado: mostrar datos completos
                val p = arr.getJSONObject(0)
                val afiliado = p.optString("afiliacion_plra_2025", "").lowercase() == "si"
                campos.add("ESTADO" to if (afiliado) "✓ Afiliado al PLRA" else "✗ No afiliado")

                fun add(label: String, key: String) {
                    val v = p.optString(key, "").trim()
                    if (v.isNotEmpty() && v != "null") campos.add(label to v)
                }
                add("NOMBRE",           "nombre")
                add("APELLIDO",         "apellido")
                add("SEXO",             "sexo")
                add("FECHA NACIMIENTO", "fec_nac")
                add("FECHA INSCRIPCIÓN","fec_inscri")
                add("DIRECCIÓN",        "direcc")
                add("DEPARTAMENTO",     "departamento_nombre")
                add("DISTRITO",         "distrito_nombre")
                add("ZONA",             "zona_nombre")
                add("COMITÉ",           "comite_nombre")
                add("LOCAL INTERNAS",   "local_inerna")
                add("LOCAL GENERALES",  "local_genrales")
                add("MESA",             "mesa")
            } else {
                // Múltiples resultados (búsqueda por nombre): resumen
                campos.add("RESULTADOS" to "${arr.length()} personas encontradas")
                val max = minOf(arr.length(), 8)
                for (i in 0 until max) {
                    val p   = arr.getJSONObject(i)
                    val nom = p.optString("nombresYApellido", "—").trim()
                    val af  = p.optString("afiliacion_plra_2025", "").lowercase() == "si"
                    val com = p.optString("comite_nombre", "").trim()
                    val dep = p.optString("departamento_nombre", "").trim()
                    val icon = if (af) "✓" else "✗"
                    val extra = listOf(com, dep).filter { it.isNotEmpty() }.joinToString(" · ")
                    campos.add("" to "$icon $nom${if (extra.isNotEmpty()) " — $extra" else ""}")
                }
                if (arr.length() > 8)
                    campos.add("" to "… y ${arr.length() - 8} más. Refiná con la cédula exacta.")
            }

            if (campos.isNotEmpty()) PadronState.Success(ConsultaResult(campos))
            else PadronState.NotFound("No se obtuvieron datos.")
        } catch (e: Exception) {
            PadronState.Error("Error al procesar respuesta: ${e.localizedMessage}")
        }
    }

    // ─── Parser HTML genérico (TSJE + fallback) ───────────────────────────────

    private fun parseHtml(html: String): PadronState {
        val doc    = Jsoup.parse(html)
        val campos = mutableListOf<Pair<String, String>>()

        // 1. Tablas clave→valor (típico de sitios de gobierno PHP)
        doc.select("table tr").forEach { row ->
            val cells = row.select("td, th")
            if (cells.size >= 2) {
                val k = cells[0].text().trim().removeSuffix(":").trim()
                val v = cells[1].text().trim()
                if (k.isNotEmpty() && v.isNotEmpty() && k != v && k.length < 60 && v.length < 200)
                    campos.add(k to v)
            }
        }

        // 2. Definition lists
        if (campos.isEmpty()) {
            doc.select("dt").forEach { dt ->
                val dd = dt.nextElementSibling()
                if (dd?.tagName() == "dd") campos.add(dt.text().trim() to dd.text().trim())
            }
        }

        // 3. TSJE: labels + inputs disabled en div.form-style-agile
        if (campos.isEmpty()) {
            doc.select("div.form-style-agile").forEach { div ->
                val labelEl = div.selectFirst("label")
                val inputEl = div.selectFirst("input")
                val value   = inputEl?.attr("value")?.trim()?.takeIf { it.isNotBlank() } ?: ""
                // El label puede empezar con texto de icono; quitar caracteres no-letras al inicio
                val rawLabel  = labelEl?.text()?.trim() ?: ""
                val cleanLabel = rawLabel.dropWhile { !it.isLetter() }.trim()
                if (cleanLabel.isNotEmpty() && value.isNotEmpty())
                    campos.add(cleanLabel to value)
            }
        }

        // 4. Tarjeta PLRA / sitios con headings + badges (fallback genérico)
        if (campos.isEmpty()) {
            doc.select("h1, h2, h3, h4, strong, .card-title").forEach { el ->
                if (el.closest("nav") != null || el.closest("header") != null ||
                    el.closest("footer") != null) return@forEach
                val text = el.text().trim()
                // Solo agregar si parece un nombre de persona (no un mensaje de error)
                if (text.matches(Regex("[A-ZÁÉÍÓÚÜÑ\\s]{4,}")) &&
                    text.split(" ").size in 2..5 &&
                    !text.startsWith("NO ") && !text.contains(" TIENE "))
                    campos.add("NOMBRE COMPLETO" to text)
            }
            doc.select("[class*=badge],[class*=chip],[class*=tag]").forEach { b ->
                if (b.closest("nav") == null) {
                    val t = b.text().trim()
                    if (t.isNotEmpty() && t.length < 80 &&
                        !t.equals("SISTEMA PLRA", ignoreCase = true) &&
                        !t.equals("Sistema PLRA",  ignoreCase = true))
                        campos.add("ESTADO" to t)
                }
            }
            doc.select("p, li, small").forEach { el ->
                if (el.children().size > 0) return@forEach
                val text = el.text().trim()
                if (text.length !in 2..120) return@forEach
                if (text.contains("©") || text.contains("@") ||
                    text.lowercase().let { t ->
                        t.contains("buscá por") || t.contains("consulta del") ||
                        t.contains("sistema plra") || t.contains("todos los derechos")
                    }) return@forEach
                val ci = text.indexOf(":")
                if (ci in 1..40) campos.add(text.substring(0, ci).trim() to text.substring(ci + 1).trim())
                else if (text.all { c -> c.isLetter() || c.isWhitespace() } && text.split(" ").size in 1..5)
                    campos.add("" to text)
            }
        }

        // 5. Detección "no encontrado"
        val bodyText = doc.body()?.text()?.lowercase() ?: ""
        val notFound = listOf(
            "no se encontr", "no encontrado", "no figura", "no registr",
            "afiliado no encontrado", "ninguna inscripci"
        ).any { bodyText.contains(it) }

        return when {
            campos.isNotEmpty() -> PadronState.Success(ConsultaResult(campos))
            notFound            -> PadronState.NotFound("No encontrado en el padrón")
            else                -> PadronState.NotFound("No se obtuvieron datos. Verificá la información ingresada.")
        }
    }
}
