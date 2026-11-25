package com.sabilu.alquran

import android.content.Context

/** Menyimpan penanda: nomor halaman mushaf (1 sampai 604) yang ditandai user, di penyimpanan lokal aplikasi. */
object PenandaStore {
    private const val PREF = "alquran"
    private const val KEY = "penanda"
    private const val KEY_MUSHAF = "penanda_mushaf"

    /**
     * Versi lama menyimpan nomor halaman PDF (halaman mushaf + 3). Sekali saja, penanda lama
     * dikonversi ke nomor halaman mushaf; yang jatuh di halaman sampul dibuang.
     */
    private fun migrasi(ctx: Context) {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_MUSHAF, false)) return
        val baru = sp.getString(KEY, "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .map { it - 3 }
            .filter { it in 1..Data.JUMLAH_HALAMAN }
            .distinct()
            .sorted()
        sp.edit()
            .putString(KEY, baru.joinToString(","))
            .putBoolean(KEY_MUSHAF, true)
            .apply()
    }

    /** Semua penanda, diurutkan menurut nomor halaman. */
    fun semua(ctx: Context): List<Int> {
        migrasi(ctx)
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .sorted()
    }

    fun ada(ctx: Context, halaman: Int): Boolean = halaman in semua(ctx)

    /** Tambah penanda kalau belum ada, hapus kalau sudah ada. Mengembalikan true kalau sekarang ditandai. */
    fun ubah(ctx: Context, halaman: Int): Boolean {
        val daftar = semua(ctx).toMutableList()
        val ditandai = if (halaman in daftar) {
            daftar.remove(halaman)
            false
        } else {
            daftar.add(halaman)
            true
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, daftar.sorted().joinToString(",")).apply()
        return ditandai
    }
}
