package com.roberto.compensa.ui.telas

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roberto.compensa.App
import com.roberto.compensa.Estado
import com.roberto.compensa.dados.Registro
import com.roberto.compensa.leitor.LeitorService
import com.roberto.compensa.nucleo.Cor
import com.roberto.compensa.ui.componentes.BotaoPrimario
import com.roberto.compensa.ui.componentes.BotaoSecundario
import com.roberto.compensa.ui.componentes.BotaoTexto
import com.roberto.compensa.ui.componentes.Hud
import com.roberto.compensa.ui.componentes.Linha
import com.roberto.compensa.ui.componentes.PontoStatus
import com.roberto.compensa.ui.componentes.TituloDisplay
import com.roberto.compensa.ui.componentes.marcasDeCanto
import com.roberto.compensa.ui.corSinal
import com.roberto.compensa.ui.decimal
import com.roberto.compensa.ui.hora
import com.roberto.compensa.ui.reais
import com.roberto.compensa.ui.theme.Oficina
import java.util.Calendar

/** Abre direto o interruptor do Compensa em Acessibilidade (Android 13+); senão, a lista geral. */
fun abrirAcessibilidade(contexto: Context) {
    val direto = if (Build.VERSION.SDK_INT >= 33) {
        Intent(Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS)
            .putExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName(contexto, LeitorService::class.java).flattenToString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    } else null
    val abriu = direto != null && runCatching { contexto.startActivity(direto) }.isSuccess
    if (!abriu) {
        runCatching {
            contexto.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

/** Início do dia de hoje, em milissegundos. */
fun inicioDeHoje(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun PainelScreen(app: App, onIrCriterios: () -> Unit, onIrGravar: () -> Unit, onIrTestar: () -> Unit) {
    val c = Oficina.cores
    val contexto = LocalContext.current
    val leitorAtivo by Estado.leitorAtivo.collectAsStateWithLifecycle()
    val ajustes by app.preferencias.ajustes.collectAsStateWithLifecycle()
    val pausado = ajustes.leitorPausado
    val gravando by Estado.gravando.collectAsStateWithLifecycle()
    val registros by app.historico.registros.collectAsStateWithLifecycle()
    val hoje = remember(registros) {
        val desde = inicioDeHoje()
        registros.filter { it.quando >= desde }
    }

    Pagina {
        TituloDisplay("Cada chamada, *na ponta do lápis*.")
        Spacer(Modifier.height(8.dp))
        TextoApoio("O Compensa lê o card do Uber e da 99 e mostra na hora se a corrida vale a pena.")

        Secao("01", "Leitor")
        if (leitorAtivo) {
            Bloco(Modifier.marcasDeCanto(c.lineStrong)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PontoStatus(if (pausado) c.ambar else c.verde)
                    Spacer(Modifier.width(8.dp))
                    Hud(if (pausado) "Pausado" else "Ligado", cor = if (pausado) c.ambarInk else c.verdeInk)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Abra o Uber Driver ou o 99 Motorista. Quando chegar uma chamada, o semáforo aparece por cima. " +
                        "Toque nele para fechar; arraste para mudar de lugar.",
                    style = Oficina.tipos.body, color = c.ink
                )
                Spacer(Modifier.height(16.dp))
                if (pausado) {
                    BotaoPrimario(
                        "Retomar o leitor",
                        onClick = { app.preferencias.salvarAjustes(ajustes.copy(leitorPausado = false)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    BotaoSecundario(
                        "Pausar o leitor",
                        onClick = { app.preferencias.salvarAjustes(ajustes.copy(leitorPausado = true)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(4.dp))
                TextoApoio("Pausar e retomar é na hora, sem passar pelas configurações. O botão REC continua funcionando.")
                BotaoTexto("Desativar de vez", onClick = { LeitorService.desligar() }, cor = c.muted)
            }
        } else {
            Bloco(Modifier.marcasDeCanto(c.lineStrong)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PontoStatus(c.faint)
                    Spacer(Modifier.width(8.dp))
                    Hud("Desligado")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Para ler as chamadas, ative o Compensa em Acessibilidade " +
                        "(Configurações › Acessibilidade › Apps instalados › Compensa).",
                    style = Oficina.tipos.body, color = c.ink
                )
                Spacer(Modifier.height(8.dp))
                TextoApoio(
                    "O Compensa só lê a tela do Uber Driver e do 99 Motorista. Não toca em nada nesses apps, " +
                        "não aceita corridas sozinho e não envia nada para a internet: tudo fica no seu celular."
                )
                Spacer(Modifier.height(16.dp))
                BotaoPrimario(
                    "Ligar o leitor", seta = true,
                    onClick = {
                        abrirAcessibilidade(contexto)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Secao("02", "Hoje")
        ResumoDia(hoje)

        hoje.firstOrNull()?.let { ultima ->
            Secao("03", "Última chamada")
            LinhaRegistro(ultima)
        }

        Secao(if (hoje.isEmpty()) "03" else "04", "Atalhos")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            BotaoSecundario("Critérios", onClick = onIrCriterios, modifier = Modifier.weight(1f))
            BotaoSecundario("Testar", onClick = onIrTestar, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        BotaoSecundario(
            if (gravando) "Gravando · ver" else "Gravação de segurança",
            onClick = onIrGravar,
            modifier = Modifier.fillMaxWidth(),
            perigo = gravando
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ResumoDia(lista: List<Registro>) {
    val c = Oficina.cores
    if (lista.isEmpty()) {
        TextoApoio("Nenhuma chamada lida hoje ainda.")
        return
    }
    val verdes = lista.count { it.cor == Cor.VERDE }
    val amarelas = lista.count { it.cor == Cor.AMARELO }
    val vermelhas = lista.count { it.cor == Cor.VERMELHO }
    val mediaKm = lista.map { it.rsKm }.average()
    val comHora = lista.map { it.rsHora }.filter { !it.isNaN() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Medida("Chamadas", lista.size.toString(), Modifier.weight(1f))
        Medida("Média R$/km", decimal(mediaKm, 2), Modifier.weight(1f))
        Medida("Média R$/h", if (comHora.isEmpty()) "—" else decimal(comHora.average(), 0), Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Medida("Verdes", verdes.toString(), Modifier.weight(1f), cor = c.verdeInk)
        Medida("Amarelas", amarelas.toString(), Modifier.weight(1f), cor = c.ambarInk)
        Medida("Vermelhas", vermelhas.toString(), Modifier.weight(1f), cor = c.vermelhaoInk)
    }
}

/** Uma linha do histórico: cor, hora, app, valor e R$/km. */
@Composable
fun LinhaRegistro(r: Registro) {
    val c = Oficina.cores
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PontoStatus(corSinal(r.cor), tamanho = 10.dp)
        Spacer(Modifier.width(12.dp))
        Hud("${hora(r.quando)} · ${r.app}", modifier = Modifier.width(96.dp))
        Text(reais(r.valor), style = Oficina.tipos.body, color = c.inkStrong, modifier = Modifier.weight(1f))
        Hud(
            "${decimal(r.rsKm, 2)}/km · " + (if (r.rsHora.isNaN()) "—" else decimal(r.rsHora, 0)) + "/h",
            cor = c.ink
        )
    }
    Linha()
}
