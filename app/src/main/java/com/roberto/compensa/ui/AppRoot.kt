package com.roberto.compensa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Traffic
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roberto.compensa.App
import com.roberto.compensa.Estado
import com.roberto.compensa.ui.componentes.AbaNav
import com.roberto.compensa.ui.componentes.BarraInferior
import com.roberto.compensa.ui.componentes.BarraSuperior
import com.roberto.compensa.ui.telas.CafezinhoScreen
import com.roberto.compensa.ui.telas.CriteriosScreen
import com.roberto.compensa.ui.telas.GravarScreen
import com.roberto.compensa.ui.telas.HistoricoScreen
import com.roberto.compensa.ui.telas.MaisScreen
import com.roberto.compensa.ui.telas.PainelScreen
import com.roberto.compensa.ui.telas.SobreScreen
import com.roberto.compensa.ui.telas.TestarScreen
import com.roberto.compensa.ui.theme.Oficina

object Rotas {
    const val PAINEL = "painel"
    const val CRITERIOS = "criterios"
    const val HISTORICO = "historico"
    const val GRAVAR = "gravar"
    const val MAIS = "mais"

    // Telas internas (abertas a partir de "Mais")
    const val TESTAR = "testar"
    const val CAFEZINHO = "cafezinho"
    const val SOBRE = "sobre"
}

private val abas = listOf(
    AbaNav(Rotas.PAINEL, "Painel", Icons.Outlined.Dashboard),
    AbaNav(Rotas.CRITERIOS, "Critérios", Icons.Outlined.Traffic),
    AbaNav(Rotas.HISTORICO, "Histórico", Icons.Outlined.History),
    AbaNav(Rotas.GRAVAR, "Gravar", Icons.Outlined.Videocam),
    AbaNav(Rotas.MAIS, "Mais", Icons.Outlined.MoreHoriz)
)

@Composable
fun AppRoot(app: App, abaPedida: String?, onAbaConsumida: () -> Unit) {
    val cores = Oficina.cores
    var aba by rememberSaveable { mutableStateOf(Rotas.PAINEL) }
    var interna by rememberSaveable { mutableStateOf<String?>(null) }
    val leitorAtivo by Estado.leitorAtivo.collectAsStateWithLifecycle()
    val gravando by Estado.gravando.collectAsStateWithLifecycle()

    LaunchedEffect(abaPedida) {
        if (abaPedida != null) {
            if (abas.any { it.rota == abaPedida }) {
                aba = abaPedida
                interna = null
            }
            onAbaConsumida()
        }
    }

    BackHandler(enabled = interna != null) { interna = null }
    BackHandler(enabled = interna == null && aba != Rotas.PAINEL) { aba = Rotas.PAINEL }

    val atual = interna
    if (atual != null) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            when (atual) {
                Rotas.TESTAR -> TestarScreen(app, onVoltar = { interna = null })
                Rotas.CAFEZINHO -> CafezinhoScreen(onVoltar = { interna = null })
                Rotas.SOBRE -> SobreScreen(onVoltar = { interna = null })
                else -> LaunchedEffect(Unit) { interna = null }
            }
        }
        return
    }

    Scaffold(
        containerColor = cores.bg,
        topBar = { BarraSuperior(leitorAtivo = leitorAtivo, gravando = gravando) },
        bottomBar = { BarraInferior(abas = abas, rotaAtual = aba, onAba = { aba = it }) }
    ) { recuo ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(recuo)
        ) {
            when (aba) {
                Rotas.PAINEL -> PainelScreen(
                    app,
                    onIrCriterios = { aba = Rotas.CRITERIOS },
                    onIrGravar = { aba = Rotas.GRAVAR },
                    onIrTestar = { interna = Rotas.TESTAR }
                )
                Rotas.CRITERIOS -> CriteriosScreen(app, onIrTestar = { interna = Rotas.TESTAR })
                Rotas.HISTORICO -> HistoricoScreen(app)
                Rotas.GRAVAR -> GravarScreen(app)
                else -> MaisScreen(app, onAbrir = { interna = it })
            }
        }
    }
}
