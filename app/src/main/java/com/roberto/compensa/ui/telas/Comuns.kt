package com.roberto.compensa.ui.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roberto.compensa.nucleo.Avaliacao
import com.roberto.compensa.ui.componentes.Eyebrow
import com.roberto.compensa.ui.componentes.Hud
import com.roberto.compensa.ui.componentes.PontoStatus
import com.roberto.compensa.ui.corSinal
import com.roberto.compensa.ui.corSinalTexto
import com.roberto.compensa.ui.decimal
import com.roberto.compensa.ui.porHora
import com.roberto.compensa.ui.reais
import com.roberto.compensa.ui.theme.Oficina
import kotlin.math.roundToInt

/** Corpo rolável padrão das abas. */
@Composable
fun Pagina(conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        content = conteudo
    )
}

@Composable
fun Secao(numero: String, rotulo: String) {
    Spacer(Modifier.height(28.dp))
    Eyebrow(numero, rotulo)
    Spacer(Modifier.height(12.dp))
}

/** Bloco com contorno de 1px. */
@Composable
fun Bloco(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, Oficina.cores.line)
            .padding(16.dp),
        content = conteudo
    )
}

/** Número grande com rótulo HUD em cima. */
@Composable
fun Medida(rotulo: String, valor: String, modifier: Modifier = Modifier, cor: Color = Oficina.cores.inkStrong) {
    Column(modifier) {
        Hud(rotulo)
        Spacer(Modifier.height(4.dp))
        Text(valor, style = Oficina.tipos.heading, color = cor)
    }
}

@Composable
fun LinhaValor(rotulo: String, valor: String, cor: Color = Oficina.cores.ink) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rotulo, style = Oficina.tipos.body, color = Oficina.cores.muted, modifier = Modifier.weight(1f))
        Text(valor, style = Oficina.tipos.body, color = cor)
    }
}

@Composable
fun TextoApoio(texto: String, cor: Color = Oficina.cores.muted) {
    Text(texto, style = Oficina.tipos.bodySm, color = cor)
}

/** O mesmo semáforo do balão, dentro do app (teste e última chamada). */
@Composable
fun CartaoAvaliacao(a: Avaliacao) {
    val c = Oficina.cores
    val sinal = corSinal(a.cor)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(1.dp, c.line)
    ) {
        Box(
            Modifier
                .width(10.dp)
                .fillMaxHeight()
                .background(sinal)
        )
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PontoStatus(sinal)
                Spacer(Modifier.width(8.dp))
                Hud(a.cor.rotulo, cor = corSinalTexto(a.cor))
                Hud("  ·  " + a.chamada.app)
            }
            Spacer(Modifier.height(8.dp))
            Text(reais(a.chamada.valor), style = Oficina.tipos.displayMd, color = c.inkStrong)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Medida("R$/km", decimal(a.rsKm, 2))
                Medida("R$/hora", if (a.rsHora.isNaN()) "—" else decimal(a.rsHora, 0))
                Medida("Lucro", decimal(a.lucro, 2))
            }
            Spacer(Modifier.height(12.dp))
            val busca = if (a.chamada.kmBusca >= 0) "Busca ${decimal(a.chamada.kmBusca)} km · " else ""
            TextoApoio(
                "${busca}Total ${decimal(a.chamada.kmTotal())} km · ${a.chamada.minTotal().roundToInt()} min" +
                    (if (a.chamada.tempoEstimado) " (tempo estimado)" else "")
            )
            TextoApoio("Custo ${reais(a.custo)} · Lucro ${porHora(a.lucroHora)}")
            a.motivos.forEach {
                Spacer(Modifier.height(4.dp))
                TextoApoio(it, cor = corSinalTexto(a.cor))
            }
        }
    }
}
