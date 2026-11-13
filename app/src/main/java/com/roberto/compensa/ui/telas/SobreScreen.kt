package com.roberto.compensa.ui.telas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.roberto.compensa.ui.componentes.CabecalhoInterno
import com.roberto.compensa.ui.componentes.Hud
import com.roberto.compensa.ui.theme.Oficina

@Composable
fun SobreScreen(onVoltar: () -> Unit) {
    val c = Oficina.cores
    val contexto = LocalContext.current
    val versao = runCatching {
        contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName
    }.getOrNull() ?: "?"

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(eyebrow = "Versão $versao", titulo = "Sobre o *Compensa*.", onVoltar = onVoltar)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Paragrafo("Como funciona", "O Compensa usa o recurso de acessibilidade do Android para ler o texto do card de chamada do Uber Driver e do 99 Motorista. Com o valor, as distâncias e os tempos, ele calcula R$/km, R$/hora, custo e lucro, e mostra o semáforo pelos critérios que você definiu.")
            Paragrafo("Leitura pela imagem", "O card novo do Uber esconde o texto do leitor. Nesse caso o Compensa tira um print da parte de baixo da tela e lê o texto da imagem, tudo dentro do celular. O print é descartado na hora e nada é salvo nem enviado.")
            Paragrafo("O que ele não faz", "Não aceita nem recusa corridas, não toca na tela desses apps e não lê nenhum outro app. Quem decide é sempre você.")
            Paragrafo("Privacidade", "Tudo fica no seu celular: histórico, critérios e vídeos. O app não tem internet liberada e não envia nada a ninguém. O arquivo do modo captura só sai do celular se você mesmo compartilhar.")
            Paragrafo("Leitura dos cards", "O Uber e a 99 mudam o visual dos cards de tempos em tempos. Se o semáforo parar de aparecer ou errar valores, ligue o modo captura (em Mais), receba algumas chamadas e envie o arquivo para o desenvolvedor ajustar.")
            Paragrafo("Aviso", "Os termos de uso do Uber e da 99 podem restringir apps que leem a tela do aplicativo de motorista. Use por sua conta. O Compensa não tem ligação com a Uber nem com a 99.")
            Spacer(Modifier.height(8.dp))
            Hud("Código aberto · GPL-3.0", cor = c.muted)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Paragrafo(titulo: String, texto: String) {
    Hud(titulo)
    Spacer(Modifier.height(6.dp))
    Text(texto, style = Oficina.tipos.body, color = Oficina.cores.ink)
    Spacer(Modifier.height(20.dp))
}
