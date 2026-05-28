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

  /* ── Set input value (React/Vue-compatible) ─────────────────── */
  function setNative(el, v) {
    try {
      var d = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
      if (d && d.set) d.set.call(el, v); else el.value = v;
    } catch(e) { el.value = v; }
    ['focus','input','change','keyup','keydown'].forEach(function(n) {
      el.dispatchEvent(new Event(n, {bubbles:true}));
    });
  }

  /* ── Find the voter input (inside the form with Consultar btn) ─ */
  function findVoterInput() {
    var forms = Array.from(document.querySelectorAll('form'));
    for (var fi = 0; fi < forms.length; fi++) {
      var f = forms[fi];
      var btns = Array.from(f.querySelectorAll('button,input[type=submit],input[type=button]'));
      var hasConsultar = btns.some(function(b) {
        return /consul|buscar|search/i.test(b.textContent || b.value || '');
      });
      if (!hasConsultar) continue;
      var inp = Array.from(f.querySelectorAll('input')).filter(function(i) {
        return i.type !== 'hidden' && i.type !== 'checkbox' && i.type !== 'radio' &&
               i.type !== 'submit' && i.type !== 'button' && !i.disabled && i.offsetWidth > 0;
      })[0];
      if (inp) return inp;
    }
    // Fallback: first visible text-like input on page
    return Array.from(document.querySelectorAll('input')).filter(function(i) {
      return i.type !== 'hidden' && i.type !== 'checkbox' && i.type !== 'radio' &&
             i.type !== 'submit' && i.type !== 'button' && !i.disabled && i.offsetWidth > 0;
    })[0] || null;
  }

  function fillInput() {
    var inp = findVoterInput();
    if (inp) { setNative(inp, CI); return true; }
    return false;
  }

  function clickSearch() {
    // Click the Consultar button closest to our input
    var inp = findVoterInput();
    var scope = inp ? (inp.form || document.body) : document.body;
    var btns = Array.from(scope.querySelectorAll('button,input[type=submit],input[type=button]'))
      .filter(function(b) { return b.offsetWidth > 0 && !b.disabled; });
    var btn = btns.find(function(b) { return /consul|buscar|search/i.test(b.textContent || b.value || ''); })
              || btns[btns.length - 1];
    if (btn) { btn.click(); return true; }
    if (inp && inp.form) {
      inp.form.dispatchEvent(new Event('submit', {bubbles:true, cancelable:true}));
      return true;
    }
    return false;
  }

  /* ── Detect that real results are showing ───────────────────── */
  // body.innerText does NOT include <input> values, so CI only appears there
  // after the server result is rendered on the page.
  function hasResultsLoaded() {
    var txt = document.body.innerText || '';
    return txt.indexOf(CI) !== -1;
  }

  function isNotFoundPage() {
    var txt = (document.body.innerText || '').toLowerCase();
    return ['no encontrado','no figura','no registr','no se encontr',
            'sin resultados','not found','no esta','no aparece'].some(function(p) {
      return txt.indexOf(p) !== -1;
    });
  }

  /* ── Label helpers ──────────────────────────────────────────── */
  var ACCENT_MAP = {'á':'a','é':'e','í':'i','ó':'o','ú':'u','ü':'u','ñ':'n',
                    'Á':'A','É':'E','Í':'I','Ó':'O','Ú':'U','Ü':'U','Ñ':'N'};
  function norm(s) {
    return (s||'').trim().toUpperCase().replace(/[áéíóúüñÁÉÍÓÚÜÑ]/g,function(c){return ACCENT_MAP[c]||c;});
  }

  var LABEL_SET = {
    'CEDULA DE IDENTIDAD':1,'NUMERO DE CEDULA':1,'NRO DE CEDULA':1,'NRO. DE CEDULA':1,
    'CEDULA':1,'CI':1,
    'NOMBRES Y APELLIDOS':1,'NOMBRE Y APELLIDO':1,'NOMBRES':1,'NOMBRE':1,
    'APELLIDOS':1,'APELLIDO':1,
    'FECHA DE NACIMIENTO':1,'FECHA NACIMIENTO':1,'NACIMIENTO':1,
    'DEPARTAMENTO':1,'DEPTO':1,'DISTRITO':1,'CIUDAD':1,'BARRIO':1,
    'SECCIONAL':1,'SECCION':1,'LOCAL':1,'MESA':1,'ORDEN':1,
    'FONO':1,'TELEFONO':1,'SEXO':1,'ESTADO':1
  };

  function normalLabel(raw) { return norm(raw).replace(/:${'$'}/, '').trim(); }
  function isLabel(raw)      { return !!LABEL_SET[normalLabel(raw)]; }

  /* ── Extract from body.innerText line-by-line ───────────────── */
  function extractResults() {
    var lines = (document.body.innerText || '').split(/[\n\r]+/)
      .map(function(l){ return l.trim(); })
      .filter(function(l){ return l.length > 0 && l.length < 250; });

    var found = [];
    var i = 0;
    while (i < lines.length) {
      var k = normalLabel(lines[i]);
      if (LABEL_SET[k]) {
        var matched = false;
        for (var j = i + 1; j < Math.min(i + 4, lines.length); j++) {
          var v = lines[j].trim();
          if (v.length > 0 && v.length < 200 && !LABEL_SET[normalLabel(v)]) {
            found.push([k, v]);
            i = j + 1;
            matched = true;
            break;
          }
        }
        if (!matched) i++;
      } else {
        // Try same-line "LABEL: valor" pattern
        var ci = lines[i].indexOf(':');
        if (ci > 2 && ci < 50) {
          var lk = normalLabel(lines[i].substring(0, ci));
          var lv = lines[i].substring(ci + 1).trim();
          if (LABEL_SET[lk] && lv.length > 0 && lv.length < 200) found.push([lk, lv]);
        }
        i++;
      }
    }

    var seen = {};
    return found.filter(function(r){ if(seen[r[0]])return false; seen[r[0]]=true; return true; });
  }

  /* ── Main flow ──────────────────────────────────────────────── */
  fillInput();

  setTimeout(function() {
    fillInput();
    clickSearch();

    var attempts = 0;
    var timer = setInterval(function() {
      attempts++;
      var hasRes = hasResultsLoaded();
      var notFnd = !hasRes && isNotFoundPage();

      if (hasRes || notFnd || attempts >= 60) {
        clearInterval(timer);
        if (hasRes) {
          var data = extractResults();
          window.AndroidBridge.sendResult(JSON.stringify({
            ok: data.length >= 1, results: data, notFound: false
          }));
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
