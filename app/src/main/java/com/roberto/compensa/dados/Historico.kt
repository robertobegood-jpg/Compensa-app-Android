package com.roberto.compensa.dados

import android.content.Context
import com.roberto.compensa.nucleo.Avaliacao
import com.roberto.compensa.nucleo.Cor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/** Uma chamada vista, guardada no histórico. */
data class Registro(
    val quando: Long,
    val app: String,
    val valor: Double,
    val kmBusca: Double,
    val kmViagem: Double,
    val minTotal: Double,
    val rsKm: Double,
    val rsHora: Double,
    val lucro: Double,
    val cor: Cor
) {
    fun json(): String = JSONObject()
        .put("t", quando).put("app", app).put("v", valor).put("kb", kmBusca).put("kv", kmViagem)
        .put("min", minTotal).put("rk", rsKm).put("rh", if (rsHora.isNaN()) -1.0 else rsHora)
        .put("l", lucro).put("c", cor.name)
        .toString()

    companion object {
        fun de(a: Avaliacao, quando: Long = System.currentTimeMillis()) = Registro(
            quando = quando,
            app = a.chamada.app,
            valor = a.chamada.valor,
            kmBusca = a.chamada.kmBusca,
            kmViagem = a.chamada.kmViagem,
            minTotal = a.chamada.minTotal(),
            rsKm = a.rsKm,
            rsHora = a.rsHora,
            lucro = a.lucro,
            cor = a.cor
        )

        fun ler(linha: String): Registro? = runCatching {
            val o = JSONObject(linha)
            val rh = o.getDouble("rh")
            Registro(
                quando = o.getLong("t"),
                app = o.getString("app"),
                valor = o.getDouble("v"),
                kmBusca = o.getDouble("kb"),
                kmViagem = o.getDouble("kv"),
                minTotal = o.getDouble("min"),
                rsKm = o.getDouble("rk"),
                rsHora = if (rh < 0) Double.NaN else rh,
                lucro = o.getDouble("l"),
                cor = Cor.valueOf(o.getString("c"))
            )
        }.getOrNull()
    }
}

/**
 * Histórico das chamadas vistas, em um arquivo de texto (uma chamada por linha).
 * Guarda as últimas [MAXIMO]; as mais antigas saem sozinhas.
 */
class Historico(context: Context) {
    private val arquivo = File(context.filesDir, "historico.jsonl")
    private val trava = Any()

    private val _registros = MutableStateFlow(carregar())
    /** Mais recentes primeiro. */
    val registros: StateFlow<List<Registro>> = _registros.asStateFlow()

    fun adicionar(r: Registro) = synchronized(trava) {
        val nova = (listOf(r) + _registros.value).take(MAXIMO)
        _registros.value = nova
        runCatching {
            if (nova.size >= MAXIMO) {
                // Reescreve o arquivo só com as que ficaram.
                arquivo.writeText(nova.asReversed().joinToString("\n", postfix = "\n") { it.json() })
            } else {
                arquivo.appendText(r.json() + "\n")
            }
        }
    }

    fun limpar() = synchronized(trava) {
        _registros.value = emptyList()
        runCatching { arquivo.delete() }
    }

    private fun carregar(): List<Registro> = runCatching {
        if (!arquivo.exists()) emptyList()
        else arquivo.readLines().mapNotNull { if (it.isBlank()) null else Registro.ler(it) }.asReversed().take(MAXIMO)
    }.getOrDefault(emptyList())

    companion object {
        const val MAXIMO = 2000
    }
}
