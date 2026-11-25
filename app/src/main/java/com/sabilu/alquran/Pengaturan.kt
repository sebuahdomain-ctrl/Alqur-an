package com.sabilu.alquran

import android.content.Context

/** Pengaturan yang disimpan di HP sampai diubah lagi. Default: mode terang. */
object Pengaturan {

    private const val BERKAS = "pengaturan"
    private const val MODE_MALAM = "mode_malam"

    fun modeMalam(ctx: Context): Boolean =
        ctx.getSharedPreferences(BERKAS, Context.MODE_PRIVATE).getBoolean(MODE_MALAM, false)

    fun simpanModeMalam(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(BERKAS, Context.MODE_PRIVATE).edit().putBoolean(MODE_MALAM, on).apply()
    }
}
