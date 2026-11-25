package com.sabilu.alquran

import android.content.Context

/** Menyimpan penanda: nomor halaman PDF (mulai dari 1) yang ditandai user, di penyimpanan lokal aplikasi. */
object PenandaStore {
    private const val PREF = "alquran"
    private const val KEY = "penanda"

    /** Semua penanda, diurutkan menurut nomor halaman. */
    fun semua(ctx: Context): List<Int> =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .sorted()

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
