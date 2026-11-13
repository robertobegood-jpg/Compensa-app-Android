package com.roberto.compensa.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roberto.compensa.App
import com.roberto.compensa.dados.Registro
import com.roberto.compensa.nucleo.Cor
import com.roberto.compensa.ui.componentes.BarraProgresso
import com.roberto.compensa.ui.componentes.BotaoSecundario
import com.roberto.compensa.ui.componentes.BotaoTexto
import com.roberto.compensa.ui.componentes.DialogoOficina
import com.roberto.compensa.ui.componentes.Hud
import com.roberto.compensa.ui.componentes.Segmentado
import com.roberto.compensa.ui.componentes.TituloDisplay
import com.roberto.compensa.ui.decimal
import com.roberto.compensa.ui.theme.Oficina
import java.util.Calendar

private enum class Periodo(val rotulo: String, val dias: Int?) {
    HOJE("Hoje", 0), SEMANA("7 dias", 7), MES("30 dias", 30), TUDO("Tudo", null)
}

/** Resumo de uma faixa de horário. */
private data class Faixa(val hora: Int, val qtd: Int, val mediaKm: Double, val verdes: Int)

private fun faixas(lista: List<Registro>): List<Faixa> {
    val cal = Calendar.getInstance()
    return lista.groupBy {
        cal.timeInMillis = it.quando
        cal.get(Calendar.HOUR_OF_DAY)
    }.map { (h, rs) -> Faixa(h, rs.size, rs.map { it.rsKm }.average(), rs.count { it.cor == Cor.VERDE }) }
        .sortedBy { it.hora }
}

@Composable
fun HistoricoScreen(app: App) {
    val c = Oficina.cores
    val registros by app.historico.registros.collectAsStateWithLifecycle()
    var periodo by rememberSaveable { mutableStateOf(Periodo.HOJE) }
    var confirmarLimpar by remember { mutableStateOf(false) }
    var mostrarTodos by rememberSaveable { mutableStateOf(false) }

    val filtrados = remember(registros, periodo) {
        val dias = periodo.dias
        if (dias == null) registros
        else {
            val desde = inicioDeHoje() - dias * 24L * 60 * 60 * 1000
            registros.filter { it.quando >= desde }
        }
    }
    val porHora = remember(filtrados) { faixas(filtrados) }

    Pagina {
        TituloDisplay("O que *passou* por você.")
        Spacer(Modifier.height(16.dp))
        Segmentado(Periodo.entries.toList(), periodo, { it.rotulo }, { periodo = it })

        Secao("01", "Resumo")
        ResumoDia(filtrados)

        if (porHora.isNotEmpty()) {
            Secao("02", "Por horário")
            TextoApoio("Média de R$/km das chamadas que chegaram em cada hora. Ajuda a ver os melhores horários.")
            Spacer(Modifier.height(12.dp))
            val maior = porHora.maxOf { it.mediaKm }.coerceAtLeast(0.01)
            porHora.forEach { f ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Hud(String.format("%02dh", f.hora), modifier = Modifier.width(40.dp), cor = c.ink)
                    BarraProgresso(
                        (f.mediaKm / maior).toFloat(),
                        cor = c.accent,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Hud("${decimal(f.mediaKm, 2)}/km · ${f.qtd}", modifier = Modifier.width(110.dp), cor = c.ink)
                }
            }
        }

        Secao(if (porHora.isEmpty()) "02" else "03", "Chamadas")
        if (filtrados.isEmpty()) {
            TextoApoio("Nada por aqui neste período.")
        } else {
            val visiveis = if (mostrarTodos) filtrados.take(500) else filtrados.take(50)
            visiveis.forEach { LinhaRegistro(it) }
            if (filtrados.size > visiveis.size && !mostrarTodos) {
                BotaoTexto("Mostrar mais (${filtrados.size - visiveis.size})", onClick = { mostrarTodos = true })
            }
        }

        if (registros.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                BotaoSecundario("Limpar histórico", onClick = { confirmarLimpar = true }, perigo = true)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmarLimpar) {
        DialogoOficina(
            titulo = "Apagar todo o histórico?",
            onDismiss = { confirmarLimpar = false },
            acoes = {
                BotaoTexto("Cancelar", onClick = { confirmarLimpar = false }, cor = c.muted)
                BotaoTexto("Apagar", onClick = {
                    app.historico.limpar()
                    confirmarLimpar = false
                }, cor = c.danger)
            }
        ) {
            TextoApoio("As ${registros.size} chamadas guardadas serão apagadas. Não dá para desfazer.")
        }
    }
}
