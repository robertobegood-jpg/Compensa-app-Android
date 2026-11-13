package com.roberto.compensa.leitor

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executor
import kotlin.math.roundToInt

/**
 * Plano B da leitura: tira um print da tela (recurso do serviço de acessibilidade, sem pedir
 * "gravar a tela") e lê o texto da imagem com o reconhecimento de texto do Google (ML Kit),
 * que roda dentro do celular, sem internet.
 *
 * Necessário porque o Uber marca o card como "dado sensível" e esconde o texto do leitor normal.
 */
class LeitorImagem(private val servico: AccessibilityService, private val executor: Executor) {

    private val reconhecedor: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    var ocupado = false
        private set

    val disponivel: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Lê o texto da parte de baixo da tela (onde o card de chamada aparece).
     * [resultado] recebe as linhas em ordem de leitura, ou lista vazia se não deu.
     */
    fun ler(resultado: (List<String>) -> Unit) {
        if (!disponivel || ocupado) return
        ocupado = true
        tirarPrint { bmp ->
            if (bmp == null) {
                ocupado = false
                resultado(emptyList())
                return@tirarPrint
            }
            runCatching {
                reconhecedor.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener(executor) { texto ->
                        val linhas = texto.textBlocks
                            .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
                            .flatMap { bloco -> bloco.lines.map { it.text } }
                        ocupado = false
                        resultado(linhas)
                    }
                    .addOnFailureListener(executor) {
                        ocupado = false
                        resultado(emptyList())
                    }
            }.onFailure {
                ocupado = false
                resultado(emptyList())
            }
        }
    }

    private fun tirarPrint(pronto: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            pronto(null)
            return
        }
        printApi30(pronto)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun printApi30(pronto: (Bitmap?) -> Unit) {
        try {
            servico.takeScreenshot(
                Display.DEFAULT_DISPLAY, executor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(r: AccessibilityService.ScreenshotResult) {
                        val buffer = r.hardwareBuffer
                        val recorte = runCatching {
                            val hw = Bitmap.wrapHardwareBuffer(buffer, r.colorSpace)
                            val soft = hw?.copy(Bitmap.Config.ARGB_8888, false)
                            hw?.recycle()
                            soft?.let { recortarParteDeBaixo(it) }
                        }.getOrNull()
                        buffer.close()
                        pronto(recorte)
                    }

                    override fun onFailure(errorCode: Int) {
                        pronto(null)
                    }
                }
            )
        } catch (e: Exception) {
            pronto(null)
        }
    }

    /** Fica só com os 65% de baixo da tela e reduz para no máximo 720 px de largura (mais rápido). */
    private fun recortarParteDeBaixo(b: Bitmap): Bitmap {
        val topo = (b.height * 0.35f).roundToInt()
        val cortado = Bitmap.createBitmap(b, 0, topo, b.width, b.height - topo)
        if (cortado !== b) b.recycle()
        if (cortado.width <= 720) return cortado
        val escala = 720f / cortado.width
        val menor = Bitmap.createScaledBitmap(cortado, 720, (cortado.height * escala).roundToInt(), true)
        if (menor !== cortado) cortado.recycle()
        return menor
    }

    fun fechar() {
        runCatching { reconhecedor.close() }
    }
}
