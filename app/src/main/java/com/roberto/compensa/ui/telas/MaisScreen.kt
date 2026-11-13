package com.roberto.compensa.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roberto.compensa.App
import com.roberto.compensa.Cafezinho
import com.roberto.compensa.dados.Compartilhar
import com.roberto.compensa.dados.Tema
import com.roberto.compensa.ui.Rotas
import com.roberto.compensa.ui.componentes.BotaoSecundario
import com.roberto.compensa.ui.componentes.Hud
import com.roberto.compensa.ui.componentes.Linha
import com.roberto.compensa.ui.componentes.LinhaMarcavel
import com.roberto.compensa.ui.componentes.Segmentado
import com.roberto.compensa.ui.componentes.TituloDisplay
import com.roberto.compensa.ui.tamanhoArquivo
import com.roberto.compensa.ui.theme.Oficina

@Composable
fun MaisScreen(app: App, onAbrir: (String) -> Unit) {
    val c = Oficina.cores
    val contexto = LocalContext.current
    val ajustes by app.preferencias.ajustes.collectAsStateWithLifecycle()
    var tamanhoCaptura by remember { mutableLongStateOf(app.captura.tamanho()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tamanhoCaptura = app.captura.tamanho() }
    LaunchedEffect(ajustes.modoCaptura) { tamanhoCaptura = app.captura.tamanho() }

    Pagina {
        TituloDisplay("Mais *opções*.")

        Secao("01", "Ferramentas")
        ItemMenu("Testar cálculo", "Digite os dados de uma corrida ou cole o texto de um card") { onAbrir(Rotas.TESTAR) }

        Secao("02", "Modo captura")
        LinhaMarcavel(
            "Guardar o texto das chamadas", ajustes.modoCaptura,
            { app.preferencias.salvarAjustes(ajustes.copy(modoCaptura = !ajustes.modoCaptura)) },
            subtitulo = "Para ajustar a leitura com cards reais. Ligue, receba algumas chamadas e envie o arquivo ao desenvolvedor."
        )
        Spacer(Modifier.height(8.dp))
        Hud("Arquivo: ${tamanhoArquivo(tamanhoCaptura)}")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoSecundario(
                "Enviar arquivo",
                onClick = {
                    if (app.captura.arquivo.exists()) {
                        runCatching {
                            Compartilhar.arquivos(contexto, listOf(app.captura.arquivo), "text/plain", "Enviar captura")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                habilitado = tamanhoCaptura > 0
            )
            BotaoSecundario(
                "Limpar",
                onClick = {
                    app.captura.limpar()
                    tamanhoCaptura = 0
                },
                modifier = Modifier.weight(1f),
                habilitado = tamanhoCaptura > 0
            )
        }
        Spacer(Modifier.height(8.dp))
        TextoApoio(
            "O arquivo tem só o que aparece no card (valor, distâncias, endereços de embarque e destino). " +
                "Desligue quando terminar."
        )

        Secao("03", "Tema")
        Segmentado(
            Tema.entries.toList(), ajustes.tema, { it.rotulo },
            { app.preferencias.salvarAjustes(ajustes.copy(tema = it)) }
        )

        Secao("04", "Apoie")
        if (Cafezinho.configurado) {
            ItemMenu("Um cafezinho para o dev", "O app é gratuito e sem anúncios. Ajude com um Pix de qualquer valor") {
                onAbrir(Rotas.CAFEZINHO)
            }
        }
        ItemMenu("Sobre o Compensa", "Como funciona, privacidade e avisos") { onAbrir(Rotas.SOBRE) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ItemMenu(titulo: String, subtitulo: String, onClick: () -> Unit) {
    val c = Oficina.cores
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = Oficina.tipos.body, color = c.ink)
            Text(subtitulo, style = Oficina.tipos.bodySm, color = c.muted)
        }
        Hud("→", cor = c.ink)
    }
    Linha()
}
