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
                    settings.javaScriptEnabled    = true
                    settings.domStorageEnabled    = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

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
                            // Esperar 5s para que la SPA/WordPress monte completamente
                            mainHandler.postDelayed({
                                if (!resultHandled) {
                                    view.evaluateJavascript(buildAnrJs(cedula), null)
                                }
                            }, 5000L)

                            // Timeout de seguridad a los 35s
                            mainHandler.postDelayed({
                                if (!resultHandled) {
                                    resultHandled = true
                                    onNotFound("Tiempo agotado. Verificá tu conexión e intentá de nuevo.")
                                }
                            }, 35000L)
                        }

                        override fun onReceivedError(
                            view: WebView, req: WebResourceRequest, err: WebResourceError
                        ) {
                            if (req.isForMainFrame && !resultHandled) {
                                resultHandled = true
                                onError("Sin conexión o sitio no disponible")
                            }
                        }
                    }

                    loadUrl("https://www.anr.org.py/padron-2026/")
                }
            },
            modifier = Modifier.size(1.dp, 1.dp)
        )
    }
}

// ── JS multi-estrategia ────────────────────────────────────────────────────────

private fun buildAnrJs(cedula: String): String {
    val safeCI = cedula.replace("'", "\\'").replace("\"", "\\\"")
    return """
(function(){
  'use strict';
  var CI = '$safeCI';

  /* ── Helpers ───────────────────────────────────────────────── */

  function setNative(el, v) {
    try {
      var d = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
      if (d && d.set) d.set.call(el, v); else el.value = v;
    } catch(e) { el.value = v; }
    ['focus','input','change','keyup','keydown'].forEach(function(n) {
      el.dispatchEvent(new Event(n, {bubbles:true}));
    });
  }

  function fillInput() {
    var inp = null;
    Array.from(document.querySelectorAll('input')).forEach(function(i) {
      if (inp) return;
      if (i.type === 'hidden' || i.type === 'checkbox' || i.type === 'radio' ||
          i.type === 'submit' || i.type === 'button' || i.disabled) return;
      if (i.offsetWidth > 0) inp = i;
    });
    if (inp) { setNative(inp, CI); return true; }
    return false;
  }

  function clickSearch() {
    var btn = null;
    Array.from(document.querySelectorAll('button,input[type=submit],input[type=button]'))
      .forEach(function(b) {
        if (btn || b.offsetWidth === 0 || b.disabled) return;
        var t = (b.textContent || b.value || '').toLowerCase();
        if (/consul|buscar|search|enviar|busque/i.test(t)) btn = b;
      });
    if (!btn) {
      var all = Array.from(document.querySelectorAll('button,input[type=submit]'))
                    .filter(function(b) { return b.offsetWidth > 0; });
      if (all.length) btn = all[all.length - 1];
    }
    if (btn) { btn.click(); return true; }
    var form = document.querySelector('form');
    if (form) {
      var ev = new Event('submit', {bubbles:true, cancelable:true});
      form.dispatchEvent(ev);
      return true;
    }
    return false;
  }

  /* ── Extractor universal ────────────────────────────────────── */

  var KNOWN = ['CEDULA DE IDENTIDAD','CEDULA','CI','NOMBRES','APELLIDOS','APELLIDO',
    'NOMBRE','FECHA DE NACIMIENTO','NACIMIENTO','DEPARTAMENTO','DEPTO','DISTRITO',
    'SECCIONAL','SECCION','LOCAL','MESA','ORDEN','FONO','BARRIO','CIUDAD'];

  function extractResults() {
    var found = [];
    var body  = document.body;

    // --- 1. Tablas ---
    body.querySelectorAll('table tr').forEach(function(row) {
      var cells = row.querySelectorAll('td,th');
      if (cells.length >= 2) {
        var k = cells[0].innerText.trim().replace(/:${'$'}/, '').trim();
        var v = cells[1].innerText.trim();
        if (k && v && k !== v && k.length < 80 && v.length < 300)
          found.push([k, v]);
      }
    });

    // --- 2. dl/dt/dd ---
    if (!found.length) {
      body.querySelectorAll('dt').forEach(function(dt) {
        var dd = dt.nextElementSibling;
        if (dd && dd.tagName === 'DD') found.push([dt.innerText.trim(), dd.innerText.trim()]);
      });
    }

    // --- 3. Labels conocidos via XPath ---
    if (!found.length) {
      KNOWN.forEach(function(label) {
        if (found.find(function(r){ return r[0].toUpperCase().indexOf(label) !== -1; })) return;
        try {
          var xpath = "//*[contains(translate(normalize-space(text())," +
            "'abcdefghijklmnopqrstuvwxyzáéíóúüñ'," +
            "'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÜÑ'),'" + label + "')]";
          var iter = document.evaluate(xpath, body, null,
            XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
          var node = iter.iterateNext();
          while (node) {
            var txt = (node.innerText || node.textContent || '').trim();
            var pos = txt.toUpperCase().indexOf(label);
            if (pos !== -1) {
              var after = txt.substring(pos + label.length).replace(/^[:\s\-]+/, '').trim();
              if (after && after.length > 0 && after.length < 200 &&
                  after.toUpperCase() !== label) {
                found.push([label, after]);
                break;
              }
            }
            node = iter.iterateNext();
          }
        } catch(ex) {}
      });
    }

    // --- 4. li / p con ":" ---
    if (!found.length) {
      body.querySelectorAll('li, p').forEach(function(el) {
        if (el.children.length > 2) return;
        var txt = (el.innerText || '').trim();
        if (txt.length < 4 || txt.length > 400) return;
        var ci = txt.indexOf(':');
        if (ci > 0 && ci < 70) {
          var k = txt.substring(0, ci).trim();
          var v = txt.substring(ci + 1).trim();
          if (k && v && k.length < 70 && v.length < 200) found.push([k, v]);
        }
      });
    }

    // Deduplicar
    var seen = {};
    return found.filter(function(r) {
      var key = r[0].toUpperCase();
      if (seen[key]) return false;
      seen[key] = true;
      return true;
    });
  }

  function isNotFoundPage() {
    var txt = (document.body.innerText || '').toLowerCase();
    return ['no encontrado','no figura','no registr','no se encontr',
            'sin resultados','not found','no está'].some(function(p) {
      return txt.indexOf(p) !== -1;
    });
  }

  /* ── Flujo principal ────────────────────────────────────────── */

  fillInput();

  setTimeout(function() {
    fillInput(); // re-llenar por si hay reactivity delay
    clickSearch();

    var attempts = 0;
    var timer = setInterval(function() {
      attempts++;
      var data    = extractResults();
      var hasData = data.length >= 2;
      var notFnd  = isNotFoundPage();

      if (hasData || notFnd || attempts >= 60) {
        clearInterval(timer);
        if (hasData) {
          window.AndroidBridge.sendResult(JSON.stringify({ok:true, results:data}));
        } else {
          window.AndroidBridge.sendResult(JSON.stringify({ok:false, notFound:notFnd}));
        }
      }
    }, 500);
  }, 700);

})();
""".trimIndent()
}

private fun parseAnrJson(
    json      : String,
    onResult  : (List<Pair<String, String>>) -> Unit,
    onNotFound: (String) -> Unit
) {
    val cleaned = json.trim().let {
        if (it.startsWith("\"") && it.endsWith("\""))
            it.substring(1, it.length - 1)
              .replace("\\\"","\"").replace("\\n","").replace("\\t","")
        else it
    }
    try {
        val obj = JSONObject(cleaned)
        if (obj.optBoolean("ok")) {
            val arr    = obj.optJSONArray("results") ?: JSONArray()
            val campos = mutableListOf<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val p = arr.optJSONArray(i) ?: continue
                val k = p.optString(0, "").trim()
                val v = p.optString(1, "").trim()
                if (v.isNotEmpty()) campos.add(k to v)
            }
            if (campos.isNotEmpty()) onResult(campos)
            else onNotFound("No se obtuvieron datos para la cédula consultada.")
        } else {
            val nf = obj.optBoolean("notFound", false)
            onNotFound(if (nf) "No encontrado en el Padrón ANR." else "No se obtuvieron datos.")
        }
    } catch (e: Exception) {
        onNotFound("Error al procesar respuesta: ${e.localizedMessage}")
    }
}
