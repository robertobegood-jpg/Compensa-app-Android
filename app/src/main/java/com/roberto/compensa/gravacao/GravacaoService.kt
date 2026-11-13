package com.roberto.compensa.gravacao

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.roberto.compensa.App
import com.roberto.compensa.Estado
import com.roberto.compensa.MainActivity
import com.roberto.compensa.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gravação de segurança (vídeo + áudio) em segundo plano, com a tela apagada ou com outro app aberto.
 * O Android exige um aviso fixo na barra enquanto grava; ele é discreto ("Proteção ativa").
 * Os vídeos ficam em Filmes/Compensa (galeria).
 */
class GravacaoService : LifecycleService() {

    private var gravacao: Recording? = null
    private var iniciando = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACAO_PARAR) {
            parar()
            return START_NOT_STICKY
        }
        if (gravacao != null || iniciando) return START_NOT_STICKY
        iniciar()
        return START_NOT_STICKY
    }

    private fun temPermissao(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun iniciar() {
        val ajustes = (application as App).preferencias.ajustes.value
        if (!temPermissao(Manifest.permission.CAMERA)) {
            falhar("Sem permissão da câmera. Abra o Compensa e permita.")
            return
        }
        val comAudio = ajustes.gravarAudio && temPermissao(Manifest.permission.RECORD_AUDIO)

        var tipo = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tipo = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            if (comAudio && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                tipo = tipo or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
        }
        try {
            ServiceCompat.startForeground(this, ID_NOTIFICACAO, notificacao(), tipo)
        } catch (e: Exception) {
            falhar("O Android não deixou iniciar a gravação agora. Tente pelo app.")
            return
        }

        // Libera espaço: apaga vídeos antigos, se o motorista escolheu isso.
        if (ajustes.apagarAposDias > 0) Gravacoes.apagarAntigos(this, ajustes.apagarAposDias)

        iniciando = true
        val futuro = ProcessCameraProvider.getInstance(this)
        futuro.addListener({
            try {
                val provedor = futuro.get()
                val q = ajustes.qualidade
                val alvo = if (q.resolucao <= 480) Quality.SD else Quality.HD
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(alvo, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                    // Taxa de bits baixa = arquivo pequeno. Para prova, 480p já mostra rostos e vozes.
                    .setTargetVideoEncodingBitRate(q.bitsPorSegundo)
                    .build()
                val captura = VideoCapture.withOutput(recorder)

                val preferida = if (ajustes.cameraFrontal) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val outra = if (ajustes.cameraFrontal) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                val camera = when {
                    provedor.hasCamera(preferida) -> preferida
                    provedor.hasCamera(outra) -> outra
                    else -> null
                }
                if (camera == null) {
                    iniciando = false
                    falhar("Nenhuma câmera disponível.")
                    return@addListener
                }
                provedor.unbindAll()
                provedor.bindToLifecycle(this, camera, captura)

                val preparo = prepararSaida(recorder)
                val pronta = if (comAudio) comAudio(preparo) else preparo
                gravacao = pronta.start(ContextCompat.getMainExecutor(this)) { evento ->
                    when (evento) {
                        is VideoRecordEvent.Start -> {
                            Estado.gravando.value = true
                            Estado.erroGravacao.value = null
                        }
                        is VideoRecordEvent.Finalize -> {
                            Estado.gravando.value = false
                            if (evento.hasError() &&
                                evento.error != VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED &&
                                evento.error != VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA
                            ) {
                                Estado.erroGravacao.value = "A gravação parou com erro (${evento.error})."
                            }
                            gravacao = null
                            provedor.unbindAll()
                            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    }
                }
                iniciando = false
            } catch (e: Exception) {
                iniciando = false
                falhar("Não foi possível abrir a câmera: ${e.message ?: e.javaClass.simpleName}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("MissingPermission") // conferida antes em iniciar()
    private fun comAudio(p: PendingRecording): PendingRecording = p.withAudioEnabled()

    private fun prepararSaida(recorder: Recorder): PendingRecording {
        val nome = "Compensa_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valores = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nome)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Gravacoes.PASTA_RELATIVA)
            }
            val saida = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(valores)
                .setDurationLimitMillis(LIMITE_MS)
                .build()
            recorder.prepareRecording(this, saida)
        } else {
            val pasta = Gravacoes.pastaAntiga(this)
            val saida = FileOutputOptions.Builder(File(pasta, "$nome.mp4"))
                .setDurationLimitMillis(LIMITE_MS)
                .build()
            recorder.prepareRecording(this, saida)
        }
    }

    private fun parar() {
        val g = gravacao
        if (g != null) {
            g.stop() // o evento Finalize encerra o serviço
        } else {
            Estado.gravando.value = false
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun falhar(msg: String) {
        Estado.erroGravacao.value = msg
        Estado.gravando.value = false
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun notificacao(): Notification {
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pararIntent = PendingIntent.getService(
            this, 1, Intent(this, GravacaoService::class.java).setAction(ACAO_PARAR),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, App.CANAL_PROTECAO)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("Proteção ativa")
            .setContentIntent(abrir)
            .addAction(0, "Parar", pararIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        gravacao?.stop()
        gravacao = null
        Estado.gravando.value = false
        super.onDestroy()
    }

    companion object {
        private const val ACAO_PARAR = "com.roberto.compensa.PARAR_GRAVACAO"
        private const val ID_NOTIFICACAO = 42
        /** Cada arquivo tem no máximo 3 horas. */
        private const val LIMITE_MS = 3L * 60 * 60 * 1000

        fun iniciar(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, GravacaoService::class.java))
        }

        fun parar(context: Context) {
            context.startService(Intent(context, GravacaoService::class.java).setAction(ACAO_PARAR))
        }
    }
}
