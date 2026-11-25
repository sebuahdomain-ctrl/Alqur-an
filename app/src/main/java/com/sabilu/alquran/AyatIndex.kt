package com.sabilu.alquran

import android.content.Context
import org.json.JSONObject

/** Indeks ayat ke halaman dari verses.json (kunci "surat:ayat"). Dibaca sekali lalu diingat. */
object AyatIndex {

    private var peta: JSONObject? = null

    @Synchronized
    private fun muat(ctx: Context): JSONObject? {
        peta?.let { return it }
        val teks = QuranFiles.bacaVerses(ctx) ?: return null
        return try {
            JSONObject(teks).also { peta = it }
        } catch (e: Exception) {
            null
        }
    }

    fun kunci(surat: Int, ayat: Int): String = "$surat:$ayat"

    /** Nomor halaman tempat ayat berada, atau null kalau data tidak ada. Panggil dari thread latar. */
    fun halamanAyat(ctx: Context, surat: Int, ayat: Int): Int? {
        val o = muat(ctx) ?: return null
        val e = o.optJSONObject(kunci(surat, ayat)) ?: return null
        val h = e.optInt("page", 0)
        return if (h in 1..Data.JUMLAH_HALAMAN) h else null
    }
}
