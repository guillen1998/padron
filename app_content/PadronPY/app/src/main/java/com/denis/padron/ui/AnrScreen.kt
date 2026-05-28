package com.denis.padron.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.denis.padron.data.ConsultaResult
import com.denis.padron.data.PadronState
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun AnrScreen(onBack: () -> Unit) {
    var cedula      by remember { mutableStateOf("") }
    var triggerKey  by remember { mutableIntStateOf(0) }
    var padronState by remember { mutableStateOf<PadronState>(PadronState.Idle) }
    val focusManager = LocalFocusManager.current
    val accentRed    = Color(0xFFCC0000)
    val lightRed     = Color(0xFFFF5252)

    if (triggerKey > 0) {
        AnrWebView(
            triggerKey  = triggerKey,
            cedula      = cedula,
            onResult    = { campos -> padronState = PadronState.Success(ConsultaResult(campos)) },
            onNotFound  = { msg    -> padronState = PadronState.NotFound(msg) },
            onError     = { msg    -> padronState = PadronState.Error(msg) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF0F0404), Color(0xFF1A0808))))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0x22CC0000), size.width*.65f, Offset(size.width*.85f, size.height*.2f))
            drawCircle(Color(0x16CC0000), size.width*.50f, Offset(size.width*.10f, size.height*.8f))
        }
        Box(Modifier.fillMaxWidth().height(5.dp)
            .background(Brush.horizontalGradient(listOf(Color(0xFF8B0000), accentRed, Color(0xFF8B0000)))))

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(5.dp))
            PadronScreenHeader(
                title="Padrón ANR",
                subtitle="Asociación Nacional Republicana — Partido Colorado",
                accentColor=lightRed, backgroundColor=Color(0xDDCC0000),
                onBack={ padronState=PadronState.Idle; onBack() }
            )
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(14.dp)) {
                InfoCard("🟥","Padrón ANR 2026",
                    "Consultá si estás inscripto en el padrón del Partido Colorado y verificá tu seccional.",
                    lightRed)
                SearchCard(
                    cedula=cedula, onCedulaChange={ cedula=it },
                    isLoading=padronState is PadronState.Loading,
                    accentColor=accentRed,
                    onSearch={
                        focusManager.clearFocus()
                        if(cedula.isNotBlank()){ padronState=PadronState.Loading; triggerKey++ }
                    }
                )
                PadronResultSection(state=padronState, accentColor=lightRed)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── WebView oculto ─────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AnrWebView(
    triggerKey: Int,
    cedula    : String,
    onResult  : (List<Pair<String, String>>) -> Unit,
    onNotFound: (String) -> Unit,
    onError   : (String) -> Unit
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    key(triggerKey) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled  = true
                    settings.userAgentString    =
                        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                    var resultHandled = false

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun sendResult(json: String) {
                            mainHandler.post {
                                if (!resultHandled) {
                                    resultHandled = true
                                    parseAnrJson(json, onResult, onNotFound)
                                }
                            }
                        }
                    }, "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, pageUrl: String) {
                            // Esperar 4s para que la SPA monte completamente, luego llenar y enviar
                            mainHandler.postDelayed({
                                val fillJs = buildAnrFillAndExtractJs(cedula)
                                view.evaluateJavascript(fillJs, null)
                            }, 4000L)

                            // Fallback: forzar extracción después de 25s
                            mainHandler.postDelayed({
                                if (!resultHandled) {
                                    resultHandled = true
                                    onNotFound("Tiempo de espera agotado. Intentá de nuevo.")
                                }
                            }, 25000L)
                        }

                        override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                            if (req.isForMainFrame && !resultHandled) {
                                resultHandled = true
                                onError("Sin conexión o sitio no disponible")
                            }
                        }
                    }

                    loadUrl("https://www.anr.org.py/pre-padron-2026/")
                }
            },
            modifier = Modifier.size(1.dp, 1.dp)
        )
    }
}

// ── JS: llenar formulario + esperar resultado + extraer ────────────────────────
// La estructura de ANR es lista <ul><li> con los labels conocidos.
// Busca cada label por texto y extrae el valor que le sigue.

