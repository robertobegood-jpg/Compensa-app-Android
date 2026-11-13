package com.roberto.compensa.dados

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modo captura: guarda o texto que o leitor viu na tela do Uber/99, com o resultado da leitura.
 * Serve para mandar ao desenvolvedor e ajustar a leitura com cards reais.
 * Não guarda nada além do texto da tela do app de corrida.
 */
class Captura(context: Context) {
    val arquivo = File(context.cacheDir, "exportacoes/captura-compensa.txt")
    private val formato = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("pt-BR"))

    fun registrar(app: String, textos: List<String>, resultado: String) = synchronized(this) {
        runCatching {
            arquivo.parentFile?.mkdirs()
            // Limite de ~2 MB: ao passar, recomeça do zero.
            if (arquivo.length() > 2_000_000) arquivo.delete()
            arquivo.appendText(buildString {
                append("=== ").append(formato.format(Date())).append(" · ").append(app).append('\n')
                textos.forEach { append("  » ").append(it.replace('\n', ' ')).append('\n') }
                append("  = ").append(resultado).append("\n\n")
            })
        }
    }

    fun tamanho(): Long = if (arquivo.exists()) arquivo.length() else 0L

    fun limpar() {
        runCatching { arquivo.delete() }
    }
}
