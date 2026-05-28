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
import androidx.compose.ui.draw.alpha
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

                        private var jsInjected = false

                        override fun shouldOverrideUrlLoading(
                            view: WebView, request: WebResourceRequest
                        ): Boolean {
                            val url = request.url.toString()
                            // Block navigation away from the padron page
                            if (!url.contains("anr.org.py")) return true
                            if (!url.contains("padron-2026") && !url.contains("wp-admin")
                                && !url.contains("wp-json") && !url.contains("wp-content")) {
                                return true  // block
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView, pageUrl: String) {
                            // Only inject once, and only on the voter page
                            if (jsInjected || !pageUrl.contains("padron-2026")) return
                            jsInjected = true

                            // Wait 3s for WordPress/shortcode to fully mount
                            mainHandler.postDelayed({
                                if (!resultHandled) {
                                    view.evaluateJavascript(buildAnrJs(cedula), null)
                                }
                            }, 3000L)

                            // Safety timeout at 70s
                            mainHandler.postDelayed({
                                if (!resultHandled) {
                                    resultHandled = true
                                    onNotFound("Tiempo agotado. Verificá tu conexión e intentá de nuevo.")
                                }
                            }, 70000L)
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
            // 360x640 dp keeps mobile CSS layout active; alpha=0 hides it
            modifier = Modifier.size(360.dp, 640.dp).alpha(0f)
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
  var DONE = false;
  var SUBMITTED_AT = 0;

  function done(payload) {
    if (DONE) return;
    DONE = true;
    try { window.AndroidBridge.sendResult(JSON.stringify(payload)); } catch(e){}
  }

  /* ── Set input value (React/Vue-compatible) ─────────────────── */
  function setNative(el, v) {
    try {
      var d = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
      if (d && d.set) d.set.call(el, v); else el.value = v;
    } catch(e) { el.value = v; }
    ['focus','input','change','keyup','keydown','blur'].forEach(function(n) {
      try { el.dispatchEvent(new Event(n, {bubbles:true})); } catch(e){}
    });
  }

  /* ── Normalización ──────────────────────────────────────────── */
  var ACC = {'á':'a','é':'e','í':'i','ó':'o','ú':'u','ü':'u','ñ':'n',
             'Á':'A','É':'E','Í':'I','Ó':'O','Ú':'U','Ü':'U','Ñ':'N'};
  function norm(s){return (s||'').trim().toUpperCase().replace(/[áéíóúüñÁÉÍÓÚÜÑ]/g,function(c){return ACC[c]||c;});}
  function normalLabel(raw){ return norm(raw).replace(/:${'$'}/, '').trim(); }

  var LABEL_SET = {
    'CEDULA DE IDENTIDAD':1,'NUMERO DE CEDULA':1,'NRO DE CEDULA':1,'NRO. DE CEDULA':1,
    'CEDULA':1,'CI':1,
    'NOMBRES Y APELLIDOS':1,'NOMBRE Y APELLIDO':1,'NOMBRES':1,'NOMBRE':1,
    'APELLIDOS':1,'APELLIDO':1,
    'FECHA DE NACIMIENTO':1,'FECHA NACIMIENTO':1,'NACIMIENTO':1,
    'DEPARTAMENTO':1,'DEPTO':1,'DISTRITO':1,'CIUDAD':1,'BARRIO':1,
    'SECCIONAL':1,'SECCION':1,'LOCAL':1,'MESA':1,'ORDEN':1,
    'FONO':1,'TELEFONO':1,'SEXO':1,'ESTADO':1,
    'RESULTADO DE LA CONSULTA':1
  };

  /* ── Find consultar button — NO <a> tags to avoid page navigation ─ */
  function findConsultarButton() {
    var btns = Array.from(document.querySelectorAll('button, input[type=submit], input[type=button]'));
    // First: exact-match "consultar"
    for (var i = 0; i < btns.length; i++) {
      var b = btns[i];
      if (b.disabled) continue;
      var t = (b.textContent || b.value || '').trim().toLowerCase();
      if (t === 'consultar') return b;
    }
    // Second: contains "consultar" anywhere
    for (var i = 0; i < btns.length; i++) {
      var b = btns[i];
      if (b.disabled) continue;
      var t = (b.textContent || b.value || '').trim().toLowerCase();
      if (/\bconsultar\b/.test(t) && !/aplicaci/i.test(t)) return b;
    }
    return null;
  }

  /* ── Find the cédula input within or near the form ──────────── */
  function findCedulaInput(scope) {
    var root = scope || document;
    var inputs = Array.from(root.querySelectorAll('input'));
    for (var i = 0; i < inputs.length; i++) {
      var inp = inputs[i];
      if (inp.type === 'hidden' || inp.type === 'checkbox' || inp.type === 'radio' ||
          inp.type === 'submit' || inp.type === 'button' || inp.disabled) continue;
      var name = (inp.name || '').toLowerCase();
      var id   = (inp.id   || '').toLowerCase();
      var ph   = (inp.placeholder || '').toLowerCase();
      // Skip WordPress site search
      if (name === 's' || id === 's' || /search/.test(name + id) || /buscar.*sitio/.test(ph)) continue;
      return inp;
    }
    return null;
  }

  /* ── Submit using multiple strategies ───────────────────────── */
  function trySubmit(input, button) {
    var form = (button && (button.form || button.closest('form'))) ||
               (input  && (input.form  || input.closest('form')));
    // 1) button.click()
    try { if (button) { button.click(); SUBMITTED_AT = Date.now(); return 'click'; } } catch(e){}
    // 2) form.requestSubmit()
    try { if (form && form.requestSubmit) { form.requestSubmit(); SUBMITTED_AT = Date.now(); return 'requestSubmit'; } } catch(e){}
    // 3) form.submit()
    try { if (form) { form.submit(); SUBMITTED_AT = Date.now(); return 'submit'; } } catch(e){}
    // 4) Enter key on input
    try {
      if (input) {
        ['keydown','keypress','keyup'].forEach(function(n){
          input.dispatchEvent(new KeyboardEvent(n, {key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true}));
        });
        SUBMITTED_AT = Date.now();
        return 'enter';
      }
    } catch(e){}
    return null;
  }

  /* ── Detection: real results loaded ─────────────────────────── */
  function hasResults() {
    var txt = document.body.innerText || '';
    if (txt.indexOf(CI) === -1) return false;
    var nu = norm(txt);
    // Must have a result marker AND the CI rendered as content (not input value)
    return nu.indexOf('RESULTADO DE LA CONSULTA') !== -1 ||
           nu.indexOf('CEDULA DE IDENTIDAD') !== -1;
  }

  function isNotFoundNow() {
    // Only valid AFTER we submitted AND enough time passed
    if (!SUBMITTED_AT || Date.now() - SUBMITTED_AT < 4000) return false;
    var t = (document.body.innerText || '').toLowerCase();
    return /no\s+se\s+encontr[oó]/.test(t) ||
           /sin\s+resultados/.test(t) ||
           /no\s+se\s+hall[oó]/.test(t) ||
           /c[eé]dula\s+no\s+v[aá]lida/.test(t) ||
           /no\s+est[aá]\s+inscript/.test(t) ||
           /no\s+figura\s+en\s+el\s+padr/.test(t);
  }

  /* ── Extract label/value from rendered text ─────────────────── */
  function extractResults() {
    var lines = (document.body.innerText || '').split(/[\n\r]+/)
      .map(function(l){return l.trim();})
      .filter(function(l){return l.length > 0 && l.length < 250;});

    var found = [];
    var i = 0;
    while (i < lines.length) {
      var k = normalLabel(lines[i]);
      // Skip header marker, not a real label
      if (k === 'RESULTADO DE LA CONSULTA') { i++; continue; }
      if (LABEL_SET[k]) {
        var matched = false;
        for (var j = i + 1; j < Math.min(i + 4, lines.length); j++) {
          var v = lines[j].trim();
          var nv = normalLabel(v);
          if (v.length > 0 && v.length < 200 && !LABEL_SET[nv]) {
            found.push([k, v]);
            i = j + 1;
            matched = true;
            break;
          }
        }
        if (!matched) i++;
      } else {
        i++;
      }
    }

    var seen = {};
    return found.filter(function(r){if(seen[r[0]])return false; seen[r[0]]=true; return true;});
  }

  /* ── Try to fill+submit, retry until form is available ──────── */
  var fillAttempts = 0;
  function attemptFillAndSubmit() {
    if (DONE) return;
    var btn = findConsultarButton();
    if (!btn) {
      fillAttempts++;
      if (fillAttempts < 20) setTimeout(attemptFillAndSubmit, 1000);
      return;
    }
    var form = btn.form || btn.closest('form');
    var inp = findCedulaInput(form || document);
    if (!inp) {
      fillAttempts++;
      if (fillAttempts < 20) setTimeout(attemptFillAndSubmit, 1000);
      return;
    }
    setNative(inp, CI);
    setTimeout(function() {
      // Re-fetch in case form re-rendered
      var btn2 = findConsultarButton() || btn;
      var form2 = btn2.form || btn2.closest('form') || form;
      var inp2 = findCedulaInput(form2 || document) || inp;
      if (inp2.value !== CI) setNative(inp2, CI);
      trySubmit(inp2, btn2);
    }, 700);
  }

  /* ── Main flow ──────────────────────────────────────────────── */

  // If results already showing (e.g. page reloaded with them), extract immediately
  if (hasResults()) {
    var d0 = extractResults();
    if (d0.length >= 1) { done({ok:true, results:d0}); return; }
  }

  attemptFillAndSubmit();

  // Poll for result or not-found
  var pollCount = 0;
  var poll = setInterval(function() {
    if (DONE) { clearInterval(poll); return; }
    pollCount++;
    if (hasResults()) {
      var data = extractResults();
      if (data.length >= 1) {
        clearInterval(poll);
        done({ok:true, results:data});
        return;
      }
    }
    if (isNotFoundNow()) {
      clearInterval(poll);
      done({ok:false, notFound:true});
      return;
    }
    if (pollCount >= 120) { // ~60s
      clearInterval(poll);
      // Final fallback: extract whatever we have
      var fb = extractResults();
      if (fb.length >= 2 && (document.body.innerText||'').indexOf(CI) !== -1) {
        done({ok:true, results:fb});
      } else {
        done({ok:false, notFound:false});
      }
    }
  }, 500);

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
