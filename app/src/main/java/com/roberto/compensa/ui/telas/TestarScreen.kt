package com.roberto.compensa.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roberto.compensa.App
import com.roberto.compensa.nucleo.Calculadora
import com.roberto.compensa.nucleo.Chamada
import com.roberto.compensa.nucleo.LeitorCard
import com.roberto.compensa.ui.componentes.CabecalhoInterno
import com.roberto.compensa.ui.componentes.CampoNumero
import com.roberto.compensa.ui.componentes.CampoTexto
import com.roberto.compensa.ui.componentes.Segmentado
import com.roberto.compensa.ui.theme.Oficina

private enum class Modo(val rotulo: String) { DIGITAR("Digitar"), COLAR("Colar card") }

private const val EXEMPLO_CARD = "UberX\nR$ 18,40\n8 min (2,8 km) de distância\nViagem de 22 min (9,6 km)\nAceitar"

@Composable
fun TestarScreen(app: App, onVoltar: () -> Unit) {
    val criterios by app.preferencias.criterios.collectAsStateWithLifecycle()
    var modo by rememberSaveable { mutableStateOf(Modo.DIGITAR) }

    var valor by rememberSaveable { mutableDoubleStateOf(18.40) }
    var kmBusca by rememberSaveable { mutableDoubleStateOf(2.8) }
    var minBusca by rememberSaveable { mutableDoubleStateOf(8.0) }
    var kmViagem by rememberSaveable { mutableDoubleStateOf(9.6) }
    var minViagem by rememberSaveable { mutableDoubleStateOf(22.0) }
    var textoCard by rememberSaveable { mutableStateOf(EXEMPLO_CARD) }

    val avaliacao = remember(modo, valor, kmBusca, minBusca, kmViagem, minViagem, textoCard, criterios) {
        val chamada = when (modo) {
            Modo.DIGITAR -> Chamada("Teste", valor, kmBusca, minBusca, kmViagem, minViagem, false)
            Modo.COLAR -> LeitorCard.ler("Teste", textoCard.lines(), criterios.velocidadeMediaKmH)
        }
        chamada?.let { Calculadora.avaliar(it, criterios) }
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(eyebrow = "Ferramentas", titulo = "Testar o *cálculo*.", onVoltar = onVoltar)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Segmentado(Modo.entries.toList(), modo, { it.rotulo }, { modo = it })
            Spacer(Modifier.height(16.dp))
            when (modo) {
                Modo.DIGITAR -> {
                    CampoNumero(valor, { valor = it }, "Valor da corrida", Modifier.fillMaxWidth(), sufixo = "R$")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoNumero(kmBusca, { kmBusca = it }, "Busca", Modifier.weight(1f), casas = 1, sufixo = "km")
                        CampoNumero(minBusca, { minBusca = it }, "Busca", Modifier.weight(1f), casas = 0, sufixo = "min")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoNumero(kmViagem, { kmViagem = it }, "Viagem", Modifier.weight(1f), casas = 1, sufixo = "km")
                        CampoNumero(minViagem, { minViagem = it }, "Viagem", Modifier.weight(1f), casas = 0, sufixo = "min")
                    }
                }
                Modo.COLAR -> {
                    CampoTexto(
                        valor = textoCard,
                        onValor = { textoCard = it },
                        rotulo = "Texto do card (uma informação por linha)",
                        linhaUnica = false,
                        minLinhas = 6
                    )
                    Spacer(Modifier.height(4.dp))
                    TextoApoio("Use para conferir a leitura com textos do modo captura.")
                }
            }

            Spacer(Modifier.height(24.dp))
            val a = avaliacao
            if (a != null) {
                CartaoAvaliacao(a)
            } else {
                TextoApoio(
                    "O leitor não reconheceu isso como uma chamada. Ele precisa de um valor em R$, " +
                        "pelo menos uma distância e a palavra \"Aceitar\" (ou os dois trechos com tempo e km).",
                    cor = Oficina.cores.ambarInk
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
