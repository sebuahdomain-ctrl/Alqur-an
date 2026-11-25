package com.sabilu.alquran

import android.content.Context
import android.net.Uri

/** Menyimpan dan memeriksa file PDF Quran yang dipilih user (lewat pemilih file Android). */
object PdfStore {
    private const val PREF = "alquran"
    private const val KEY_URI = "pdf_uri"

    fun save(ctx: Context, uri: Uri) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_URI, uri.toString()).apply()
    }

    fun get(ctx: Context): Uri? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_URI, null)?.let { Uri.parse(it) }

    /** True kalau file pernah dipilih, izinnya masih ada, dan filenya masih bisa dibuka. */
    fun isReadable(ctx: Context): Boolean {
        val uri = get(ctx) ?: return false
        return try {
            val granted = ctx.contentResolver.persistedUriPermissions
                .any { it.uri == uri && it.isReadPermission }
            granted && ctx.contentResolver.openFileDescriptor(uri, "r")?.use { true } == true
        } catch (e: Exception) {
            false
        }
    }
}
