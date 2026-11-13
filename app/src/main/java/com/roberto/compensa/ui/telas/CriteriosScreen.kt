package com.roberto.compensa.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roberto.compensa.App
import com.roberto.compensa.nucleo.Criterios
import com.roberto.compensa.ui.componentes.BotaoSecundario
import com.roberto.compensa.ui.componentes.BotaoTexto
import com.roberto.compensa.ui.componentes.CampoNumero
import com.roberto.compensa.ui.componentes.DialogoOficina
import com.roberto.compensa.ui.componentes.Hud
import com.roberto.compensa.ui.componentes.LinhaMarcavel
import com.roberto.compensa.ui.componentes.PontoStatus
import com.roberto.compensa.ui.componentes.TituloDisplay
import com.roberto.compensa.ui.theme.Oficina

@Composable
fun CriteriosScreen(app: App, onIrTestar: () -> Unit) {
    val criterios by app.preferencias.criterios.collectAsStateWithLifecycle()
    val ajustes by app.preferencias.ajustes.collectAsStateWithLifecycle()
    var confirmarPadrao by remember { mutableStateOf(false) }

    /** Copia, altera e salva (os critérios são imutáveis para quem está ouvindo). */
    fun mudar(bloco: Criterios.() -> Unit) {
        app.preferencias.salvarCriterios(criterios.copia().apply(bloco))
    }

    Pagina {
        TituloDisplay("Quando a corrida *compensa*.")
        Spacer(Modifier.height(8.dp))
        TextoApoio(
            "Cada critério tem um limite para verde e outro para amarelo; passando dele, fica vermelho. " +
                "A cor final é a do pior critério ligado."
        )

        Secao("01", "R$ por km")
        Criterio(
            ligado = criterios.usarKm, onLigado = { v -> mudar { usarKm = v } },
            titulo = "Valor por km rodado", subtitulo = "Conta a ida até o passageiro + a viagem",
            verde = criterios.kmVerde, onVerde = { v -> mudar { kmVerde = v } },
            amarelo = criterios.kmAmarelo, onAmarelo = { v -> mudar { kmAmarelo = v } },
            sufixo = "R$/km", maiorMelhor = true
        )

        Secao("02", "R$ por hora")
        Criterio(
            ligado = criterios.usarHora, onLigado = { v -> mudar { usarHora = v } },
            titulo = "Valor por hora", subtitulo = "Tempo de busca + tempo de viagem",
            verde = criterios.horaVerde, onVerde = { v -> mudar { horaVerde = v } },
            amarelo = criterios.horaAmarelo, onAmarelo = { v -> mudar { horaAmarelo = v } },
            sufixo = "R$/h", maiorMelhor = true, casas = 0
        )

        Secao("03", "Busca")
        Criterio(
            ligado = criterios.usarBusca, onLigado = { v -> mudar { usarBusca = v } },
            titulo = "Distância até o passageiro", subtitulo = "Quanto menor, melhor",
            verde = criterios.buscaVerde, onVerde = { v -> mudar { buscaVerde = v } },
            amarelo = criterios.buscaAmarelo, onAmarelo = { v -> mudar { buscaAmarelo = v } },
            sufixo = "km", maiorMelhor = false, casas = 1
        )

        Secao("04", "Valor mínimo")
        Criterio(
            ligado = criterios.usarValor, onLigado = { v -> mudar { usarValor = v } },
            titulo = "Valor da corrida", subtitulo = "Corridas curtas demais costumam não pagar o deslocamento",
            verde = criterios.valorVerde, onVerde = { v -> mudar { valorVerde = v } },
            amarelo = criterios.valorAmarelo, onAmarelo = { v -> mudar { valorAmarelo = v } },
            sufixo = "R$", maiorMelhor = true
        )

        Secao("05", "Custos do carro")
        TextoApoio("Usados para calcular o lucro de cada corrida.")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoNumero(criterios.consumoKmL, { v -> mudar { consumoKmL = v } }, "Consumo", Modifier.weight(1f), casas = 1, sufixo = "km/l")
            CampoNumero(criterios.precoLitro, { v -> mudar { precoLitro = v } }, "Combustível", Modifier.weight(1f), sufixo = "R$/l")
        }
        Spacer(Modifier.height(8.dp))
        CampoNumero(
            criterios.outrosCustosKm, { v -> mudar { outrosCustosKm = v } }, "Outros custos",
            Modifier.fillMaxWidth(), sufixo = "R$/km", ajuda = "Manutenção, pneus, desgaste, seguro..."
        )
        Spacer(Modifier.height(8.dp))
        CampoNumero(
            criterios.velocidadeMediaKmH, { v -> mudar { velocidadeMediaKmH = v } }, "Velocidade média",
            Modifier.fillMaxWidth(), casas = 0, sufixo = "km/h",
            ajuda = "Usada para estimar o tempo quando o card só mostra a distância"
        )

        Secao("06", "Leitura")
        LinhaMarcavel("Ler Uber Driver", ajustes.lerUber, { app.preferencias.salvarAjustes(ajustes.copy(lerUber = !ajustes.lerUber)) })
        LinhaMarcavel("Ler 99 Motorista", ajustes.ler99, { app.preferencias.salvarAjustes(ajustes.copy(ler99 = !ajustes.ler99)) })
        Spacer(Modifier.height(8.dp))
        CampoNumero(
            ajustes.tempoBalao.toDouble(),
            { v -> app.preferencias.salvarAjustes(ajustes.copy(tempoBalao = v.toInt().coerceIn(3, 120))) },
            "Tempo do semáforo na tela", Modifier.fillMaxWidth(), casas = 0, sufixo = "segundos",
            ajuda = "Ele também some sozinho quando o card da chamada sai da tela"
        )

        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoSecundario("Testar critérios", onClick = onIrTestar, modifier = Modifier.weight(1f))
            BotaoSecundario("Restaurar padrão", onClick = { confirmarPadrao = true }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmarPadrao) {
        DialogoOficina(
            titulo = "Voltar aos valores de fábrica?",
            onDismiss = { confirmarPadrao = false },
            acoes = {
                BotaoTexto("Cancelar", onClick = { confirmarPadrao = false }, cor = Oficina.cores.muted)
                BotaoTexto("Restaurar", onClick = {
                    app.preferencias.restaurarCriterios()
                    confirmarPadrao = false
                })
            }
        ) {
            TextoApoio("R$ 2,00/km e R$ 40/h para verde, busca até 3 km, corrida a partir de R$ 10, carro fazendo 11 km/l.")
        }
    }
}

