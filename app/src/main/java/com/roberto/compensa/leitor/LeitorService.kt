package com.roberto.compensa.leitor

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.roberto.compensa.App
import com.roberto.compensa.Estado
import com.roberto.compensa.dados.Registro
import com.roberto.compensa.gravacao.AtivarGravacaoActivity
import com.roberto.compensa.gravacao.GravacaoService
import com.roberto.compensa.nucleo.Calculadora
import com.roberto.compensa.nucleo.LeitorCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Lê a tela do Uber Driver e do 99 Motorista (só leitura: nunca toca em nada nesses apps).
 * Quando reconhece um card de chamada, calcula e mostra o semáforo por cima.
 */
class LeitorService : AccessibilityService() {

    private lateinit var app: App
    private lateinit var balao: BalaoSemaforo
    private lateinit var rec: BotaoRec
    private val principal = Handler(Looper.getMainLooper())
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var pacotePendente: String? = null
    private var leituraAgendada = false
    private var ultimaImagemEm = 0L
    private lateinit var imagem: LeitorImagem
    private var ultimaAssinatura: String? = null
    private var ultimaAssinaturaEm = 0L
    private var ultimoCardEm = 0L
    private var ultimaCapturaHash = 0

    private val lerAgora = Runnable {
        leituraAgendada = false
        pacotePendente?.let { lerTela(it) }
    }
    private val fecharPorTempo = Runnable { balao.esconder() }

    /**
     * Vigia: a cada 1,5 s confere se o Uber/99 está na tela e lê, mesmo sem aviso do app.
     * Necessário porque alguns cards (ex.: 99) não avisam o leitor quando aparecem.
     */
    private val vigia = object : Runnable {
        override fun run() {
            if (encerrado) return
            runCatching { vigiar() }
            principal.postDelayed(this, INTERVALO_VIGIA_MS)
        }
    }

    private fun vigiar() {
        if (leituraAgendada) return // já tem leitura vindo pelos avisos do app
        val tela = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!tela.isInteractive) return
        val pacote = windows.firstNotNullOfOrNull { w ->
            w.root?.packageName?.toString()?.takeIf { nomeDoApp(it) != null }
        } ?: return
        lerTela(pacote)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        app = application as App
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val fontes = FontesBalao(this)
        balao = BalaoSemaforo(this, wm, app.preferencias, fontes)
        rec = BotaoRec(this, wm, app.preferencias, fontes)
        rec.aoTocar = { alternarGravacao() }
        imagem = LeitorImagem(this, ContextCompat.getMainExecutor(this))
        principal.postDelayed(vigia, INTERVALO_VIGIA_MS)
        Estado.leitorAtivo.value = true
        instancia = this

        escopo.launch {
            app.preferencias.ajustes.collect { if (it.leitorPausado) balao.esconder() }
        }
        escopo.launch {
            combine(app.preferencias.ajustes, Estado.gravando, Estado.appNaFrente) { a, g, frente ->
                Triple(a.botaoRec && !frente, g, frente)
            }.collect { (mostrarRec, gravando, _) ->
                if (mostrarRec) rec.mostrar(gravando) else rec.esconder()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::app.isInitialized || encerrado) return
        val pacote = event?.packageName?.toString() ?: return
        if (nomeDoApp(pacote) == null) return
        // Lê no máximo a cada 250 ms. Não dá para esperar a tela "parar": o card do Uber tem
        // uma barra animada no botão que gera eventos sem parar enquanto ele está na tela.
        pacotePendente = pacote
        if (!leituraAgendada) {
            leituraAgendada = true
            principal.postDelayed(lerAgora, ATRASO_LEITURA_MS)
        }
    }

    /** null = app que não lemos (ou desligado nos ajustes). */
    private fun nomeDoApp(pacote: String): String? {
        val a = app.preferencias.ajustes.value
        if (a.leitorPausado) return null
        return when (pacote) {
            PACOTE_UBER -> if (a.lerUber) "Uber" else null
            PACOTE_99 -> if (a.ler99) "99" else null
            else -> null
        }
    }

    private fun lerTela(pacote: String) {
        val nome = nomeDoApp(pacote) ?: return
        val textos = ArrayList<String>()
        val raizes = runCatching { windows.mapNotNull { it.root }.filter { it.packageName?.toString() == pacote } }
            .getOrDefault(emptyList())
            .ifEmpty { listOfNotNull(rootInActiveWindow?.takeIf { it.packageName?.toString() == pacote }) }
        raizes.forEach { coletar(it, textos, 0) }

        if (processar(nome, textos, "texto (${raizes.size} janela(s))")) return

        // Plano B: o Uber esconde o texto do card do leitor normal. Lê pela imagem da tela.
        val agora = System.currentTimeMillis()
        val tela = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (app.preferencias.ajustes.value.leituraImagem && imagem.disponivel && !imagem.ocupado &&
            tela.isInteractive && agora - ultimaImagemEm >= INTERVALO_IMAGEM_MS
        ) {
            ultimaImagemEm = agora
            imagem.ler { linhas ->
                val n = nomeDoApp(pacote) ?: return@ler
                processar(n, linhas, "imagem")
            }
        }
    }

