package com.roberto.compensa.gravacao

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.roberto.compensa.MainActivity

/**
 * Tela invisível que só existe por um instante, para iniciar a gravação a partir do botão flutuante.
 * O Android só libera câmera e microfone em segundo plano se o pedido sair de uma tela aberta.
 */
class AtivarGravacaoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val temCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (temCamera) {
            GravacaoService.iniciar(this)
        } else {
            // Sem permissão: abre o app na aba de gravação para pedir.
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_ABA, "gravar")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
