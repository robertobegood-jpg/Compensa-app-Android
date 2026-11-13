package com.roberto.compensa

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roberto.compensa.dados.Tema
import com.roberto.compensa.ui.AppRoot
import com.roberto.compensa.ui.theme.CompensaTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** Aba pedida por fora (ex.: botão REC sem permissão abre direto em "Gravar"). */
    private val abaPedida = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        lerAba(intent)

        setContent {
            val ajustes by app.preferencias.ajustes.collectAsStateWithLifecycle()
            val pedida by abaPedida.collectAsStateWithLifecycle()
            val escuro = when (ajustes.tema) {
                Tema.ESCURO -> true
                Tema.CLARO -> false
                Tema.SISTEMA -> isSystemInDarkTheme()
            }
            DisposableEffect(escuro) {
                val estilo = if (escuro) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose { }
            }
            CompensaTheme(escuro = escuro, fontes = app.fontes) {
                AppRoot(app = app, abaPedida = pedida, onAbaConsumida = { abaPedida.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lerAba(intent)
    }

    private fun lerAba(intent: Intent?) {
        val aba = intent?.getStringExtra(EXTRA_ABA) ?: return
        abaPedida.value = aba
        intent.removeExtra(EXTRA_ABA)
    }

    companion object {
        const val EXTRA_ABA = "aba"
    }
}
