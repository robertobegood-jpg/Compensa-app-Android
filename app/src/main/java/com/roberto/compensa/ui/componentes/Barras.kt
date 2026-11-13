package com.roberto.compensa.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.roberto.compensa.ui.theme.Oficina

data class AbaNav(val rota: String, val rotulo: String, val icone: ImageVector)

/** Topo das abas principais: situação do leitor à esquerda, marca à direita. */
@Composable
fun BarraSuperior(leitorAtivo: Boolean, gravando: Boolean) {
    val c = Oficina.cores
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PontoStatus(if (leitorAtivo) c.verde else c.faint, tamanho = 6.dp)
                    Spacer(Modifier.width(6.dp))
                    Hud(if (leitorAtivo) "Leitor ligado" else "Leitor desligado")
                }
                if (gravando) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PontoStatus(c.vermelhao, tamanho = 6.dp)
                        Spacer(Modifier.width(6.dp))
                        Hud("Gravando", cor = c.vermelhaoInk)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            TituloDisplay(
                "Com*pensa*?",
                estilo = Oficina.tipos.heading.copy(fontSize = 18.sp, lineHeight = 20.sp, letterSpacing = (-0.01).em),
                cor = c.ink
            )
        }
        Linha()
    }
}

/**
 * Menu inferior: a aba ativa vira bloco invertido.
 * [marcas] coloca um ponto de status no ícone de uma aba (rota → cor).
 */
@Composable
fun BarraInferior(
    abas: List<AbaNav>,
    rotaAtual: String?,
    onAba: (String) -> Unit,
    marcas: Map<String, Color> = emptyMap()
) {
    val c = Oficina.cores
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.bg)
    ) {
        Linha()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
        ) {
            abas.forEach { aba ->
                val ativa = aba.rota == rotaAtual
                val frente = if (ativa) c.invertFg else c.muted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (ativa) c.invertBg else Color.Transparent)
                        .clickable(role = Role.Tab) { onAba(aba.rota) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box {
                        Icon(aba.icone, contentDescription = null, modifier = Modifier.size(20.dp), tint = frente)
                        marcas[aba.rota]?.let { cor ->
                            PontoStatus(
                                cor,
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-2).dp),
                                tamanho = 7.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Com 5 abas o espaço é curto: letra menor e menos espaçada para não encostar.
                    val estilo = if (abas.size >= 5) Oficina.tipos.hud.copy(fontSize = 9.sp, letterSpacing = 0.02.em)
                    else Oficina.tipos.hud.copy(letterSpacing = 0.08.em)
                    Hud(aba.rotulo, cor = frente, estilo = estilo)
                }
            }
        }
    }
}
