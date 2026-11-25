package com.sabilu.alquran

import android.content.Context

/** Menyimpan nomor halaman mushaf (1 sampai 604) yang terakhir dibaca. 0 kalau belum ada. */
object TerakhirStore {
    private const val PREF = "alquran"
    private const val KEY = "halaman_terakhir"

    fun ambil(ctx: Context): Int =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY, 0)

    fun simpan(ctx: Context, halaman: Int) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY, halaman).apply()
    }
}
