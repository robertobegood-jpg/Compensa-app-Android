package com.roberto.compensa.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.roberto.compensa.nucleo.Cor
import com.roberto.compensa.ui.theme.Oficina
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BR = Locale.forLanguageTag("pt-BR")

fun reais(v: Double): String = "R$ " + String.format(BR, "%.2f", v)

/** Valor em centavos (usado no cafezinho). */
fun formatarDinheiro(centavos: Long): String = reais(centavos / 100.0)

fun decimal(v: Double, casas: Int = 1): String = String.format(BR, "%.${casas}f", v)

fun porHora(v: Double): String = if (v.isNaN()) "—" else reais(v) + "/h"

fun hora(ms: Long): String = SimpleDateFormat("HH:mm", BR).format(Date(ms))

fun dataHora(ms: Long): String = SimpleDateFormat("dd/MM HH:mm", BR).format(Date(ms))

fun tamanhoArquivo(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> decimal(bytes / 1e9) + " GB"
    bytes >= 1_000_000 -> decimal(bytes / 1e6) + " MB"
    bytes >= 1_000 -> decimal(bytes / 1e3, 0) + " KB"
    else -> "$bytes B"
}

fun duracao(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) String.format(BR, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
    else String.format(BR, "%d:%02d", s / 60, s % 60)
}

/** Cor de fundo do semáforo. */
@Composable
fun corSinal(cor: Cor): Color = when (cor) {
    Cor.VERDE -> Oficina.cores.verde
    Cor.AMARELO -> Oficina.cores.ambar
    Cor.VERMELHO -> Oficina.cores.vermelhao
}

/** Cor de texto do semáforo (legível no fundo do app). */
@Composable
fun corSinalTexto(cor: Cor): Color = when (cor) {
    Cor.VERDE -> Oficina.cores.verdeInk
    Cor.AMARELO -> Oficina.cores.ambarInk
    Cor.VERMELHO -> Oficina.cores.vermelhaoInk
}