@Composable
private fun Criterio(
    ligado: Boolean,
    onLigado: (Boolean) -> Unit,
    titulo: String,
    subtitulo: String,
    verde: Double,
    onVerde: (Double) -> Unit,
    amarelo: Double,
    onAmarelo: (Double) -> Unit,
    sufixo: String,
    maiorMelhor: Boolean,
    casas: Int = 2
) {
    val c = Oficina.cores
    LinhaMarcavel(titulo, ligado, { onLigado(!ligado) }, subtitulo = subtitulo)
    if (!ligado) return
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoNumero(verde, onVerde, if (maiorMelhor) "Verde a partir de" else "Verde até", Modifier.weight(1f), casas = casas, sufixo = sufixo)
        CampoNumero(amarelo, onAmarelo, if (maiorMelhor) "Amarelo a partir de" else "Amarelo até", Modifier.weight(1f), casas = casas, sufixo = sufixo)
    }
    val invertido = if (maiorMelhor) amarelo > verde else amarelo < verde
    Row(verticalAlignment = Alignment.CenterVertically) {
        PontoStatus(c.vermelhao, tamanho = 6.dp)
        Spacer(Modifier.width(6.dp))
        Hud(
            if (invertido) "Atenção: o amarelo passou do verde"
            else if (maiorMelhor) "Vermelho abaixo do amarelo" else "Vermelho acima do amarelo",
            cor = if (invertido) c.danger else c.muted
        )
    }
}
