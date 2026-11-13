package com.roberto.compensa

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.roberto.compensa.dados.Captura
import com.roberto.compensa.dados.Historico
import com.roberto.compensa.dados.Preferencias
import com.roberto.compensa.ui.theme.FontesOficina

class App : Application() {
    lateinit var preferencias: Preferencias
        private set
    lateinit var historico: Historico
        private set
    lateinit var captura: Captura
        private set
    val fontes: FontesOficina by lazy { FontesOficina(this) }

    override fun onCreate() {
        super.onCreate()
        preferencias = Preferencias(this)
        historico = Historico(this)
        captura = Captura(this)

        val nm = getSystemService(NotificationManager::class.java)
        // Canal discreto: sem som, sem vibração, sem aparecer na tela bloqueada com detalhes.
        nm.createNotificationChannel(
            NotificationChannel(CANAL_PROTECAO, "Proteção", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Aviso fixo enquanto a gravação de segurança está ligada"
                setShowBadge(false)
            }
        )
    }

    companion object {
        const val CANAL_PROTECAO = "protecao"
    }
}

/** Estado que o leitor, a gravação e as telas compartilham enquanto o app está rodando. */
object Estado {
    /** O serviço de acessibilidade está ligado. */
    val leitorAtivo = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** A gravação de segurança está rodando. */
    val gravando = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** Último erro da gravação, para mostrar na tela. */
    val erroGravacao = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
}
