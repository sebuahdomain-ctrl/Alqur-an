package com.sabilu.alquran

import android.content.Context
import androidx.documentfile.provider.DocumentFile

/** Satu baris daftar. [uri] = alamat PDF di kartu SD; null kalau tidak punya PDF (mis. penanda contoh). */
data class Entry(val title: String, val sub: String? = null, val uri: String? = null)

/** Hasil baca folder induk. [juzGalat] / [suratGalat] berisi pesan kalau folder tidak ketemu atau kosong. */
data class Scan(
    val juz: List<Entry>,
    val surat: List<Entry>,
    val juzGalat: String?,
    val suratGalat: String?,
)

object Data {

    /**
     * Baca folder induk: subfolder yang namanya mengandung "juz" = Per-Juz,
     * subfolder lainnya = Per-Surat. PDF diurutkan menurut nama (angka diurutkan sebagai angka).
     * Panggil dari thread latar, bukan thread utama.
     */
    fun scan(ctx: Context): Scan {
        val uri = FolderStore.get(ctx)
        val root = uri?.let { runCatching { DocumentFile.fromTreeUri(ctx, it) }.getOrNull() }
        val dirs = try {
            root?.listFiles()?.filter { it.isDirectory }.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }

        val juzDir = dirs.firstOrNull { it.name?.contains("juz", ignoreCase = true) == true }
        val suratDir = dirs.firstOrNull { it != juzDir }

        val juz = juzDir?.let { baca(it, surat = false) }.orEmpty()
        val surat = suratDir?.let { baca(it, surat = true) }.orEmpty()

        return Scan(
            juz = juz,
            surat = surat,
            juzGalat = when {
                juzDir == null -> "Folder Juz tidak ditemukan"
                juz.isEmpty() -> "Tidak ada PDF di folder Juz"
                else -> null
            },
            suratGalat = when {
                suratDir == null -> "Folder Surat tidak ditemukan"
                surat.isEmpty() -> "Tidak ada PDF di folder Surat"
                else -> null
            },
        )
    }

    private fun baca(dir: DocumentFile, surat: Boolean): List<Entry> =
        try {
            dir.listFiles()
                .filter { it.isFile && it.name?.endsWith(".pdf", ignoreCase = true) == true }
                .sortedWith { a, b -> alami(a.name.orEmpty(), b.name.orEmpty()) }
                .map { f -> entri(f.name.orEmpty().dropLast(4), f.uri.toString(), surat) }
        } catch (e: Exception) {
            emptyList()
        }

    // "001 Al-Fatihah" -> judul "Al-Fatihah", sub "Surat 1". Nama lain dipakai apa adanya.
    private fun entri(nama: String, uri: String, surat: Boolean): Entry {
        val m = if (surat) Regex("^(\\d+)[\\s._-]+(.+)$").find(nama) else null
        return if (m != null) {
            Entry(m.groupValues[2].trim(), "Surat ${m.groupValues[1].toInt()}", uri)
        } else {
            Entry(nama.replace('_', ' ').trim(), null, uri)
        }
    }

    /** Urut alami: "Juz 2" sebelum "Juz 10". */
    private fun alami(a: String, b: String): Int {
        val pa = Regex("\\d+|\\D+").findAll(a.lowercase()).map { it.value }.toList()
        val pb = Regex("\\d+|\\D+").findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(pa.size, pb.size)) {
            val x = pa[i]
            val y = pb[i]
            val c = if (x[0].isDigit() && y[0].isDigit()) {
                val tx = x.trimStart('0')
                val ty = y.trimStart('0')
                if (tx.length != ty.length) tx.length - ty.length else tx.compareTo(ty)
            } else {
                x.compareTo(y)
            }
            if (c != 0) return c
        }
        return pa.size - pb.size
    }

    // Penanda belum diminta, masih data contoh.
    fun penanda(): List<Entry> = listOf(
        Entry("Al-Baqarah", "Halaman 12"),
        Entry("Juz 3", "Halaman 5"),
        Entry("An-Nisa’", "Halaman 3"),
    )
}
