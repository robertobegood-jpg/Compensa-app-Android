package com.roberto.compensa.gravacao

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** Um vídeo da gravação de segurança. */
data class Video(val uri: Uri, val nome: String, val quando: Long, val bytes: Long, val duracaoMs: Long)

/** Lista, abre, compartilha e apaga os vídeos gravados pelo app. */
object Gravacoes {
    const val PASTA_RELATIVA = "Movies/Compensa"

    /** Android 8 e 9: os vídeos ficam na pasta do próprio app. */
    fun pastaAntiga(context: Context): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, "Compensa").apply { mkdirs() }

    fun listar(context: Context): List<Video> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listarMediaStore(context) else listarArquivos(context)
    }.getOrDefault(emptyList())

    private fun listarMediaStore(context: Context): List<Video> {
        val colecao = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val colunas = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION
        )
        val lista = mutableListOf<Video>()
        context.contentResolver.query(
            colecao, colunas,
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?", arrayOf("$PASTA_RELATIVA%"),
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val iNome = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val iData = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val iTam = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                lista += Video(
                    uri = ContentUris.withAppendedId(colecao, c.getLong(iId)),
                    nome = c.getString(iNome) ?: "vídeo",
                    quando = c.getLong(iData) * 1000,
                    bytes = c.getLong(iTam),
                    duracaoMs = c.getLong(iDur)
                )
            }
        }
        return lista
    }

    private fun listarArquivos(context: Context): List<Video> =
        (pastaAntiga(context).listFiles() ?: emptyArray())
            .filter { it.isFile && it.extension == "mp4" }
            .sortedByDescending { it.lastModified() }
            .map {
                Video(
                    uri = FileProvider.getUriForFile(context, "${context.packageName}.arquivos", it),
                    nome = it.name,
                    quando = it.lastModified(),
                    bytes = it.length(),
                    duracaoMs = 0
                )
            }

    fun abrir(context: Context, v: Video) {
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(v.uri, "video/mp4")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
    }

    fun compartilhar(context: Context, v: Video) {
        val envio = Intent(Intent.ACTION_SEND).setType("video/mp4")
            .putExtra(Intent.EXTRA_STREAM, v.uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching {
            context.startActivity(
                Intent.createChooser(envio, "Enviar vídeo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** true se apagou. */
    fun apagar(context: Context, v: Video): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.delete(v.uri, null, null) > 0
        } else {
            File(pastaAntiga(context), v.nome).delete()
        }
    }.getOrDefault(false)
}
