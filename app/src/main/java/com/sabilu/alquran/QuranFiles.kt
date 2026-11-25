package com.sabilu.alquran

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.DocumentsContract
import android.util.LruCache
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** Satu baris di halaman mushaf. [teks] sudah dibalik urutannya supaya tergambar benar dari kiri ke kanan. */
class Baris(val font: String, val teks: String, val jenis: String, val kunci: List<String?> = emptyList())

class Halaman(val nomor: Int, val baris: List<Baris>)

/**
 * Membaca data Quran (folder pages dan fonts dari paket quran-qcf4) langsung dari folder
 * yang dipilih user, misalnya di kartu SD. Isi folder didaftar sekali lalu diingat.
 */
object QuranFiles {

    private class Anak(val id: String, val nama: String, val folder: Boolean)

    private var siapUntuk: String? = null
    private var halamanUri: Map<String, Uri> = emptyMap()
    private var fontUri: Map<String, Uri> = emptyMap()
    private var versesUri: Uri? = null
    private val cacheFont = LruCache<String, Typeface>(8)

    /**
     * Cari folder pages dan fonts di folder pilihan user (atau satu tingkat di bawahnya).
     * Panggil dari thread latar. True kalau data lengkap dan siap dibaca.
     */
    @Synchronized
    fun siapkan(ctx: Context): Boolean {
        val tree = FolderStore.get(ctx) ?: return false
        if (siapUntuk == tree.toString()) return true
        try {
            val akar = anak(ctx, tree, DocumentsContract.getTreeDocumentId(tree))
            var pages = akar.firstOrNull { it.folder && it.nama.equals("pages", true) }
            var fonts = akar.firstOrNull { it.folder && it.nama.equals("fonts", true) }
            if (pages == null || fonts == null) {
                for (sub in akar.filter { it.folder }) {
                    val isi = anak(ctx, tree, sub.id)
                    val p = isi.firstOrNull { it.folder && it.nama.equals("pages", true) }
                    val f = isi.firstOrNull { it.folder && it.nama.equals("fonts", true) }
                    if (p != null && f != null) {
                        pages = p
                        fonts = f
                        break
                    }
                }
            }
            if (pages == null || fonts == null) return false

            versesUri = cariVerses(ctx, tree, akar)
                ?: akar.filter { it.folder }.firstNotNullOfOrNull { sub -> cariVerses(ctx, tree, anak(ctx, tree, sub.id)) }
            halamanUri = anak(ctx, tree, pages.id).filter { !it.folder }
                .associate { it.nama to DocumentsContract.buildDocumentUriUsingTree(tree, it.id) }
            fontUri = anak(ctx, tree, fonts.id).filter { !it.folder }
                .associate { it.nama to DocumentsContract.buildDocumentUriUsingTree(tree, it.id) }

            val lengkap = halamanUri.containsKey("001.json") &&
                halamanUri.containsKey("604.json") &&
                fontUri.containsKey("QCF4_Hafs_01_W.ttf")
            if (lengkap) siapUntuk = tree.toString()
            return lengkap
        } catch (e: Exception) {
            return false
        }
    }

    private fun anak(ctx: Context, tree: Uri, indukId: String): List<Anak> {
        val hasil = ArrayList<Anak>()
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, indukId)
        val kolom = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        ctx.contentResolver.query(uri, kolom, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                hasil.add(
                    Anak(
                        c.getString(0),
                        c.getString(1) ?: "",
                        c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                    )
                )
            }
        }
        return hasil
    }

    private fun cariVerses(ctx: Context, tree: Uri, daftar: List<Anak>): Uri? =
        daftar.firstOrNull { !it.folder && it.nama == "verses.json" }
            ?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it.id) }

    /** Isi verses.json (indeks ayat ke halaman). Null kalau file tidak ada di folder. */
    fun bacaVerses(ctx: Context): String? {
        val uri = versesUri ?: return null
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
        } catch (e: Exception) {
            null
        }
    }

    /** Baca satu halaman mushaf (1 sampai 604). Panggil dari thread latar. */
    fun muatHalaman(ctx: Context, nomor: Int): Halaman? {
        return try {
            val uri = halamanUri[String.format(Locale.US, "%03d.json", nomor)] ?: return null
            val teks = ctx.contentResolver.openInputStream(uri)?.use {
                String(it.readBytes(), Charsets.UTF_8)
            } ?: return null
            val garis = JSONObject(teks).getJSONArray("lines")
            val hasil = ArrayList<Baris>()
            for (i in 0 until garis.length()) {
                val kata = garis.getJSONObject(i).getJSONArray("words")
                if (kata.length() == 0) continue
                val pertama = kata.getJSONObject(0)
                // Kata pertama ada di kanan. Huruf di font ini dianggap arah kiri-ke-kanan,
                // jadi urutan dibalik sebelum digambar.
                val sb = StringBuilder()
                val kunci = ArrayList<String?>()
                for (j in kata.length() - 1 downTo 0) {
                    val kw = kata.getJSONObject(j)
                    sb.append(kw.getString("char"))
                    kunci.add(kw.optString("verse_key", "").ifEmpty { null })
                }
                hasil.add(Baris(pertama.getString("font"), sb.toString(), pertama.getString("type"), kunci))
            }
            Halaman(nomor, hasil)
        } catch (e: Exception) {
            null
        }
    }

    /** Ambil font berdasarkan nama di data, misalnya QCF4_Hafs_01. Panggil dari thread latar. */
    @Synchronized
    fun typeface(ctx: Context, nama: String): Typeface? {
        cacheFont.get(nama)?.let { return it }
        val berkas = if (nama == "QCF4_QBSML") "QCF4_QBSML.ttf" else "${nama}_W.ttf"
        val uri = fontUri[berkas] ?: return null

        var tf: Typeface? = try {
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                Typeface.Builder(pfd.fileDescriptor).build()
            }
        } catch (e: Exception) {
            null
        }

        if (tf == null) {
            // Cadangan: salin dulu ke penyimpanan aplikasi, lalu muat dari sana.
            tf = try {
                val salinan = File(ctx.cacheDir, berkas)
                if (!salinan.exists() || salinan.length() == 0L) {
                    ctx.contentResolver.openInputStream(uri)?.use { masuk ->
                        salinan.outputStream().use { keluar -> masuk.copyTo(keluar) }
                    }
                }
                Typeface.createFromFile(salinan)
            } catch (e: Exception) {
                null
            }
        }

        if (tf != null) cacheFont.put(nama, tf)
        return tf
    }
}
