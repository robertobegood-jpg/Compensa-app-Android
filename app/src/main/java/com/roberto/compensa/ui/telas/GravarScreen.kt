package com.roberto.compensa.ui.telas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roberto.compensa.App
import com.roberto.compensa.Estado
import com.roberto.compensa.gravacao.GravacaoService
import com.roberto.compensa.gravacao.Gravacoes
import com.roberto.compensa.gravacao.Video
import com.roberto.compensa.ui.componentes.BotaoPrimario
import com.roberto.compensa.ui.componentes.BotaoSecundario
import com.roberto.compensa.ui.componentes.BotaoTexto
import com.roberto.compensa.ui.componentes.DialogoOficina
import com.roberto.compensa.ui.componentes.Hud
import com.roberto.compensa.ui.componentes.Linha
import com.roberto.compensa.ui.componentes.LinhaMarcavel
import com.roberto.compensa.ui.componentes.PontoStatus
import com.roberto.compensa.ui.componentes.Segmentado
import com.roberto.compensa.ui.componentes.TituloDisplay
import com.roberto.compensa.ui.componentes.marcasDeCanto
import com.roberto.compensa.ui.dataHora
import com.roberto.compensa.ui.duracao
import com.roberto.compensa.ui.tamanhoArquivo
import com.roberto.compensa.ui.theme.Oficina

@Composable
fun GravarScreen(app: App) {
    val c = Oficina.cores
    val contexto = LocalContext.current
    val ajustes by app.preferencias.ajustes.collectAsStateWithLifecycle()
    val gravando by Estado.gravando.collectAsStateWithLifecycle()
    val erro by Estado.erroGravacao.collectAsStateWithLifecycle()

    fun tem(p: String) = ContextCompat.checkSelfPermission(contexto, p) == PackageManager.PERMISSION_GRANTED
    var atualizar by remember { mutableIntStateOf(0) }
    val temCamera = remember(atualizar) { tem(Manifest.permission.CAMERA) }
    val temAudio = remember(atualizar) { tem(Manifest.permission.RECORD_AUDIO) }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var apagar by remember { mutableStateOf<Video?>(null) }
    var negouAntes by remember { mutableStateOf(false) }

    LaunchedEffect(gravando, atualizar) { videos = Gravacoes.listar(contexto) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { atualizar++ }

    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        atualizar++
        if (r[Manifest.permission.CAMERA] == false) negouAntes = true
        else if (r[Manifest.permission.CAMERA] == true) GravacaoService.iniciar(contexto)
    }

    fun permissoesNecessarias(): Array<String> = buildList {
        add(Manifest.permission.CAMERA)
        if (ajustes.gravarAudio) add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    Pagina {
        TituloDisplay("Gravação de *segurança*.")
        Spacer(Modifier.height(8.dp))
        TextoApoio(
            "Grava vídeo e áudio do carro em segundo plano, com o Uber/99 aberto ou com a tela apagada. " +
                "Serve de prova em caso de golpe, briga ou acusação falsa."
        )

        Spacer(Modifier.height(20.dp))
        Bloco(Modifier.marcasDeCanto(c.lineStrong)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PontoStatus(if (gravando) c.vermelhao else c.faint)
                Spacer(Modifier.width(8.dp))
                Hud(if (gravando) "Gravando" else "Parada", cor = if (gravando) c.vermelhaoInk else c.muted)
            }
            Spacer(Modifier.height(16.dp))
            if (gravando) {
                BotaoSecundario(
                    "Parar gravação", onClick = { GravacaoService.parar(contexto) },
                    modifier = Modifier.fillMaxWidth(), perigo = true
                )
            } else {
                BotaoPrimario(
                    "Iniciar gravação",
                    onClick = {
                        if (temCamera && (!ajustes.gravarAudio || temAudio)) GravacaoService.iniciar(contexto)
                        else pedir.launch(permissoesNecessarias())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            erro?.let {
                Spacer(Modifier.height(12.dp))
                TextoApoio(it, cor = c.danger)
            }
            if (negouAntes && !temCamera) {
                Spacer(Modifier.height(12.dp))
                TextoApoio("Sem a câmera não dá para gravar. Libere nas configurações do app.", cor = c.ambarInk)
                BotaoTexto("Abrir configurações", onClick = {
                    runCatching {
                        contexto.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                })
            }
        }
        Spacer(Modifier.height(12.dp))
        TextoApoio(
            "O Android sempre mostra um aviso enquanto grava: a notificação \"Proteção ativa\" e, a partir do Android 12, " +
                "um pontinho verde no canto da tela. Nenhum app consegue esconder isso."
        )

        Secao("01", "Ajustes")
        Hud("Câmera")
        Spacer(Modifier.height(8.dp))
        Segmentado(
            listOf(true, false), ajustes.cameraFrontal, { if (it) "Frontal (cabine)" else "Traseira (rua)" },
            { app.preferencias.salvarAjustes(ajustes.copy(cameraFrontal = it)) }
        )
        Spacer(Modifier.height(8.dp))
        LinhaMarcavel(
            "Gravar áudio", ajustes.gravarAudio,
            { app.preferencias.salvarAjustes(ajustes.copy(gravarAudio = !ajustes.gravarAudio)) },
            subtitulo = if (ajustes.gravarAudio && !temAudio) "Falta permitir o microfone" else "Recomendado: a conversa é a melhor prova"
        )
        LinhaMarcavel(
            "Botão REC flutuante", ajustes.botaoRec,
            { app.preferencias.salvarAjustes(ajustes.copy(botaoRec = !ajustes.botaoRec)) },
            subtitulo = "Quadradinho por cima do Uber/99 para gravar com um toque (precisa do leitor ligado)"
        )
        Spacer(Modifier.height(8.dp))
        TextoApoio(
            "Dica: um adesivo \"veículo monitorado por câmera\" no vidro deixa a gravação às claras, " +
                "evita questionamentos e já inibe golpe. Cada vídeo tem no máximo 3 horas."
        )

        Secao("02", "Vídeos")
        if (videos.isEmpty()) {
            TextoApoio("Nenhum vídeo ainda. Eles ficam na galeria, em Filmes › Compensa.")
        } else {
            videos.forEach { v ->
                Column(Modifier.padding(vertical = 10.dp)) {
                    Text(v.nome, style = Oficina.tipos.body, color = c.ink)
                    Hud(
                        listOfNotNull(
                            dataHora(v.quando),
                            v.duracaoMs.takeIf { it > 0 }?.let { duracao(it) },
                            tamanhoArquivo(v.bytes)
                        ).joinToString(" · ")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BotaoTexto("Abrir", onClick = { Gravacoes.abrir(contexto, v) })
                        BotaoTexto("Enviar", onClick = { Gravacoes.compartilhar(contexto, v) })
                        BotaoTexto("Apagar", onClick = { apagar = v }, cor = c.danger)
                    }
                }
                Linha()
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    apagar?.let { v ->
        DialogoOficina(
            titulo = "Apagar este vídeo?",
            onDismiss = { apagar = null },
            acoes = {
                BotaoTexto("Cancelar", onClick = { apagar = null }, cor = c.muted)
                BotaoTexto("Apagar", onClick = {
                    Gravacoes.apagar(contexto, v)
                    apagar = null
                    atualizar++
                }, cor = c.danger)
            }
        ) {
            TextoApoio("${v.nome} será apagado do celular. Se ele for prova de algo, envie antes para um lugar seguro.")
        }
    }
}