private fun buildAnrFillAndExtractJs(cedula: String) = """
(function(){
  var ci = '$cedula';

  // Campos conocidos del resultado ANR (exactamente como aparecen en la página)
  var LABELS = [
    'CEDULA DE IDENTIDAD', 'NOMBRES', 'APELLIDOS', 'FECHA DE NACIMIENTO',
    'DEPARTAMENTO', 'DISTRITO', 'SECCIONAL', 'LOCAL', 'MESA', 'ORDEN'
  ];

  function setNative(el, v) {
    if(!el) return;
    try {
      var s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
      if(s && s.set) s.set.call(el, v);
      else el.value = v;
    } catch(e) { el.value = v; }
    ['focus','input','change','keyup'].forEach(function(ev) {
      el.dispatchEvent(new Event(ev, {bubbles:true}));
    });
  }

  // ─── 1. Llenar el input de cédula ─────────────────────────────────────────
  var inp = null;
  Array.from(document.querySelectorAll('input')).forEach(function(i) {
    if(!inp && i.type !== 'hidden' && i.type !== 'checkbox' &&
       i.type !== 'submit' && i.offsetWidth > 0 && !i.disabled) inp = i;
  });
  if(inp) setNative(inp, ci);

  // ─── 2. Click en "Consultar" ──────────────────────────────────────────────
  setTimeout(function() {
    var btn = null;
    Array.from(document.querySelectorAll('button,input[type=button],input[type=submit]')).forEach(function(b) {
      if(!btn && /consul|buscar/i.test((b.textContent||'')+(b.value||''))) btn = b;
    });
    if(!btn) {
      var all = Array.from(document.querySelectorAll('button')).filter(function(b) { return b.offsetWidth>0; });
      if(all.length) btn = all[all.length-1];
    }
    if(btn) btn.click();

    // ─── 3. Esperar resultado y extraer ─────────────────────────────────────
    // Polling cada 500ms durante máximo 20s
    var attempts = 0;
    var interval = setInterval(function() {
      attempts++;

      // Comprobar "Afiliado no encontrado" (sin datos)
      var bodyTxt = document.body.textContent;
      var liItems = Array.from(document.querySelectorAll('ul li, ol li'));

      // Extraer por labels conocidos
      var results = [];
      LABELS.forEach(function(label) {
        // Buscar elemento que contenga el label
        var found = liItems.find(function(li) {
          return li.textContent.indexOf(label) !== -1;
        });
        if(found) {
          var fullText = found.textContent.trim();
          var idx = fullText.indexOf(label);
          if(idx !== -1) {
            var after = fullText.substring(idx + label.length).trim().replace(/^[:\\s]+/, '');
            if(after && after.length > 0 && after !== label) {
              results.push([label, after]);
            }
          }
        }
      });

      // También buscar en divs / spans si no hay <li>
      if(!results.length) {
        LABELS.forEach(function(label) {
          var xpath = "//*[contains(text(),'"+label+"')]";
          try {
            var iter = document.evaluate(xpath, document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
            var node = iter.iterateNext();
            while(node) {
              var txt = node.textContent.trim();
              var pos = txt.indexOf(label);
              if(pos !== -1) {
                var val = txt.substring(pos + label.length).trim().replace(/^[:\\s]+/, '');
                if(val && val.length > 0 && val !== label && results.findIndex(function(r){return r[0]===label;}) === -1) {
                  results.push([label, val]);
                }
              }
              node = iter.iterateNext();
            }
          } catch(ex){}
        });
      }

      var hasRealData = results.length >= 2; // Al menos 2 campos = hay resultado
      var noEncontrado = bodyTxt.includes('Afiliado no encontrado') && !hasRealData;

      if(hasRealData || noEncontrado || attempts >= 40) {
        clearInterval(interval);
        if(hasRealData) {
          window.AndroidBridge.sendResult(JSON.stringify({ok:true, results:results}));
        } else {
          window.AndroidBridge.sendResult(JSON.stringify({ok:false, notFound:noEncontrado}));
        }
      }
    }, 500);
  }, 400);
})();
""".trimIndent()

private fun parseAnrJson(
    json      : String,
    onResult  : (List<Pair<String, String>>) -> Unit,
    onNotFound: (String) -> Unit
) {
    val cleaned = json.trim().let {
        if (it.startsWith("\"") && it.endsWith("\""))
            it.substring(1, it.length-1).replace("\\\"","\"").replace("\\n","").replace("\\t","")
        else it
    }
    try {
        val obj = JSONObject(cleaned)
        if (obj.optBoolean("ok")) {
            val arr = obj.optJSONArray("results") ?: JSONArray()
            val campos = mutableListOf<Pair<String,String>>()
            for (i in 0 until arr.length()) {
                val p = arr.optJSONArray(i) ?: continue
                val k = p.optString(0,"").trim()
                val v = p.optString(1,"").trim()
                if (v.isNotEmpty()) campos.add(k to v)
            }
            if (campos.isNotEmpty()) onResult(campos)
            else onNotFound("No se obtuvieron datos para la cédula consultada")
        } else {
            val notFound = obj.optBoolean("notFound", false)
            onNotFound(if (notFound) "No encontrado en el Padrón ANR" else "No se obtuvieron datos")
        }
    } catch (e: Exception) {
        onNotFound("Error al procesar respuesta")
    }
}