    /** Tenta achar uma chamada nos textos. true = achou (e mostrou o semáforo, se for nova). */
    private fun processar(nome: String, textos: List<String>, origem: String): Boolean {
        val agora = System.currentTimeMillis()
        if (textos.isEmpty()) {
            if (balao.visivel && agora - ultimoCardEm > SUMIU_MS) balao.esconder()
            return false
        }

        val criterios = app.preferencias.criterios.value
        val chamada = runCatching { LeitorCard.ler(nome, textos, criterios.velocidadeMediaKmH) }.getOrNull()

        if (app.preferencias.ajustes.value.modoCaptura &&
            textos.any { it.contains("R$") || it.contains("km", ignoreCase = true) }
        ) {
            val hash = textos.hashCode()
            if (hash != ultimaCapturaHash) {
                ultimaCapturaHash = hash
                app.captura.registrar(
                    "$nome · lido por $origem", textos,
                    chamada?.toString() ?: "não reconhecido como chamada"
                )
            }
        }

        if (chamada == null) {
            // O card sumiu (aceitou, recusou ou expirou): fecha o balão depois de um tempinho.
            if (balao.visivel && agora - ultimoCardEm > SUMIU_MS) balao.esconder()
            return false
        }

        ultimoCardEm = agora
        val assinatura = chamada.assinatura()
        if (assinatura == ultimaAssinatura && agora - ultimaAssinaturaEm < REPETICAO_MS) return true
        ultimaAssinatura = assinatura
        ultimaAssinaturaEm = agora

        val avaliacao = Calculadora.avaliar(chamada, criterios)
        balao.mostrar(avaliacao)
        app.historico.adicionar(Registro.de(avaliacao, agora))

        principal.removeCallbacks(fecharPorTempo)
        principal.postDelayed(fecharPorTempo, app.preferencias.ajustes.value.tempoBalao.coerceIn(3, 120) * 1000L)
        return true
    }

    /** Junta texto e descrição de cada elemento, na ordem da tela. */
    private fun coletar(no: AccessibilityNodeInfo, saida: MutableList<String>, profundidade: Int) {
        if (profundidade > 40 || saida.size > 600) return
        no.text?.toString()?.takeIf { it.isNotBlank() }?.let { saida.add(it) }
        no.contentDescription?.toString()?.takeIf { it.isNotBlank() && it != no.text?.toString() }?.let { saida.add(it) }
        for (i in 0 until no.childCount) {
            val filho = runCatching { no.getChild(i) }.getOrNull() ?: continue
            coletar(filho, saida, profundidade + 1)
        }
    }

    private fun alternarGravacao() {
        if (Estado.gravando.value) {
            GravacaoService.parar(this)
        } else {
            // Abre uma tela invisível por um instante: o Android só libera câmera/microfone
            // para gravação em segundo plano se ela começar com o app "na frente".
            startActivity(
                Intent(this, AtivarGravacaoActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            )
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        encerrar()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        encerrar()
        super.onDestroy()
    }

    private var encerrado = false
    private fun encerrar() {
        if (encerrado) return
        encerrado = true
        Estado.leitorAtivo.value = false
        if (instancia === this) instancia = null
        principal.removeCallbacksAndMessages(null)
        escopo.cancel()
        if (::balao.isInitialized) balao.esconder()
        if (::rec.isInitialized) rec.esconder()
        if (::imagem.isInitialized) imagem.fechar()
    }

    companion object {
        @Volatile
        private var instancia: LeitorService? = null

        /** Desliga o leitor (o mesmo que desativar em Acessibilidade). Para religar, só pelas configurações. */
        fun desligar() {
            val s = instancia ?: return
            s.encerrar()
            s.disableSelf()
        }

        const val PACOTE_UBER = "com.ubercab.driver"
        const val PACOTE_99 = "com.app99.driver"
        private const val ATRASO_LEITURA_MS = 250L
        private const val SUMIU_MS = 3500L
        /** Leitura pela imagem: no máximo uma a cada 2 s (poupa bateria; o card fica uns 15 s na tela). */
        private const val INTERVALO_IMAGEM_MS = 2000L
        private const val INTERVALO_VIGIA_MS = 1500L
        private const val REPETICAO_MS = 60_000L
    }
}
