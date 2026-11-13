package com.roberto.compensa.dados

import android.content.Context
import com.roberto.compensa.nucleo.Criterios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Qualidade da gravação de segurança. O tamanho por hora é aproximado (vídeo + áudio).
 * [resolucao]: 480 = SD, 720 = HD.
 */
enum class QualidadeVideo(val rotulo: String, val resolucao: Int, val bitsPorSegundo: Int, val mbPorHora: Int) {
    ECONOMICA("Econômica", 480, 700_000, 360),
    MEDIA("Média", 720, 1_500_000, 720),
    ALTA("Alta", 720, 3_000_000, 1400)
}

enum class Tema(val rotulo: String) { ESCURO("Escuro"), CLARO("Claro"), SISTEMA("Sistema") }

/** Ajustes gerais do app (fora os critérios). */
data class Ajustes(
    val lerUber: Boolean = true,
    /** Leitor ligado em Acessibilidade, mas sem ler (pausa rápida, sem ir às configurações). */
    val leitorPausado: Boolean = false,
    val ler99: Boolean = true,
    /** Se o texto do card vier escondido, lê pela imagem da tela (Android 11+). */
    val leituraImagem: Boolean = true,
    /** Segundos que o balão fica na tela. */
    val tempoBalao: Int = 15,
    /** Salva o texto lido de cada chamada em arquivo, para calibrar a leitura. */
    val modoCaptura: Boolean = false,
    /** Botão flutuante de gravação por cima dos apps. */
    val botaoRec: Boolean = true,
    val cameraFrontal: Boolean = true,
    val gravarAudio: Boolean = true,
    val qualidade: QualidadeVideo = QualidadeVideo.ECONOMICA,
    /** Apaga vídeos mais velhos que isso ao iniciar uma gravação. 0 = nunca apaga. */
    val apagarAposDias: Int = 0,
    val tema: Tema = Tema.ESCURO
)

/** Guarda critérios e ajustes no celular (SharedPreferences) e avisa quem estiver ouvindo. */
class Preferencias(context: Context) {
    private val prefs = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    private val _criterios = MutableStateFlow(lerCriterios())
    val criterios: StateFlow<Criterios> = _criterios.asStateFlow()

    private val _ajustes = MutableStateFlow(lerAjustes())
    val ajustes: StateFlow<Ajustes> = _ajustes.asStateFlow()

    fun salvarCriterios(c: Criterios) {
        prefs.edit()
            .putBoolean("usarKm", c.usarKm).putFloat("kmVerde", c.kmVerde.toFloat()).putFloat("kmAmarelo", c.kmAmarelo.toFloat())
            .putBoolean("usarHora", c.usarHora).putFloat("horaVerde", c.horaVerde.toFloat()).putFloat("horaAmarelo", c.horaAmarelo.toFloat())
            .putBoolean("usarBusca", c.usarBusca).putFloat("buscaVerde", c.buscaVerde.toFloat()).putFloat("buscaAmarelo", c.buscaAmarelo.toFloat())
            .putBoolean("usarValor", c.usarValor).putFloat("valorVerde", c.valorVerde.toFloat()).putFloat("valorAmarelo", c.valorAmarelo.toFloat())
            .putFloat("consumo", c.consumoKmL.toFloat()).putFloat("preco", c.precoLitro.toFloat())
            .putFloat("outros", c.outrosCustosKm.toFloat()).putFloat("velocidade", c.velocidadeMediaKmH.toFloat())
            .apply()
        _criterios.value = c.copia()
    }

    fun restaurarCriterios() = salvarCriterios(Criterios())

    fun salvarAjustes(a: Ajustes) {
        prefs.edit()
            .putBoolean("lerUber", a.lerUber).putBoolean("ler99", a.ler99)
            .putBoolean("leitorPausado", a.leitorPausado).putBoolean("leituraImagem", a.leituraImagem)
            .putInt("tempoBalao", a.tempoBalao).putBoolean("modoCaptura", a.modoCaptura)
            .putBoolean("botaoRec", a.botaoRec).putBoolean("cameraFrontal", a.cameraFrontal)
            .putBoolean("gravarAudio", a.gravarAudio).putString("tema", a.tema.name)
            .putString("qualidade", a.qualidade.name).putInt("apagarAposDias", a.apagarAposDias)
            .apply()
        _ajustes.value = a
    }

    /** Posição do balão e do botão REC (para lembrar onde você deixou). */
    fun posicao(nome: String, padraoX: Int, padraoY: Int): Pair<Int, Int> =
        prefs.getInt("${nome}_x", padraoX) to prefs.getInt("${nome}_y", padraoY)

    fun salvarPosicao(nome: String, x: Int, y: Int) {
        prefs.edit().putInt("${nome}_x", x).putInt("${nome}_y", y).apply()
    }

    private fun d(chave: String, padrao: Double): Double =
        if (prefs.contains(chave)) prefs.getFloat(chave, padrao.toFloat()).toDouble() else padrao

    private fun lerCriterios(): Criterios {
        val p = Criterios()
        return Criterios().apply {
            usarKm = prefs.getBoolean("usarKm", p.usarKm); kmVerde = d("kmVerde", p.kmVerde); kmAmarelo = d("kmAmarelo", p.kmAmarelo)
            usarHora = prefs.getBoolean("usarHora", p.usarHora); horaVerde = d("horaVerde", p.horaVerde); horaAmarelo = d("horaAmarelo", p.horaAmarelo)
            usarBusca = prefs.getBoolean("usarBusca", p.usarBusca); buscaVerde = d("buscaVerde", p.buscaVerde); buscaAmarelo = d("buscaAmarelo", p.buscaAmarelo)
            usarValor = prefs.getBoolean("usarValor", p.usarValor); valorVerde = d("valorVerde", p.valorVerde); valorAmarelo = d("valorAmarelo", p.valorAmarelo)
            consumoKmL = d("consumo", p.consumoKmL); precoLitro = d("preco", p.precoLitro)
            outrosCustosKm = d("outros", p.outrosCustosKm); velocidadeMediaKmH = d("velocidade", p.velocidadeMediaKmH)
        }
    }

    private fun lerAjustes(): Ajustes {
        val p = Ajustes()
        return Ajustes(
            lerUber = prefs.getBoolean("lerUber", p.lerUber),
            ler99 = prefs.getBoolean("ler99", p.ler99),
            leitorPausado = prefs.getBoolean("leitorPausado", p.leitorPausado),
            leituraImagem = prefs.getBoolean("leituraImagem", p.leituraImagem),
            tempoBalao = prefs.getInt("tempoBalao", p.tempoBalao),
            modoCaptura = prefs.getBoolean("modoCaptura", p.modoCaptura),
            botaoRec = prefs.getBoolean("botaoRec", p.botaoRec),
            cameraFrontal = prefs.getBoolean("cameraFrontal", p.cameraFrontal),
            gravarAudio = prefs.getBoolean("gravarAudio", p.gravarAudio),
            qualidade = QualidadeVideo.entries.firstOrNull { it.name == prefs.getString("qualidade", null) } ?: p.qualidade,
            apagarAposDias = prefs.getInt("apagarAposDias", p.apagarAposDias),
            tema = Tema.entries.firstOrNull { it.name == prefs.getString("tema", null) } ?: p.tema
        )
    }
}
