package com.roberto.compensa.ui.componentes

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import java.util.Locale

private val BR = Locale.forLanguageTag("pt-BR")

/** Número com vírgula, sem zeros sobrando: 2.0 → "2", 1.5 → "1,5", 6.3 → "6,30" com casas = 2. */
fun numeroBr(v: Double, casas: Int = 2): String {
    val s = String.format(BR, "%.${casas}f", v)
    return if (casas > 0 && s.substringAfter(',').all { it == '0' }) s.substringBefore(',') else s
}

/** Lê "1,5", "1.5" ou "1" (null se não for número). */
fun lerNumero(texto: String): Double? =
    texto.trim().replace(" ", "").replace(',', '.').toDoubleOrNull()

/**
 * Campo de número decimal. Mantém o texto que a pessoa está digitando ("1," por exemplo)
 * e só avisa [onValor] quando vira um número válido.
 */
@Composable
fun CampoNumero(
    valor: Double,
    onValor: (Double) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    casas: Int = 2,
    sufixo: String? = null,
    ajuda: String? = null
) {
    var texto by remember { mutableStateOf(numeroBr(valor, casas)) }
    // Valor mudou por fora (ex.: "restaurar padrão"): atualiza o texto.
    LaunchedEffect(valor) {
        if (lerNumero(texto) != valor) texto = numeroBr(valor, casas)
    }
    val invalido = lerNumero(texto) == null
    CampoTexto(
        valor = texto,
        onValor = { novo ->
            val limpo = novo.filter { it.isDigit() || it == ',' || it == '.' }.take(9)
            texto = limpo
            lerNumero(limpo)?.let { if (it != valor) onValor(it) }
        },
        rotulo = if (sufixo != null) "$rotulo ($sufixo)" else rotulo,
        modifier = modifier,
        erro = if (invalido) "Número inválido" else null,
        ajuda = ajuda,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}
