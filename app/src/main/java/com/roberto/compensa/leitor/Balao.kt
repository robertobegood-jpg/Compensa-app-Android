package com.roberto.compensa.leitor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.roberto.compensa.dados.Preferencias
import com.roberto.compensa.nucleo.Avaliacao
import com.roberto.compensa.nucleo.Cor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Cores do Oficina para as janelas flutuantes (fora do Compose). */
internal object CoresBalao {
    val FUNDO = Color.parseColor("#121316")
    val TINTA = Color.parseColor("#F2F1EE")
    val APAGADO = Color.parseColor("#99F2F1EE")
    val LINHA = Color.parseColor("#26F2F1EE")
    val VERDE = Color.parseColor("#1F9D55")
    val AMARELO = Color.parseColor("#F2A516")
    val VERMELHO = Color.parseColor("#E8573F")

    fun de(cor: Cor) = when (cor) {
        Cor.VERDE -> VERDE
        Cor.AMARELO -> AMARELO
        Cor.VERMELHO -> VERMELHO
    }
}

private val BR = Locale.forLanguageTag("pt-BR")
internal fun reais(v: Double) = "R$ " + String.format(BR, "%.2f", v)
internal fun dec(v: Double, casas: Int = 1) = String.format(BR, "%.${casas}f", v)

/** Fontes do Oficina, se estiverem nos assets; senão, as do Android. */
internal class FontesBalao(context: Context) {
    val mono: Typeface = runCatching { Typeface.createFromAsset(context.assets, "fonts/geist_mono.ttf") }
        .getOrDefault(Typeface.MONOSPACE)
    val display: Typeface = runCatching { Typeface.createFromAsset(context.assets, "fonts/archivo.ttf") }
        .getOrDefault(Typeface.DEFAULT_BOLD)
}

/**
 * Janela flutuante que pode ser arrastada. Toque rápido = [aoTocar]; arrastar move e salva a posição.
 * Usa TYPE_ACCESSIBILITY_OVERLAY, que não precisa da permissão "sobrepor outros apps".
 */
internal open class Flutuante(
    protected val context: Context,
    private val wm: WindowManager,
    private val prefs: Preferencias,
    private val nome: String,
    padraoX: Int,
    padraoY: Int
) {
    protected val dp = context.resources.displayMetrics.density
    var visivel = false
        private set

    protected val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        val (x0, y0) = prefs.posicao(nome, padraoX, padraoY)
        x = x0
        y = y0
    }

    var raiz: View? = null
        protected set

    var aoTocar: () -> Unit = {}

    @SuppressLint("ClickableViewAccessibility")
    protected fun prepararArraste(v: View) {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var toqueX = 0f
        var toqueY = 0f
        var iniX = 0
        var iniY = 0
        var arrastando = false
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    toqueX = e.rawX; toqueY = e.rawY; iniX = params.x; iniY = params.y; arrastando = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - toqueX
                    val dy = e.rawY - toqueY
                    if (!arrastando && (abs(dx) > slop || abs(dy) > slop)) arrastando = true
                    if (arrastando) {
                        params.x = (iniX + dx).roundToInt().coerceAtLeast(0)
                        params.y = (iniY + dy).roundToInt().coerceAtLeast(0)
                        runCatching { wm.updateViewLayout(v, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (arrastando) prefs.salvarPosicao(nome, params.x, params.y) else aoTocar()
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    protected fun mostrar(v: View) {
        if (raiz === v && visivel) return
        esconder()
        raiz = v
        runCatching { wm.addView(v, params) }.onSuccess { visivel = true }
    }

    fun esconder() {
        val v = raiz ?: return
        if (visivel) runCatching { wm.removeView(v) }
        visivel = false
    }
}

/** O semáforo: aparece por cima do Uber/99 quando chega uma chamada. */
internal class BalaoSemaforo(
    context: Context,
    wm: WindowManager,
    prefs: Preferencias,
    private val fontes: FontesBalao
) : Flutuante(
    context, wm, prefs, "balao",
    padraoX = (16 * context.resources.displayMetrics.density).roundToInt(),
    padraoY = (96 * context.resources.displayMetrics.density).roundToInt()
) {
    var mostradoEm = 0L
        private set

    init {
        // Tocar fecha (pedido comum: não ficar atrapalhando o card).
        aoTocar = { esconder() }
    }

    private fun texto(t: String, tamanho: Float, cor: Int, face: Typeface, espaco: Float = 0f) =
        TextView(context).apply {
            text = t
            textSize = tamanho
            setTextColor(cor)
            typeface = face
            letterSpacing = espaco
            includeFontPadding = false
        }

    fun mostrar(a: Avaliacao) {
        val corSinal = CoresBalao.de(a.cor)
        val pad = (14 * dp).roundToInt()

        val caixa = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(CoresBalao.FUNDO)
                setStroke((1 * dp).roundToInt(), CoresBalao.LINHA)
            }
            elevation = 8 * dp
        }
        // Faixa de cor do semáforo (bem visível de relance).
        caixa.addView(View(context).apply { setBackgroundColor(corSinal) },
            LinearLayout.LayoutParams((10 * dp).roundToInt(), LinearLayout.LayoutParams.MATCH_PARENT))

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (10 * dp).roundToInt(), pad, (12 * dp).roundToInt())
        }
        val topo = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topo.addView(View(context).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(corSinal) }
        }, LinearLayout.LayoutParams((8 * dp).roundToInt(), (8 * dp).roundToInt()).apply { marginEnd = (6 * dp).roundToInt() })
        topo.addView(texto(a.cor.rotulo.uppercase(BR), 11f, corSinal, fontes.mono, 0.12f))
        topo.addView(texto("  ·  " + a.chamada.app.uppercase(BR), 11f, CoresBalao.APAGADO, fontes.mono, 0.12f))
        col.addView(topo)

        col.addView(texto(reais(a.chamada.valor), 30f, CoresBalao.TINTA, fontes.display).apply {
            setPadding(0, (6 * dp).roundToInt(), 0, (4 * dp).roundToInt())
        })

        val hora = if (a.rsHora.isNaN()) "—" else reais(a.rsHora).removePrefix("R$ ")
        col.addView(texto("R$ ${dec(a.rsKm, 2)}/km   R$ $hora/h", 15f, CoresBalao.TINTA, fontes.mono))

        val busca = if (a.chamada.kmBusca >= 0) "busca ${dec(a.chamada.kmBusca)} km · " else ""
        val est = if (a.chamada.tempoEstimado) " (tempo est.)" else ""
        col.addView(texto(
            "${busca}total ${dec(a.chamada.kmTotal())} km · ${a.chamada.minTotal().roundToInt()} min$est",
            12f, CoresBalao.APAGADO, fontes.mono
        ).apply { setPadding(0, (4 * dp).roundToInt(), 0, 0) })
        col.addView(texto("lucro ${reais(a.lucro)}", 12f, CoresBalao.APAGADO, fontes.mono))

        a.motivos.firstOrNull()?.let {
            col.addView(texto(it, 11f, corSinal, fontes.mono).apply { setPadding(0, (6 * dp).roundToInt(), 0, 0) })
        }
        caixa.addView(col)

        prepararArraste(caixa)
        mostrar(caixa)
        mostradoEm = System.currentTimeMillis()
    }
}

/** Botão quadrado de gravação, sempre por cima enquanto o leitor está ligado. */
internal class BotaoRec(
    context: Context,
    wm: WindowManager,
    prefs: Preferencias,
    private val fontes: FontesBalao
) : Flutuante(
    context, wm, prefs, "rec",
    padraoX = 0,
    padraoY = (context.resources.displayMetrics.heightPixels * 0.55).roundToInt()
) {
    private var rotulo: TextView? = null
    private var caixa: LinearLayout? = null

    fun mostrar(gravando: Boolean) {
        val c = caixa ?: criar().also { caixa = it }
        atualizar(gravando)
        mostrar(c)
    }

    fun atualizar(gravando: Boolean) {
        val c = caixa ?: return
        (c.background as? GradientDrawable)?.setColor(if (gravando) CoresBalao.VERMELHO else CoresBalao.FUNDO)
        rotulo?.text = if (gravando) "● REC" else "○ REC"
        c.contentDescription = if (gravando) "Parar gravação de segurança" else "Iniciar gravação de segurança"
    }

    private fun criar(): LinearLayout {
        val lado = (52 * dp).roundToInt()
        return LinearLayout(context).apply {
            gravity = Gravity.CENTER
            minimumWidth = lado
            minimumHeight = lado
            background = GradientDrawable().apply {
                setColor(CoresBalao.FUNDO)
                setStroke((1 * dp).roundToInt(), CoresBalao.LINHA)
            }
            alpha = 0.85f
            val t = TextView(context).apply {
                text = "○ REC"
                textSize = 11f
                setTextColor(CoresBalao.TINTA)
                typeface = fontes.mono
                letterSpacing = 0.1f
            }
            rotulo = t
            addView(t)
            prepararArraste(this)
        }
    }
}
